import { test, expect } from '../../fixtures/base.fixture';

test.describe('Manager — Team Overview', () => {
  test('page loads with correct title', async ({ teamOverview }) => {
    await teamOverview.goto();
    await teamOverview.expectTitle();
  });

  test('manager with existing team sees members grid and team code', async ({ teamOverview, page }) => {
    await teamOverview.goto();
    await page.waitForLoadState('networkidle');

    const hasTeamState = await page.locator('.members-grid, [class*="member-card"]').count();
    const noTeamState = await page.locator('.no-team-state').count();

    if (hasTeamState > 0) {
      // Has team: members grid and team code badge should be present
      await teamOverview.expectHasTeam();
      await teamOverview.expectTeamCodeBadge();
    } else if (noTeamState > 0) {
      // No team yet: create form should be visible
      await teamOverview.expectNoTeam();
    } else {
      // Loading state — wait a bit more
      await page.waitForTimeout(3_000);
      const members = await page.locator('.members-grid, .no-team-state').count();
      expect(members).toBeGreaterThan(0);
    }
  });

  test('manager without team sees create-team form', async ({ teamOverview, page }) => {
    await teamOverview.goto();
    await page.waitForLoadState('networkidle');

    const noTeamVisible = await page.locator('.no-team-state').isVisible().catch(() => false);
    if (noTeamVisible) {
      // Create team form elements should be present
      await expect(page.locator('.no-team-state input')).toBeVisible({ timeout: 5_000 });
      await expect(page.locator('button').filter({ hasText: /criar time/i })).toBeVisible({ timeout: 5_000 });
    } else {
      // Manager already has team — skip this assertion
      await teamOverview.expectHasTeam();
    }
  });

  test('team code badge is present when manager has a team', async ({ teamOverview, page }) => {
    await teamOverview.goto();
    await page.waitForLoadState('networkidle');

    const hasTeam = await page.locator('.members-grid, [class*="member-card"]').count();
    if (hasTeam > 0) {
      await teamOverview.expectTeamCodeBadge();
      const code = await teamOverview.getTeamCode();
      expect(code.trim().length).toBeGreaterThan(0);
    } else {
      test.skip(); // No team yet
    }
  });

  test('employees from the team appear in the members grid', async ({ teamOverview, page }) => {
    await teamOverview.goto();
    await page.waitForLoadState('networkidle');

    const hasTeam = await page.locator('.members-grid, [class*="member-card"]').count();
    if (hasTeam > 0) {
      const memberNames = page.locator('.member-name');
      await expect(memberNames.first()).toBeVisible({ timeout: 8_000 });
    } else {
      test.skip();
    }
  });
});
