import { Page, expect } from '@playwright/test';

export class DashboardHomePage {
  constructor(private page: Page) {}

  async goto() {
    await this.page.goto('/dashboard/home');
    await this.page.waitForLoadState('networkidle');
  }

  async expectStatCards() {
    // At least one stat card is visible
    const cards = this.page.locator('[class*="stat-card"], [class*="stat_card"], .card, mat-card');
    await expect(cards.first()).toBeVisible({ timeout: 10_000 });
  }

  async expectTitle() {
    await expect(this.page.locator('h2, .page-title')).toBeVisible({ timeout: 8_000 });
  }

  async expectTeamFilterVisible() {
    await expect(this.page.locator('mat-select, select').filter({ hasText: /time|team/i }).first()).toBeVisible({ timeout: 8_000 });
  }

  async expectNoTeamFilter() {
    const teamFilter = this.page.locator('mat-select, select').filter({ hasText: /time|team/i });
    await expect(teamFilter).toHaveCount(0);
  }

  async expectChartOrCard() {
    // Page has rendered some meaningful content (charts or stat cards)
    await expect(this.page.locator('canvas, svg, mat-card').first()).toBeVisible({ timeout: 10_000 });
  }
}
