import { Component, OnInit, inject, signal } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { forkJoin, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { EmployeeResultService } from '../../../../core/services/employee-result.service';
import { FormService, FormResponseDto } from '../../../../core/services/form.service';
import { EmployeeService } from '../../../../core/services/employee.service';
import { TherapistEvaluationService } from '../../../../core/services/therapist-evaluation.service';

interface Report {
  title: string;
  formId: string;
  date: string;
  score: number;
  risk: 'low' | 'moderate' | 'high';
  commentary: string;
  counselor: string;
  stressScore: number | null;
  sleepScore: number | null;
  overloadScore: number | null;
  fatigueScore: number | null;
  disengagementScore: number | null;
  isolationScore: number | null;
}

@Component({
  selector: 'app-my-reports',
  imports: [MatCardModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './my-reports.html',
  styleUrl: './my-reports.scss',
})
export class MyReports implements OnInit {
  private resultService = inject(EmployeeResultService);
  private formService = inject(FormService);
  private employeeService = inject(EmployeeService);
  private evaluationService = inject(TherapistEvaluationService);

  loading = signal(true);
  reports = signal<Report[]>([]);

  ngOnInit(): void {
    this.employeeService.getMe().subscribe({
      next: me => {
        forkJoin({
          results: this.resultService.getAll({ employeeId: me.id }).pipe(catchError(() => of([]))),
          forms: this.formService.getAll().pipe(catchError(() => of([]))),
        }).subscribe(({ results, forms }) => {
          const formMap = new Map<string, FormResponseDto>(
            (forms as FormResponseDto[]).map(f => [f.id, f])
          );

          if (forms.length === 0) {
            this.loading.set(false);
            return;
          }

          // Try to fetch published analysis for each form (only returns data when PUBLISHED)
          const lookups = (forms as FormResponseDto[]).map(form =>
            this.evaluationService.employeeView(form.id).pipe(
              map(evaluation => ({ form, evaluation })),
              catchError(() => of(null)),
            )
          );

          forkJoin(lookups).subscribe(evalResults => {
            const resultByForm = new Map(
              (results as any[]).map(r => [r.formId, r])
            );

            const published: Report[] = evalResults
              .filter((item): item is { form: FormResponseDto; evaluation: any } => item !== null)
              .map(({ form, evaluation }) => {
                const result = resultByForm.get(form.id);
                const score = result?.finalScore ?? result?.score ?? 0;
                const risk = this.toRisk(result?.riskLevel ?? 'LOW');
                return {
                  title: form.title,
                  formId: form.id,
                  date: evaluation.publishedAt
                    ? new Date(evaluation.publishedAt).toLocaleDateString('pt-BR', { day: '2-digit', month: 'short', year: 'numeric' })
                    : '—',
                  score,
                  risk,
                  commentary: evaluation.closingCommentary ?? '',
                  counselor: evaluation.createdBy ?? 'Conselheiro',
                  stressScore: evaluation.stressScore ?? null,
                  sleepScore: evaluation.sleepScore ?? null,
                  overloadScore: evaluation.overloadScore ?? null,
                  fatigueScore: evaluation.fatigueScore ?? null,
                  disengagementScore: evaluation.disengagementScore ?? null,
                  isolationScore: evaluation.isolationScore ?? null,
                };
              });

            this.reports.set(published);
            this.loading.set(false);
          });
        });
      },
      error: () => this.loading.set(false),
    });
  }

  private toRisk(level: string): 'low' | 'moderate' | 'high' {
    return level === 'HIGH' ? 'high' : level === 'MEDIUM' ? 'moderate' : 'low';
  }

  riskLabel(r: string): string {
    return ({ low: 'Baixo', moderate: 'Moderado', high: 'Alto' } as Record<string, string>)[r] ?? r;
  }

  riskWidth(score: number): string {
    return `${score}%`;
  }

  clinicalScores(r: Report): { label: string; value: number | null }[] {
    return [
      { label: 'Estresse',       value: r.stressScore },
      { label: 'Sono',           value: r.sleepScore },
      { label: 'Sobrecarga',     value: r.overloadScore },
      { label: 'Fadiga',         value: r.fatigueScore },
      { label: 'Desengajamento', value: r.disengagementScore },
      { label: 'Isolamento',     value: r.isolationScore },
    ].filter(s => s.value !== null);
  }
}


interface Report {
  title: string;
  formId: string;
  date: string;
  score: number;
  risk: 'low' | 'moderate' | 'high';
}



