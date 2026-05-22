import { Page, expect } from '@playwright/test';

export class ManagerAssessmentsPage {
  constructor(private page: Page) {}

  async goto() {
    await this.page.goto('/dashboard/manager-assessments');
    await this.page.waitForLoadState('networkidle');
  }

  async expectTitle() {
    await expect(this.page.locator('h2, .page-title')).toContainText(/Assessment|Avalia/i);
  }

  async expectEmptyOrLoaded() {
    // Either a report selector or an empty state is visible — page rendered
    const selector = this.page.locator('mat-select, select, [class*="empty"], .page-title');
    await expect(selector.first()).toBeVisible({ timeout: 10_000 });
  }

  async expectMemberRow(name: string) {
    await expect(this.page.locator('td, [class*="member"]').filter({ hasText: name }).first()).toBeVisible({ timeout: 8_000 });
  }

  async expectTableVisible() {
    await expect(this.page.locator('table, [class*="table"]').first()).toBeVisible({ timeout: 10_000 });
  }
}
