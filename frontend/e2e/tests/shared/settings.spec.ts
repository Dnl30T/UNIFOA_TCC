import { test, expect } from '@playwright/test';

// This spec is matched by the 'manager' project in playwright.config.ts
test.describe('Settings — Manager', () => {
  test('page loads with correct title', async ({ page }) => {
    await page.goto('/dashboard/settings');
    await page.waitForLoadState('networkidle');
    await expect(page.locator('h2, .page-title').first()).toBeVisible({ timeout: 8_000 });
  });

  test('settings page is accessible when authenticated', async ({ page }) => {
    await page.goto('/dashboard/settings');
    await page.waitForLoadState('networkidle');
    expect(page.url()).not.toContain('/auth/login');
  });

  test('settings page renders content without JS errors', async ({ page }) => {
    const errors: string[] = [];
    page.on('pageerror', (err) => errors.push(err.message));

    await page.goto('/dashboard/settings');
    await page.waitForLoadState('networkidle');

    const fatalErrors = errors.filter(e => !e.includes('ExpressionChangedAfterItHasBeenCheckedError'));
    expect(fatalErrors).toHaveLength(0);
  });

  test('Notifications tab does not exist', async ({ page }) => {
    await page.goto('/dashboard/settings');
    await page.waitForLoadState('networkidle');
    await expect(page.getByRole('tab', { name: /notifica/i })).toHaveCount(0);
  });

  test('Privacy tab shows only the anonymize toggle', async ({ page }) => {
    await page.route('**/users/me/privacy', async (route) => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ fullyAnonymized: false }),
        });
      } else {
        const body = JSON.parse(route.request().postData() ?? '{}');
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ fullyAnonymized: body.fullyAnonymized }),
        });
      }
    });

    await page.goto('/dashboard/settings');
    await page.waitForLoadState('networkidle');
    await page.getByRole('tab', { name: /privacidade/i }).click();
    await page.waitForLoadState('networkidle');

    const toggles = page.locator('.tab-content mat-slide-toggle');
    await expect(toggles).toHaveCount(1);
    await expect(toggles.first()).toContainText(/anonimizar completamente/i);
  });

  test('privacy preference is loaded from backend on init', async ({ page }) => {
    // Mock GET returning fullyAnonymized: true — toggle should render as checked
    await page.route('**/users/me/privacy', async (route) => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ fullyAnonymized: true }),
        });
      } else {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ fullyAnonymized: true }),
        });
      }
    });

    await page.goto('/dashboard/settings');
    await page.waitForLoadState('networkidle');
    await page.getByRole('tab', { name: /privacidade/i }).click();
    await page.waitForLoadState('networkidle');

    const toggleBtn = page
      .locator('mat-slide-toggle')
      .filter({ hasText: /anonimizar completamente/i })
      .locator('button[role="switch"]');
    await expect(toggleBtn).toHaveAttribute('aria-checked', 'true', { timeout: 8_000 });
  });
});
