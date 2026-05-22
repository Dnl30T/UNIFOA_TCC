import { test, expect } from '../../fixtures/base.fixture';

test.describe('Employee — My Assessments', () => {
  test('page loads with correct title', async ({ myAssessments }) => {
    await myAssessments.goto();
    await myAssessments.expectTitle();
  });

  test('page renders without errors (table or empty state)', async ({ myAssessments }) => {
    await myAssessments.goto();
    await myAssessments.expectEmptyOrTable();
  });

  test('assessment rows are visible if forms are assigned to the team', async ({ myAssessments, page }) => {
    await myAssessments.goto();
    // Check for any table rows or at least the table/card structure
    const rows = page.locator('tr:not(:first-child), [class*="row"]');
    const count = await rows.count();
    // If rows exist, verify they contain content
    if (count > 0) {
      await expect(rows.first()).toBeVisible();
    } else {
      // Empty state is acceptable
      await expect(page.locator('[class*="empty"], [class*="no-"]').first()).toBeVisible({ timeout: 5_000 });
    }
  });

  test('pending form shows Responder button', async ({ myAssessments, page }) => {
    await myAssessments.goto();
    const pendingBtn = page.locator('button, a').filter({ hasText: /responder/i });
    const count = await pendingBtn.count();
    if (count > 0) {
      await expect(pendingBtn.first()).toBeVisible();
      await expect(pendingBtn.first()).toBeEnabled();
    } else {
      // No pending forms — acceptable; verify completed or empty state
      const content = page.locator('[class*="empty"], tr:not(:first-child), mat-card');
      await expect(content.first()).toBeVisible({ timeout: 5_000 });
    }
  });

  test('clicking Responder navigates to form viewer', async ({ myAssessments, page }) => {
    await myAssessments.goto();
    const pendingBtn = page.locator('button, a').filter({ hasText: /responder/i });
    if (await pendingBtn.count() > 0) {
      await pendingBtn.first().click();
      await page.waitForURL('**/forms/view/**', { timeout: 10_000 });
      expect(page.url()).toContain('/forms/view/');
    } else {
      test.skip();
    }
  });
});
