import { Component, OnInit, inject, signal, Input, Output, EventEmitter, Optional } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { FormsModule } from '@angular/forms';
import { MatTabsModule } from '@angular/material/tabs';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { FormSubmissionService } from '../../../../core/services/form-submission.service';
import { EmployeeResultService, EmployeeResultRequestDto } from '../../../../core/services/employee-result.service';

export interface MembersPanelDialogData {
  teamName: string;
  members: Member[];
  formId: string | null;
}

// Custom pipe to filter members by status
import { Pipe, PipeTransform } from '@angular/core';

interface Member {
  id: string;
  name: string;
}

type MemberStatus = 'pending' | 'responded' | 'evaluated';

interface MemberRow {
  id: string;
  name: string;
  status: MemberStatus;
  existingResult: { score: number; riskLevel: string } | null;
  generating: boolean;
  showForm: boolean;
  scoreInput: number;
}
@Pipe({
  name: 'filterByStatus',
  standalone: true,
})
export class FilterByStatusPipe implements PipeTransform {
  transform(rows: MemberRow[], status: MemberStatus): MemberRow[] {
    return rows.filter(r => r.status === status);
  }
}


@Component({
  selector: 'app-members-panel',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatFormFieldModule,
    MatInputModule,
    MatTabsModule,
    MatDialogModule,
    FormsModule,
    FilterByStatusPipe,
  ],
  template: `
    <div class="members-panel">
      <!-- Header -->
      <div class="panel-header">
        <h2>{{ teamName }}</h2>
        <button mat-icon-button (click)="onClose()" aria-label="Close panel">
          <mat-icon>close</mat-icon>
        </button>
      </div>

      <!-- Content -->
      <div class="panel-content">
        @if (loading()) {
          <div class="loading-wrap">
            <mat-spinner diameter="40"></mat-spinner>
          </div>
        } @else if (rows().length === 0) {
          <div class="empty">
            <mat-icon>people_outline</mat-icon>
            <p>Nenhum membro encontrado.</p>
          </div>
        } @else {
          <!-- Tabs: All / Pending / Responded / Evaluated -->
          <mat-tab-group animationDuration="200ms" class="members-tabs">
            <mat-tab label="Todos ({{ rows().length }})">
              <div class="member-list">
                @for (row of rows(); track row.id) {
                  <ng-container *ngTemplateOutlet="memberTemplate; context: { row }"></ng-container>
                }
              </div>
            </mat-tab>
            <mat-tab label="Pendentes ({{ pendingCount() }})">
              <div class="member-list">
                @for (row of rows() | filterByStatus: 'pending'; track row.id) {
                  <ng-container *ngTemplateOutlet="memberTemplate; context: { row }"></ng-container>
                }
              </div>
            </mat-tab>
            <mat-tab label="Respondidos ({{ respondedCount() }})">
              <div class="member-list">
                @for (row of rows() | filterByStatus: 'responded'; track row.id) {
                  <ng-container *ngTemplateOutlet="memberTemplate; context: { row }"></ng-container>
                }
              </div>
            </mat-tab>
            <mat-tab label="Avaliados ({{ evaluatedCount() }})">
              <div class="member-list">
                @for (row of rows() | filterByStatus: 'evaluated'; track row.id) {
                  <ng-container *ngTemplateOutlet="memberTemplate; context: { row }"></ng-container>
                }
              </div>
            </mat-tab>
          </mat-tab-group>

          <!-- Member Template -->
          <ng-template #memberTemplate let-row="row">
            <div class="member-card" [class]="row.status">
              <div class="member-header">
                <div class="avatar">{{ initials(row.name) }}</div>
                <div class="member-info">
                  <strong>{{ row.name }}</strong>
                  <span class="status-chip" [class]="row.status">
                    <mat-icon>{{ statusIcon(row.status) }}</mat-icon>
                    {{ statusLabel(row.status) }}
                  </span>
                </div>
              </div>

              @if (row.status === 'evaluated' && row.existingResult) {
                <div class="result-info">
                  <div><span class="label">Score:</span> <strong>{{ row.existingResult.score }}/100</strong></div>
                  <div><span class="label">Risco:</span> <strong>{{ riskLabel(row.existingResult.riskLevel) }}</strong></div>
                </div>
              }

              @if (row.status === 'responded' && !row.showForm) {
                <div class="member-actions">
                  <button mat-stroked-button (click)="viewResponses(row)">
                    <mat-icon>visibility</mat-icon> Ver Respostas
                  </button>
                  <button mat-flat-button color="primary" (click)="showGenerateForm(row)">
                    <mat-icon>assessment</mat-icon> Gerar Avaliação
                  </button>
                </div>
              }

              @if (row.showForm) {
                <div class="generate-form">
                  <mat-form-field appearance="outline">
                    <mat-label>Score (0–100)</mat-label>
                    <input matInput type="number" min="0" max="100" [(ngModel)]="row.scoreInput">
                  </mat-form-field>
                  <div class="risk-preview">Risco: <strong>{{ riskLabel(computeRisk(row.scoreInput)) }}</strong></div>
                  <button mat-flat-button color="primary"
                          [disabled]="row.generating || row.scoreInput < 0 || row.scoreInput > 100"
                          (click)="generateResult(row)">
                    {{ row.generating ? 'Gerando…' : 'Confirmar' }}
                  </button>
                  <button mat-button (click)="cancelForm(row)">Cancelar</button>
                </div>
              }
            </div>
          </ng-template>
        }
      </div>
    </div>
  `,
  styles: [`
    .members-panel {
      display: flex;
      flex-direction: column;
      height: 100%;
      background: #ffffff;
    }

    .panel-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 16px;
      border-bottom: 1px solid #e5e7eb;
      flex-shrink: 0;

      h2 {
        margin: 0;
        font-size: 1.1rem;
        font-weight: 600;
        color: #111827;
      }
    }

    .panel-content {
      flex: 1;
      overflow-y: auto;
      padding: 16px;
    }

    .loading-wrap {
      display: flex;
      justify-content: center;
      padding: 48px 16px;
    }

    .empty {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 12px;
      padding: 48px 16px;
      color: #9ca3af;
      text-align: center;

      mat-icon {
        font-size: 48px;
        width: 48px;
        height: 48px;
        color: #d1d5db;
      }

      p {
        margin: 0;
        font-size: 0.9rem;
      }
    }

    .members-tabs {
      margin: -16px -16px 0 -16px;
    }

    .member-list {
      display: flex;
      flex-direction: column;
      gap: 12px;
      padding: 16px;
    }

    .member-card {
      border: 1px solid #e5e7eb;
      border-radius: 8px;
      padding: 12px 14px;
      transition: all 0.2s ease;

      &.responded {
        border-color: #bfdbfe;
        background: #f0f7ff;
      }

      &.evaluated {
        border-color: #bbf7d0;
        background: #f0fdf4;
      }
    }

    .member-header {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 8px;
    }

    .avatar {
      width: 36px;
      height: 36px;
      border-radius: 50%;
      background: #e5e7eb;
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: 700;
      font-size: 0.8rem;
      color: #374151;
      flex-shrink: 0;
    }

    .member-info {
      flex: 1;
      display: flex;
      align-items: center;
      gap: 8px;

      strong {
        font-size: 0.9rem;
        color: #111827;
      }
    }

    .status-chip {
      display: inline-flex;
      align-items: center;
      gap: 3px;
      padding: 2px 8px;
      border-radius: 12px;
      font-size: 0.7rem;
      font-weight: 600;
      margin-left: auto;
      flex-shrink: 0;

      mat-icon {
        font-size: 12px;
        width: 12px;
        height: 12px;
      }

      &.pending {
        background: #f3f4f6;
        color: #6b7280;
      }

      &.responded {
        background: #dbeafe;
        color: #1d4ed8;
      }

      &.evaluated {
        background: #dcfce7;
        color: #15803d;
      }
    }

    .result-info {
      margin: 8px 0 0 44px;
      padding: 8px 0;
      display: flex;
      gap: 16px;
      font-size: 0.8rem;
      border-top: 1px solid #e5e7eb;

      div {
        display: flex;
        gap: 4px;
      }

      .label {
        color: #6b7280;
      }
    }

    .member-actions {
      display: flex;
      gap: 8px;
      margin: 8px 0 0 44px;
      padding-top: 8px;
      border-top: 1px solid #e5e7eb;

      button {
        height: 32px;
        font-size: 0.75rem;
        flex: 1;
      }
    }

    .generate-form {
      display: flex;
      flex-direction: column;
      gap: 8px;
      margin: 8px 0 0 44px;
      padding: 8px 0;
      border-top: 1px solid #e5e7eb;

      mat-form-field {
        width: 100%;
      }

      .risk-preview {
        font-size: 0.75rem;
        color: #6b7280;
        padding: 0 0 4px 0;

        strong {
          color: #111827;
        }
      }

      button {
        height: 32px;
        font-size: 0.75rem;
        width: 100%;
      }
    }

  `],
})
export class MembersPanelComponent implements OnInit {
  @Input() teamName: string = '';
  @Input() members: Member[] = [];
  @Input() formId: string | null = null;
  @Output() closePanel = new EventEmitter<void>();

  private submissionService = inject(FormSubmissionService);
  private resultService = inject(EmployeeResultService);
  private router = inject(Router);
  private dialogRef = inject<MatDialogRef<MembersPanelComponent>>(MatDialogRef, { optional: true });
  private dialogData = inject<MembersPanelDialogData>(MAT_DIALOG_DATA, { optional: true });

  loading = signal(true);
  rows = signal<MemberRow[]>([]);

  pendingCount = signal(0);
  respondedCount = signal(0);
  evaluatedCount = signal(0);

  ngOnInit(): void {
    // If opened as a dialog, use dialog data (overrides @Input)
    if (this.dialogData) {
      this.teamName = this.dialogData.teamName;
      this.members = this.dialogData.members;
      this.formId = this.dialogData.formId;
    }
    if (!this.formId || this.members.length === 0) {
      this.rows.set(this.members.map(m => this.makeRow(m, false, null)));
      this.updateCounts();
      this.loading.set(false);
      return;
    }

    forkJoin({
      submissions: this.submissionService.getAll({ formId: this.formId }).pipe(catchError(() => of([]))),
      results: this.resultService.getAll({ formId: this.formId }).pipe(catchError(() => of([]))),
    }).subscribe(({ submissions, results }) => {
      const respondedIds = new Set((submissions as any[]).map(s => s.employeeId));
      const resultByEmp = new Map((results as any[]).map(r => [r.employeeId, r]));

      this.rows.set(
        this.members.map(m => {
          const hasResult = resultByEmp.has(m.id);
          const hasResponse = respondedIds.has(m.id);
          const existing = hasResult
            ? { score: resultByEmp.get(m.id).score, riskLevel: resultByEmp.get(m.id).riskLevel }
            : null;
          return this.makeRow(m, hasResponse, existing);
        })
      );
      this.updateCounts();
      this.loading.set(false);
    });
  }

  private updateCounts(): void {
    const allRows = this.rows();
    this.pendingCount.set(allRows.filter(r => r.status === 'pending').length);
    this.respondedCount.set(allRows.filter(r => r.status === 'responded').length);
    this.evaluatedCount.set(allRows.filter(r => r.status === 'evaluated').length);
  }

  private makeRow(m: Member, hasResponse: boolean, existingResult: { score: number; riskLevel: string } | null): MemberRow {
    const status: MemberStatus = existingResult ? 'evaluated' : hasResponse ? 'responded' : 'pending';
    return {
      id: m.id,
      name: m.name,
      status,
      existingResult,
      generating: false,
      showForm: false,
      scoreInput: 50,
    };
  }

  showGenerateForm(row: MemberRow): void {
    row.showForm = true;
    this.rows.set([...this.rows()]);
  }

  cancelForm(row: MemberRow): void {
    row.showForm = false;
    this.rows.set([...this.rows()]);
  }

  viewResponses(row: MemberRow): void {
    if (!this.formId) return;
    this.onClose();
    this.router.navigate(['/dashboard/employee-responses'], {
      state: { employeeId: row.id, formId: this.formId },
    });
  }

  onClose(): void {
    if (this.dialogRef) {
      this.dialogRef.close();
    } else {
      this.closePanel.emit();
    }
  }

  generateResult(row: MemberRow): void {
    if (!this.formId) return;
    row.generating = true;
    this.rows.set([...this.rows()]);

    const body: EmployeeResultRequestDto = {
      employeeId: row.id,
      formId: this.formId,
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
        this.updateCounts();
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
    return name
      .split(' ')
      .map(p => p[0])
      .slice(0, 2)
      .join('')
      .toUpperCase();
  }

  statusLabel(s: MemberStatus): string {
    return { pending: 'Pendente', responded: 'Respondeu', evaluated: 'Avaliado' }[s] || s;
  }

  statusIcon(s: MemberStatus): string {
    return { pending: 'schedule', responded: 'done', evaluated: 'verified' }[s] || 'help';
  }

  riskLabel(r: string): string {
    return { LOW: 'Baixo', MEDIUM: 'Médio', HIGH: 'Alto' }[r] || r;
  }
}

