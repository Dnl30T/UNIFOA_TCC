import { Page, expect } from '@playwright/test';

export class MyReportsPage {
  constructor(private page: Page) {}

  async goto() {
    await this.page.goto('/dashboard/my-reports');
    await this.page.waitForLoadState('networkidle');
  }

  async expectTitle() {
    await expect(this.page.locator('h2, .page-title')).toContainText(/report|resultado|relatório/i, { timeout: 8_000 });
  }

  async expectEmptyOrContent() {
    const content = this.page.locator('[class*="empty"], [class*="no-"], mat-card, table');
    await expect(content.first()).toBeVisible({ timeout: 10_000 });
  }
}
