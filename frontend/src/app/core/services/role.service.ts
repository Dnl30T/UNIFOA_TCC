import { Injectable, computed, inject } from '@angular/core';
import { AuthService } from './auth.service';

export type UserRole = 'employee' | 'manager' | 'counselor';

@Injectable({ providedIn: 'root' })
export class RoleService {
  private auth = inject(AuthService);

  /** Derives the UI role from the JWT backend role. */
  readonly role = computed<UserRole>(() => {
    switch (this.auth.backendRole()) {
      case 'MANAGER': return 'manager';
      case 'COUNSELOR': return 'counselor';
      case 'ADMIN': return 'counselor';
      default: return 'employee';
    }
  });
}
