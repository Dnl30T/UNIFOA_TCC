import { Page, expect } from '@playwright/test';

export class TeamOverviewPage {
  constructor(private page: Page) {}

  async goto() {
    await this.page.goto('/dashboard/team-overview');
    await this.page.waitForLoadState('networkidle');
  }

  async expectHasTeam() {
    await expect(this.page.locator('.members-grid, [class*="member-card"]').first()).toBeVisible({ timeout: 10_000 });
  }

  async expectNoTeam() {
    await expect(this.page.locator('.no-team-state')).toBeVisible({ timeout: 10_000 });
  }

  async createTeam(name: string) {
    await this.page.locator('.no-team-state input[matinput], .create-team-form input').fill(name);
    await this.page.locator('button').filter({ hasText: /criar time/i }).click();
    await this.page.waitForLoadState('networkidle');
  }

  async expectTeamCodeBadge() {
    await expect(this.page.locator('.team-code-badge')).toBeVisible({ timeout: 10_000 });
  }

  async getTeamCode(): Promise<string> {
    const codeEl = this.page.locator('.team-code-value');
    await expect(codeEl).toBeVisible({ timeout: 8_000 });
    return await codeEl.textContent() ?? '';
  }

  async expectMemberCount(n: number) {
    await expect(this.page.locator('.member-card')).toHaveCount(n, { timeout: 10_000 });
  }

  async expectMemberName(name: string) {
    await expect(this.page.locator('.member-name').filter({ hasText: name })).toBeVisible({ timeout: 8_000 });
  }

  async expectCreateError() {
    await expect(this.page.locator('.create-error')).toBeVisible({ timeout: 5_000 });
  }

  async expectTitle() {
    await expect(this.page.locator('h2')).toContainText('Team Overview');
  }
}
