import { Page, expect } from '@playwright/test';

export class RegisterPage {
  constructor(private page: Page) {}

  async goto() {
    await this.page.goto('/auth/register');
  }

  async selectRole(role: 'employee' | 'manager' | 'counselor') {
    await this.page.getByRole('radio', { name: new RegExp(role, 'i') }).click();
  }

  async fillFullName(name: string) {
    await this.page.locator('input[formcontrolname="fullName"], input[placeholder*="Name" i]').fill(name);
  }

  async fillEmail(email: string) {
    await this.page.locator('input[formcontrolname="email"], input[type="email"]').fill(email);
  }

  async fillPassword(password: string) {
    await this.page.locator('input[formcontrolname="password"], input[type="password"]').fill(password);
  }

  async fillTeamCode(code: string) {
    await this.page.locator('input[formcontrolname="teamCode"], input[placeholder*="code" i]').fill(code);
  }

  async acceptPolicy() {
    const checkbox = this.page.locator('mat-checkbox input[type="checkbox"], input[formcontrolname="agreedToPolicy"]');
    if (!(await checkbox.isChecked())) {
      await this.page.locator('mat-checkbox, label:has(input[formcontrolname="agreedToPolicy"])').click();
    }
  }

  async submit() {
    await this.page.locator('button[type="submit"]').click();
  }

  async registerEmployee(opts: { name: string; email: string; password: string; teamCode: string }) {
    await this.goto();
    await this.selectRole('employee');
    await this.fillFullName(opts.name);
    await this.fillEmail(opts.email);
    await this.fillPassword(opts.password);
    await this.fillTeamCode(opts.teamCode);
    await this.acceptPolicy();
    await this.submit();
  }

  async registerManager(opts: { name: string; email: string; password: string }) {
    await this.goto();
    await this.selectRole('manager');
    await this.fillFullName(opts.name);
    await this.fillEmail(opts.email);
    await this.fillPassword(opts.password);
    await this.acceptPolicy();
    await this.submit();
  }

  async registerCounselor(opts: { name: string; email: string; password: string }) {
    await this.goto();
    await this.selectRole('counselor');
    await this.fillFullName(opts.name);
    await this.fillEmail(opts.email);
    await this.fillPassword(opts.password);
    await this.acceptPolicy();
    await this.submit();
  }

  async expectError(text?: string) {
    const error = this.page.locator('mat-error, [class*="error"], [role="alert"]');
    await expect(error.first()).toBeVisible();
    if (text) {
      await expect(this.page.locator('body')).toContainText(text);
    }
  }

  async expectRedirectToDashboard() {
    await this.page.waitForURL('**/dashboard**', { timeout: 15_000 });
  }
}
