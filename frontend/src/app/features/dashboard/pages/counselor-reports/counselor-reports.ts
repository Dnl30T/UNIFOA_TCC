import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatTabsModule } from '@angular/material/tabs';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatSelectModule } from '@angular/material/select';
import { MatDialogModule } from '@angular/material/dialog';
import { MatChipsModule } from '@angular/material/chips';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { DatePipe, DecimalPipe, CommonModule } from '@angular/common';
import { forkJoin, of } from 'rxjs';
import { catchError, switchMap } from 'rxjs/operators';
import { ActionCardComponent, ActionCardButton } from '../../../../shared/components/action-card/action-card.component';
import { StepIndicatorComponent, Step } from '../../../../shared/components/step-indicator/step-indicator.component';
import { FilterControl } from '../../../../shared/components/filter-bar/filter-bar.component';
import { TeamService, TeamResponseDto } from '../../../../core/services/team.service';
import { FormService, FormResponseDto } from '../../../../core/services/form.service';
import { EmployeeService, EmployeeResponseDto } from '../../../../core/services/employee.service';
import { FormSubmissionService } from '../../../../core/services/form-submission.service';
import { EmployeeResultService, EmployeeResultRequestDto } from '../../../../core/services/employee-result.service';
import { TherapistEvaluationService, TherapistEvaluationDto } from '../../../../core/services/therapist-evaluation.service';
import { ReportService, ReportResponseDto } from '../../../../core/services/report.service';

type MemberStatus = 'pending' | 'responded' | 'evaluated' | 'no_response';
type AnalysisStatus = 'none' | 'draft' | 'done' | 'submitted';

interface MemberRow {
  id: string;
  name: string;
  responseCount: number;
  status: MemberStatus;
  analysisStatus: AnalysisStatus;
  analysisFeedback: string | null;
  existingScore: number | null;
  existingRisk: string | null;
  showForm: boolean;
  scoreInput: number;
  generating: boolean;
  publishing: boolean;
}

interface FormReadinessRow {
  formId: string;
  formTitle: string;
  formStatus: string;
  totalMembers: number;
  respondedCount: number;
  publishedCount: number;
  reports: ReportResponseDto[];
  canGenerate: boolean;
  generating: boolean;
  showNameInput: boolean;
  newReportName: string;
}

@Component({
  selector: 'app-counselor-reports',
  imports: [
    CommonModule,
    DecimalPipe,
    DatePipe,
    MatCardModule, MatTableModule, MatButtonModule, MatIconModule,
    MatTooltipModule, MatTabsModule, MatProgressSpinnerModule,
    MatFormFieldModule, MatInputModule, MatCheckboxModule, MatSelectModule,
    MatDialogModule, MatChipsModule, FormsModule, ReactiveFormsModule,
    ActionCardComponent, StepIndicatorComponent,
  ],
  templateUrl: './counselor-reports.html',
  styleUrl: './counselor-reports.scss',
})
export class CounselorReports implements OnInit {
  private teamService = inject(TeamService);
  private formService = inject(FormService);
  private employeeService = inject(EmployeeService);
  private submissionService = inject(FormSubmissionService);
  private resultService = inject(EmployeeResultService);
  private evaluationService = inject(TherapistEvaluationService);
  private reportService = inject(ReportService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  // ── Tab 1 — Relatórios de Time ────────────────────────────────────────
  selectedTabIndex = signal(0);
  reportsColumns = ['name', 'generatedAt', 'respondents', 'avgScore', 'generalRisk', 'actions'];
  formsReadiness = signal<FormReadinessRow[]>([]);
  allReports = signal<ReportResponseDto[]>([]);
  reportsLoading = signal(true);
  generateError = signal<string | null>(null);

  // ── Tab 2 — Avaliações Individuais ────────────────────────────────────
  membersLoading = signal(true);

  // All forms available for filter
  allForms = signal<FormResponseDto[]>([]);
  selectedFormId = signal<string | null>(null);

  // Members + evaluations for selected form
  members = signal<MemberRow[]>([]);
  selectedForBatch = signal<Set<string>>(new Set());
  batchPublishing = signal(false);
  batchMessage = signal<string | null>(null);

  // Team
  myTeam = signal<TeamResponseDto | null>(null);
  teamMembers = signal<EmployeeResponseDto[]>([]);

  // Report for selected form (if exists)
  formReport = signal<ReportResponseDto | null>(null);

  selectedFormTitle = computed(() =>
    this.allForms().find(f => f.id === this.selectedFormId())?.title ?? ''
  );

  // Computed: Filter controls for Tab 2
  filterControls = computed(() => {
    const statuses = [
      { label: 'Pendente', value: 'pending' },
      { label: 'Respondeu', value: 'responded' },
      { label: 'Avaliado', value: 'evaluated' },
      { label: 'Sem resposta', value: 'no_response' }
    ];
    return [
      {
        key: 'search',
        type: 'search' as const,
        label: 'Buscar membros',
        placeholder: 'Nome ou e-mail...',
      },
      {
        key: 'status',
        type: 'select' as const,
        label: 'Status',
        options: statuses,
      }
    ] as FilterControl[];
  });

  // Active filters
  activeFilters = signal<Record<string, any>>({});
  filteredMembers = computed(() => {
    const filters = this.activeFilters();
    const members = this.members();

    if (!filters['search'] && !filters['status']) return members;

    return members.filter(m => {
      if (filters['search'] && !m.name.toLowerCase().includes(String(filters['search']).toLowerCase())) {
        return false;
      }
      if (filters['status'] && m.status !== filters['status']) {
        return false;
      }
      return true;
    });
  });

  // Computed: Step indicators for each form (Tab 1)
  getStepsForForm(row: FormReadinessRow): Step[] {
    return [
      {
        id: 'responses',
        label: 'Respostas',
        status: row.respondedCount > 0 ? 'complete' : row.respondedCount > 0 && row.respondedCount < row.totalMembers ? 'active' : 'pending',
        description: `${row.respondedCount} de ${row.totalMembers} membros responderam`
      },
      {
        id: 'publish',
        label: 'Publicar Análises',
        status: row.publishedCount > 0 ? 'complete' : row.respondedCount > 0 ? 'active' : 'pending',
        description: `${row.publishedCount} análise(s) publicada(s)`
      },
      {
        id: 'report',
        label: 'Gerar Relatório',
        status: row.canGenerate ? 'active' : row.reports.length > 0 ? 'complete' : 'pending',
        description: row.reports.length > 0 ? `${row.reports.length} relatório(s) gerado(s)` : 'Pronto para gerar'
      }
    ];
  }

  // Computed: Action buttons for Tab 1 report generation
  getReportActions(row: FormReadinessRow): ActionCardButton[] {
    return [
      {
        id: 'generate',
        label: row.generating ? 'Gerando...' : 'Gerar Relatório',
        icon: row.generating ? 'hourglass_empty' : 'summarize',
        type: 'primary',
        disabled: !row.canGenerate || row.generating,
      }
    ];
  }

  getReportNameActions(row: FormReadinessRow): ActionCardButton[] {
    return [
      {
        id: 'confirm',
        label: 'Confirmar',
        icon: 'check',
        type: 'primary',
        disabled: !row.newReportName.trim(),
      },
      {
        id: 'cancel',
        label: 'Cancelar',
        type: 'secondary',
      },
    ];
  }

  // Computed: Action buttons for batch publish (Tab 2)
  getBatchActions(): ActionCardButton[] {
    return [
      {
        id: 'publishBatch',
        label: `Publicar Selecionados (${this.selectedBatchCount()})`,
        icon: 'publish',
        type: 'primary',
        disabled: this.selectedBatchCount() === 0 || this.batchPublishing(),
      }
    ];
  }

  ngOnInit(): void {
    const tab = this.route.snapshot.queryParamMap.get('tab');
    if (tab !== null) this.selectedTabIndex.set(Number(tab));

    this.teamService.getMyTeam().pipe(
      catchError(() => of(null)),
      switchMap(team => {
        this.myTeam.set(team);
        if (!team) return of({ employees: [], forms: [] });
        return forkJoin({
          employees: this.employeeService.getAll().pipe(catchError(() => of([]))),
          forms: this.formService.getAll().pipe(catchError(() => of([]))),
        });
      }),
    ).subscribe(({ employees, forms }) => {
      const team = this.myTeam();
      if (!team) {
        this.reportsLoading.set(false);
        this.membersLoading.set(false);
        return;
      }

      const members = (employees as EmployeeResponseDto[]).filter(e => e.teamId === team.id);
      this.teamMembers.set(members);

      const teamForms = (forms as FormResponseDto[]).filter(f => (f.teamIds ?? []).includes(team.id));
      this.allForms.set(teamForms);

      // Pre-select first form for individual evaluations
      if (teamForms.length > 0) {
        this.selectedFormId.set(teamForms[0].id);
      }

      this.loadReportsTab(team.id, members, teamForms);
      this.loadIndividualTab();
    });
  }

  // ── Tab 1: generate report for a form ────────────────────────────────
  onReportAction(actionId: string, row: FormReadinessRow): void {
    if (actionId === 'generate') {
      this.showReportNameInput(row);
    }
  }

  // ── Filter changes (Tab 2) ───────────────────────────────────────────
  onFiltersChanged(filters: Record<string, any>): void {
    this.activeFilters.set(filters);
  }

  // ── Batch action click ──────────────────────────────────────────────
  onBatchAction(actionId: string): void {
    if (actionId === 'publishBatch') {
      this.publishBatch();
    }
  }

  private loadReportsTab(teamId: string, members: EmployeeResponseDto[], forms: FormResponseDto[]): void {
    if (forms.length === 0) {
      this.reportsLoading.set(false);
      return;
    }

    forkJoin({
      reports: this.reportService.listByTeamId(teamId).pipe(catchError(() => of([]))),
      submissions: this.submissionService.getAll().pipe(catchError(() => of([]))),
    }).subscribe(({ reports, submissions }) => {
      const allReports = reports as ReportResponseDto[];
      this.allReports.set(allReports);

      const subsByForm = new Map<string, Set<string>>();
      (submissions as any[]).forEach(s => {
        if (s.responseStatus === 'RESPONDED') {
          if (!subsByForm.has(s.formId)) subsByForm.set(s.formId, new Set());
          subsByForm.get(s.formId)!.add(s.employeeId);
        }
      });

      if (forms.length === 0) {
        this.formsReadiness.set([]);
        this.reportsLoading.set(false);
        return;
      }

      // For each form, count published evaluations with commentary
      forkJoin(
        forms.map(f => this.evaluationService.getAllByForm(f.id).pipe(catchError(() => of([]))))
      ).subscribe(evalLists => {
        const rows: FormReadinessRow[] = forms.map((f, i) => {
          const evals = (evalLists[i] as TherapistEvaluationDto[]);
          const publishedWithCommentary = evals.filter(
            e => e.status === 'PUBLISHED' && !!e.closingCommentary?.trim()
          ).length;
          const responded = subsByForm.get(f.id)?.size ?? 0;
          const formReports = allReports.filter(r => r.formId === f.id);
          // Can generate when: has published analyses with commentary AND form is not just CREATED
          const canGenerate = publishedWithCommentary > 0 && f.status !== 'CREATED';

          return {
            formId: f.id,
            formTitle: f.title,
            formStatus: f.status,
            totalMembers: members.length,
            respondedCount: responded,
            publishedCount: publishedWithCommentary,
            reports: formReports,
            canGenerate,
            generating: false,
            showNameInput: false,
            newReportName: `Relatório ${f.title} – ${new Date().toLocaleDateString('pt-BR', { month: 'short', year: 'numeric' })}`,
          };
        });
        this.formsReadiness.set(rows);
        this.reportsLoading.set(false);
      });
    });
  }

  // ── Tab 1: generate report for a form ────────────────────────────────
  showReportNameInput(row: FormReadinessRow): void {
    row.showNameInput = true;
    this.formsReadiness.set([...this.formsReadiness()]);
    this.generateError.set(null);
  }

  cancelReportGeneration(row: FormReadinessRow): void {
    row.showNameInput = false;
    this.formsReadiness.set([...this.formsReadiness()]);
  }

  confirmGenerateReport(row: FormReadinessRow): void {
    const team = this.myTeam();
    if (!team || row.generating || !row.newReportName.trim()) return;

    row.generating = true;
    row.showNameInput = false;
    this.generateError.set(null);
    this.formsReadiness.set([...this.formsReadiness()]);

    this.reportService.generate({ formId: row.formId, teamId: team.id, name: row.newReportName.trim() }).subscribe({
      next: (report) => {
        row.generating = false;
        row.reports = [...row.reports, report];
        this.allReports.set([...this.allReports(), report]);
        this.formsReadiness.set([...this.formsReadiness()]);
      },
      error: (err) => {
        row.generating = false;
        this.generateError.set(err?.error?.message ?? 'Erro ao gerar relatório. Verifique se todas as análises foram publicadas.');
        this.formsReadiness.set([...this.formsReadiness()]);
      },
    });
  }

  // ── Tab 2: load individual evaluations for selected form ──────────────
  onFormFilterChange(formId: string | null): void {
    this.selectedFormId.set(formId);
    this.loadIndividualTab();
  }

  private loadIndividualTab(): void {
    const formId = this.selectedFormId();
    const members = this.teamMembers();
    this.membersLoading.set(true);
    this.batchMessage.set(null);

    if (!formId || members.length === 0) {
      this.members.set(members.map(e => this.makeRow(e, 0, false, null)));
      this.formReport.set(null);
      this.membersLoading.set(false);
      return;
    }

    const team = this.myTeam();

    forkJoin({
      submissions: this.submissionService.getAll({ formId }).pipe(catchError(() => of([]))),
      results: this.resultService.getAll({ formId }).pipe(catchError(() => of([]))),
      evaluations: forkJoin(
        members.map(m => this.evaluationService.get(formId, m.id).pipe(catchError(() => of(null))))
      ).pipe(catchError(() => of([]))),
      reports: team ? this.reportService.listByTeamId(team.id).pipe(catchError(() => of([]))) : of([]),
    }).subscribe(({ submissions, results, evaluations, reports }) => {
      // Check if a report exists for this form
      const report = (reports as ReportResponseDto[]).find(r => r.formId === formId) ?? null;
      this.formReport.set(report);

      const countByEmp = new Map<string, number>((submissions as any[]).map(s => [s.employeeId, s.answerCount]));
      const submissionByEmp = new Map<string, any>((submissions as any[]).map(s => [s.employeeId, s]));
      const resultByEmp = new Map<string, any>((results as any[]).map(r => [r.employeeId, r]));
      const evaluationByEmp = new Map<string, TherapistEvaluationDto>();
      (evaluations as Array<TherapistEvaluationDto | null>).forEach(item => {
        if (item?.employeeId) evaluationByEmp.set(item.employeeId, item);
      });

      // If report exists: only show respondents in the report
      const reportRespondentSet = report ? new Set(report.respondentIds) : null;
      const displayMembers = reportRespondentSet
        ? members.filter(m => reportRespondentSet.has(m.id))
        : members;

      const rows = displayMembers.map(e => {
        const submission = submissionByEmp.get(e.id) ?? null;
        const generatedFromSub = submission?.score != null
          ? { score: submission.score, riskLevel: submission.burnoutRiskPreAnalysis }
          : null;
        return this.makeRow(
          e,
          countByEmp.get(e.id) ?? 0,
          submission?.responseStatus === 'RESPONDED',
          resultByEmp.get(e.id) ?? generatedFromSub,
          submission?.responseStatus,
          evaluationByEmp.get(e.id) ?? null,
        );
      });

      rows.sort((a, b) => {
        const aSubmitted = a.analysisStatus === 'submitted' ? 1 : 0;
        const bSubmitted = b.analysisStatus === 'submitted' ? 1 : 0;
        if (aSubmitted !== bSubmitted) return aSubmitted - bSubmitted;
        const order: Record<MemberStatus, number> = { evaluated: 0, responded: 1, no_response: 2, pending: 3 };
        if (order[a.status] !== order[b.status]) return order[a.status] - order[b.status];
        return b.responseCount - a.responseCount;
      });

      this.members.set(rows);
      this.membersLoading.set(false);
    });
  }

  private makeRow(
    e: EmployeeResponseDto,
    count: number,
    hasResponse: boolean,
    existing: any | null,
    responseStatus?: string,
    evaluation?: TherapistEvaluationDto | null,
  ): MemberRow {
    const status: MemberStatus = existing || evaluation
      ? 'evaluated'
      : responseStatus === 'NO_RESPONSE'
        ? 'no_response'
        : hasResponse
          ? 'responded'
          : 'pending';
    return {
      id: e.id,
      name: e.name,
      responseCount: count,
      status,
      analysisStatus: this.computeAnalysisStatus(evaluation, existing),
      analysisFeedback: null,
      existingScore: existing?.score ?? existing?.finalScore ?? null,
      existingRisk: existing?.riskLevel ?? null,
      showForm: false,
      scoreInput: 50,
      generating: false,
      publishing: false,
    };
  }

  // ── Batch + publish actions ───────────────────────────────────────────
  toggleSelected(memberId: string, checked: boolean): void {
    const next = new Set(this.selectedForBatch());
    if (checked) next.add(memberId); else next.delete(memberId);
    this.selectedForBatch.set(next);
  }

  isSelected(memberId: string): boolean { return this.selectedForBatch().has(memberId); }
  canBatchPublish(row: MemberRow): boolean { return row.status === 'responded' || row.status === 'evaluated'; }
  selectedBatchCount(): number { return this.selectedForBatch().size; }

  publishRow(row: MemberRow): void {
    const formId = this.selectedFormId();
    if (!formId || row.publishing) return;
    row.publishing = true;
    row.analysisFeedback = null;
    this.members.set([...this.members()]);
    this.evaluationService.publish(formId, row.id).subscribe({
      next: () => {
        row.publishing = false;
        row.status = 'evaluated';
        row.analysisStatus = 'submitted';
        row.analysisFeedback = 'Análise publicada para o colaborador.';
        this.members.set([...this.members()]);
      },
      error: () => {
        row.publishing = false;
        row.analysisFeedback = 'Não foi possível publicar. Verifique se o rascunho está completo.';
        this.members.set([...this.members()]);
      },
    });
  }

  publishBatch(): void {
    const formId = this.selectedFormId();
    const ids = Array.from(this.selectedForBatch());
    if (!formId || ids.length === 0 || this.batchPublishing()) return;
    this.batchPublishing.set(true);
    this.batchMessage.set(null);
    this.evaluationService.publishBatch(formId, ids).subscribe({
      next: publishedItems => {
        const publishedIds = new Set(publishedItems.map(item => item.employeeId));
        this.members.update(rows => rows.map(row => {
          if (!ids.includes(row.id)) return row;
          if (!publishedIds.has(row.id)) return { ...row, analysisFeedback: 'Rascunho incompleto: não publicado.' };
          return { ...row, status: 'evaluated' as MemberStatus, analysisStatus: 'submitted' as AnalysisStatus, analysisFeedback: 'Publicado em lote.' };
        }));
        this.selectedForBatch.set(new Set());
        this.batchPublishing.set(false);
        this.batchMessage.set(`Publicadas ${publishedIds.size} de ${ids.length} análises.`);
      },
      error: () => {
        this.batchPublishing.set(false);
        this.batchMessage.set('Falha ao publicar em lote.');
      },
    });
  }

  showGenerateForm(row: MemberRow): void { row.showForm = true; this.members.set([...this.members()]); }
  cancelForm(row: MemberRow): void { row.showForm = false; this.members.set([...this.members()]); }

  generateResult(row: MemberRow): void {
    const formId = this.selectedFormId();
    if (!formId) return;
    row.generating = true;
    this.members.set([...this.members()]);
    const body: EmployeeResultRequestDto = {
      employeeId: row.id, formId,
      score: row.scoreInput,
      riskLevel: this.computeRisk(row.scoreInput),
    };
    this.resultService.create(body).subscribe({
      next: (result) => {
        row.status = 'evaluated';
        row.existingScore = result.score;
        row.existingRisk = result.riskLevel;
        row.showForm = false;
        row.generating = false;
        this.members.set([...this.members()]);
      },
      error: () => { row.generating = false; this.members.set([...this.members()]); },
    });
  }

  viewResponses(row: MemberRow): void {
    const formId = this.selectedFormId();
    if (!formId) return;
    this.router.navigate(['/dashboard/employee-responses'], { state: { employeeId: row.id, formId } });
  }

  // ── Helpers ───────────────────────────────────────────────────────────
  computeRisk(score: number): 'LOW' | 'MEDIUM' | 'HIGH' {
    return score >= 67 ? 'HIGH' : score >= 34 ? 'MEDIUM' : 'LOW';
  }

  initials(name: string): string {
    return name.split(' ').map(p => p[0]).slice(0, 2).join('').toUpperCase();
  }

  statusLabel(s: MemberStatus): string {
    return ({ pending: 'Pendente', responded: 'Respondeu', evaluated: 'Avaliado', no_response: 'Sem resposta' })[s];
  }

  analysisStatusLabel(s: AnalysisStatus): string {
    return ({ none: 'Sem análise', draft: 'Rascunho', done: 'Concluído', submitted: 'Enviado' })[s];
  }

  analysisStatusIcon(s: AnalysisStatus): string {
    return ({ none: '', draft: 'edit_note', done: 'task_alt', submitted: 'check_circle' })[s];
  }

  viewReportDashboard(r: ReportResponseDto): void {
    const teamId = this.myTeam()?.id;
    if (!teamId) return;
    this.router.navigate(['/dashboard/report-viewer', r.id], { queryParams: { teamId } });
  }

  riskLabel(r: string): string {
    return ({ low: 'Baixo', moderate: 'Moderado', high: 'Alto', LOW: 'Baixo', MEDIUM: 'Médio', HIGH: 'Alto' } as Record<string, string>)[r] ?? r;
  }

  riskClass(r: string): string {
    return ({ LOW: 'low', MEDIUM: 'moderate', HIGH: 'high', low: 'low', moderate: 'moderate', high: 'high' } as Record<string, string>)[r] ?? '';
  }

  statusIcon(s: MemberStatus): string {
    return ({ pending: 'schedule', responded: 'done', evaluated: 'verified', no_response: 'person_off' })[s];
  }

  formStatusLabel(s: string): string {
    return ({ CREATED: 'Criado', ACTIVE: 'Ativo', ENDED: 'Encerrado' })[s as 'CREATED' | 'ACTIVE' | 'ENDED'] ?? s;
  }

  private computeAnalysisStatus(eval_: TherapistEvaluationDto | null | undefined, existing: any | null): AnalysisStatus {
    if (!eval_) return 'none';
    if (eval_.status === 'PUBLISHED') return 'submitted';
    if (this.isDraftComplete(eval_, existing)) return 'done';
    return 'draft';
  }

  private isDraftComplete(eval_: TherapistEvaluationDto, existing: any | null): boolean {
    const hasClosing = !!eval_.closingCommentary?.trim();
    const hasFinalScore = existing?.score != null;
    const risks = [eval_.stressScore, eval_.sleepScore, eval_.overloadScore, eval_.fatigueScore, eval_.disengagementScore, eval_.isolationScore];
    return hasClosing && hasFinalScore && risks.every(v => v != null);
  }
}
