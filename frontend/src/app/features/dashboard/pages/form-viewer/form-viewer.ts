import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { RouterLink, ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatRadioModule } from '@angular/material/radio';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatSliderModule } from '@angular/material/slider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { FormField, FIELD_TYPE_DEFS } from '../form-builder/form-builder';
import { FormService } from '../../../../core/services/form.service';
import { AuthService } from '../../../../core/services/auth.service';
import { FormRendererService } from '../../../../core/services/form-renderer.service';
import { EmployeeService } from '../../../../core/services/employee.service';
import { FormSubmissionService } from '../../../../core/services/form-submission.service';

@Component({
  selector: 'app-form-viewer',
  imports: [
    RouterLink,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatInputModule,
    MatFormFieldModule,
    MatRadioModule,
    MatCheckboxModule,
    MatSliderModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './form-viewer.html',
  styleUrl: './form-viewer.scss',
})
export class FormViewer implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private formService = inject(FormService);
  private renderer = inject(FormRendererService);
  private auth = inject(AuthService);
  private employeeService = inject(EmployeeService);
  private submissionService = inject(FormSubmissionService);

  formId = signal<string | null>(null);

  assessment = signal<{ id: string; title: string; description: string; status: string }>({
    id: '',
    title: 'Loading…',
    description: '',
    status: '',
  });

  fields = signal<FormField[]>([]);
  questionIds = signal<string[]>([]);

  /** Employee submission state */
  employeeId = signal<string | null>(null);
  alreadyAnswered = signal(false);
  answers = signal<Map<number, number>>(new Map());
  /** Text answers for TEXT / LONG_TEXT / DATE fields: fieldId -> string */
  textAnswers = signal<Map<number, string>>(new Map());
  submitting = signal(false);
  submitSuccess = signal(false);
  submitError = signal<string | null>(null);

  /** Stores where the user placed their dot on each MAP field: fieldId -> {x,y} in [0,1] */
  mapPositions = signal<Map<number, { x: number; y: number }>>(new Map());

  /** RANKING: fieldId -> option indices in ranked order (index 0 = rank 1) */
  rankOrders = signal<Map<number, number[]>>(new Map());

  /** MATRIX: fieldId -> (rowIndex -> selected colIndex) */
  matrixSelections = signal<Map<number, Map<number, number>>>(new Map());

  canPublish = computed(() => {
    const role = this.auth.backendRole();
    return (role === 'COUNSELOR' || role === 'ADMIN') && this.assessment().status !== 'ACTIVE';
  });

  canClose = computed(() => {
    const role = this.auth.backendRole();
    return (role === 'COUNSELOR' || role === 'ADMIN') && this.assessment().status === 'ACTIVE';
  });

  isActive = computed(() => this.assessment().status === 'ACTIVE');
  isEmployee = computed(() => this.auth.backendRole() === 'EMPLOYEE');

  fieldTypes = FIELD_TYPE_DEFS;

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) return;
    this.formId.set(id);

    this.renderer.load(id).subscribe({
      next: ({ form, fields, questionIds }) => {
        this.assessment.set({
          id: String(form.id),
          title: form.title,
          description: form.description ?? '',
          status: form.status ?? '',
        });
        this.fields.set(fields);
        this.questionIds.set(questionIds);
      },
    });

    if (this.auth.backendRole() === 'EMPLOYEE') {
      this.employeeService.getMe().pipe(catchError(() => of(null))).subscribe(employee => {
        if (!employee) return;
        this.employeeId.set(employee.id);
        // Check if employee already submitted this form
        this.submissionService.getAll({ employeeId: employee.id })
          .pipe(catchError(() => of([])))
          .subscribe(submissions => {
            if (submissions.some(s => s.formId === id)) {
              this.alreadyAnswered.set(true);
            }
          });
      });
    }
  }

  getAnswer(fieldId: number): number {
    return this.answers().get(fieldId) ?? 0;
  }

  setAnswer(fieldId: number, value: number): void {
    this.answers.update(m => { const next = new Map(m); next.set(fieldId, value); return next; });
  }

  getTextAnswer(fieldId: number): string {
    return this.textAnswers().get(fieldId) ?? '';
  }

  setTextAnswer(fieldId: number, value: string): void {
    this.textAnswers.update(m => { const next = new Map(m); next.set(fieldId, value); return next; });
  }

  // --- MULTIPLE_CHOICE helpers (bitmask) ---
  toggleCheckbox(fieldId: number, optIdx: number): void {
    const current = this.answers().get(fieldId) ?? 0;
    this.setAnswer(fieldId, current ^ (1 << optIdx));
  }

  isChecked(fieldId: number, optIdx: number): boolean {
    return ((this.answers().get(fieldId) ?? 0) >> optIdx & 1) === 1;
  }

  // --- RANKING helpers ---
  rankItem(fieldId: number, optIdx: number): void {
    const orders = new Map(this.rankOrders());
    const current = [...(orders.get(fieldId) ?? [])];
    const pos = current.indexOf(optIdx);
    if (pos >= 0) {
      current.splice(pos, 1);
    } else {
      current.push(optIdx);
    }
    orders.set(fieldId, current);
    this.rankOrders.set(orders);
    // Encode: each option index as one decimal digit, from rank-1 to rank-N
    let encoded = 0;
    for (let i = 0; i < current.length; i++) {
      encoded += current[i] * Math.pow(10, current.length - 1 - i);
    }
    this.setAnswer(fieldId, encoded);
  }

  getItemRank(fieldId: number, optIdx: number): number {
    const order = this.rankOrders().get(fieldId) ?? [];
    const pos = order.indexOf(optIdx);
    return pos >= 0 ? pos + 1 : 0;
  }

  isRanked(fieldId: number, optIdx: number): boolean {
    return this.getItemRank(fieldId, optIdx) > 0;
  }

  // --- MATRIX helpers ---
  setMatrixCell(fieldId: number, rowIdx: number, colIdx: number): void {
    const allSelections = new Map(this.matrixSelections());
    const rowMap = new Map(allSelections.get(fieldId) ?? new Map<number, number>());
    if (rowMap.get(rowIdx) === colIdx) {
      rowMap.delete(rowIdx);
    } else {
      rowMap.set(rowIdx, colIdx);
    }
    allSelections.set(fieldId, rowMap);
    this.matrixSelections.set(allSelections);
    // Encode: colIndex for row i stored at decimal position i (10^i)
    const field = this.fields().find(f => f.id === fieldId);
    if (!field) return;
    let encoded = 0;
    for (let r = 0; r < field.options.length; r++) {
      const col = rowMap.get(r) ?? 0;
      encoded += col * Math.pow(10, r);
    }
    this.setAnswer(fieldId, encoded);
  }

  getMatrixCell(fieldId: number, rowIdx: number): number {
    return this.matrixSelections().get(fieldId)?.get(rowIdx) ?? -1;
  }

  getMapPos(fieldId: number): { x: number; y: number } | null {
    return this.mapPositions().get(fieldId) ?? null;
  }

  onMapClick(field: FormField, event: MouseEvent): void {
    const target = event.currentTarget as HTMLElement;
    const rect = target.getBoundingClientRect();
    const x = (event.clientX - rect.left) / rect.width;
    const y = 1 - (event.clientY - rect.top) / rect.height;
    this.mapPositions.update(m => {
      const next = new Map(m);
      next.set(field.id, { x: Math.max(0, Math.min(1, x)), y: Math.max(0, Math.min(1, y)) });
      return next;
    });
    const ix = Math.round(Math.max(0, Math.min(1, x)) * 100);
    const iy = Math.round(Math.max(0, Math.min(1, y)) * 100);
    this.setAnswer(field.id, ix * 100 + iy);
  }

  scaleRange(field: FormField): number[] {
    const arr: number[] = [];
    for (let i = field.scaleMin; i <= field.scaleMax; i++) arr.push(i);
    return arr;
  }

  fieldLabel(type: string): string {
    return this.fieldTypes.find(t => t.type === type)?.label ?? type;
  }

  onSubmit(): void {
    const formId = this.formId();
    const empId = this.employeeId();
    if (!formId || !empId || this.submitting() || this.alreadyAnswered()) return;

    const fields = this.fields();
    const qIds = this.questionIds();
    const TEXT_TYPES = new Set(['TEXT', 'LONG_TEXT', 'DATE']);

    const answers = fields.map((field, i) => {
      const qId = qIds[i];
      if (TEXT_TYPES.has(field.type)) {
        return { questionId: qId, value: 0, textValue: this.textAnswers().get(field.id) ?? '' };
      }
      return { questionId: qId, value: this.answers().get(field.id) ?? 0 };
    });

    this.submitting.set(true);
    this.submitError.set(null);

    this.submissionService.create({ formId, employeeId: empId, answers }).subscribe({
      next: () => {
        this.submitting.set(false);
        this.submitSuccess.set(true);
        this.alreadyAnswered.set(true);
        setTimeout(() => {
          if (window.history.length > 1) {
            window.history.back();
          } else {
            this.router.navigate(['/dashboard/my-assessments']);
          }
        }, 700);
      },
      error: (err) => {
        const msg = err.status === 409
          ? 'Você já respondeu este formulário.'
          : err.status === 400
            ? 'Este formulário foi fechado e não aceita novas respostas.'
          : 'Erro ao enviar respostas. Tente novamente.';
        this.submitError.set(msg);
        this.submitting.set(false);
      },
    });
  }

  onClose(): void {
    const id = this.formId();
    if (!id || !this.canClose()) return;
    const confirmed = window.confirm('Fechar este formulário? Pessoas sem resposta ficarão com status "sem resposta".');
    if (!confirmed) return;

    this.formService.close(id).subscribe({
      next: (form) => {
        this.assessment.update(current => ({ ...current, status: form.status }));
      },
      error: () => {
        this.submitError.set('Não foi possível fechar o formulário.');
      },
    });
  }

  onPublish(): void {
    const id = this.formId();
    if (!id) return;
    this.router.navigate(['/dashboard/forms/publish', id]);
  }
}
