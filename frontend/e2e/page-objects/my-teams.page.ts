import { Page, expect } from '@playwright/test';

export class MyTeamsPage {
  constructor(private page: Page) {}

  async goto() {
    await this.page.goto('/dashboard/my-teams');
    await this.page.waitForLoadState('networkidle');
  }

  async expectTitle() {
    await expect(this.page.locator('h2, .page-title')).toContainText(/team|time/i, { timeout: 8_000 });
  }

  async expectTeamCard(name: string) {
    await expect(this.page.locator('mat-card, [class*="team-card"]').filter({ hasText: name }).first()).toBeVisible({ timeout: 10_000 });
  }

  async expectEmptyOrCard() {
    const content = this.page.locator('[class*="empty"], mat-card');
    await expect(content.first()).toBeVisible({ timeout: 10_000 });
  }

  async openMembers(teamName: string) {
    const card = this.page.locator('mat-card, [class*="team-card"]').filter({ hasText: teamName });
    await card.locator('button').filter({ hasText: /member|membr/i }).click();
    // Wait for dialog
    await expect(this.page.locator('[class*="dialog"], mat-dialog-container, [role="dialog"]')).toBeVisible({ timeout: 8_000 });
  }

  async expectMemberInDialog(name: string) {
    await expect(
      this.page.locator('[class*="dialog"], mat-dialog-container').locator('*').filter({ hasText: name }).first()
    ).toBeVisible({ timeout: 8_000 });
  }

  async closeDialog() {
    const closeBtn = this.page.locator('[class*="dialog"] button').filter({ hasText: /fechar|close|✕|×/i });
    if (await closeBtn.count() > 0) {
      await closeBtn.first().click();
    } else {
      await this.page.keyboard.press('Escape');
    }
  }

  async navigateToTeamAdd() {
    await this.page.locator('a, button').filter({ hasText: /adicionar|add.*team|entrar/i }).first().click();
    await this.page.waitForURL('**/teams/add**', { timeout: 8_000 });
  }
}
