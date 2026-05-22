import { Page, expect } from '@playwright/test';

export class FormViewerPage {
  constructor(private page: Page) {}

  async expectFormTitle() {
    await expect(this.page.locator('h2, .page-title, .form-title')).toBeVisible({ timeout: 10_000 });
  }

  async fillTextField(label: string, value: string) {
    const field = this.page.locator(`mat-form-field`).filter({ hasText: label }).locator('input, textarea');
    await field.fill(value);
  }

  async fillFirstTextField(value: string) {
    await this.page.locator('input[type="text"], textarea').first().fill(value);
  }

  async submit() {
    await this.page.locator('button[type="submit"], button').filter({ hasText: /enviar|submit/i }).click();
  }

  async expectSubmitSuccess() {
    // After submit, either toast/snack or redirect
    await expect(
      this.page.locator('[class*="snack"], [class*="toast"], [class*="success"], [role="alert"]').first()
    ).toBeVisible({ timeout: 10_000 });
  }
}
