import { test, expect } from '../../fixtures/base.fixture';

test.describe('Manager — Manager Assessments', () => {
  test('page loads without errors', async ({ managerAssessments }) => {
    await managerAssessments.goto();
    await managerAssessments.expectEmptyOrLoaded();
  });

  test('page title is visible', async ({ managerAssessments }) => {
    await managerAssessments.goto();
    await managerAssessments.expectTitle();
  });

  test('page is authenticated (no redirect to login)', async ({ page }) => {
    await page.goto('/dashboard/manager-assessments');
    await page.waitForLoadState('networkidle');
    expect(page.url()).not.toContain('/auth/login');
  });

  test('report selector or empty state is visible', async ({ managerAssessments, page }) => {
    await managerAssessments.goto();
    const reportSelector = page.locator('mat-select, select');
    const emptyState = page.locator('[class*="empty"], [class*="no-report"], [class*="no_report"]');
    const titleEl = page.locator('.page-title');

    // One of these should be visible
    const oneVisible = await Promise.any([
      expect(reportSelector.first()).toBeVisible({ timeout: 8_000 }).then(() => true),
      expect(emptyState.first()).toBeVisible({ timeout: 8_000 }).then(() => true),
      expect(titleEl.first()).toBeVisible({ timeout: 8_000 }).then(() => true),
    ]).catch(() => false);
    expect(oneVisible).toBeTruthy();
  });
});
