import { Page, expect } from '@playwright/test';

export class ReportHistoryPage {
  constructor(private page: Page) {}

  async goto() {
    await this.page.goto('/dashboard/report-history');
    await this.page.waitForLoadState('networkidle');
  }

  async expectTitle() {
    await expect(this.page.locator('h2, .page-title')).toContainText(/history|histór|report|relatório/i, { timeout: 8_000 });
  }

  async expectEmptyOrContent() {
    const content = this.page.locator('[class*="empty"], table, mat-card, [class*="row"]');
    await expect(content.first()).toBeVisible({ timeout: 10_000 });
  }
}
