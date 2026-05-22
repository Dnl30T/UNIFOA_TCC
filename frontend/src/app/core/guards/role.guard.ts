import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

/**
 * Usage in route: canActivate: [roleGuard('MANAGER', 'ADMIN')]
 * Maps backend roles: ADMIN | COUNSELOR | MANAGER | EMPLOYEE | PENDING
 */
export const roleGuard = (...allowedRoles: string[]): CanActivateFn =>
  () => {
    const auth = inject(AuthService);
    const router = inject(Router);
    if (!auth.isLoggedIn()) return router.createUrlTree(['/auth/login']);
    const role = auth.backendRole();
    if (role && allowedRoles.includes(role)) return true;
    if (role === 'EMPLOYEE') return router.createUrlTree(['/dashboard/my-assessments']);
    return router.createUrlTree(['/dashboard/home']);
  };
