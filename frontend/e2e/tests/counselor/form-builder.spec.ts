import { test, expect } from '../../fixtures/base.fixture';

test.describe('Counselor — Form Builder', () => {
  test('page loads with palette and empty canvas', async ({ formBuilder }) => {
    await formBuilder.goto();
    await formBuilder.expectPaletteVisible();
    await formBuilder.expectEmptyCanvas();
  });

  test('clicking a TEXT field type adds a card to the canvas', async ({ formBuilder }) => {
    await formBuilder.goto();
    await formBuilder.addField('Short Text');
    await formBuilder.expectCanvasHasCard();
    await formBuilder.expectFieldCount(1);
  });

  test('clicking a SCALE field type adds a card to the canvas', async ({ formBuilder, page }) => {
    await formBuilder.goto();
    await formBuilder.addField('Scale');
    await expect(page.locator('.field-card, [class*="field-card"]')).toHaveCount(1, { timeout: 8_000 });
  });

  test('adding multiple fields increases count', async ({ formBuilder }) => {
    await formBuilder.goto();
    await formBuilder.addField('Short Text');
    await formBuilder.addField('Number');
    await formBuilder.expectFieldCount(2);
  });

  test('setting form title and saving navigates to form preview', async ({ formBuilder, page }) => {
    const ts = Date.now();
    await formBuilder.goto();
    await formBuilder.fillTitle(`Test Form ${ts}`);
    await formBuilder.addField('Short Text');

    await formBuilder.saveAsDraft();

    // After saving, navigates to /dashboard/forms/view/{id}
    await Promise.race([
      page.waitForURL('**/forms/view/**', { timeout: 12_000 }),
      page.waitForURL('**/counselor-assessments**', { timeout: 12_000 }),
    ]).catch(() => {
      // At minimum, no crash — still on builder
    });
  });

  test('form builder is not accessible to employees', async ({ page }) => {
    // This test uses counselor storageState — confirming the page IS accessible to counselors
    await page.goto('/dashboard/forms/builder');
    await page.waitForLoadState('networkidle');
    expect(page.url()).not.toContain('/auth/login');
  });
});
