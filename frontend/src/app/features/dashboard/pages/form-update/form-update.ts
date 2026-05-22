import { Component, OnInit, signal, inject } from '@angular/core';
import { RouterLink, ActivatedRoute, Router } from '@angular/router';
import { ReactiveFormsModule, FormBuilder as NgFormBuilder, FormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { FormField, FieldType, FIELD_TYPE_DEFS } from '../form-builder/form-builder';
import { FormRendererService } from '../../../../core/services/form-renderer.service';
import { FormService } from '../../../../core/services/form.service';
import { FormSubmissionService } from '../../../../core/services/form-submission.service';
import { catchError } from 'rxjs/operators';
import { of } from 'rxjs';

@Component({
  selector: 'app-form-update',
  imports: [
    RouterLink,
    ReactiveFormsModule,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatInputModule,
    MatFormFieldModule,
    MatSlideToggleModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './form-update.html',
  styleUrl: './form-update.scss',
})
export class FormUpdate implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private fb = inject(NgFormBuilder);
  private renderer = inject(FormRendererService);
  private formService = inject(FormService);
  private submissionService = inject(FormSubmissionService);

  private formId: string | null = null;
  private originalQuestionIds: string[] = [];
  private nextLocalId = 1000;

  fieldTypes = FIELD_TYPE_DEFS;
  fields = signal<FormField[]>([]);
  loading = signal(true);
  saving = signal(false);
  saveError = signal<string | null>(null);
  lockedByResponses = signal(false);

  metaForm = this.fb.group({
    title: ['', Validators.required],
    description: [''],
  });

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) { this.router.navigate(['/dashboard/counselor-assessments']); return; }
    this.formId = id;

    this.renderer.load(id).subscribe({
      next: ({ form, fields, questionIds }) => {
        this.metaForm.patchValue({ title: form.title, description: form.description ?? '' });
        this.fields.set(fields);
        this.originalQuestionIds = questionIds;
        this.submissionService.getAll({ formId: id })
          .pipe(catchError(() => of([])))
          .subscribe(submissions => {
            const locked = submissions.length > 0;
            this.lockedByResponses.set(locked);
            if (locked) {
              this.metaForm.disable({ emitEvent: false });
            }
            this.loading.set(false);
          });
      },
      error: () => {
        this.loading.set(false);
        this.saveError.set('Falha ao carregar formulário.');
      },
    });
  }

  addField(type: FieldType): void {
    if (this.lockedByResponses()) return;
    const hasOptions = ['SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'RANKING'].includes(type);
    const isLikert   = type === 'LIKERT';
    const isMatrix   = type === 'MATRIX';
    this.fields.update(f => [...f, {
      id: this.nextLocalId++, type, label: 'New Question', required: false,
      options: hasOptions ? ['Option 1', 'Option 2'] : isLikert ? ['Strongly Disagree', 'Disagree', 'Neutral', 'Agree', 'Strongly Agree'] : isMatrix ? ['Row 1', 'Row 2'] : [],
      matrixColumns: isMatrix ? ['Column 1', 'Column 2'] : [],
      scaleMin: 1, scaleMax: 10, scaleMinLabel: 'Very Low', scaleMaxLabel: 'Extreme',
      sliderMin: 0, sliderMax: 100, sliderStep: 1,
      mapXLeftLabel: 'Left', mapXRightLabel: 'Right',
      mapYTopLabel: 'High',  mapYBottomLabel: 'Low',
      weight: 0,
      flagStress: false,
      flagSleep: false,
      flagOverload: false,
      flagFatigue: false,
      flagDisengagement: false,
      flagIsolation: false,
    }]);
  }

  removeField(id: number): void {
    if (this.lockedByResponses()) return;
    this.fields.update(f => f.filter(x => x.id !== id));
  }

  moveUp(index: number): void {
    if (this.lockedByResponses()) return;
    if (index === 0) return;
    this.fields.update(f => { const arr = [...f]; [arr[index - 1], arr[index]] = [arr[index], arr[index - 1]]; return arr; });
  }

  moveDown(index: number): void {
    if (this.lockedByResponses()) return;
    this.fields.update(f => { if (index >= f.length - 1) return f; const arr = [...f]; [arr[index], arr[index + 1]] = [arr[index + 1], arr[index]]; return arr; });
  }

  addOption(field: FormField): void { if (this.lockedByResponses()) return; field.options = [...field.options, `Option ${field.options.length + 1}`]; this.fields.update(f => [...f]); }
  removeOption(field: FormField, i: number): void { if (this.lockedByResponses()) return; field.options = field.options.filter((_, idx) => idx !== i); this.fields.update(f => [...f]); }

  addMatrixColumn(field: FormField): void {
    if (this.lockedByResponses()) return;
    field.matrixColumns = [...field.matrixColumns, `Column ${field.matrixColumns.length + 1}`];
    this.fields.update(f => [...f]);
  }

  removeMatrixColumn(field: FormField, i: number): void {
    if (this.lockedByResponses()) return;
    field.matrixColumns = field.matrixColumns.filter((_, idx) => idx !== i);
    this.fields.update(f => [...f]);
  }

  fieldIcon(type: string): string { return FIELD_TYPE_DEFS.find(t => t.type === type)?.icon ?? 'help_outline'; }
  fieldLabel(type: string): string { return FIELD_TYPE_DEFS.find(t => t.type === type)?.label ?? type; }

  scaleRange(field: FormField): number[] {
    const arr: number[] = [];
    for (let i = field.scaleMin; i <= field.scaleMax; i++) arr.push(i);
    return arr;
  }

  onUpdate(): void {
    if (!this.formId || this.metaForm.invalid || this.saving() || this.lockedByResponses()) return;
    this.saving.set(true);
    this.saveError.set(null);

    const { title, description } = this.metaForm.value;

    this.formService.update(this.formId, {
      title: title!,
      description: description ?? '',
      status: 'CREATED',
    }).subscribe({
      next: () => {
        this.renderer.saveQuestions(this.fields(), this.originalQuestionIds, this.formId!).subscribe({
          next: () => {
            this.saving.set(false);
            this.router.navigate(['/dashboard/forms/view', this.formId]);
          },
          error: () => {
            this.saveError.set('Metadados salvos, mas falha ao atualizar perguntas.');
            this.saving.set(false);
          },
        });
      },
      error: () => {
        this.saveError.set('Falha ao salvar formulário. Tente novamente.');
        this.saving.set(false);
      },
    });
  }
}
