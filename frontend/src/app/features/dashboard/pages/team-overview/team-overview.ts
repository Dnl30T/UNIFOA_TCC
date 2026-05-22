import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatMenuModule } from '@angular/material/menu';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { EmployeeService, EmployeeResponseDto } from '../../../../core/services/employee.service';
import { TeamService } from '../../../../core/services/team.service';

type ResponseStatus = 'responded' | 'pending';
type FilterOption = 'all' | ResponseStatus;

interface TeamMember {
  name: string;
  jobTitle: string;
  responded: boolean;
  initials: string;
}

@Component({
  selector: 'app-team-overview',
  imports: [
    FormsModule,
    MatCardModule, MatButtonModule, MatIconModule, MatTooltipModule, MatMenuModule,
    MatFormFieldModule, MatInputModule, MatProgressSpinnerModule,
  ],
  templateUrl: './team-overview.html',
  styleUrl: './team-overview.scss',
})
export class TeamOverview implements OnInit {
  private employeeService = inject(EmployeeService);
  private teamService = inject(TeamService);

  members = signal<TeamMember[]>([]);
  teamCode = signal<string | null>(null);
  codeCopied = signal(false);
  filter = signal<FilterOption>('all');

  /** null = loading, true = has team, false = no team */
  hasTeam = signal<boolean | null>(null);
  newTeamName = signal('');
  creating = signal(false);
  createError = signal<string | null>(null);

  filteredMembers = computed(() => {
    const f = this.filter();
    const all = this.members();
    if (f === 'all') return all;
    return all.filter(m => f === 'responded' ? m.responded : !m.responded);
  });

  filterLabel = computed(() => {
    return ({ all: 'Todos', responded: 'Responderam', pending: 'Pendentes' } as Record<FilterOption, string>)[this.filter()];
  });

  ngOnInit(): void {
    this.loadTeam();
  }

  private loadTeam(): void {
    this.teamService.getMyTeam().pipe(catchError(() => of(null))).subscribe(t => {
      if (t) {
        this.hasTeam.set(true);
        if (t.teamCode) this.teamCode.set(t.teamCode);
        this.loadMembers();
      } else {
        this.hasTeam.set(false);
      }
    });
  }

  private loadMembers(): void {
    this.employeeService.getAll().subscribe({
      next: (employees: EmployeeResponseDto[]) => {
        this.members.set(employees.map(e => ({
          name: e.name,
          jobTitle: e.status === 'INACTIVE' ? 'Inativo' : 'Colaborador',
          responded: false,
          initials: e.name.split(' ').map((p: string) => p[0]).slice(0, 2).join('').toUpperCase(),
        })));
      },
    });
  }

  createTeam(): void {
    const name = this.newTeamName().trim();
    if (!name) return;
    this.creating.set(true);
    this.createError.set(null);
    this.teamService.create({ name }).subscribe({
      next: () => {
        this.creating.set(false);
        this.newTeamName.set('');
        this.loadTeam();
      },
      error: () => {
        this.creating.set(false);
        this.createError.set('Não foi possível criar o time. Verifique o nome e tente novamente.');
      },
    });
  }

  setFilter(f: FilterOption): void {
    this.filter.set(f);
  }

  copyCode(): void {
    const code = this.teamCode();
    if (!code) return;
    navigator.clipboard.writeText(code).then(() => {
      this.codeCopied.set(true);
      setTimeout(() => this.codeCopied.set(false), 2000);
    });
  }

  statusLabel(responded: boolean): string {
    return responded ? 'Respondeu' : 'Pendente';
  }

  avatarColor(responded: boolean): string {
    return responded ? '#2e7d32' : '#6b7280';
  }
}
