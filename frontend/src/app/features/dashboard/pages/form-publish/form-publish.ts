import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { RouterLink, ActivatedRoute, Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { FormService } from '../../../../core/services/form.service';
import { TeamService, TeamResponseDto } from '../../../../core/services/team.service';

@Component({
  selector: 'app-form-publish',
  imports: [
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './form-publish.html',
  styleUrl: './form-publish.scss',
})
export class FormPublish implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private formService = inject(FormService);
  private teamService = inject(TeamService);

  formId = signal<string | null>(null);
  assessment = signal<{ id: string; title: string; description: string } | null>(null);
  myTeam = signal<TeamResponseDto | null>(null);

  loading = signal(true);
  publishing = signal(false);
  error = signal<string | null>(null);

  canPublish = computed(() => this.myTeam() !== null && !this.publishing());

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) { this.router.navigate(['/dashboard/counselor-assessments']); return; }
    this.formId.set(id);

    forkJoin({
      form: this.formService.getById(id),
      team: this.teamService.getMyTeam().pipe(catchError(() => of(null))),
    }).subscribe({
      next: ({ form, team }) => {
        this.assessment.set({ id: String(form.id), title: form.title, description: form.description ?? '' });
        this.myTeam.set(team);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Falha ao carregar dados. Tente novamente.');
        this.loading.set(false);
      },
    });
  }

  onPublish(): void {
    const id = this.formId();
    const form = this.assessment();
    const team = this.myTeam();
    if (!id || !form || !team || this.publishing()) return;

    this.publishing.set(true);
    this.error.set(null);

    this.formService.update(id, {
      title: form.title,
      description: form.description,
      status: 'ACTIVE',
      teamIds: [team.id],
    }).subscribe({
      next: () => {
        this.publishing.set(false);
        this.router.navigate(['/dashboard/counselor-assessments']);
      },
      error: () => {
        this.error.set('Falha ao publicar. Tente novamente.');
        this.publishing.set(false);
      },
    });
  }
}
