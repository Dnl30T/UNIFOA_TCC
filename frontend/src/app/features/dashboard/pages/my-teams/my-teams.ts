import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { TeamService, TeamResponseDto } from '../../../../core/services/team.service';
import { EmployeeService, EmployeeResponseDto } from '../../../../core/services/employee.service';
import { EmployeeResultService, EmployeeResultResponseDto } from '../../../../core/services/employee-result.service';
import { FormService, FormResponseDto } from '../../../../core/services/form.service';
import { ActionCardComponent, ActionCardButton } from '../../../../shared/components/action-card/action-card.component';
import { StepIndicatorComponent, Step } from '../../../../shared/components/step-indicator/step-indicator.component';
import { MembersPanelComponent, MembersPanelDialogData } from './members-panel.component';

interface Member {
  id: string;
  name: string;
}

interface TeamCard {
  id: string;
  name: string;
  members: Member[];
  responded: number;
  total: number;
  formReceived: boolean;
  activeFormId: string | null;
}

@Component({
  selector: 'app-my-teams',
  imports: [
    CommonModule,
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatDialogModule,
    MatProgressSpinnerModule,
    ActionCardComponent,
    StepIndicatorComponent,
  ],
  templateUrl: './my-teams.html',
  styleUrl: './my-teams.scss',
})
export class MyTeams implements OnInit {
  private teamService = inject(TeamService);
  private employeeService = inject(EmployeeService);
  private resultService = inject(EmployeeResultService);
  private formService = inject(FormService);
  private dialog = inject(MatDialog);

  teams = signal<TeamCard[]>([]);
  loading = signal(true);

  ngOnInit(): void {
    forkJoin({
      teams: this.teamService.getAll(),
      employees: this.employeeService.getAll().pipe(catchError(() => of([]))),
      results: this.resultService.getAll().pipe(catchError(() => of([]))),
      forms: this.formService.getAll().pipe(catchError(() => of([]))),
    }).subscribe({
      next: ({ teams, employees, results, forms }) => {
        const empByTeam = new Map<string, Member[]>();
        for (const e of employees as EmployeeResponseDto[]) {
          const list = empByTeam.get(e.teamId) ?? [];
          list.push({ id: e.id, name: e.name });
          empByTeam.set(e.teamId, list);
        }

        const respondedIds = new Set((results as EmployeeResultResponseDto[]).map(r => r.employeeId));

        // A team "received a form" when at least one ACTIVE form lists it in teamIds
        const teamsWithForm = new Set(
          (forms as FormResponseDto[]).flatMap(f => f.teamIds ?? [])
        );

        this.teams.set(teams.map((t: TeamResponseDto) => {
          const members = empByTeam.get(t.id) ?? [];
          const responded = members.filter(m => respondedIds.has(m.id)).length;
          const teamForms = (forms as FormResponseDto[]).filter(f => (f.teamIds ?? []).includes(t.id));
          const preferredForm = teamForms.find(f => f.status === 'ACTIVE')
            ?? teamForms.find(f => f.status === 'CREATED')
            ?? teamForms[0]
            ?? null;

          const activeFormId = preferredForm?.id ?? null;
          return {
            id: t.id,
            name: t.name,
            members,
            responded,
            total: members.length,
            formReceived: teamsWithForm.has(t.id),
            activeFormId,
          };
        }));
        this.loading.set(false);
      },
    });
  }

  // Compute steps for each team showing progress
  getStepsForTeam(team: TeamCard): Step[] {
    return [
      {
        id: 'responses',
        label: 'Responses',
        status: team.responded > 0 ? (team.responded === team.total ? 'complete' : 'active') : 'pending',
        description: `${team.responded} of ${team.total} responded`
      },
      {
        id: 'report',
        label: 'Generate Report',
        status: team.responded === team.total ? 'active' : 'pending',
        description: team.responded === team.total ? 'Ready to generate' : 'Awaiting all responses'
      }
    ];
  }

  // Compute action buttons for team
  getTeamActions(team: TeamCard): ActionCardButton[] {
    return [
      {
        id: 'viewMembers',
        label: 'View Members',
        icon: 'people',
        type: 'primary',
        disabled: team.total === 0
      },
      {
        id: 'generateReport',
        label: 'Generate Report',
        icon: 'summarize',
        type: 'secondary',
        disabled: team.responded !== team.total,
        tooltip: team.responded === team.total ? 'Generate team report' : 'All members must respond first'
      }
    ];
  }

  // Handle action card clicks
  onTeamAction(actionId: string, team: TeamCard): void {
    if (actionId === 'viewMembers') {
      this.openMembersPanel(team);
    } else if (actionId === 'generateReport') {
      // TODO: Implement report generation navigation
    }
  }

  // Open members dialog
  openMembersPanel(team: TeamCard): void {
    const data: MembersPanelDialogData = {
      teamName: team.name,
      members: team.members,
      formId: team.activeFormId,
    };
    this.dialog.open(MembersPanelComponent, {
      data,
      width: '520px',
      maxHeight: '90vh',
      panelClass: 'members-panel-dialog',
    });
  }

  completionLabel(t: TeamCard): string {
    return t.total > 0 ? `${t.responded} de ${t.total} responderam` : 'Sem membros';
  }

  completionPercent(t: TeamCard): number {
    return t.total > 0 ? Math.round((t.responded / t.total) * 100) : 0;
  }

  isComplete(t: TeamCard): boolean {
    return t.total > 0 && t.responded === t.total;
  }
}
