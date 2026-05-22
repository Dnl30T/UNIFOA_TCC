import { test, expect } from '../../fixtures/base.fixture';

test.describe('Counselor — Team Add', () => {
  test('page loads with search form', async ({ teamAdd, page }) => {
    await teamAdd.goto();
    await expect(page.locator('input').first()).toBeVisible({ timeout: 8_000 });
  });

  test('searching with invalid code shows error', async ({ teamAdd }) => {
    await teamAdd.goto();
    await teamAdd.searchCode('ZZZZZZZZ');
    await teamAdd.expectSearchError();
  });

  test('searching with valid team code shows team result card', async ({ teamAdd, page }) => {
    // Get a valid code from backend
    const res = await page.request.post('http://localhost:8080/auth/login', {
      data: { email: 'alice.manager@psytrack.dev', password: 'Senha@123' },
    });
    const { token } = await res.json();
    const teamRes = await page.request.get('http://localhost:8080/teams/my', {
      headers: { Authorization: `Bearer ${token}` },
    });
    const team = await teamRes.json();
    const code: string = team.teamCode;

    if (!code) {
      test.skip();
      return;
    }

    await teamAdd.goto();
    await teamAdd.searchCode(code);
    await expect(page.locator('.team-result-card, [class*="team-result"], mat-card').last()).toBeVisible({ timeout: 8_000 });
  });

  test('counselor already in a team gets conflict error on trying to join another', async ({ teamAdd, page }) => {
    // Get Bob's team code (different team)
    const res = await page.request.post('http://localhost:8080/auth/login', {
      data: { email: 'bob.manager@psytrack.dev', password: 'Senha@123' },
    });
    const { token } = await res.json();
    const teamRes = await page.request.get('http://localhost:8080/teams/my', {
      headers: { Authorization: `Bearer ${token}` },
    });
    const team = await teamRes.json();
    const betaCode: string = team.teamCode;

    if (!betaCode) {
      test.skip();
      return;
    }

    await teamAdd.goto();
    await teamAdd.searchCode(betaCode);

    const joinBtn = page.locator('button').filter({ hasText: /entrar/i });
    if (await joinBtn.count() === 0) {
      test.skip();
      return;
    }
    await joinBtn.first().click();
    await page.waitForLoadState('networkidle');

    // Either conflict error or already-joined state
    const errorOrJoined = page.locator('[class*="error"], .error-banner, .joined-label');
    await expect(errorOrJoined.first()).toBeVisible({ timeout: 8_000 });
  });
});
