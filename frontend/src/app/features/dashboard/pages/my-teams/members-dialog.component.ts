import { Component, OnInit, inject, signal } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { FormsModule } from '@angular/forms';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { FormSubmissionService } from '../../../../core/services/form-submission.service';
import { EmployeeResultService, EmployeeResultRequestDto } from '../../../../core/services/employee-result.service';

interface Member { id: string; name: string; }
type MemberStatus = 'loading' | 'pending' | 'responded' | 'evaluated';

interface MemberRow {
  id: string;
  name: string;
  status: MemberStatus;
  existingResult: { score: number; riskLevel: string } | null;
  generating: boolean;
  showForm: boolean;
  scoreInput: number;
}

@Component({
  selector: 'app-members-dialog',
  standalone: true,
  imports: [
    MatDialogModule, MatButtonModule, MatIconModule,
    MatProgressSpinnerModule, MatFormFieldModule, MatInputModule, FormsModule,
  ],
  styles: [`
    .member-list { display: flex; flex-direction: column; gap: 12px; padding: 4px 0; }

    .member-card {
      border: 1px solid #e5e7eb;
      border-radius: 10px;
      padding: 14px 16px;

      &.responded { border-color: #bfdbfe; background: #f0f7ff; }
      &.evaluated { border-color: #bbf7d0; background: #f0fdf4; }
    }

    .member-header { display: flex; align-items: center; gap: 10px; }

    .avatar {
      width: 36px; height: 36px; border-radius: 50%;
      background: #e5e7eb; display: flex; align-items: center;
      justify-content: center; font-weight: 700; font-size: 0.8rem;
      color: #374151; flex-shrink: 0;
    }

    .member-name { flex: 1; font-weight: 600; font-size: 0.92rem; color: #111827; }

    .status-chip {
      display: inline-flex; align-items: center; gap: 4px;
      padding: 2px 8px; border-radius: 12px; font-size: 0.72rem; font-weight: 600;
      mat-icon { font-size: 13px; width: 13px; height: 13px; }
      &.pending  { background: #f3f4f6; color: #6b7280; }
      &.responded { background: #dbeafe; color: #1d4ed8; }
      &.evaluated { background: #dcfce7; color: #15803d; }
    }

    .result-info {
      margin-top: 10px; padding-top: 10px; border-top: 1px solid #e5e7eb;
      display: flex; gap: 16px; font-size: 0.82rem;
      .label { color: #6b7280; }
      .value { font-weight: 700; color: #111827; }
    }

    .generate-form {
      margin-top: 12px; padding-top: 12px; border-top: 1px solid #e5e7eb;
      display: flex; align-items: flex-end; gap: 10px;
    }

    .empty { color: #9ca3af; text-align: center; padding: 24px 0; }
    .loading-wrap { display: flex; justify-content: center; padding: 32px 0; }
  `],
  template: `
    <h2 mat-dialog-title>Membros — {{ data.teamName }}</h2>

    <mat-dialog-content>
      @if (loading()) {
        <div class="loading-wrap"><mat-spinner diameter="36"></mat-spinner></div>
      } @else if (rows().length === 0) {
        <p class="empty">Nenhum membro encontrado.</p>
      } @else {
        <div class="member-list">
          @for (row of rows(); track row.id) {
            <div class="member-card" [class]="row.status === 'evaluated' ? 'evaluated' : row.status === 'responded' ? 'responded' : ''">
              <div class="member-header">
                <div class="avatar">{{ initials(row.name) }}</div>
                <span class="member-name">{{ row.name }}</span>
                <span class="status-chip" [class]="row.status">
                  <mat-icon>{{ statusIcon(row.status) }}</mat-icon>
                  {{ statusLabel(row.status) }}
                </span>
              </div>

              @if (row.status === 'evaluated' && row.existingResult) {
                <div class="result-info">
                  <div><span class="label">Score: </span><span class="value">{{ row.existingResult.score }}/100</span></div>
                  <div><span class="label">Risco: </span><span class="value">{{ riskLabel(row.existingResult.riskLevel) }}</span></div>
                </div>
              }

              @if (row.status === 'responded' && !row.showForm) {
                <div style="margin-top:10px;display:flex;gap:8px">
                  <button mat-stroked-button style="height:32px;font-size:0.8rem"
                          (click)="viewResponses(row)">
                    <mat-icon>visibility</mat-icon> Ver Respostas
                  </button>
                  <button mat-flat-button color="primary" style="height:32px;font-size:0.8rem"
                          (click)="showGenerateForm(row)">
                    <mat-icon>assessment</mat-icon> Gerar Avaliação
                  </button>
                </div>
              }

              @if (row.showForm) {
                <div class="generate-form">
                  <mat-form-field appearance="outline" style="width:110px">
                    <mat-label>Score (0–100)</mat-label>
                    <input matInput type="number" min="0" max="100" [(ngModel)]="row.scoreInput">
                  </mat-form-field>
                  <div style="font-size:0.8rem;color:#6b7280;margin-bottom:18px">
                    Risco: <strong>{{ riskLabel(computeRisk(row.scoreInput)) }}</strong>
                  </div>
                  <button mat-flat-button color="primary" style="height:40px;margin-bottom:16px"
                          [disabled]="row.generating || row.scoreInput < 0 || row.scoreInput > 100"
                          (click)="generateResult(row)">
                    {{ row.generating ? 'Gerando…' : 'Confirmar' }}
                  </button>
                  <button mat-button style="height:40px;margin-bottom:16px"
                          (click)="row.showForm = false">Cancelar</button>
                </div>
              }
            </div>
          }
        </div>
      }
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Fechar</button>
    </mat-dialog-actions>
  `,
})
export class MembersDialogComponent implements OnInit {
  data = inject(MAT_DIALOG_DATA) as { teamName: string; members: Member[]; formId: string | null };
  private dialogRef = inject(MatDialogRef<MembersDialogComponent>);
  private router = inject(Router);
  private submissionService = inject(FormSubmissionService);
  private resultService = inject(EmployeeResultService);

  loading = signal(true);
  rows = signal<MemberRow[]>([]);

  ngOnInit(): void {
    if (!this.data.formId || this.data.members.length === 0) {
      this.rows.set(this.data.members.map(m => this.makeRow(m, false, null)));
      this.loading.set(false);
      return;
    }

    forkJoin({
      submissions: this.submissionService.getAll({ formId: this.data.formId! }).pipe(catchError(() => of([]))),
      results: this.resultService.getAll({ formId: this.data.formId! }).pipe(catchError(() => of([]))),
    }).subscribe(({ submissions, results }) => {
      const respondedIds = new Set(submissions.map(s => s.employeeId));
      const resultByEmp = new Map(results.map((r: any) => [r.employeeId as string, r]));

      this.rows.set(this.data.members.map(m => {
        const hasResult = resultByEmp.has(m.id);
        const hasResponse = respondedIds.has(m.id);
        const existing = hasResult ? { score: resultByEmp.get(m.id).score, riskLevel: resultByEmp.get(m.id).riskLevel } : null;
        return this.makeRow(m, hasResponse, existing);
      }));
      this.loading.set(false);
    });
  }

  private makeRow(m: Member, hasResponse: boolean, existingResult: { score: number; riskLevel: string } | null): MemberRow {
    const status: MemberStatus = existingResult ? 'evaluated' : hasResponse ? 'responded' : 'pending';
    return { id: m.id, name: m.name, status, existingResult, generating: false, showForm: false, scoreInput: 50 };
  }

  showGenerateForm(row: MemberRow): void {
    row.showForm = true;
    this.rows.set([...this.rows()]);
  }

  viewResponses(row: MemberRow): void {
    if (!this.data.formId) return;
    this.dialogRef.close();
    this.router.navigate(['/dashboard/employee-responses'], {
      state: { employeeId: row.id, formId: this.data.formId },
    });
  }

  generateResult(row: MemberRow): void {
    if (!this.data.formId) return;
    row.generating = true;
    this.rows.set([...this.rows()]);

    const body: EmployeeResultRequestDto = {
      employeeId: row.id,
      formId: this.data.formId,
      score: row.scoreInput,
      riskLevel: this.computeRisk(row.scoreInput),
    };

    this.resultService.create(body).subscribe({
      next: (result) => {
        row.status = 'evaluated';
        row.existingResult = { score: result.score, riskLevel: result.riskLevel };
        row.showForm = false;
        row.generating = false;
        this.rows.set([...this.rows()]);
      },
      error: () => {
        row.generating = false;
        this.rows.set([...this.rows()]);
      },
    });
  }

  computeRisk(score: number): 'LOW' | 'MEDIUM' | 'HIGH' {
    return score >= 67 ? 'HIGH' : score >= 34 ? 'MEDIUM' : 'LOW';
  }

  initials(name: string): string {
    return name.split(' ').map(p => p[0]).slice(0, 2).join('').toUpperCase();
  }

  statusLabel(s: MemberStatus): string {
    return ({ loading: '…', pending: 'Pendente', responded: 'Respondeu', evaluated: 'Avaliado' } as Record<string, string>)[s] ?? s;
  }

  statusIcon(s: MemberStatus): string {
    return ({ loading: 'sync', pending: 'schedule', responded: 'done', evaluated: 'verified' } as Record<string, string>)[s] ?? 'help';
  }

  riskLabel(r: string): string {
    return ({ LOW: 'Baixo', MEDIUM: 'Médio', HIGH: 'Alto' } as Record<string, string>)[r] ?? r;
  }
}
