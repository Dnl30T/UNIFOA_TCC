import { Component, signal, computed, inject } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive, Router } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDividerModule } from '@angular/material/divider';
import { RoleService, UserRole } from '../../core/services/role.service';
import { AuthService } from '../../core/services/auth.service';

interface NavItem {
  label: string;
  icon: string;
  route: string;
}

const NAV_CONFIG: Record<UserRole, NavItem[]> = {
  employee: [
    { label: 'Minhas Avaliações', icon: 'assignment',     route: '/dashboard/my-assessments' },
    { label: 'Meus Relatórios',    icon: 'bar_chart',      route: '/dashboard/my-reports' },
    { label: 'Configurações',       icon: 'settings',       route: '/dashboard/settings' },
  ],
  manager: [
    { label: 'Dashboard',          icon: 'dashboard',      route: '/dashboard/home' },
    { label: 'Minhas Avaliações', icon: 'assignment',     route: '/dashboard/manager-assessments' },
    { label: 'Visão do Time',      icon: 'groups',         route: '/dashboard/team-overview' },
    { label: 'Histórico de Relatórios', icon: 'history',   route: '/dashboard/report-history' },
    { label: 'Configurações',       icon: 'settings',       route: '/dashboard/settings' },
  ],
  counselor: [
    { label: 'Dashboard',          icon: 'dashboard',      route: '/dashboard/home' },
    { label: 'Minhas Avaliações', icon: 'assignment',     route: '/dashboard/counselor-assessments' },
    { label: 'Meus Relatórios',    icon: 'bar_chart',      route: '/dashboard/counselor-reports' },
    { label: 'Meus Times',         icon: 'groups',         route: '/dashboard/my-teams' },
    { label: 'Editor de Formulários', icon: 'build',       route: '/dashboard/forms/builder' },
    { label: 'Configurações',       icon: 'settings',       route: '/dashboard/settings' },
  ],
};

@Component({
  selector: 'app-dashboard-shell',
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatIconModule,
    MatButtonModule,
    MatTooltipModule,
    MatDividerModule,
  ],
  templateUrl: './dashboard-shell.html',
  styleUrl: './dashboard-shell.scss',
})
export class DashboardShell {
  private roleService = inject(RoleService);
  private authService = inject(AuthService);
  private router = inject(Router);

  sidebarOpen = signal(true);

  navItems = computed<NavItem[]>(() => NAV_CONFIG[this.roleService.role()]);
  username = this.authService.username;

  toggleSidebar() {
    this.sidebarOpen.set(!this.sidebarOpen());
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/auth/login']);
  }
}
