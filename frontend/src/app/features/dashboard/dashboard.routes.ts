import { Routes } from '@angular/router';
import { DashboardShell } from './dashboard-shell';
import { roleGuard } from '../../core/guards/role.guard';

export const dashboardRoutes: Routes = [
  {
    path: '',
    component: DashboardShell,
    children: [
      {
        path: 'home',
        canActivate: [roleGuard('MANAGER', 'COUNSELOR', 'ADMIN')],
        loadComponent: () => import('./pages/dashboard-home/dashboard-home').then(m => m.DashboardHome),
      },
      // Employee routes
      {
        path: 'my-assessments',
        canActivate: [roleGuard('EMPLOYEE')],
        loadComponent: () => import('./pages/my-assessments/my-assessments').then(m => m.MyAssessments),
      },
      {
        path: 'my-reports',
        canActivate: [roleGuard('EMPLOYEE')],
        loadComponent: () => import('./pages/my-reports/my-reports').then(m => m.MyReports),
      },
      // Manager routes
      {
        path: 'manager-assessments',
        canActivate: [roleGuard('MANAGER', 'ADMIN')],
        loadComponent: () => import('./pages/manager-assessments/manager-assessments').then(m => m.ManagerAssessments),
      },
      {
        path: 'team-overview',
        canActivate: [roleGuard('MANAGER', 'ADMIN')],
        loadComponent: () => import('./pages/team-overview/team-overview').then(m => m.TeamOverview),
      },
      {
        path: 'report-history',
        canActivate: [roleGuard('COUNSELOR', 'ADMIN')],
        loadComponent: () => import('./pages/report-history/report-history').then(m => m.ReportHistory),
      },
      // Counselor routes
      {
        path: 'counselor-assessments',
        canActivate: [roleGuard('COUNSELOR', 'ADMIN')],
        loadComponent: () => import('./pages/counselor-assessments/counselor-assessments').then(m => m.CounselorAssessments),
      },
      {
        path: 'counselor-reports',
        canActivate: [roleGuard('COUNSELOR', 'ADMIN')],
        loadComponent: () => import('./pages/counselor-reports/counselor-reports').then(m => m.CounselorReports),
      },
      {
        path: 'my-teams',
        canActivate: [roleGuard('COUNSELOR', 'ADMIN')],
        loadComponent: () => import('./pages/my-teams/my-teams').then(m => m.MyTeams),
      },
      {
        path: 'teams/add',
        canActivate: [roleGuard('COUNSELOR', 'ADMIN')],
        loadComponent: () => import('./pages/team-add/team-add').then(m => m.TeamAdd),
      },
      {
        path: 'forms/builder',
        canActivate: [roleGuard('COUNSELOR', 'ADMIN')],
        loadComponent: () => import('./pages/form-builder/form-builder').then(m => m.FormBuilder),
      },
      {
        path: 'forms/review',
        canActivate: [roleGuard('COUNSELOR', 'ADMIN')],
        loadComponent: () => import('./pages/form-review/form-review').then(m => m.FormReview),
      },
      {
        path: 'forms/view/:id',
        canActivate: [roleGuard('COUNSELOR', 'ADMIN', 'EMPLOYEE')],
        loadComponent: () => import('./pages/form-viewer/form-viewer').then(m => m.FormViewer),
      },
      {
        path: 'forms/update/:id',
        canActivate: [roleGuard('COUNSELOR', 'ADMIN')],
        loadComponent: () => import('./pages/form-update/form-update').then(m => m.FormUpdate),
      },
      {
        path: 'forms/publish/:id',
        canActivate: [roleGuard('COUNSELOR', 'ADMIN')],
        loadComponent: () => import('./pages/form-publish/form-publish').then(m => m.FormPublish),
      },
      {
        path: 'report-viewer/:reportId',
        canActivate: [roleGuard('COUNSELOR', 'MANAGER', 'ADMIN')],
        loadComponent: () => import('./pages/report-viewer/report-viewer').then(m => m.ReportViewer),
      },
      {
        path: 'employee-responses',
        canActivate: [roleGuard('COUNSELOR', 'ADMIN')],
        loadComponent: () => import('./pages/employee-responses/employee-responses').then(m => m.EmployeeResponses),
      },
      // Shared
      {
        path: 'settings',
        loadComponent: () => import('./pages/settings/settings').then(m => m.Settings),
      },
      { path: '', redirectTo: 'home', pathMatch: 'full' },
    ],
  },
];
