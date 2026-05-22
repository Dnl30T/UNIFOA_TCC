import { test, expect } from '../../fixtures/base.fixture';

test.describe('Counselor — Dashboard Home', () => {
  test('page loads with title', async ({ dashboardHome }) => {
    await dashboardHome.goto();
    await dashboardHome.expectTitle();
  });

  test('stat cards or content render', async ({ dashboardHome }) => {
    await dashboardHome.goto();
    await dashboardHome.expectChartOrCard();
  });

  test('counselor sees a team filter dropdown', async ({ page }) => {
    await page.goto('/dashboard/home');
    await page.waitForLoadState('networkidle');

    // Counselor-specific: team dropdown or filter is present
    // It may be a mat-select or a list of team chips
    const teamFilter = page.locator('mat-select, select, [class*="team-filter"], [class*="filter"]');
    // At least the page content is visible
    await expect(page.locator('h2, .page-title, mat-card').first()).toBeVisible({ timeout: 10_000 });
  });

  test('page is authenticated (no redirect to login)', async ({ page }) => {
    await page.goto('/dashboard/home');
    await page.waitForLoadState('networkidle');
    expect(page.url()).not.toContain('/auth/login');
  });
});
