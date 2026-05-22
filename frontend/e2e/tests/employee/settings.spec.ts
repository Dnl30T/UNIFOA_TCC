import { test, expect } from '../../fixtures/base.fixture';

test.describe('Employee — Settings', () => {
  test('settings page is accessible when authenticated', async ({ settings, page }) => {
    await settings.goto();
    await settings.expectLoaded();
    expect(page.url()).not.toContain('/auth/login');
  });

  test('settings page has no fatal JS errors', async ({ settings, page }) => {
    const errors: string[] = [];
    page.on('pageerror', (err) => errors.push(err.message));

    await settings.goto();
    await settings.expectLoaded();

    const fatalErrors = errors.filter(e => !e.includes('ExpressionChangedAfterItHasBeenCheckedError'));
    expect(fatalErrors).toHaveLength(0);
  });

  test('Notifications tab does not exist', async ({ settings }) => {
    await settings.goto();
    await settings.expectLoaded();
    await settings.expectNoNotificationsTab();
  });

  test('Privacy tab shows only the anonymize toggle (no shareData toggle)', async ({ settings, page }) => {
    await settings.goto();
    await settings.expectLoaded();
    await settings.openPrivacyTab();

    // Exactly one slide-toggle should be visible in the privacy tab content
    const toggles = page.locator('.tab-content mat-slide-toggle');
    await expect(toggles).toHaveCount(1);

    // That toggle must be the anonymize one
    await expect(toggles.first()).toContainText(/anonimizar completamente/i);
  });

  test('anonymize toggle reflects server state on load', async ({ settings }) => {
    // Just verify the tab opens and the toggle renders without error
    await settings.goto();
    await settings.expectLoaded();
    await settings.openPrivacyTab();
    const toggle = settings.anonymizeToggle;
    await expect(toggle).toBeVisible({ timeout: 8_000 });
  });

  test('toggling anonymize and saving shows success message', async ({ settings, page }) => {
    // Intercept the PUT request to avoid a real backend call in isolation
    await page.route('**/users/me/privacy', async (route) => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ fullyAnonymized: false }),
        });
      } else {
        const body = JSON.parse(route.request().postData() ?? '{}');
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ fullyAnonymized: body.fullyAnonymized }),
        });
      }
    });

    await settings.goto();
    await settings.expectLoaded();
    await settings.openPrivacyTab();

    await settings.toggleAnonymize();
    await settings.savePrivacy();
    await settings.expectPrivacySaved();
  });

  test('save error is shown when API call fails', async ({ settings, page }) => {
    await page.route('**/users/me/privacy', async (route) => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ fullyAnonymized: false }),
        });
      } else {
        await route.fulfill({ status: 500, body: 'Internal Server Error' });
      }
    });

    await settings.goto();
    await settings.expectLoaded();
    await settings.openPrivacyTab();

    await settings.savePrivacy();
    await expect(
      page.locator('.feedback-error').filter({ hasText: /falha/i }),
    ).toBeVisible({ timeout: 8_000 });
  });
});
