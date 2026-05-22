import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { forkJoin } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { of } from 'rxjs';
import { AuthService } from '../../../../core/services/auth.service';
import { EmployeeService } from '../../../../core/services/employee.service';
import { TeamService, TeamResponseDto } from '../../../../core/services/team.service';
import { EmployeeResultService, EmployeeResultResponseDto } from '../../../../core/services/employee-result.service';
import { FormService } from '../../../../core/services/form.service';
import { FormSubmissionService } from '../../../../core/services/form-submission.service';
import { TherapistEvaluationService, TherapistEvaluationDto } from '../../../../core/services/therapist-evaluation.service';

interface StatCard {
  title: string;
  value: string;
  trend: number;
  icon: string;
  iconColor: string;
  iconBg: string;
}

interface ActivityItem {
  name: string;
  description: string;
  amount: string;
  positive: boolean;
  status: 'completed' | 'pending' | 'failed';
  date: string;
  icon: string;
  iconBg: string;
}

@Component({
  selector: 'app-dashboard-home',
  imports: [
    DecimalPipe,
    DatePipe,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatChipsModule,
    MatDividerModule,
    MatFormFieldModule,
    MatSelectModule,
    FormsModule,
  ],
  templateUrl: './dashboard-home.html',
  styleUrl: './dashboard-home.scss',
})
export class DashboardHome implements OnInit {
  private employeeService = inject(EmployeeService);
  private teamService = inject(TeamService);
  private employeeResultService = inject(EmployeeResultService);
  private authService = inject(AuthService);
  private formService = inject(FormService);
  private submissionService = inject(FormSubmissionService);
  private evaluationService = inject(TherapistEvaluationService);

  teams = signal<TeamResponseDto[]>([]);
  selectedTeamId = signal<string | null>(null);

  burnoutTrend = signal<{ linePoints: string; areaPoints: string; labels: string[] }>({
    linePoints: '50,160 170,160 290,160 410,160 530,160 650,160',
    areaPoints: '50,160 170,160 290,160 410,160 530,160 650,160 650,170 50,170',
    labels: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun'],
  });

  riskRadarPoints = signal<string>('110,110 110,110 110,110 110,110 110,110 110,110');

  stats = signal<StatCard[]>([
    { title: 'Funcionários Ativos',    value: '—', trend: 0, icon: 'people',         iconColor: '#2e7d32', iconBg: '#e8f5e9' },
    { title: 'Avaliações Pendentes',  value: '—', trend: 0, icon: 'assignment_late', iconColor: '#7b1fa2', iconBg: '#f3e5f5' },
    { title: 'Funcionários em Risco', value: '—', trend: 0, icon: 'warning_amber',   iconColor: '#c62828', iconBg: '#ffebee' },
    { title: 'Score Médio de Burnout', value: '—', trend: 0, icon: 'monitor_heart',   iconColor: '#f57c00', iconBg: '#fff3e0' },
    { title: 'Times Monitorados',     value: '—', trend: 0, icon: 'groups',          iconColor: '#0277bd', iconBg: '#e1f5fe' },
  ]);

  activities = signal<ActivityItem[]>([]);

  riskDistribution = signal([
    { label: 'Baixo Risco', color: '#2e7d32', pct: 0 },
    { label: 'Moderado',    color: '#f59e0b', pct: 0 },
    { label: 'Alto Risco',  color: '#c62828', pct: 0 },
  ]);

  ngOnInit(): void {
    const backendRole = this.authService.backendRole();

    if (backendRole === 'MANAGER') {
      this.teamService.getMyTeam().pipe(catchError(() => of(null))).subscribe(team => {
        if (!team) {
          this.teams.set([]);
          this.selectedTeamId.set(null);
          this.loadDashboardData(null);
          return;
        }
        this.teams.set([team]);
        this.selectedTeamId.set(team.id);
        this.loadDashboardData(team.id);
      });
      return;
    }

    this.teamService.getAll().pipe(catchError(() => of([]))).subscribe(teams => {
      this.teams.set(teams);
      this.selectedTeamId.set(teams.length ? teams[0].id : null);
      this.loadDashboardData(this.selectedTeamId());
    });
  }

  onTeamChange(teamId: string | null): void {
    this.selectedTeamId.set(teamId);
    this.loadDashboardData(teamId);
  }

  canFilterTeam(): boolean {
    return this.authService.backendRole() === 'COUNSELOR' && this.teams().length > 0;
  }

  private loadDashboardData(teamId: string | null): void {
    forkJoin({
      employees: this.employeeService.getAll().pipe(catchError(() => of([]))),
      forms: this.formService.getAll('ACTIVE').pipe(catchError(() => of([]))),
      submissions: this.submissionService.getAll().pipe(catchError(() => of([]))),
      results: this.employeeResultService.getAll().pipe(catchError(() => of([]))),
    }).subscribe(({ employees, forms, submissions, results }) => {
      const scopedEmployees = teamId
        ? employees.filter(employee => employee.teamId === teamId)
        : employees;
      const scopedEmployeeIds = new Set(scopedEmployees.map(employee => employee.id));

      const scopedForms = teamId
        ? forms.filter(form => (form.teamIds ?? []).includes(teamId))
        : forms;
      const scopedFormIds = new Set(scopedForms.map(form => form.id));

      const scopedSubmissions = submissions.filter(submission =>
        (scopedFormIds.size === 0 || scopedFormIds.has(submission.formId)) &&
        scopedEmployeeIds.has(submission.employeeId));

      const scopedResults = results.filter(result =>
        (scopedFormIds.size === 0 || scopedFormIds.has(result.formId)) &&
        scopedEmployeeIds.has(result.employeeId));

      const activeEmployees = scopedEmployees.filter(employee => employee.status === 'ACTIVE').length;
      const atRisk = scopedResults.filter(result => result.riskLevel === 'HIGH').length;
      const avgScore = scopedResults.length
        ? scopedResults.reduce((sum, result) => sum + result.score, 0) / scopedResults.length
        : 0;

      const low = scopedResults.filter(result => result.riskLevel === 'LOW').length;
      const medium = scopedResults.filter(result => result.riskLevel === 'MEDIUM').length;
      const high = scopedResults.filter(result => result.riskLevel === 'HIGH').length;
      const total = scopedResults.length || 1;

      const responded = scopedSubmissions.filter(submission => submission.responseStatus === 'RESPONDED').length;
      const noResponse = scopedSubmissions.filter(submission => submission.responseStatus === 'NO_RESPONSE').length;
      const pending = Math.max(activeEmployees - responded - noResponse, 0);

      this.stats.set([
        { title: 'Funcionários Ativos', value: activeEmployees.toString(), trend: 0, icon: 'people', iconColor: '#2e7d32', iconBg: '#e8f5e9' },
        { title: 'Avaliações Pendentes', value: pending.toString(), trend: 0, icon: 'assignment_late', iconColor: '#7b1fa2', iconBg: '#f3e5f5' },
        { title: 'Funcionários em Risco', value: atRisk.toString(), trend: 0, icon: 'warning_amber', iconColor: '#c62828', iconBg: '#ffebee' },
        { title: 'Score Médio de Burnout', value: `${avgScore.toFixed(1)} / 100`, trend: 0, icon: 'monitor_heart', iconColor: '#f57c00', iconBg: '#fff3e0' },
        { title: 'Times Monitorados', value: (teamId ? 1 : this.teams().length).toString(), trend: 0, icon: 'groups', iconColor: '#0277bd', iconBg: '#e1f5fe' },
      ]);

      this.riskDistribution.set([
        { label: 'Baixo Risco', color: '#2e7d32', pct: Math.round((low / total) * 100) },
        { label: 'Moderado',    color: '#f59e0b', pct: Math.round((medium / total) * 100) },
        { label: 'Alto Risco',  color: '#c62828', pct: Math.round((high / total) * 100) },
      ]);

      this.activities.set(
        scopedSubmissions
          .slice()
          .sort((a, b) => b.submittedAt.localeCompare(a.submittedAt))
          .slice(0, 6)
          .map(item => ({
            name: scopedEmployees.find(employee => employee.id === item.employeeId)?.name ?? 'Employee',
            description: item.responseStatus === 'RESPONDED' ? 'Avaliação enviada' : 'Avaliação encerrada sem resposta',
            amount: item.burnoutRiskPreAnalysis ? `Risco: ${item.burnoutRiskPreAnalysis}` : `Respostas: ${item.answerCount}`,
            positive: item.responseStatus === 'RESPONDED',
            status: item.responseStatus === 'RESPONDED' ? 'completed' : 'pending',
            date: new Date(item.submittedAt).toLocaleDateString('pt-BR', { day: '2-digit', month: 'short', year: 'numeric' }),
            icon: item.responseStatus === 'RESPONDED' ? 'check_circle' : 'schedule',
            iconBg: item.responseStatus === 'RESPONDED' ? '#e8f5e9' : '#fff3e0',
          })),
      );

      // Burnout trend from results
      this.burnoutTrend.set(this.computeBurnoutTrend(scopedResults));

      // Risk radar from therapist evaluations — fetch per scoped form in parallel
      const formIds = Array.from(scopedFormIds);
      if (formIds.length === 0) {
        this.riskRadarPoints.set('110,110 110,110 110,110 110,110 110,110 110,110');
        return;
      }
      forkJoin(formIds.map(fid => this.evaluationService.getPublishedByForm(fid).pipe(catchError(() => of([])))))
        .subscribe(evalLists => {
          const allEvals: TherapistEvaluationDto[] = evalLists.flat();
          const scopedEvals = allEvals.filter(e => scopedEmployeeIds.has(e.employeeId));
          this.riskRadarPoints.set(this.computeRiskRadar(scopedEvals));
        });
    });
  }

  private computeBurnoutTrend(results: EmployeeResultResponseDto[]): { linePoints: string; areaPoints: string; labels: string[] } {
    const now = new Date();
    const months: { key: string; label: string; x: number }[] = [];
    for (let i = 5; i >= 0; i--) {
      const d = new Date(now.getFullYear(), now.getMonth() - i, 1);
      const key = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
      const label = d.toLocaleDateString('en-US', { month: 'short' });
      const x = 50 + (5 - i) * 120;
      months.push({ key, label, x });
    }

    const byMonth: { [key: string]: number[] } = {};
    for (const r of results) {
      const d = new Date(r.calculatedAt);
      const key = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
      if (!byMonth[key]) byMonth[key] = [];
      byMonth[key].push(r.score);
    }

    const points = months.map(m => {
      const scores = byMonth[m.key] ?? [];
      const avg = scores.length ? scores.reduce((a, b) => a + b, 0) / scores.length : 0;
      const y = Math.round(160 - (avg / 100) * 140);
      return { x: m.x, y, label: m.label };
    });

    const linePoints = points.map(p => `${p.x},${p.y}`).join(' ');
    const areaPoints = linePoints + ` ${points[points.length - 1].x},170 ${points[0].x},170`;
    return { linePoints, areaPoints, labels: points.map(p => p.label) };
  }

  private computeRiskRadar(evaluations: TherapistEvaluationDto[]): string {
    const withScores = evaluations.filter(e =>
      e.stressScore != null || e.sleepScore != null || e.overloadScore != null ||
      e.fatigueScore != null || e.disengagementScore != null || e.isolationScore != null);

    if (!withScores.length) return '110,110 110,110 110,110 110,110 110,110 110,110';

    const avgField = (key: keyof TherapistEvaluationDto): number => {
      const vals = withScores.map(e => e[key] as number | undefined).filter((v): v is number => v != null);
      return vals.length ? vals.reduce((a, b) => a + b, 0) / vals.length : 50;
    };

    const stress = avgField('stressScore') / 100;
    const fatigue = avgField('fatigueScore') / 100;
    const disengagement = avgField('disengagementScore') / 100;
    const overload = avgField('overloadScore') / 100;
    const isolation = avgField('isolationScore') / 100;
    const sleep = avgField('sleepScore') / 100;

    const pts = [
      [110, 110 - 90 * stress],
      [110 + 78 * fatigue, 110 - 45 * fatigue],
      [110 + 78 * disengagement, 110 + 45 * disengagement],
      [110, 110 + 90 * overload],
      [110 - 78 * isolation, 110 + 45 * isolation],
      [110 - 78 * sleep, 110 - 45 * sleep],
    ];
    return pts.map(([x, y]) => `${x.toFixed(1)},${y.toFixed(1)}`).join(' ');
  }

  getStrokeDasharray(pct: number, radius = 52): string {
    const circ = 2 * Math.PI * radius;
    return `${(pct / 100) * circ} ${circ}`;
  }

  getStrokeDashoffset(startPct: number, radius = 52): string {
    const circ = 2 * Math.PI * radius;
    return `${-((startPct / 100) * circ)}`;
  }
}
