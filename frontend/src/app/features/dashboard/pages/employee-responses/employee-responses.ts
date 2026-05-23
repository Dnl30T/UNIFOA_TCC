import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { Router } from '@angular/router';
import { SlicePipe, UpperCasePipe, LowerCasePipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatTooltipModule } from '@angular/material/tooltip';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { HttpClient, HttpParams } from '@angular/common/http';
import { FormService, FormResponseDto } from '../../../../core/services/form.service';
import { EmployeeService, EmployeeResponseDto } from '../../../../core/services/employee.service';
import { AuthService } from '../../../../core/services/auth.service';
import { questionToField } from '../../../../core/services/form-renderer.service';
import { FormField } from '../form-builder/form-builder';
import { environment } from '../../../../../environments/environment';
import { TherapistEvaluationForm } from './therapist-evaluation-form';

interface AnswerRow {
  index: number;
  questionText: string;
  field: FormField;
  typeLabel: string;
  value: number;
  labels: string[];
  isList: boolean;
  flags: string[];
}

@Component({
  selector: 'app-employee-responses',
  imports: [MatCardModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule,
            MatChipsModule, MatDividerModule, MatTooltipModule, TherapistEvaluationForm,
            SlicePipe, UpperCasePipe, LowerCasePipe],
  templateUrl: './employee-responses.html',
  styleUrl: './employee-responses.scss',
})
export class EmployeeResponses implements OnInit {
  private router = inject(Router);
  private formService = inject(FormService);
  private employeeService = inject(EmployeeService);
  private auth = inject(AuthService);
  private http = inject(HttpClient);

  loading = signal(true);
  employeeName = signal('');
  formTitle = signal('');
  submittedAt = signal('');
  totalScore = signal<number | null>(null);
  burnoutRisk = signal<'LOW' | 'MEDIUM' | 'HIGH' | null>(null);
  responseStatus = signal<'RESPONDED' | 'NO_RESPONSE' | null>(null);
  answers = signal<AnswerRow[]>([]);
  notFound = signal(false);

  /** IDs surfaced to the evaluation sub-component */
  currentFormId = signal('');
  currentEmployeeId = signal('');

  isCounselor = computed(() => {
    const r = this.auth.backendRole();
    return r === 'COUNSELOR' || r === 'ADMIN';
  });

  ngOnInit(): void {
    const state = history.state as { employeeId?: string; formId?: string };
    const employeeId = state?.employeeId ?? '';
    const formId = state?.formId ?? '';

    if (!employeeId || !formId) {
      this.notFound.set(true);
      this.loading.set(false);
      return;
    }

    this.currentFormId.set(formId);
    this.currentEmployeeId.set(employeeId);

    forkJoin({
      employee: this.employeeService.getById(employeeId).pipe(catchError(() => of(null))),
      form: this.formService.getById(formId).pipe(catchError(() => of(null))),
      submission: this.http.get<{ answers: Record<string, number>; textAnswers: Record<string, string>; submittedAt: string }>(
        `${environment.apiUrl}/form-responses/by-form-and-employee`,
        { params: new HttpParams().set('formId', formId).set('employeeId', employeeId) }
      ).pipe(catchError(() => of(null))),
      comprehensive: this.http.get<{
        score: number | null;
        burnoutRiskPreAnalysis: 'LOW' | 'MEDIUM' | 'HIGH' | null;
        responseStatus: 'RESPONDED' | 'NO_RESPONSE';
        submittedAt: string;
      }>(`${environment.apiUrl}/therapist-evaluations/${formId}/${employeeId}/comprehensive`)
        .pipe(catchError(() => of(null))),
    }).subscribe(({ employee, form, submission, comprehensive }) => {
      if (!submission) {
        this.notFound.set(true);
        this.loading.set(false);
        return;
      }

      this.employeeName.set((employee as EmployeeResponseDto | null)?.name ?? employeeId);
      this.formTitle.set((form as FormResponseDto | null)?.title ?? formId);
      const submittedAt = comprehensive?.submittedAt ?? submission.submittedAt;
      this.submittedAt.set(
        new Date(submittedAt).toLocaleString('pt-BR', {
          day: '2-digit', month: 'short', year: 'numeric',
          hour: '2-digit', minute: '2-digit',
        })
      );
      this.totalScore.set(comprehensive?.score ?? null);
      this.burnoutRisk.set(comprehensive?.burnoutRiskPreAnalysis ?? null);
      this.responseStatus.set(comprehensive?.responseStatus ?? 'RESPONDED');

      const TEXT_TYPES = new Set(['TEXT', 'LONG_TEXT', 'DATE']);

      // Sort questions by their defined order so display matches form order
      const questions = ((form as FormResponseDto | null)?.questions ?? [])
        .slice()
        .sort((a, b) => (a.order ?? 0) - (b.order ?? 0));

      const rows: AnswerRow[] = questions
        .filter(q =>
          submission.answers[q.id] !== undefined ||
          submission.textAnswers?.[q.id] !== undefined)
        .map((q, i) => {
          const field = questionToField(q, i);
          const isText = TEXT_TYPES.has(field.type);
          const value = isText ? 0 : (submission.answers[q.id] ?? 0);
          const labels = isText
            ? [submission.textAnswers?.[q.id] ?? '(sem resposta)']
            : this.decodeAnswer(value, field);
          const isList = ['MULTIPLE_CHOICE', 'RANKING', 'MATRIX'].includes(field.type);
          const typeLabel = this.typeLabel(field.type);
          const flags = this.fieldFlags(field);
          return { index: i + 1, questionText: q.text, field, typeLabel, value, labels, isList, flags };
        });

      this.answers.set(rows);
      this.loading.set(false);
    });
  }

  private typeLabel(type: string): string {
    const map: Record<string, string> = {
      BOOLEAN: 'Sim/Não',
      SCALE: 'Escala',
      LIKERT: 'Likert',
      SINGLE_CHOICE: 'Escolha única',
      MULTIPLE_CHOICE: 'Múltipla escolha',
      RANKING: 'Classificação',
      MATRIX: 'Matriz',
      TEXT: 'Texto',
      LONG_TEXT: 'Texto longo',
      DATE: 'Data',
    };
    return map[type] ?? type;
  }

  private fieldFlags(field: FormField): string[] {
    const FLAG_LABELS: [keyof FormField, string][] = [
      ['flagStress',        'Estresse'],
      ['flagSleep',         'Sono'],
      ['flagOverload',      'Sobrecarga'],
      ['flagFatigue',       'Fadiga'],
      ['flagDisengagement', 'Desengajamento'],
      ['flagIsolation',     'Isolamento'],
    ];
    return FLAG_LABELS.filter(([key]) => !!field[key]).map(([, label]) => label);
  }

  private decodeAnswer(value: number, field: FormField): string[] {
    switch (field.type) {
      case 'BOOLEAN':
        return [value === 1 ? 'Sim' : 'Não'];

      case 'NUMERIC':
      case 'SLIDER':
        return [`${value}`];

      case 'SCALE':
        return [`${value} / ${field.scaleMax}`];

      case 'LIKERT': {
        const label = field.options[value - 1];
        return [`${value}${label ? ` — ${label}` : ''}`];
      }

      case 'SINGLE_CHOICE': {
        return [field.options[value] ?? `Opção ${value}`];
      }

      case 'MULTIPLE_CHOICE': {
        const selected: string[] = [];
        for (let i = 0; i < field.options.length; i++) {
          if (((value >> i) & 1) === 1) selected.push(field.options[i]);
        }
        return selected.length ? selected : ['Nenhuma opção selecionada'];
      }

      case 'RANKING': {
        const n = field.options.length;
        if (!value && n > 0) return ['Sem resposta'];
        const ranked: string[] = [];
        let remaining = value;
        for (let i = n - 1; i >= 0; i--) {
          const power = Math.pow(10, i);
          const optIdx = Math.floor(remaining / power);
          remaining = remaining % power;
          const label = field.options[optIdx];
          ranked.push(`${n - i}. ${label ?? `Opção ${optIdx}`}`);
        }
        return ranked;
      }

      case 'MATRIX': {
        const results: string[] = [];
        for (let r = 0; r < field.options.length; r++) {
          const colIdx = Math.floor(value / Math.pow(10, r)) % 10;
          const rowLabel = field.options[r];
          const colLabel = field.matrixColumns[colIdx];
          results.push(`${rowLabel}: ${colLabel ?? `Coluna ${colIdx}`}`);
        }
        return results;
      }

      case 'MAP': {
        const ix = Math.floor(value / 100);
        const iy = value % 100;
        return [`X: ${ix}%   Y: ${iy}%`];
      }

      case 'TEXT':
      case 'DATE':
        // Handled via textAnswers above — this path should not be reached
        return ['—'];
      default:
        return ['—'];
    }
  }

  goBack(): void {
    if (this.isCounselor()) {
      this.router.navigate(['/dashboard/counselor-reports'], { queryParams: { tab: 1 } });
    } else {
      this.router.navigate(['/dashboard/my-reports']);
    }
  }
}
