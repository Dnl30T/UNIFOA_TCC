import { Page, expect } from '@playwright/test';

export class SettingsPage {
  constructor(private page: Page) {}

  async goto() {
    await this.page.goto('/dashboard/settings');
    await this.page.waitForLoadState('networkidle');
  }

  async expectLoaded() {
    await expect(this.page.locator('h2, .page-title')).toContainText(/setting|configura/i, { timeout: 8_000 });
  }

  async expectNoError() {
    const errorEl = this.page.locator('[class*="error"], .error');
    const count = await errorEl.count();
    for (let i = 0; i < count; i++) {
      await expect(errorEl.nth(i)).not.toBeVisible();
    }
  }

  /** Open the Privacidade & LGPD tab */
  async openPrivacyTab() {
    await this.page.getByRole('tab', { name: /privacidade/i }).click();
    await this.page.waitForLoadState('networkidle');
  }

  /** Returns the slide-toggle for "Anonimizar Completamente" */
  get anonymizeToggle() {
    return this.page.locator('mat-slide-toggle').filter({ hasText: /anonimizar completamente/i });
  }

  /** Returns the checked state of the anonymize toggle */
  async isAnonymizeEnabled(): Promise<boolean> {
    const toggle = this.anonymizeToggle;
    const checked = await toggle.locator('button[role="switch"]').getAttribute('aria-checked');
    return checked === 'true';
  }

  /** Click the anonymize toggle */
  async toggleAnonymize() {
    await this.anonymizeToggle.locator('button[role="switch"]').click();
  }

  /** Click the save button in the privacy tab */
  async savePrivacy() {
    await this.page.getByRole('button', { name: /salvar prefer/i }).last().click();
  }

  /** Wait for the privacy success feedback message */
  async expectPrivacySaved() {
    await expect(
      this.page.locator('.feedback-success').filter({ hasText: /privacidade/i }),
    ).toBeVisible({ timeout: 8_000 });
  }

  /** Expect the Notifications tab to NOT exist */
  async expectNoNotificationsTab() {
    await expect(this.page.getByRole('tab', { name: /notifica/i })).toHaveCount(0);
  }
}
