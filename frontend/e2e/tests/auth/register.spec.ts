import { test, expect } from '../../fixtures/base.fixture';

// Generate unique suffix to avoid duplicate email conflicts across runs
const ts = () => Date.now();

test.describe('Registration', () => {
  test('register employee with valid team code succeeds', async ({ registerPage, page }) => {
    // We need a valid team code — fetch it from the running backend
    const res = await page.request.post('http://localhost:8080/auth/login', {
      data: { email: 'alice.manager@psytrack.dev', password: 'Senha@123' },
    });
    const { token } = await res.json();
    const teamRes = await page.request.get('http://localhost:8080/teams/my', {
      headers: { Authorization: `Bearer ${token}` },
    });
    const team = await teamRes.json();
    const teamCode: string = team.teamCode ?? 'AAAAAAAA';

    const unique = ts();
    await registerPage.registerEmployee({
      name: `Test Emp ${unique}`,
      email: `test.emp.${unique}@psytrack.dev`,
      password: 'Senha@123',
      teamCode,
    });
    await page.waitForURL('**/dashboard**', { timeout: 15_000 });
    expect(page.url()).toContain('/dashboard');
  });

  test('register employee with invalid team code shows error', async ({ registerPage, page }) => {
    const unique = ts();
    await registerPage.goto();
    await registerPage.selectRole('employee');
    await registerPage.fillFullName(`Bad Code Emp ${unique}`);
    await registerPage.fillEmail(`bad.code.${unique}@psytrack.dev`);
    await registerPage.fillPassword('Senha@123');
    await registerPage.fillTeamCode('ZZZZZZZZ');
    await registerPage.acceptPolicy();
    await registerPage.submit();
    await registerPage.expectError();
  });

  test('register manager succeeds', async ({ registerPage, page }) => {
    const unique = ts();
    await registerPage.registerManager({
      name: `Test Manager ${unique}`,
      email: `test.mgr.${unique}@psytrack.dev`,
      password: 'Senha@123',
    });
    await page.waitForURL('**/dashboard**', { timeout: 15_000 });
    expect(page.url()).toContain('/dashboard');
  });

  test('register counselor succeeds', async ({ registerPage, page }) => {
    const unique = ts();
    await registerPage.registerCounselor({
      name: `Test Counselor ${unique}`,
      email: `test.csl.${unique}@psytrack.dev`,
      password: 'Senha@123',
    });
    await page.waitForURL('**/dashboard**', { timeout: 15_000 });
    expect(page.url()).toContain('/dashboard');
  });

  test('register with duplicate email shows conflict error', async ({ registerPage, page }) => {
    // alice already exists
    await registerPage.registerManager({
      name: 'Alice Again',
      email: 'alice.manager@psytrack.dev',
      password: 'Senha@123',
    });
    await registerPage.expectError();
  });
});
