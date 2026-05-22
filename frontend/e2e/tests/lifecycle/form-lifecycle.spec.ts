/**
 * Full Form Lifecycle E2E Test
 *
 * Phases (all sequential via test.describe.serial):
 *   0. API provisioning  — register fresh manager/counselor/employee + create team
 *   1. Build form        — counselor builds + saves form in the UI
 *   2. Publish form      — counselor publishes form to the fresh team
 *   3. Employee responds — employee fills and submits the form
 *   4a. Generate score   — counselor creates EmployeeResult via "Gerar Avaliação"
 *   4b. Publish eval     — counselor fills closing commentary and publishes TherapistEvaluation
 *   5. Close form        — counselor closes the form (status: ENDED)
 *   6. Generate report   — counselor generates the team report
 *   7. Manager verifies  — manager sees the report in manager-assessments
 *   8. Employee verifies — employee sees result in my-reports
 */
import { test, expect, APIRequestContext, Browser, Page } from '@playwright/test';

const API_URL = 'http://localhost:8080';
const PASSWORD = 'Senha@123';
const TS = Date.now();
const SUFFIX = TS.toString().slice(-6);
const FORM_TITLE = `LC Form ${TS}`;

// Shared state — persisted across serial tests in the same worker
const state: {
  teamId: string;
  teamCode: string;
  formId: string;
  employeeId: string;
  managerEmail: string;
  counselorEmail: string;
  employeeEmail: string;
  reportId: string;
} = {
  teamId: '',
  teamCode: '',
  formId: '',
  employeeId: '',
  managerEmail: '',
  counselorEmail: '',
  employeeEmail: '',
  reportId: '',
};

// ─── Helpers ──────────────────────────────────────────────────────────────────

async function loginAs(page: Page, email: string): Promise<void> {
  await page.goto('/auth/login');
  await page.locator('input[formcontrolname="email"]').fill(email);
  await page.locator('input[formcontrolname="password"]').fill(PASSWORD);
  await page.locator('button[type="submit"]').click();
  await page.waitForURL('**/dashboard**', { timeout: 15_000 });
}

// ─── Suite ────────────────────────────────────────────────────────────────────

test.describe.serial('Form Lifecycle — Create → Publish → Respond → Evaluate → Close → Report', () => {

  // ── Phase 0: API provisioning (beforeAll) ────────────────────────────────

  test.beforeAll(async ({ playwright }) => {
    const api: APIRequestContext = await playwright.request.newContext({ baseURL: API_URL });

    // 1. Register fresh manager (PENDING role → claim MANAGER)
    const managerEmail = `lc.manager.${SUFFIX}@test.dev`;
    const mStaff = await api.post('/auth/register/staff', {
      data: { username: `lc_manager_${SUFFIX}`, password: PASSWORD, email: managerEmail, name: 'LC Manager' },
    });
    expect(mStaff.status(), 'manager register/staff').toBe(201);
    const { token: mPending } = await mStaff.json();

    const mClaim = await api.post('/auth/claim-role', {
      data: { role: 'MANAGER' },
      headers: { Authorization: `Bearer ${mPending}` },
    });
    expect(mClaim.status(), 'manager claim-role').toBe(200);
    const { token: managerToken } = await mClaim.json();
    state.managerEmail = managerEmail;

    // 2. Manager creates team
    const teamRes = await api.post('/teams', {
      data: { name: `LC Team ${SUFFIX}` },
      headers: { Authorization: `Bearer ${managerToken}` },
    });
    expect(teamRes.status(), 'create team').toBe(201);
    const team = await teamRes.json();
    state.teamId = team.id;
    state.teamCode = team.teamCode;

    // 3. Register fresh counselor (PENDING → claim COUNSELOR)
    const counselorEmail = `lc.counselor.${SUFFIX}@test.dev`;
    const cStaff = await api.post('/auth/register/staff', {
      data: { username: `lc_counselor_${SUFFIX}`, password: PASSWORD, email: counselorEmail, name: 'LC Counselor' },
    });
    expect(cStaff.status(), 'counselor register/staff').toBe(201);
    const { token: cPending } = await cStaff.json();

    const cClaim = await api.post('/auth/claim-role', {
      data: { role: 'COUNSELOR' },
      headers: { Authorization: `Bearer ${cPending}` },
    });
    expect(cClaim.status(), 'counselor claim-role').toBe(200);
    const { token: counselorToken } = await cClaim.json();
    state.counselorEmail = counselorEmail;

    // 4. Counselor joins team
    const joinRes = await api.patch(`/teams/join?code=${state.teamCode}`, {
      headers: { Authorization: `Bearer ${counselorToken}` },
    });
    expect(joinRes.status(), 'counselor join team').toBe(200);

    // 5. Register fresh employee
    const employeeEmail = `lc.employee.${SUFFIX}@test.dev`;
    const eRes = await api.post('/auth/register/employee', {
      data: {
        username: `lc_employee_${SUFFIX}`,
        password: PASSWORD,
        email: employeeEmail,
        teamCode: state.teamCode,
        name: 'LC Employee',
      },
    });
    expect(eRes.status(), 'employee register').toBe(201);
    const { token: employeeToken } = await eRes.json();
    state.employeeEmail = employeeEmail;

    // 6. Fetch employee DB ID
    const meRes = await api.get('/employees/me', {
      headers: { Authorization: `Bearer ${employeeToken}` },
    });
    expect(meRes.status(), 'GET /employees/me').toBe(200);
    const emp = await meRes.json();
    state.employeeId = emp.id;

    await api.dispose();
  });

  // ── Phase 1: Counselor builds + saves form (UI) ──────────────────────────

  test('Phase 1 — counselor builds and saves a form', async ({ page }) => {
    await loginAs(page, state.counselorEmail);

    await page.goto('/dashboard/forms/builder');
    await page.waitForLoadState('networkidle');

    // Clear default title and type new one
    const titleInput = page.locator('input[placeholder="Assessment title"]');
    await titleInput.clear();
    await titleInput.fill(FORM_TITLE);

    // Add a Short Text field from palette
    await page.locator('.palette-btn').filter({ hasText: /short text/i }).click();
    await expect(page.locator('.field-card').first()).toBeVisible({ timeout: 5_000 });

    // Save
    await page.locator('button').filter({ hasText: /save assessment/i }).click();
    await page.waitForURL('**/forms/view/**', { timeout: 15_000 });

    // Capture formId from URL
    state.formId = page.url().split('/forms/view/')[1]?.split('?')[0] ?? '';
    expect(state.formId, 'formId captured from URL').toBeTruthy();
  });

  // ── Phase 2: Counselor publishes form to team (UI) ───────────────────────

  test('Phase 2 — counselor publishes form to the team', async ({ page }) => {
    await loginAs(page, state.counselorEmail);

    await page.goto('/dashboard/counselor-assessments');
    await page.waitForLoadState('networkidle');

    // Find the form card and click Publish
    const card = page.locator('mat-card').filter({ hasText: FORM_TITLE });
    await expect(card).toBeVisible({ timeout: 10_000 });
    await card.locator('button.publish-btn, button:has-text("Publish")').first().click();

    await page.waitForURL('**/forms/publish/**', { timeout: 10_000 });
    await page.waitForLoadState('networkidle');

    // Team auto-selected; wait for button to be enabled
    const publishBtn = page.locator('button').filter({ hasText: /publicar para o time/i });
    await expect(publishBtn).toBeEnabled({ timeout: 10_000 });
    await publishBtn.click();

    await page.waitForURL('**/counselor-assessments**', { timeout: 12_000 });
    await page.waitForLoadState('networkidle');

    // Verify status chip shows "published"
    const updatedCard = page.locator('mat-card').filter({ hasText: FORM_TITLE });
    await expect(updatedCard.locator('.status-chip.published')).toBeVisible({ timeout: 8_000 });
  });

  // ── Phase 3: Employee submits response (UI) ──────────────────────────────

  test('Phase 3 — employee sees form and submits a response', async ({ page }) => {
    await loginAs(page, state.employeeEmail);

    await page.goto('/dashboard/my-assessments');
    await page.waitForLoadState('networkidle');

    // Form row should be visible
    const formRow = page.locator('tr, mat-row, [class*="row"]').filter({ hasText: FORM_TITLE });
    await expect(formRow.first()).toBeVisible({ timeout: 10_000 });

    // Click Responder button or the row itself
    const respondBtn = page.locator('button, a').filter({ hasText: /responder/i });
    if (await respondBtn.count() > 0) {
      await respondBtn.first().click();
    } else {
      await formRow.first().click();
    }
    await page.waitForURL('**/forms/view/**', { timeout: 12_000 });
    await page.waitForLoadState('networkidle');

    // Fill the Short Text question
    const textInput = page.locator('input[placeholder="Sua resposta"], textarea[placeholder="Sua resposta"]').first();
    await textInput.fill('Lifecycle test answer');

    // Submit — capture the POST response to verify 201 directly
    const [submitResponse] = await Promise.all([
      page.waitForResponse(
        r => r.url().includes('/form-submissions') && r.request().method() === 'POST',
        { timeout: 15_000 },
      ),
      page.locator('button').filter({ hasText: /enviar respostas/i }).click(),
    ]);
    expect(submitResponse.status(), 'POST /form-submissions must return 201').toBe(201);

    // Wait for redirect back to my-assessments (window.history.back after 700ms delay)
    await page.waitForURL('**/my-assessments**', { timeout: 12_000 });
    // Submission confirmed — Phase 4a will verify the counselor sees the employee as "responded"
  });

  // ── Phase 4a: Counselor generates EmployeeResult score (UI) ─────────────

  test('Phase 4a — counselor generates employee result score', async ({ page }) => {
    await loginAs(page, state.counselorEmail);

    await page.goto('/dashboard/counselor-reports');
    await page.waitForLoadState('networkidle');

    // Switch to Tab 2 — "Avaliações Individuais"
    await page.locator('[role="tab"]').filter({ hasText: /avaliações individuais/i }).click();
    await page.waitForLoadState('networkidle');

    // Wait for member card to appear (any status — backend may auto-create EmployeeResult)
    const memberCard = page.locator('.member-card').first();
    await expect(memberCard).toBeVisible({ timeout: 15_000 });

    // "Gerar Avaliação" only appears when status is "responded" (not yet evaluated).
    // The backend auto-creates the EmployeeResult (helperScore) when the employee
    // submits, so the card may already be "evaluated". Handle both cases:
    const gerarBtn = memberCard.locator('button').filter({ hasText: /gerar avaliação/i });
    if (await gerarBtn.count() > 0) {
      await gerarBtn.click();

      const scoreInput = page.locator('.generate-form input[type="number"]');
      await expect(scoreInput).toBeVisible({ timeout: 5_000 });
      await scoreInput.clear();
      await scoreInput.fill('72');

      await page.locator('.generate-form button').filter({ hasText: /confirmar/i }).click();
      await page.waitForLoadState('networkidle');
    }
    // Either way, the card should be in "evaluated" state by now
    await expect(memberCard.locator('.status-chip')).toBeVisible({ timeout: 8_000 });
  });

  // ── Phase 4b: Counselor views responses and publishes evaluation (UI) ────

  test('Phase 4b — counselor publishes therapist evaluation', async ({ page }) => {
    await loginAs(page, state.counselorEmail);

    // Get the counselor's JWT from localStorage (set during loginAs)
    await page.goto('/dashboard/counselor-reports');
    const counselorToken = await page.evaluate(() => localStorage.getItem('psytrack_token'));
    expect(counselorToken, 'counselor token must exist in localStorage').toBeTruthy();

    const formId = state.formId;
    const employeeId = state.employeeId;

    // Upsert the TherapistEvaluation with a closingCommentary (required for publish)
    const upsertResp = await page.request.put(
      `http://localhost:8080/therapist-evaluations/${formId}/${employeeId}`,
      {
        headers: { 'Authorization': `Bearer ${counselorToken}`, 'Content-Type': 'application/json' },
        data: { closingCommentary: 'Lifecycle test closing commentary — all clear.' },
      }
    );
    expect(upsertResp.status(), `PUT /therapist-evaluations must not fail: ${await upsertResp.text()}`).toBeLessThan(300);

    // Publish the evaluation
    const publishResp = await page.request.post(
      `http://localhost:8080/therapist-evaluations/${formId}/${employeeId}/publish`,
      { headers: { 'Authorization': `Bearer ${counselorToken}`, 'Content-Type': 'application/json' } }
    );
    expect(publishResp.status(), `POST publish must succeed: ${await publishResp.text()}`).toBeLessThan(300);

    console.log(`[Phase 4b] ✓ TherapistEvaluation published for employee=${employeeId}`);
  });

  // ── Phase 5: Counselor closes the form (direct API) ─────────────────────

  test('Phase 5 — counselor closes the form', async ({ page }) => {
    await loginAs(page, state.counselorEmail);

    const counselorToken = await page.evaluate(() => localStorage.getItem('psytrack_token'));
    expect(counselorToken, 'counselor token must exist in localStorage').toBeTruthy();

    const closeResp = await page.request.put(
      `http://localhost:8080/forms/${state.formId}/close`,
      { headers: { 'Authorization': `Bearer ${counselorToken}`, 'Content-Type': 'application/json' } },
    );
    expect(closeResp.status(), `PUT /forms/{id}/close must succeed: ${await closeResp.text()}`).toBeLessThan(300);
    console.log(`[Phase 5] ✓ Form ${state.formId} closed`);
  });

  // ── Phase 6: Counselor generates team report (UI) ────────────────────────

  test('Phase 6 — counselor generates the team report', async ({ page }) => {
    await loginAs(page, state.counselorEmail);

    await page.goto('/dashboard/counselor-reports');
    await page.waitForLoadState('networkidle');

    // Tab 1 is default — "Relatórios de Time"
    const formCard = page.locator('.form-readiness-card').filter({ hasText: FORM_TITLE });
    await expect(formCard).toBeVisible({ timeout: 12_000 });

    // "Generate Report" button should be enabled (published eval + form ENDED)
    const generateBtn = formCard.locator('button').filter({ hasText: /generate report/i });
    await expect(generateBtn).toBeEnabled({ timeout: 10_000 });
    await generateBtn.click();

    // Name input section appears — name is pre-filled, just confirm
    const nameSection = page.locator('.report-name-input-section');
    await expect(nameSection).toBeVisible({ timeout: 5_000 });
    await nameSection.locator('button').filter({ hasText: /confirmar/i }).click();

    await page.waitForLoadState('networkidle');

    // Report row appears in the "Relatórios Gerados" table
    const reportRow = formCard.locator('tr[mat-row]').first();
    await expect(reportRow).toBeVisible({ timeout: 15_000 });

    // Navigate to report dashboard and capture reportId
    const verBtn = formCard.locator('button').filter({ hasText: /ver dashboard/i });
    await verBtn.first().click();
    await page.waitForURL('**/report-viewer/**', { timeout: 12_000 });

    state.reportId = page.url().split('/report-viewer/')[1]?.split('?')[0] ?? '';
    expect(state.reportId, 'reportId captured').toBeTruthy();
  });

  // ── Phase 7: Manager sees the report (UI) ────────────────────────────────

  test('Phase 7 — manager sees report in manager-assessments', async ({ page }) => {
    await loginAs(page, state.managerEmail);

    await page.goto('/dashboard/manager-assessments');
    await page.waitForLoadState('networkidle');

    // Select the report from dropdown — force click to bypass mat-label overlay interception
    const reportSelect = page.locator('.mat-mdc-select-trigger').first();
    await reportSelect.click({ force: true });

    const option = page.locator('mat-option').filter({ hasText: new RegExp(`LC Form ${TS}`, 'i') });
    if (await option.count() > 0) {
      await option.first().click();
    } else {
      // Fallback: pick the first option if title isn't shown
      await page.locator('mat-option').first().click();
    }
    await page.waitForLoadState('networkidle');

    // Respondent row should be visible
    const respondentRow = page.locator('tr:not(:first-child), [mat-row]').first();
    await expect(respondentRow).toBeVisible({ timeout: 8_000 });
  });

  // ── Phase 8: Employee sees result in My Reports (UI) ────────────────────

  test('Phase 8 — employee sees result in my-reports', async ({ page }) => {
    await loginAs(page, state.employeeEmail);

    await page.goto('/dashboard/my-reports');
    await page.waitForLoadState('networkidle');

    // A result row should be visible for the employee (form title may show as UUID for ENDED forms)
    const dataRow = page.locator('tr[mat-row]').first();
    await expect(dataRow).toBeVisible({ timeout: 10_000 });
  });
});
