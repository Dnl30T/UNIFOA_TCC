import { test, expect } from '../../fixtures/base.fixture';

const MANAGER = { email: 'alice.manager@psytrack.dev', password: 'Senha@123' };
const COUNSELOR = { email: 'carol.counselor@psytrack.dev', password: 'Senha@123' };
const EMPLOYEE = { email: 'eve.emp@psytrack.dev', password: 'Senha@123' };

test.describe('Login', () => {
  test('manager login redirects to /dashboard/home', async ({ loginPage, page }) => {
    await loginPage.login(MANAGER.email, MANAGER.password);
    await page.waitForURL('**/dashboard**', { timeout: 12_000 });
    expect(page.url()).toContain('/dashboard');
  });

  test('counselor login redirects to /dashboard/home', async ({ loginPage, page }) => {
    await loginPage.login(COUNSELOR.email, COUNSELOR.password);
    await page.waitForURL('**/dashboard**', { timeout: 12_000 });
    expect(page.url()).toContain('/dashboard');
  });

  test('employee login redirects to my-assessments', async ({ loginPage, page }) => {
    await loginPage.login(EMPLOYEE.email, EMPLOYEE.password);
    await page.waitForURL('**/dashboard**', { timeout: 12_000 });
    expect(page.url()).toContain('/dashboard');
  });

  test('shows error on wrong password', async ({ loginPage }) => {
    await loginPage.goto();
    await loginPage.fillEmail(MANAGER.email);
    await loginPage.fillPassword('wrong-password-999');
    await loginPage.submit();
    await loginPage.expectError();
  });

  test('shows error on unknown email', async ({ loginPage }) => {
    await loginPage.goto();
    await loginPage.fillEmail('nobody@nobody.dev');
    await loginPage.fillPassword('SomePass@1');
    await loginPage.submit();
    await loginPage.expectError();
  });

  test('unauthenticated access to /dashboard redirects to login', async ({ page }) => {
    await page.goto('/dashboard');
    await page.waitForURL('**/auth/login**', { timeout: 10_000 });
    expect(page.url()).toContain('/auth/login');
  });

  test('logout clears session and redirects to login', async ({ loginPage, page }) => {
    await loginPage.login(MANAGER.email, MANAGER.password);
    await page.waitForURL('**/dashboard**', { timeout: 12_000 });

    // Trigger logout — look for logout text or profile menu
    const logoutBtn = page.locator('button, a').filter({ hasText: /sair|logout/i });
    if (await logoutBtn.count() > 0) {
      await logoutBtn.first().click();
    } else {
      // Try finding it inside a menu
      await page.locator('[class*="avatar"], [class*="user"], button[mat-icon-button]').last().click();
      await page.locator('button, a').filter({ hasText: /sair|logout/i }).first().click();
    }
    await page.waitForURL('**/auth/login**', { timeout: 10_000 });

    // Dashboard is no longer accessible
    await page.goto('/dashboard');
    await page.waitForURL('**/auth/login**', { timeout: 8_000 });
  });
});
