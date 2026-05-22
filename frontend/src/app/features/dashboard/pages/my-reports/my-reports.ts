import { Component, OnInit, inject, signal } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { forkJoin, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { EmployeeResultService, EmployeeResultResponseDto } from '../../../../core/services/employee-result.service';
import { FormService, FormResponseDto } from '../../../../core/services/form.service';
import { EmployeeService } from '../../../../core/services/employee.service';
import { TherapistEvaluationService } from '../../../../core/services/therapist-evaluation.service';

interface Report {
  title: string;
  formId: string;
  date: string;
  score: number;
  risk: 'low' | 'moderate' | 'high';
}

interface PublishedAnalysis {
  formId: string;
  title: string;
  publishedAt: string;
  commentary: string;
  counselor: string;
}

@Component({
  selector: 'app-my-reports',
  imports: [MatCardModule, MatTableModule, MatButtonModule, MatIconModule],
  templateUrl: './my-reports.html',
  styleUrl: './my-reports.scss',
})
export class MyReports implements OnInit {
  private resultService = inject(EmployeeResultService);
  private formService = inject(FormService);
  private employeeService = inject(EmployeeService);
  private evaluationService = inject(TherapistEvaluationService);

  columns = ['title', 'date', 'score', 'risk', 'action'];
  items = signal<Report[]>([]);
  analyses = signal<PublishedAnalysis[]>([]);

  ngOnInit(): void {
    this.employeeService.getMe().subscribe({
      next: me => {
        forkJoin({
          results: this.resultService.getAll({ employeeId: me.id }),
          forms: this.formService.getAll(),
        }).subscribe({
          next: ({ results, forms }) => {
            const formMap = new Map<string, FormResponseDto>(forms.map(f => [f.id, f]));
            this.items.set(results.map((r: EmployeeResultResponseDto) => ({
              title: formMap.get(r.formId)?.title ?? r.formId,
              formId: r.formId,
              date: r.calculatedAt ? new Date(r.calculatedAt).toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' }) : '—',
              score: r.finalScore ?? r.score,
              risk: this.toRisk(r.riskLevel),
            })));

            this.loadPublishedAnalyses(forms, formMap);
          },
        });
      },
    });
  }

  private loadPublishedAnalyses(forms: FormResponseDto[], formMap: Map<string, FormResponseDto>): void {
    if (forms.length === 0) {
      this.analyses.set([]);
      return;
    }

    const lookups = forms.map(form =>
      this.evaluationService.employeeView(form.id).pipe(
        map(evaluation => ({
          formId: form.id,
          title: formMap.get(form.id)?.title ?? form.id,
          publishedAt: evaluation.publishedAt
            ? new Date(evaluation.publishedAt).toLocaleDateString('pt-BR', { day: '2-digit', month: 'short', year: 'numeric' })
            : '—',
          commentary: evaluation.closingCommentary ?? 'Sem comentário',
          counselor: evaluation.createdBy ?? 'Conselheiro',
        } as PublishedAnalysis)),
        catchError(() => of(null)),
      ));

    forkJoin(lookups).subscribe(results => {
      const published = results.filter((item): item is PublishedAnalysis => item !== null);
      this.analyses.set(published);
    });
  }

  private toRisk(level: 'LOW' | 'MEDIUM' | 'HIGH'): 'low' | 'moderate' | 'high' {
    return level === 'HIGH' ? 'high' : level === 'MEDIUM' ? 'moderate' : 'low';
  }

  riskLabel(r: string) {
    return ({ low: 'Baixo Risco', moderate: 'Moderado', high: 'Alto Risco' } as Record<string, string>)[r] ?? r;
  }

  riskWidth(score: number): string {
    return `${score}%`;
  }
}
