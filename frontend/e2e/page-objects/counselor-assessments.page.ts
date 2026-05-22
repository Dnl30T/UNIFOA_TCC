import { Page, expect } from '@playwright/test';

export class CounselorAssessmentsPage {
  constructor(private page: Page) {}

  async goto() {
    await this.page.goto('/dashboard/counselor-assessments');
    await this.page.waitForLoadState('networkidle');
  }

  async expectTitle() {
    await expect(this.page.locator('h2, .page-title')).toContainText(/assessment|avalia/i, { timeout: 8_000 });
  }

  async expectEmptyOrCards() {
    const content = this.page.locator('[class*="empty"], [class*="no-"], .form-card, mat-card');
    await expect(content.first()).toBeVisible({ timeout: 10_000 });
  }

  async expectFormCard(title: string) {
    await expect(this.page.locator('.form-card, mat-card').filter({ hasText: title }).first()).toBeVisible({ timeout: 8_000 });
  }

  async filterByStatus(status: 'DRAFT' | 'PUBLISHED' | 'CLOSED') {
    const labels: Record<string, string> = { DRAFT: /rascunho|draft/i, PUBLISHED: /publicad|published/i, CLOSED: /encerrad|closed/i };
    const label = labels[status];
    const filterBtn = this.page.locator('button, mat-button-toggle').filter({ hasText: label as unknown as string });
    await filterBtn.first().click();
    await this.page.waitForLoadState('networkidle');
  }

  async searchByTitle(title: string) {
    await this.page.locator('input[placeholder*="search" i], input[placeholder*="buscar" i], input[type="search"]').first().fill(title);
    await this.page.waitForLoadState('networkidle');
  }

  async clickPublish(title: string) {
    const card = this.page.locator('.form-card, mat-card').filter({ hasText: title });
    await card.locator('button').filter({ hasText: /publish|publicar/i }).click();
    await this.page.waitForURL('**/forms/publish/**', { timeout: 8_000 });
  }

  async deleteForm(title: string) {
    const card = this.page.locator('.form-card, mat-card').filter({ hasText: title });
    await card.locator('button').filter({ hasText: /delete|excluir|remover/i }).click();
    // Confirm dialog if present
    const confirmBtn = this.page.locator('button').filter({ hasText: /confirm|sim|yes/i });
    if (await confirmBtn.count() > 0) {
      await confirmBtn.first().click();
    }
    await this.page.waitForLoadState('networkidle');
  }
}
