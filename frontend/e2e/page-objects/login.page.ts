import { Page, expect } from '@playwright/test';

export class LoginPage {
  constructor(private page: Page) {}

  async goto() {
    await this.page.goto('/auth/login');
  }

  async fillEmail(email: string) {
    await this.page.locator('input[formcontrolname="email"]').fill(email);
  }

  async fillPassword(password: string) {
    await this.page.locator('input[formcontrolname="password"]').fill(password);
  }

  async submit() {
    await this.page.locator('button[type="submit"]').click();
  }

  async login(email: string, password: string) {
    await this.goto();
    await this.fillEmail(email);
    await this.fillPassword(password);
    await this.submit();
  }

  async expectError(text?: string) {
    const error = this.page.locator('.error-message, [class*="error"], mat-error, [role="alert"]');
    await expect(error.first()).toBeVisible();
    if (text) {
      await expect(error.first()).toContainText(text);
    }
  }

  async expectRedirectToDashboard() {
    await this.page.waitForURL('**/dashboard**', { timeout: 10_000 });
  }

  async expectRedirectToAssessments() {
    await this.page.waitForURL('**/my-assessments**', { timeout: 10_000 });
  }
}
