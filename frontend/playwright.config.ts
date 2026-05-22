import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './e2e/tests',
  fullyParallel: false,
  forbidOnly: !!process.env['CI'],
  retries: process.env['CI'] ? 1 : 0,
  workers: 1,
  reporter: [['html', { outputFolder: 'playwright-report' }], ['list']],
  globalSetup: './e2e/global-setup.ts',

  use: {
    baseURL: 'http://localhost:4200',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    actionTimeout: 10_000,
    navigationTimeout: 15_000,
  },

  projects: [
    // Unauthenticated — no storageState
    {
      name: 'unauthenticated',
      use: { ...devices['Desktop Chrome'] },
      testMatch: '**/auth/**/*.spec.ts',
    },
    // Role-guarded redirect tests
    {
      name: 'role-guard',
      use: { ...devices['Desktop Chrome'] },
      testMatch: '**/shared/role-guard.spec.ts',
    },
    // Manager project
    {
      name: 'manager',
      use: {
        ...devices['Desktop Chrome'],
        storageState: 'e2e/.auth/manager.json',
      },
      testMatch: ['**/manager/**/*.spec.ts', '**/shared/settings.spec.ts'],
    },
    // Counselor project
    {
      name: 'counselor',
      use: {
        ...devices['Desktop Chrome'],
        storageState: 'e2e/.auth/counselor.json',
      },
      testMatch: '**/counselor/**/*.spec.ts',
    },
    // Employee project
    {
      name: 'employee',
      use: {
        ...devices['Desktop Chrome'],
        storageState: 'e2e/.auth/employee.json',
      },
      testMatch: '**/employee/**/*.spec.ts',
    },
    // Full lifecycle — no default storageState; each test manages its own auth
    {
      name: 'lifecycle',
      use: { ...devices['Desktop Chrome'] },
      testMatch: '**/lifecycle/**/*.spec.ts',
    },
  ],

  webServer: {
    command: '/home/dnl/.local/share/nvm/v24.15.0/bin/ng serve',
    url: 'http://localhost:4200',
    reuseExistingServer: true,
    timeout: 120_000,
    cwd: process.cwd(),
  },
});
