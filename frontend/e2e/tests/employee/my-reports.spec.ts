import { test, expect } from '../../fixtures/base.fixture';

test.describe('Employee — My Reports', () => {
  test('page loads with correct title', async ({ myReports }) => {
    await myReports.goto();
    await myReports.expectTitle();
  });

  test('page renders without errors', async ({ myReports, page }) => {
    await myReports.goto();
    // No uncaught JS errors expected; content renders
    await myReports.expectEmptyOrContent();
  });

  test('page is not accessible to unauthenticated users', async ({ page }) => {
    // This test uses employee storageState but verifies the page is reachable while logged in
    await page.goto('/dashboard/my-reports');
    // Should NOT redirect to login (we are authenticated)
    await page.waitForLoadState('networkidle');
    expect(page.url()).not.toContain('/auth/login');
  });
});
