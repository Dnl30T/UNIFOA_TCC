import { Component, OnInit, inject, signal } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { forkJoin } from 'rxjs';
import { TeamResultResponseDto, TeamResultService } from '../../../../core/services/team-result.service';
import { FormService, FormResponseDto } from '../../../../core/services/form.service';
import { TeamService } from '../../../../core/services/team.service';

interface HistoryEntry {
  team: string;
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
  private readonly resultService = inject(TeamResultService);
  private readonly formService = inject(FormService);
  private readonly teamService = inject(TeamService);

  columns = ['team', 'report', 'date', 'score', 'risk', 'trend', 'action'];
  items = signal<HistoryEntry[]>([]);

  ngOnInit(): void {
    this.teamService.getMyTeam().subscribe({
      next: team => {
        forkJoin({
          results: this.resultService.getAll({ teamId: team.id }),
          forms: this.formService.getAll(),
        }).subscribe({
          next: ({ results, forms }) => {
            const formMap = new Map<string, FormResponseDto>(forms.map(f => [f.id, f]));

            this.items.set(results.map((r: TeamResultResponseDto) => ({
              team: team.name,
              report: formMap.get(r.formId)?.title ?? r.formId,
              date: r.calculatedAt
                ? new Date(r.calculatedAt).toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' })
                : '—',
              score: r.averageScore,
              risk: this.dominantRisk(r.riskLevelDistribution),
              trend: 'stable' as const,
            })));
          },
        });
      },
    });
  }

  private dominantRisk(distribution: Record<string, number> | undefined): 'low' | 'moderate' | 'high' {
    if (!distribution) return 'moderate';

    const low = distribution['LOW'] ?? 0;
    const medium = distribution['MEDIUM'] ?? 0;
    const high = distribution['HIGH'] ?? 0;

    if (high >= medium && high >= low) return 'high';
    if (medium >= low) return 'moderate';
    return 'low';
  }

  riskLabel(r: string) {
    return ({ low: 'Baixo', moderate: 'Moderado', high: 'Alto' } as Record<string, string>)[r] ?? r;
  }

  trendIcon(t: string) {
    return { up: 'trending_up', down: 'trending_down', stable: 'trending_flat' }[t] ?? 'trending_flat';
  }

  trendColor(t: string) {
    return { up: '#dc2626', down: '#16a34a', stable: '#6b7280' }[t] ?? '#6b7280';
  }
}
