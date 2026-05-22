import { Page, expect } from '@playwright/test';

export class EmployeeResponsesPage {
  constructor(private page: Page) {}

  async goto() {
    await this.page.goto('/dashboard/employee-responses');
    await this.page.waitForLoadState('networkidle');
  }

  async expectTitle() {
    await expect(this.page.locator('h2, .page-title')).toContainText(/resposta|response/i, { timeout: 8_000 });
  }

  async expectEmptyOrRows() {
    const content = this.page.locator('[class*="empty"], table, mat-card, [class*="row"]');
    await expect(content.first()).toBeVisible({ timeout: 10_000 });
  }

  async expectResponseRow(name: string) {
    await expect(this.page.locator('td, [class*="row"]').filter({ hasText: name }).first()).toBeVisible({ timeout: 8_000 });
  }
}
