import { Component, inject, signal, computed } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TeamService, TeamResponseDto } from '../../../../core/services/team.service';

@Component({
  selector: 'app-team-add',
  imports: [
    RouterLink,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatInputModule,
    MatFormFieldModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './team-add.html',
  styleUrl: './team-add.scss',
})
export class TeamAdd {
  private teamService = inject(TeamService);
  private router = inject(Router);

  code = signal('');
  searching = signal(false);
  found = signal<TeamResponseDto | null>(null);
  searchError = signal<string | null>(null);

  joining = signal(false);
  joined = signal(false);
  joinError = signal<string | null>(null);

  canSearch = computed(() => this.code().trim().length > 0 && !this.searching());

  onSearch(): void {
    const code = this.code().trim().toUpperCase();
    if (!code || this.searching()) return;
    this.searching.set(true);
    this.found.set(null);
    this.searchError.set(null);
    this.joined.set(false);
    this.joinError.set(null);

    this.teamService.getByCode(code).subscribe({
      next: team => {
        this.found.set(team);
        this.searching.set(false);
      },
      error: () => {
        this.searchError.set('Nenhum time encontrado com esse código. Verifique e tente novamente.');
        this.searching.set(false);
      },
    });
  }

  join(): void {
    const code = this.code().trim().toUpperCase();
    if (!code || this.joining()) return;
    this.joining.set(true);
    this.joinError.set(null);

    this.teamService.joinByCode(code).subscribe({
      next: () => {
        this.joining.set(false);
        this.joined.set(true);
        setTimeout(() => this.router.navigate(['/dashboard/my-teams']), 1500);
      },
      error: (err) => {
        this.joining.set(false);
        if (err?.status === 409) {
          this.joinError.set('Você já faz parte de outro time. Saia dele primeiro para entrar neste.');
        } else {
          this.joinError.set('Não foi possível entrar no time. Tente novamente.');
        }
      },
    });
  }
}
