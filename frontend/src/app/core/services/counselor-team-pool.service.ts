import { Injectable, inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { AuthService } from './auth.service';

@Injectable({ providedIn: 'root' })
export class CounselorTeamPoolService {
  private auth = inject(AuthService);
  private platformId = inject(PLATFORM_ID);

  private key(): string {
    return `psytrack_team_pool_${this.auth.username() ?? 'unknown'}`;
  }

  getPool(): Set<string> {
    if (!isPlatformBrowser(this.platformId)) return new Set();
    try {
      const raw = localStorage.getItem(this.key());
      return raw ? new Set<string>(JSON.parse(raw) as string[]) : new Set();
    } catch {
      return new Set();
    }
  }

  isInPool(teamId: string): boolean {
    return this.getPool().has(teamId);
  }

  add(teamId: string): void {
    if (!isPlatformBrowser(this.platformId)) return;
    const pool = this.getPool();
    pool.add(teamId);
    localStorage.setItem(this.key(), JSON.stringify([...pool]));
  }

  remove(teamId: string): void {
    if (!isPlatformBrowser(this.platformId)) return;
    const pool = this.getPool();
    pool.delete(teamId);
    localStorage.setItem(this.key(), JSON.stringify([...pool]));
  }

  toggle(teamId: string): void {
    if (this.isInPool(teamId)) this.remove(teamId);
    else this.add(teamId);
  }
}
