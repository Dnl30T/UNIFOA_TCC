import { test, expect } from '../../fixtures/base.fixture';

test.describe('Manager — Dashboard Home', () => {
  test('page loads and renders content', async ({ dashboardHome }) => {
    await dashboardHome.goto();
    await dashboardHome.expectTitle();
  });

  test('stat cards are visible', async ({ dashboardHome, page }) => {
    await dashboardHome.goto();
    // At least one stat/metric card rendered
    const cards = page.locator('mat-card, [class*="stat"], [class*="card"]');
    await expect(cards.first()).toBeVisible({ timeout: 10_000 });
  });

  test('charts or visualizations render', async ({ dashboardHome }) => {
    await dashboardHome.goto();
    await dashboardHome.expectChartOrCard();
  });

  test('manager does not see a team-filter dropdown on dashboard', async ({ dashboardHome, page }) => {
    await dashboardHome.goto();
    // Team filter dropdown is counselor-specific — manager should not see it
    const teamFilter = page.locator('mat-select, select').filter({ hasText: /time|team/i });
    // It either doesn't exist or is hidden
    const count = await teamFilter.count();
    if (count > 0) {
      for (let i = 0; i < count; i++) {
        await expect(teamFilter.nth(i)).not.toBeVisible();
      }
    }
    // If count === 0 the assertion already passes
  });

  test('page is authenticated (no redirect to login)', async ({ page }) => {
    await page.goto('/dashboard/home');
    await page.waitForLoadState('networkidle');
    expect(page.url()).not.toContain('/auth/login');
  });
});
