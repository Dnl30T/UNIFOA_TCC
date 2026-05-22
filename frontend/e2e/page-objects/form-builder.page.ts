import { Page, expect } from '@playwright/test';

export class FormBuilderPage {
  constructor(private page: Page) {}

  async goto() {
    await this.page.goto('/dashboard/forms/builder');
    await this.page.waitForLoadState('networkidle');
  }

  async expectPaletteVisible() {
    await expect(this.page.locator('aside.palette')).toBeVisible({ timeout: 8_000 });
  }

  async expectEmptyCanvas() {
    await expect(this.page.locator('.empty-canvas, [class*="empty-canvas"]')).toBeVisible({ timeout: 8_000 });
  }

  async addField(type: string) {
    // Click the palette button matching the type label
    const btn = this.page.locator('.palette-btn, [class*="palette"] button').filter({ hasText: new RegExp(type, 'i') });
    await btn.first().click();
    await this.page.waitForLoadState('networkidle');
  }

  async fillTitle(title: string) {
    const titleInput = this.page.locator('input[placeholder*="title" i], input[placeholder*="título" i]').first();
    await titleInput.fill(title);
  }

  async fillDescription(desc: string) {
    const descInput = this.page.locator('textarea[placeholder*="desc" i]').first();
    await descInput.fill(desc);
  }

  async expectFieldCount(n: number) {
    await expect(this.page.locator('.field-card, [class*="field-card"]')).toHaveCount(n, { timeout: 8_000 });
  }

  async saveAsDraft() {
    await this.page.locator('button').filter({ hasText: /save assessment/i }).click();
    await this.page.waitForLoadState('networkidle');
  }

  async expectCanvasHasCard() {
    await expect(this.page.locator('.field-card, [class*="field-card"]').first()).toBeVisible({ timeout: 8_000 });
  }
}
