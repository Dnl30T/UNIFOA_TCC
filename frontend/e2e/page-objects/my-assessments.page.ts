import { Page, expect } from '@playwright/test';

export class MyAssessmentsPage {
  constructor(private page: Page) {}

  async goto() {
    await this.page.goto('/dashboard/my-assessments');
    await this.page.waitForLoadState('networkidle');
  }

  async expectTitle() {
    await expect(this.page.locator('h2, .page-title')).toContainText(/Assessment|Avalia/i, { timeout: 8_000 });
  }

  async expectEmptyOrTable() {
    const content = this.page.locator('table, [class*="empty"], [class*="no-"], mat-card');
    await expect(content.first()).toBeVisible({ timeout: 10_000 });
  }

  async expectFormRow(title: string) {
    await expect(this.page.locator('td, [class*="row"]').filter({ hasText: title }).first()).toBeVisible({ timeout: 8_000 });
  }

  async clickRespond(title: string) {
    const row = this.page.locator('tr, [class*="row"]').filter({ hasText: title });
    await row.locator('button, a').filter({ hasText: /responder/i }).click();
    await this.page.waitForURL('**/forms/view/**', { timeout: 10_000 });
  }

  async expectPendingButton(title: string) {
    const row = this.page.locator('tr, [class*="row"]').filter({ hasText: title });
    await expect(row.locator('button, a').filter({ hasText: /responder/i })).toBeVisible({ timeout: 8_000 });
  }

  async expectCompletedStatus(title: string) {
    const row = this.page.locator('tr, [class*="row"]').filter({ hasText: title });
    await expect(row.locator('[class*="chip"], [class*="status"], span').filter({ hasText: /respondid|complet/i })).toBeVisible({ timeout: 8_000 });
  }
}
