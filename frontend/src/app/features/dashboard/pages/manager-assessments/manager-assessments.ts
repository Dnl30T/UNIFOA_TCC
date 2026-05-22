import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { FormsModule } from '@angular/forms';
import { DatePipe, DecimalPipe } from '@angular/common';
import { catchError } from 'rxjs/operators';
import { forkJoin, of } from 'rxjs';
import { EmployeeService, EmployeeResponseDto } from '../../../../core/services/employee.service';
import { FormService } from '../../../../core/services/form.service';
import { TeamService } from '../../../../core/services/team.service';
import { EmployeeResultService } from '../../../../core/services/employee-result.service';
import { ReportService, ReportResponseDto } from '../../../../core/services/report.service';

interface ReportMemberRow {
  employeeId: string;
  member: string;
  score: number;
  riskLevel: string;
}

@Component({
  selector: 'app-manager-assessments',
  imports: [
    MatCardModule, MatTableModule, MatButtonModule, MatIconModule,
    MatFormFieldModule, MatSelectModule, FormsModule, DatePipe, DecimalPipe,
  ],
  templateUrl: './manager-assessments.html',
  styleUrl: './manager-assessments.scss',
})
export class ManagerAssessments implements OnInit {
  private employeeService = inject(EmployeeService);
  private formService = inject(FormService);
  private teamService = inject(TeamService);
  private resultService = inject(EmployeeResultService);
  private reportService = inject(ReportService);
  private router = inject(Router);

  columns = ['member', 'score', 'risk'];
  reports = signal<ReportResponseDto[]>([]);
  selectedReportId = signal<string | null>(null);
  reportRows = signal<ReportMemberRow[]>([]);
  loading = signal(true);
  allEmployees = signal<EmployeeResponseDto[]>([]);
  allForms = signal<{ id: string; title: string }[]>([]);

  selectedReport = computed(() =>
    this.reports().find(r => r.id === this.selectedReportId()) ?? null
  );

  formTitleForReport = computed(() => {
    const r = this.selectedReport();
    return r ? (this.allForms().find(f => f.id === r.formId)?.title ?? r.formId) : '';
  });

  ngOnInit(): void {
    this.teamService.getMyTeam().pipe(catchError(() => of(null))).subscribe(team => {
      if (!team) { this.loading.set(false); return; }

      forkJoin({
        reports: this.reportService.listForManager().pipe(catchError(() => of([]))),
        employees: this.employeeService.getAll().pipe(catchError(() => of([]))),
        forms: this.formService.getAll().pipe(catchError(() => of([]))),
      }).subscribe(({ reports, employees, forms }) => {
        const teamEmployees = (employees as EmployeeResponseDto[]).filter(e => e.teamId === team.id);
        this.allEmployees.set(teamEmployees);
        this.allForms.set((forms as any[]).map(f => ({ id: f.id, title: f.title })));

        const teamReports = (reports as ReportResponseDto[]).filter(r => r.teamId === team.id)
          .sort((a, b) => b.generatedAt.localeCompare(a.generatedAt));
        this.reports.set(teamReports);
        this.loading.set(false);
      });
    });
  }

  onReportSelected(reportId: string | null): void {
    this.selectedReportId.set(reportId);
    const report = this.reports().find(r => r.id === reportId);
    if (!report) { this.reportRows.set([]); return; }

    // Load employee results for respondents in this report
    forkJoin(
      report.respondentIds.map(empId =>
        this.resultService.getAll({ employeeId: empId, formId: report.formId }).pipe(catchError(() => of([])))
      )
    ).subscribe(resultArrays => {
      const rows: ReportMemberRow[] = report.respondentIds.map((empId, i) => {
        const emp = this.allEmployees().find(e => e.id === empId);
        const results = (resultArrays[i] as any[]);
        const result = results.find((r: any) => r.formId === report.formId) ?? results[0];
        return {
          employeeId: empId,
          member: emp?.name ?? empId,
          score: result?.finalScore ?? result?.score ?? result?.helperScore ?? 0,
          riskLevel: result?.riskLevel ?? '—',
        };
      });
      this.reportRows.set(rows);
    });
  }

  viewDashboard(): void {
    const r = this.selectedReport();
    if (!r) return;
    this.router.navigate(['/dashboard/report-viewer', r.id], { queryParams: { teamId: r.teamId } });
  }

  riskLabel(r: string): string {
    return ({ LOW: 'Baixo', MEDIUM: 'Médio', HIGH: 'Alto' } as Record<string, string>)[r] ?? r;
  }

  riskClass(r: string): string {
    return ({ LOW: 'low', MEDIUM: 'moderate', HIGH: 'high' } as Record<string, string>)[r] ?? '';
  }
}
