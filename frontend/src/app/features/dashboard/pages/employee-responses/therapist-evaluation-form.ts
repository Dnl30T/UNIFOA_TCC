import { Component, OnInit, inject, signal, input } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatExpansionModule } from '@angular/material/expansion';
import { catchError, forkJoin, of } from 'rxjs';
import {
  TherapistEvaluationService,
  TherapistEvaluationDto,
  TherapistEvaluationRequestDto,
} from '../../../../core/services/therapist-evaluation.service';
import { EmployeeResultService } from '../../../../core/services/employee-result.service';

@Component({
  selector: 'app-therapist-evaluation-form',
  imports: [
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatInputModule,
    MatFormFieldModule,
    MatProgressSpinnerModule,
    MatExpansionModule,
  ],
  templateUrl: './therapist-evaluation-form.html',
  styleUrl: './therapist-evaluation-form.scss',
})
export class TherapistEvaluationForm implements OnInit {
  formId = input.required<string>();
  employeeId = input.required<string>();

  private svc = inject(TherapistEvaluationService);
  private resultService = inject(EmployeeResultService);

  loading = signal(true);
  saving = signal(false);
  successMessage = signal<string | null>(null);
  saveError = signal<string | null>(null);
  publishing = signal(false);

  // Single module kept: Fechamento e Risco
  closingCommentary = signal('');

  stressScore = signal<number | null>(null);
  sleepScore = signal<number | null>(null);
  overloadScore = signal<number | null>(null);
  fatigueScore = signal<number | null>(null);
  disengagementScore = signal<number | null>(null);
  isolationScore = signal<number | null>(null);

  finalScore = signal<number | null>(null);
  status = signal<'DRAFT' | 'PUBLISHED'>('DRAFT');

  // Metadata display
  createdBy = signal('');
  updatedAt = signal('');

  ngOnInit(): void {
    forkJoin({
      comprehensive: this.svc.getComprehensive(this.formId(), this.employeeId()).pipe(catchError(() => of(null))),
    }).subscribe(({ comprehensive }) => {
      const evaluation = comprehensive?.therapistEvaluation;
      if (evaluation) this.applyDto(evaluation);

      // Pre-fill risk scores from autoScores where the therapist hasn't saved a value yet
      const auto = comprehensive?.autoScores;
      if (auto) {
        if (this.stressScore()        === null && auto.stressScore        !== null) this.stressScore.set(auto.stressScore);
        if (this.sleepScore()         === null && auto.sleepScore         !== null) this.sleepScore.set(auto.sleepScore);
        if (this.overloadScore()      === null && auto.overloadScore      !== null) this.overloadScore.set(auto.overloadScore);
        if (this.fatigueScore()       === null && auto.fatigueScore       !== null) this.fatigueScore.set(auto.fatigueScore);
        if (this.disengagementScore() === null && auto.disengagementScore !== null) this.disengagementScore.set(auto.disengagementScore);
        if (this.isolationScore()     === null && auto.isolationScore     !== null) this.isolationScore.set(auto.isolationScore);
      }

      // Pre-fill final score: prefer saved manual value, then auto overall
      if (comprehensive?.finalScore != null) {
        this.finalScore.set(comprehensive.finalScore);
      } else if (auto?.overallScore != null && this.finalScore() === null) {
        this.finalScore.set(auto.overallScore);
      }

      this.loading.set(false);
    });
  }

  private applyDto(d: TherapistEvaluationDto): void {
    this.closingCommentary.set(d.closingCommentary ?? '');
    this.stressScore.set(d.stressScore ?? null);
    this.sleepScore.set(d.sleepScore ?? null);
    this.overloadScore.set(d.overloadScore ?? null);
    this.fatigueScore.set(d.fatigueScore ?? null);
    this.disengagementScore.set(d.disengagementScore ?? null);
    this.isolationScore.set(d.isolationScore ?? null);
    this.status.set((d.status ?? 'DRAFT') as 'DRAFT' | 'PUBLISHED');
    this.createdBy.set(d.createdBy ?? '');
    this.updatedAt.set(d.updatedAt
      ? new Date(d.updatedAt).toLocaleString('pt-BR', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' })
      : '');
  }

  save(): void {
    this.saving.set(true);
    this.successMessage.set(null);
    this.saveError.set(null);

    const body: TherapistEvaluationRequestDto = {
      closingCommentary: this.closingCommentary() || undefined,
      stressScore: this.stressScore() ?? undefined,
      sleepScore: this.sleepScore() ?? undefined,
      overloadScore: this.overloadScore() ?? undefined,
      fatigueScore: this.fatigueScore() ?? undefined,
      disengagementScore: this.disengagementScore() ?? undefined,
      isolationScore: this.isolationScore() ?? undefined,
      status: 'DRAFT',
    };

    const onSaved = () => {
      if (this.finalScore() !== null) {
        this.resultService.finalize({
          employeeId: this.employeeId(),
          formId: this.formId(),
          finalScore: this.finalScore()!,
        }).subscribe({
          next: () => {
            this.saving.set(false);
            this.successMessage.set(this.currentAnalysisStatus() === 'Done'
              ? 'Análise salva como Done (completa e pronta para envio).'
              : 'Análise salva como Draft.');
          },
          error: () => {
            this.saveError.set('Avaliação salva, mas não foi possível salvar pontuação final.');
            this.saving.set(false);
          },
        });
        return;
      }
      this.saving.set(false);
      this.successMessage.set(this.currentAnalysisStatus() === 'Done'
        ? 'Análise salva como Done (completa e pronta para envio).'
        : 'Análise salva como Draft.');
    };

    this.svc.upsert(this.formId(), this.employeeId(), body).subscribe({
      next: data => {
        this.applyDto(data);
        onSaved();
      },
      error: () => {
        this.saveError.set('Erro ao salvar avaliação. Tente novamente.');
        this.saving.set(false);
      },
    });
  }

  publish(): void {
    if (!this.closingCommentary().trim()) {
      this.saveError.set('Comentário de fechamento é obrigatório para publicar a análise.');
      return;
    }
    this.publishing.set(true);
    this.saveError.set(null);

    const body: TherapistEvaluationRequestDto = {
      closingCommentary: this.closingCommentary() || undefined,
      stressScore: this.stressScore() ?? undefined,
      sleepScore: this.sleepScore() ?? undefined,
      overloadScore: this.overloadScore() ?? undefined,
      fatigueScore: this.fatigueScore() ?? undefined,
      disengagementScore: this.disengagementScore() ?? undefined,
      isolationScore: this.isolationScore() ?? undefined,
      status: 'DRAFT',
    };

    this.svc.upsert(this.formId(), this.employeeId(), body).subscribe({
      next: () => {
        const publishNow = () => {
          this.svc.publish(this.formId(), this.employeeId()).subscribe({
            next: data => {
              this.applyDto(data);
              this.publishing.set(false);
              this.successMessage.set('Análise enviada com sucesso. Status: Submitted.');
            },
            error: () => {
              this.saveError.set('Não foi possível publicar a análise. Verifique se a análise está completa.');
              this.publishing.set(false);
            },
          });
        };

        if (this.finalScore() !== null) {
          this.resultService.finalize({
            employeeId: this.employeeId(),
            formId: this.formId(),
            finalScore: this.finalScore()!,
          }).subscribe({
            next: () => publishNow(),
            error: () => {
              this.saveError.set('Não foi possível salvar pontuação final antes da publicação.');
              this.publishing.set(false);
            },
          });
          return;
        }

        publishNow();
      },
      error: () => {
        this.saveError.set('Não foi possível salvar rascunho antes da publicação.');
        this.publishing.set(false);
      },
    });
  }

  setFinalScore(value: number | null): void {
    if (value === null || Number.isNaN(value)) {
      this.finalScore.set(null);
      return;
    }
    this.finalScore.set(Math.max(0, Math.min(100, value)));
  }

  clampRiskScore(field: 'stress' | 'sleep' | 'overload' | 'fatigue' | 'disengagement' | 'isolation', value: number): void {
    const clamped = Math.max(0, Math.min(100, value));
    if (field === 'stress') this.stressScore.set(clamped);
    if (field === 'sleep') this.sleepScore.set(clamped);
    if (field === 'overload') this.overloadScore.set(clamped);
    if (field === 'fatigue') this.fatigueScore.set(clamped);
    if (field === 'disengagement') this.disengagementScore.set(clamped);
    if (field === 'isolation') this.isolationScore.set(clamped);
  }

  currentAnalysisStatus(): 'Draft' | 'Done' | 'Submitted' {
    if (this.status() === 'PUBLISHED') return 'Submitted';
    return this.isDraftComplete() ? 'Done' : 'Draft';
  }

  private isDraftComplete(): boolean {
    const hasClosing = !!this.closingCommentary().trim();
    const hasFinal = this.finalScore() !== null;
    const risks = [
      this.stressScore(),
      this.sleepScore(),
      this.overloadScore(),
      this.fatigueScore(),
      this.disengagementScore(),
      this.isolationScore(),
    ];
    const risksFilled = risks.every(score => score !== null);
    return hasClosing && hasFinal && risksFilled;
  }
}
