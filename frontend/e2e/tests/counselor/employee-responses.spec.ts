import { test, expect } from '../../fixtures/base.fixture';

test.describe('Counselor — Employee Responses', () => {
  test('page loads with correct title', async ({ employeeResponses }) => {
    await employeeResponses.goto();
    await employeeResponses.expectTitle();
  });

  test('page renders rows or empty state', async ({ employeeResponses }) => {
    await employeeResponses.goto();
    await employeeResponses.expectEmptyOrRows();
  });

  test('page is authenticated (no redirect to login)', async ({ page }) => {
    await page.goto('/dashboard/employee-responses');
    await page.waitForLoadState('networkidle');
    expect(page.url()).not.toContain('/auth/login');
  });
});
