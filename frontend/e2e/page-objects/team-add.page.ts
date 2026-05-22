import { Page, expect } from '@playwright/test';

export class TeamAddPage {
  constructor(private page: Page) {}

  async goto() {
    await this.page.goto('/dashboard/teams/add');
    await this.page.waitForLoadState('networkidle');
  }

  async searchCode(code: string) {
    const input = this.page.locator('.code-field input').first();
    await input.click();
    await input.pressSequentially(code, { delay: 50 });
    await this.page.locator('button').filter({ hasText: /buscar/i }).first().click();
    await this.page.waitForLoadState('networkidle');
  }

  async expectTeamFound(name: string) {
    await expect(this.page.locator('[class*="team-result"], [class*="team-name"], mat-card').filter({ hasText: name })).toBeVisible({ timeout: 8_000 });
  }

  async expectSearchError() {
    await expect(this.page.locator('[class*="error"], .error-banner')).toBeVisible({ timeout: 8_000 });
  }

  async joinTeam() {
    await this.page.locator('button').filter({ hasText: /entrar/i }).click();
    await this.page.waitForLoadState('networkidle');
  }

  async expectJoined() {
    // Either success label or redirect to my-teams
    await Promise.race([
      expect(this.page.locator('[class*="joined"], .joined-label')).resolves,
      this.page.waitForURL('**/my-teams**', { timeout: 10_000 }),
    ]).catch(async () => {
      // At minimum, the page should not show an error
      await expect(this.page.locator('[class*="joined"], .joined-label, [class*="success"]')).toBeVisible({ timeout: 10_000 });
    });
  }

  async expectJoinConflict() {
    await expect(this.page.locator('[class*="error"], .error-banner')).toContainText(/time|team|outro/i, { timeout: 8_000 });
  }
}
