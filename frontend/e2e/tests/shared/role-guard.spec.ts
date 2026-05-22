import { test, expect } from '@playwright/test';

// These tests run with NO storageState (unauthenticated project in playwright.config.ts)
test.describe('Role Guard — Unauthenticated Redirects', () => {
  test('visiting /dashboard redirects to /auth/login', async ({ page }) => {
    await page.goto('/dashboard');
    await page.waitForURL('**/auth/login**', { timeout: 10_000 });
    expect(page.url()).toContain('/auth/login');
  });

  test('visiting /dashboard/home redirects to /auth/login', async ({ page }) => {
    await page.goto('/dashboard/home');
    await page.waitForURL('**/auth/login**', { timeout: 10_000 });
    expect(page.url()).toContain('/auth/login');
  });

  test('visiting /dashboard/my-assessments redirects to /auth/login', async ({ page }) => {
    await page.goto('/dashboard/my-assessments');
    await page.waitForURL('**/auth/login**', { timeout: 10_000 });
    expect(page.url()).toContain('/auth/login');
  });

  test('visiting /dashboard/counselor-assessments redirects to /auth/login', async ({ page }) => {
    await page.goto('/dashboard/counselor-assessments');
    await page.waitForURL('**/auth/login**', { timeout: 10_000 });
    expect(page.url()).toContain('/auth/login');
  });

  test('visiting /dashboard/team-overview redirects to /auth/login', async ({ page }) => {
    await page.goto('/dashboard/team-overview');
    await page.waitForURL('**/auth/login**', { timeout: 10_000 });
    expect(page.url()).toContain('/auth/login');
  });

  test('visiting /dashboard/settings redirects to /auth/login', async ({ page }) => {
    await page.goto('/dashboard/settings');
    await page.waitForURL('**/auth/login**', { timeout: 10_000 });
    expect(page.url()).toContain('/auth/login');
  });

  test('login page is accessible without authentication', async ({ page }) => {
    await page.goto('/auth/login');
    await page.waitForLoadState('networkidle');
    // Should stay on login
    await expect(page.locator('form, [class*="login"]').first()).toBeVisible({ timeout: 8_000 });
    expect(page.url()).toContain('/auth/login');
  });

  test('register page is accessible without authentication', async ({ page }) => {
    await page.goto('/auth/register');
    await page.waitForLoadState('networkidle');
    await expect(page.locator('form, [class*="register"]').first()).toBeVisible({ timeout: 8_000 });
  });
});
