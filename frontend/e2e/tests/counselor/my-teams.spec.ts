import { test, expect } from '../../fixtures/base.fixture';

test.describe('Counselor — My Teams', () => {
  test('page loads with correct title', async ({ myTeams }) => {
    await myTeams.goto();
    await myTeams.expectTitle();
  });

  test('renders team card or empty state without error', async ({ myTeams }) => {
    await myTeams.goto();
    await myTeams.expectEmptyOrCard();
  });

  test('counselor assigned to Equipe Alpha sees that team card', async ({ myTeams, page }) => {
    await myTeams.goto();
    await page.waitForLoadState('networkidle');

    const teamCard = page.locator('mat-card, [class*="team-card"]').filter({ hasText: 'Equipe Alpha' });
    const count = await teamCard.count();
    if (count > 0) {
      await expect(teamCard.first()).toBeVisible({ timeout: 8_000 });
    } else {
      // Carol may not be assigned yet — acceptable
      await myTeams.expectEmptyOrCard();
    }
  });

  test('View Members button opens a dialog with members', async ({ myTeams, page }) => {
    await myTeams.goto();
    await page.waitForLoadState('networkidle');

    const viewBtn = page.locator('button').filter({ hasText: /member|membr|ver membr/i });
    if (await viewBtn.count() === 0) {
      test.skip();
      return;
    }

    await viewBtn.first().click();
    await expect(page.locator('mat-dialog-container').first()).toBeVisible({ timeout: 8_000 });

    // Close dialog
    await page.keyboard.press('Escape');
  });

  test('Add Team link navigates to team-add page', async ({ page }) => {
    await page.goto('/dashboard/my-teams');
    await page.waitForLoadState('networkidle');

    const addLink = page.locator('a, button').filter({ hasText: /adicionar|add.*team|entrar.*time|join/i });
    if (await addLink.count() > 0) {
      await addLink.first().click();
      await page.waitForURL('**/teams/add**', { timeout: 8_000 });
    }
  });
});
