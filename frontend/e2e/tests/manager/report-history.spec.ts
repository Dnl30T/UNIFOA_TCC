import { test, expect } from '../../fixtures/base.fixture';

test.describe('Manager — Report History', () => {
  test('page loads with correct title', async ({ reportHistory }) => {
    await reportHistory.goto();
    await reportHistory.expectTitle();
  });

  test('page renders empty state or list of reports', async ({ reportHistory }) => {
    await reportHistory.goto();
    await reportHistory.expectEmptyOrContent();
  });

  test('page is authenticated (no redirect to login)', async ({ page }) => {
    await page.goto('/dashboard/report-history');
    await page.waitForLoadState('networkidle');
    expect(page.url()).not.toContain('/auth/login');
  });
});
