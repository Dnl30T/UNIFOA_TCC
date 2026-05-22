import { Component, OnInit, inject, signal } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { forkJoin } from 'rxjs';
import { EmployeeResultService, EmployeeResultResponseDto } from '../../../../core/services/employee-result.service';
import { EmployeeService, EmployeeResponseDto } from '../../../../core/services/employee.service';
import { FormService, FormResponseDto } from '../../../../core/services/form.service';
import { TeamService } from '../../../../core/services/team.service';

interface HistoryEntry {
  member: string;
  report: string;
  date: string;
  score: number;
  risk: 'low' | 'moderate' | 'high';
  trend: 'up' | 'down' | 'stable';
}

@Component({
  selector: 'app-report-history',
  imports: [MatCardModule, MatTableModule, MatButtonModule, MatIconModule, MatTooltipModule],
  templateUrl: './report-history.html',
  styleUrl: './report-history.scss',
})
export class ReportHistory implements OnInit {
  private resultService = inject(EmployeeResultService);
  private employeeService = inject(EmployeeService);
  private formService = inject(FormService);
  private teamService = inject(TeamService);

  columns = ['member', 'report', 'date', 'score', 'risk', 'trend', 'action'];
  items = signal<HistoryEntry[]>([]);

  ngOnInit(): void {
    this.teamService.getMyTeam().subscribe({
      next: team => {
        forkJoin({
          results: this.resultService.getAll(),
          employees: this.employeeService.getAll(),
          forms: this.formService.getAll(),
        }).subscribe({
          next: ({ results, employees, forms }) => {
            const scopedEmployees = employees.filter(employee => employee.teamId === team.id);
            const employeeIds = new Set(scopedEmployees.map(employee => employee.id));
            const scopedResults = results.filter(result => employeeIds.has(result.employeeId));

            const employeeMap = new Map<string, EmployeeResponseDto>(scopedEmployees.map(e => [e.id, e]));
            const formMap = new Map<string, FormResponseDto>(forms.map(f => [f.id, f]));

            this.items.set(scopedResults.map((r: EmployeeResultResponseDto) => ({
              member: employeeMap.get(r.employeeId)?.name ?? r.employeeId,
              report: formMap.get(r.formId)?.title ?? r.formId,
              date: r.calculatedAt
                ? new Date(r.calculatedAt).toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' })
                : '—',
              score: r.score,
              risk: r.riskLevel === 'HIGH' ? 'high' : r.riskLevel === 'MEDIUM' ? 'moderate' : 'low',
              trend: 'stable' as const,
            })));
          },
        });
      },
    });
  }

  riskLabel(r: string) {
    return ({ low: 'Baixo', moderate: 'Moderado', high: 'Alto' } as Record<string, string>)[r] ?? r;
  }

  trendIcon(t: string) {
    return { up: 'trending_up', down: 'trending_down', stable: 'trending_flat' }[t as string] ?? 'trending_flat';
  }

  trendColor(t: string) {
    return { up: '#dc2626', down: '#16a34a', stable: '#6b7280' }[t as string] ?? '#6b7280';
  }
}
