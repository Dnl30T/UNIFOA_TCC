import {
  Component, OnInit, OnDestroy, inject, signal, computed,
  PLATFORM_ID, ViewChild, ElementRef, AfterViewInit,
} from '@angular/core';
import { CommonModule, DatePipe, DecimalPipe, isPlatformBrowser } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { forkJoin, of } from 'rxjs';
import { catchError, switchMap } from 'rxjs/operators';
import * as echarts from 'echarts';

import { AuthService } from '../../../../core/services/auth.service';
import { ReportService, ReportResponseDto } from '../../../../core/services/report.service';
import { FormService } from '../../../../core/services/form.service';
import { TeamService } from '../../../../core/services/team.service';
import { EmployeeService, EmployeeResponseDto } from '../../../../core/services/employee.service';
import { EmployeeResultService } from '../../../../core/services/employee-result.service';
import { TherapistEvaluationService, TherapistEvaluationDto } from '../../../../core/services/therapist-evaluation.service';

interface MemberRow {
  id: string;
  name: string;
  score: number;
  riskLevel: string;
  stressScore: number | null;
  sleepScore: number | null;
  overloadScore: number | null;
  fatigueScore: number | null;
  disengagementScore: number | null;
  isolationScore: number | null;
  hrSummary: string | null;
  closingCommentary: string | null;
  expanded: boolean;
}

@Component({
  selector: 'app-report-viewer',
  standalone: true,
  imports: [
    CommonModule,
    DatePipe,
    DecimalPipe,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
  ],
  templateUrl: './report-viewer.html',
  styleUrl: './report-viewer.scss',
})
export class ReportViewer implements OnInit, OnDestroy {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private platformId = inject(PLATFORM_ID);
  private authService = inject(AuthService);
  private reportService = inject(ReportService);
  private formService = inject(FormService);
  private teamService = inject(TeamService);
  private employeeService = inject(EmployeeService);
  private resultService = inject(EmployeeResultService);
  private evaluationService = inject(TherapistEvaluationService);

  @ViewChild('donutChart', { static: false }) donutRef?: ElementRef<HTMLDivElement>;
  @ViewChild('radarChart', { static: false }) radarRef?: ElementRef<HTMLDivElement>;
  @ViewChild('barChart',   { static: false }) barRef?:   ElementRef<HTMLDivElement>;

  loading = signal(true);
  error = signal<string | null>(null);
  report = signal<ReportResponseDto | null>(null);
  formTitle = signal('');
  teamName = signal('');
  members = signal<MemberRow[]>([]);
  publishedEvalCount = signal(0);

  isCounselor = computed(() => {
    const role = this.authService.backendRole();
    return role === 'COUNSELOR' || role === 'ADMIN';
  });

  showPrivacyNotice = computed(() =>
    !this.isCounselor() && (this.report()?.respondentCount ?? 0) < 5
  );

  private donutChart?: echarts.ECharts;
  private radarChart?: echarts.ECharts;
  private barChart?: echarts.ECharts;

  ngOnInit(): void {
    const reportId = this.route.snapshot.paramMap.get('reportId') ?? '';
    const teamId   = this.route.snapshot.queryParamMap.get('teamId') ?? '';

    if (!reportId || !teamId) {
      this.error.set('Parâmetros inválidos. Navegue a partir de um relatório.');
      this.loading.set(false);
      return;
    }

    this.reportService.getById(teamId, reportId).pipe(
      switchMap(report => {
        this.report.set(report);
        return forkJoin({
          form:        this.formService.getById(report.formId).pipe(catchError(() => of(null))),
          team:        this.teamService.getById(teamId).pipe(catchError(() => of(null))),
          employees:   this.employeeService.getAll().pipe(catchError(() => of([]))),
          results:     this.resultService.getAll({ formId: report.formId }).pipe(catchError(() => of([]))),
          evaluations: this.evaluationService.getPublishedByForm(report.formId).pipe(catchError(() => of([]))),
        });
      }),
      catchError(() => {
        this.error.set('Não foi possível carregar o relatório.');
        this.loading.set(false);
        return of(null);
      }),
    ).subscribe(data => {
      if (!data) return;
      const report = this.report()!;

      this.formTitle.set((data.form as any)?.title ?? '');
      this.teamName.set((data.team as any)?.name ?? '');
      this.publishedEvalCount.set((data.evaluations as TherapistEvaluationDto[]).length);

      const resultByEmp = new Map((data.results as any[]).map(r => [r.employeeId, r]));
      const evalByEmp   = new Map((data.evaluations as TherapistEvaluationDto[]).map(e => [e.employeeId, e]));
      const empById     = new Map((data.employees as EmployeeResponseDto[]).map(e => [e.id, e]));

      const rows: MemberRow[] = report.respondentIds.map((id, i) => {
        const emp    = empById.get(id);
        const result = resultByEmp.get(id);
        const eval_  = evalByEmp.get(id);
        return {
          id,
          name:               emp?.name ?? `Membro ${i + 1}`,
          score:              result?.finalScore ?? result?.score ?? result?.helperScore ?? 0,
          riskLevel:          result?.riskLevel ?? '—',
          stressScore:        eval_?.stressScore        ?? null,
          sleepScore:         eval_?.sleepScore         ?? null,
          overloadScore:      eval_?.overloadScore      ?? null,
          fatigueScore:       eval_?.fatigueScore       ?? null,
          disengagementScore: eval_?.disengagementScore ?? null,
          isolationScore:     eval_?.isolationScore     ?? null,
          hrSummary:          eval_?.hrSummary          ?? null,
          closingCommentary:  eval_?.closingCommentary  ?? null,
          expanded: false,
        };
      }).sort((a, b) => b.score - a.score);

      this.members.set(rows);
      this.loading.set(false);

      if (isPlatformBrowser(this.platformId)) {
        setTimeout(() => this.initCharts(), 80);
      }
    });
  }

  ngOnDestroy(): void {
    this.donutChart?.dispose();
    this.radarChart?.dispose();
    this.barChart?.dispose();
  }

  private initCharts(): void {
    const report = this.report();
    if (!report) return;
    this.initDonut(report);
    this.initRadar();
    if (this.isCounselor()) this.initBar();
  }

  private initDonut(report: ReportResponseDto): void {
    const el = this.donutRef?.nativeElement;
    if (!el) return;
    this.donutChart?.dispose();
    this.donutChart = echarts.init(el);
    const dist = report.riskDistribution ?? {};

    this.donutChart.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
      legend: {
        bottom: 4, left: 'center',
        data: ['Baixo', 'Médio', 'Alto'],
        textStyle: { color: '#374151', fontSize: 12 },
      },
      series: [{
        type: 'pie',
        radius: ['46%', '68%'],
        center: ['50%', '44%'],
        avoidLabelOverlap: false,
        label: { show: false },
        emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' } },
        labelLine: { show: false },
        data: [
          { value: dist['LOW']    ?? 0, name: 'Baixo', itemStyle: { color: '#15803d' } },
          { value: dist['MEDIUM'] ?? 0, name: 'Médio', itemStyle: { color: '#f59e0b' } },
          { value: dist['HIGH']   ?? 0, name: 'Alto',  itemStyle: { color: '#ef4444' } },
        ],
      }],
      graphic: [
        { type: 'text', left: 'center', top: '34%', style: { text: report.averageScore.toFixed(1), textAlign: 'center', fontSize: 26, fontWeight: 'bold', fill: '#111827' } },
        { type: 'text', left: 'center', top: '47%', style: { text: 'score médio', textAlign: 'center', fontSize: 11, fill: '#6b7280' } },
      ],
    });
  }

  private initRadar(): void {
    const el = this.radarRef?.nativeElement;
    if (!el) return;
    const withScores = this.members().filter(m =>
      m.stressScore != null || m.sleepScore != null || m.fatigueScore != null);
    if (!withScores.length) {
      el.innerHTML = '<div style="display:flex;align-items:center;justify-content:center;height:100%;color:#9ca3af;font-size:0.85rem">Sem avaliações publicadas</div>';
      return;
    }
    const avg = (fn: (m: MemberRow) => number | null): number => {
      const vals = withScores.map(fn).filter((v): v is number => v != null);
      return vals.length ? +(vals.reduce((a, b) => a + b, 0) / vals.length).toFixed(1) : 0;
    };

    this.radarChart?.dispose();
    this.radarChart = echarts.init(el);
    this.radarChart.setOption({
      tooltip: {},
      radar: {
        indicator: [
          { name: 'Estresse',      max: 100 },
          { name: 'Fadiga',        max: 100 },
          { name: 'Sobrecarga',    max: 100 },
          { name: 'Sono',          max: 100 },
          { name: 'Isolamento',    max: 100 },
          { name: 'Desengajamento',max: 100 },
        ],
        shape: 'polygon',
        splitNumber: 4,
        axisName: { color: '#374151', fontSize: 11 },
        splitLine:  { lineStyle: { color: '#e5e7eb' } },
        splitArea:  { areaStyle: { color: ['rgba(249,250,251,0.8)', 'rgba(243,244,246,0.5)'] } },
        axisLine:   { lineStyle: { color: '#d1d5db' } },
      },
      series: [{
        type: 'radar',
        data: [{
          value: [
            avg(m => m.stressScore),
            avg(m => m.fatigueScore),
            avg(m => m.overloadScore),
            avg(m => m.sleepScore),
            avg(m => m.isolationScore),
            avg(m => m.disengagementScore),
          ],
          name: 'Média da Equipe',
          areaStyle: { color: 'rgba(21,128,61,0.13)' },
          lineStyle: { color: '#15803d', width: 2 },
          itemStyle: { color: '#15803d' },
          label: {
            show: true, fontSize: 10, color: '#374151',
            formatter: (p: any) => p.value > 0 ? String(p.value) : '',
          },
        }],
      }],
    });
  }

  private initBar(): void {
    const el = this.barRef?.nativeElement;
    if (!el) return;
    const counts = [0, 0, 0, 0, 0];
    for (const m of this.members()) counts[Math.min(Math.floor(m.score / 20), 4)]++;

    this.barChart?.dispose();
    this.barChart = echarts.init(el);
    this.barChart.setOption({
      tooltip: {
        trigger: 'axis',
        formatter: (p: any) => `Faixa ${p[0].name}: <strong>${p[0].value}</strong> membro(s)`,
      },
      xAxis: {
        type: 'category',
        data: ['0–19', '20–39', '40–59', '60–79', '80–100'],
        axisLabel: { color: '#6b7280', fontSize: 11 },
        axisLine: { lineStyle: { color: '#e5e7eb' } },
        axisTick: { show: false },
      },
      yAxis: {
        type: 'value',
        minInterval: 1,
        axisLabel: { color: '#6b7280', fontSize: 11 },
        splitLine: { lineStyle: { color: '#f3f4f6' } },
      },
      series: [{
        type: 'bar',
        data: counts.map((v, i) => ({
          value: v,
          itemStyle: {
            color: ['#ef4444', '#f59e0b', '#f59e0b', '#15803d', '#15803d'][i],
            borderRadius: [4, 4, 0, 0],
          },
        })),
        barMaxWidth: 52,
      }],
      grid: { left: 36, right: 16, top: 12, bottom: 32 },
    });
  }

  toggleRow(row: MemberRow): void {
    row.expanded = !row.expanded;
    this.members.set([...this.members()]);
  }

  back(): void {
    this.router.navigate([
      this.authService.backendRole() === 'MANAGER'
        ? '/dashboard/manager-assessments'
        : '/dashboard/counselor-reports',
    ]);
  }

  riskLabel(r: string): string {
    return ({ LOW: 'Baixo', MEDIUM: 'Médio', HIGH: 'Alto' } as Record<string, string>)[r] ?? r;
  }

  riskClass(r: string): string {
    return ({ LOW: 'low', MEDIUM: 'moderate', HIGH: 'high' } as Record<string, string>)[r] ?? '';
  }

  scoreColor(s: number): string {
    return s >= 67 ? '#ef4444' : s >= 34 ? '#f59e0b' : '#15803d';
  }

  initials(name: string): string {
    return name.split(' ').map(p => p[0]).slice(0, 2).join('').toUpperCase();
  }

  dimColor(v: number | null): string {
    if (v == null) return '#e5e7eb';
    return v >= 67 ? '#fecaca' : v >= 34 ? '#fef3c7' : '#dcfce7';
  }

  dimTextColor(v: number | null): string {
    if (v == null) return '#9ca3af';
    return v >= 67 ? '#b91c1c' : v >= 34 ? '#92400e' : '#15803d';
  }
}
