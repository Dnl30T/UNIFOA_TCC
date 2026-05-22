import { Routes } from '@angular/router';
import { Landing } from './features/landing/landing';
import { PrivacyPolicy } from './features/privacy-policy/privacy-policy';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  { path: '', component: Landing },
  {
    path: 'auth',
    loadChildren: () =>
      import('./features/auth/auth.routes').then((m) => m.authRoutes),
  },
  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadChildren: () =>
      import('./features/dashboard/dashboard.routes').then(
        (m) => m.dashboardRoutes
      ),
  },
  { path: 'privacy-policy', component: PrivacyPolicy },
  { path: '**', redirectTo: '' },
];
