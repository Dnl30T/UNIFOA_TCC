import { test, expect } from '../../fixtures/base.fixture';

test.describe('Employee — Form Viewer', () => {
  test('form viewer page renders when navigating to a valid form URL', async ({ page }) => {
    // First, get a valid form ID from the backend using the employee token stored in auth state
    await page.goto('/dashboard/my-assessments');
    await page.waitForLoadState('networkidle');

    const respondBtn = page.locator('button, a').filter({ hasText: /responder/i });
    if (await respondBtn.count() === 0) {
      test.skip(); // No pending forms — skip
      return;
    }

    await respondBtn.first().click();
    await page.waitForURL('**/forms/view/**', { timeout: 10_000 });
    await page.waitForLoadState('networkidle');

    // Form title or some content should be visible
    const content = page.locator('h2, .page-title, .form-title, mat-card');
    await expect(content.first()).toBeVisible({ timeout: 10_000 });
  });

  test('form viewer has a submit button', async ({ page }) => {
    await page.goto('/dashboard/my-assessments');
    await page.waitForLoadState('networkidle');

    const respondBtn = page.locator('button, a').filter({ hasText: /responder/i });
    if (await respondBtn.count() === 0) {
      test.skip();
      return;
    }

    await respondBtn.first().click();
    await page.waitForURL('**/forms/view/**', { timeout: 10_000 });
    await page.waitForLoadState('networkidle');

    const submitBtn = page.locator('button').filter({ hasText: /enviar|submit/i });
    await expect(submitBtn.first()).toBeVisible({ timeout: 8_000 });
  });
});
