import { Page, expect } from '@playwright/test';

export class DashboardShellPage {
  constructor(private page: Page) {}

  async navigateTo(label: string) {
    await this.page.locator(`nav a, .nav-link, [class*="nav-item"]`).filter({ hasText: label }).click();
    await this.page.waitForLoadState('networkidle');
  }

  async logout() {
    // Try sidebar logout button or menu
    const logoutBtn = this.page.locator('button, a').filter({ hasText: /logout|sair/i });
    if (await logoutBtn.count() > 0) {
      await logoutBtn.first().click();
    } else {
      // Try avatar/profile menu
      await this.page.locator('[class*="avatar"], [class*="user-menu"], [class*="profile"]').first().click();
      await this.page.locator('button, a').filter({ hasText: /logout|sair/i }).first().click();
    }
    await this.page.waitForURL('**/auth/login**', { timeout: 10_000 });
  }

  async expectSidebarVisible() {
    await expect(this.page.locator('nav, aside, [class*="sidebar"]').first()).toBeVisible();
  }

  async expectUrl(path: string) {
    await expect(this.page).toHaveURL(new RegExp(path));
  }
}
