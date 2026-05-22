import { test as base, Page } from '@playwright/test';
import { LoginPage } from '../page-objects/login.page';
import { RegisterPage } from '../page-objects/register.page';
import { DashboardShellPage } from '../page-objects/dashboard-shell.page';
import { TeamOverviewPage } from '../page-objects/team-overview.page';
import { ManagerAssessmentsPage } from '../page-objects/manager-assessments.page';
import { DashboardHomePage } from '../page-objects/dashboard-home.page';
import { MyAssessmentsPage } from '../page-objects/my-assessments.page';
import { MyReportsPage } from '../page-objects/my-reports.page';
import { FormViewerPage } from '../page-objects/form-viewer.page';
import { MyTeamsPage } from '../page-objects/my-teams.page';
import { TeamAddPage } from '../page-objects/team-add.page';
import { FormBuilderPage } from '../page-objects/form-builder.page';
import { CounselorAssessmentsPage } from '../page-objects/counselor-assessments.page';
import { EmployeeResponsesPage } from '../page-objects/employee-responses.page';
import { SettingsPage } from '../page-objects/settings.page';
import { ReportHistoryPage } from '../page-objects/report-history.page';

interface Fixtures {
  loginPage: LoginPage;
  registerPage: RegisterPage;
  shell: DashboardShellPage;
  teamOverview: TeamOverviewPage;
  managerAssessments: ManagerAssessmentsPage;
  dashboardHome: DashboardHomePage;
  myAssessments: MyAssessmentsPage;
  myReports: MyReportsPage;
  formViewer: FormViewerPage;
  myTeams: MyTeamsPage;
  teamAdd: TeamAddPage;
  formBuilder: FormBuilderPage;
  counselorAssessments: CounselorAssessmentsPage;
  employeeResponses: EmployeeResponsesPage;
  settings: SettingsPage;
  reportHistory: ReportHistoryPage;
}

export const test = base.extend<Fixtures>({
  loginPage: async ({ page }: { page: Page }, use: (r: LoginPage) => Promise<void>) => {
    await use(new LoginPage(page));
  },
  registerPage: async ({ page }: { page: Page }, use: (r: RegisterPage) => Promise<void>) => {
    await use(new RegisterPage(page));
  },
  shell: async ({ page }: { page: Page }, use: (r: DashboardShellPage) => Promise<void>) => {
    await use(new DashboardShellPage(page));
  },
  teamOverview: async ({ page }: { page: Page }, use: (r: TeamOverviewPage) => Promise<void>) => {
    await use(new TeamOverviewPage(page));
  },
  managerAssessments: async ({ page }: { page: Page }, use: (r: ManagerAssessmentsPage) => Promise<void>) => {
    await use(new ManagerAssessmentsPage(page));
  },
  dashboardHome: async ({ page }: { page: Page }, use: (r: DashboardHomePage) => Promise<void>) => {
    await use(new DashboardHomePage(page));
  },
  myAssessments: async ({ page }: { page: Page }, use: (r: MyAssessmentsPage) => Promise<void>) => {
    await use(new MyAssessmentsPage(page));
  },
  myReports: async ({ page }: { page: Page }, use: (r: MyReportsPage) => Promise<void>) => {
    await use(new MyReportsPage(page));
  },
  formViewer: async ({ page }: { page: Page }, use: (r: FormViewerPage) => Promise<void>) => {
    await use(new FormViewerPage(page));
  },
  myTeams: async ({ page }: { page: Page }, use: (r: MyTeamsPage) => Promise<void>) => {
    await use(new MyTeamsPage(page));
  },
  teamAdd: async ({ page }: { page: Page }, use: (r: TeamAddPage) => Promise<void>) => {
    await use(new TeamAddPage(page));
  },
  formBuilder: async ({ page }: { page: Page }, use: (r: FormBuilderPage) => Promise<void>) => {
    await use(new FormBuilderPage(page));
  },
  counselorAssessments: async ({ page }: { page: Page }, use: (r: CounselorAssessmentsPage) => Promise<void>) => {
    await use(new CounselorAssessmentsPage(page));
  },
  employeeResponses: async ({ page }: { page: Page }, use: (r: EmployeeResponsesPage) => Promise<void>) => {
    await use(new EmployeeResponsesPage(page));
  },
  settings: async ({ page }: { page: Page }, use: (r: SettingsPage) => Promise<void>) => {
    await use(new SettingsPage(page));
  },
  reportHistory: async ({ page }: { page: Page }, use: (r: ReportHistoryPage) => Promise<void>) => {
    await use(new ReportHistoryPage(page));
  },
});

export { expect } from '@playwright/test';
