/**
 * Multi-Respondent Form Lifecycle E2E Test
 *
 * Variante do lifecycle principal que testa o sistema com 3 funcionários
 * respondendo ao mesmo formulário, seguido de batch-publish das avaliações.
 *
 * Phases (all sequential via test.describe.serial):
 *   0. API provisioning  — manager + counselor + 3 employees, 1 team
 *   1. Build form        — counselor builds + saves form (UI)
 *   2. Publish form      — counselor publishes form to the team (UI)
 *   3. Employees respond — all 3 employees submit the form via API
 *   4. Counselor batch-publishes evaluations (UI — checkbox + "Publish Selected")
 *   5. Close form        — counselor closes the form (API)
 *   6. Generate report   — counselor generates the team report (UI)
 *   7. Manager verifies  — manager sees the report with 3 respondents
 *   8. Employees verify  — each employee sees their result in My Reports
 */
import { test, expect, APIRequestContext, Page } from '@playwright/test';

const API_URL = 'http://localhost:8080';
const PASSWORD = 'Senha@123';
const TS = Date.now();
const SUFFIX = TS.toString().slice(-6);
const FORM_TITLE = `LC Multi Form ${TS}`;
const EMPLOYEE_COUNT = 3;

// ─── Shared state ─────────────────────────────────────────────────────────────

const state: {
  teamId: string;
  teamCode: string;
  formId: string;
  managerEmail: string;
  counselorEmail: string;
  employees: Array<{ email: string; id: string }>;
} = {
  teamId: '',
  teamCode: '',
  formId: '',
  managerEmail: '',
  counselorEmail: '',
  employees: [],
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

test.describe.serial(`Multi-Respondent Lifecycle — ${EMPLOYEE_COUNT} employees + batch publish`, () => {

  // ── Phase 0: API provisioning ────────────────────────────────────────────

  test.beforeAll(async ({ playwright }) => {
    const api: APIRequestContext = await playwright.request.newContext({ baseURL: API_URL });

    // 1. Register manager
    const managerEmail = `lc.mgr.${SUFFIX}@test.dev`;
    const mStaff = await api.post('/auth/register/staff', {
      data: { username: `lc_mgr_${SUFFIX}`, password: PASSWORD, email: managerEmail, name: 'LC Manager Multi' },
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
      data: { name: `LC Multi Team ${SUFFIX}` },
      headers: { Authorization: `Bearer ${managerToken}` },
    });
    expect(teamRes.status(), 'create team').toBe(201);
    const team = await teamRes.json();
    state.teamId = team.id;
    state.teamCode = team.teamCode;

    // 3. Register counselor
    const counselorEmail = `lc.csl.${SUFFIX}@test.dev`;
    const cStaff = await api.post('/auth/register/staff', {
      data: { username: `lc_csl_${SUFFIX}`, password: PASSWORD, email: counselorEmail, name: 'LC Counselor Multi' },
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

    // 5. Register 3 employees
    for (let i = 1; i <= EMPLOYEE_COUNT; i++) {
      const employeeEmail = `lc.emp${i}.${SUFFIX}@test.dev`;
      const eRes = await api.post('/auth/register/employee', {
        data: {
          username: `lc_emp${i}_${SUFFIX}`,
          password: PASSWORD,
          email: employeeEmail,
          teamCode: state.teamCode,
          name: `LC Employee ${i}`,
        },
      });
      expect(eRes.status(), `employee ${i} register`).toBe(201);
      const { token: employeeToken } = await eRes.json();

      const meRes = await api.get('/employees/me', {
        headers: { Authorization: `Bearer ${employeeToken}` },
      });
      expect(meRes.status(), `GET /employees/me employee ${i}`).toBe(200);
      const emp = await meRes.json();
      state.employees.push({ email: employeeEmail, id: emp.id });
    }

    expect(state.employees.length, '3 employees registered').toBe(EMPLOYEE_COUNT);
    await api.dispose();
  });

  // ── Phase 1: Counselor builds form (UI) ──────────────────────────────────

  test('Phase 1 — counselor builds and saves a form', async ({ page }) => {
    await loginAs(page, state.counselorEmail);

    await page.goto('/dashboard/forms/builder');
    await page.waitForLoadState('networkidle');

    const titleInput = page.locator('input[placeholder="Assessment title"]');
    await titleInput.clear();
    await titleInput.fill(FORM_TITLE);

    // Add a numeric (scale) question from the palette
    await page.locator('.palette-btn').filter({ hasText: /short text/i }).click();
    await expect(page.locator('.field-card').first()).toBeVisible({ timeout: 5_000 });

    await page.locator('button').filter({ hasText: /save assessment/i }).click();
    await page.waitForURL('**/forms/view/**', { timeout: 15_000 });

    state.formId = page.url().split('/forms/view/')[1]?.split('?')[0] ?? '';
    expect(state.formId, 'formId captured from URL').toBeTruthy();
    console.log(`[Phase 1] formId=${state.formId}`);
  });

  // ── Phase 2: Counselor publishes form to team (UI) ───────────────────────

  test('Phase 2 — counselor publishes form to the team', async ({ page }) => {
    await loginAs(page, state.counselorEmail);

    await page.goto('/dashboard/counselor-assessments');
    await page.waitForLoadState('networkidle');

    const card = page.locator('mat-card').filter({ hasText: FORM_TITLE });
    await expect(card).toBeVisible({ timeout: 10_000 });
    await card.locator('button.publish-btn, button:has-text("Publish")').first().click();

    await page.waitForURL('**/forms/publish/**', { timeout: 10_000 });
    await page.waitForLoadState('networkidle');

    const publishBtn = page.locator('button').filter({ hasText: /publicar para o time/i });
    await expect(publishBtn).toBeEnabled({ timeout: 10_000 });
    await publishBtn.click();

    await page.waitForURL('**/counselor-assessments**', { timeout: 12_000 });
    const updatedCard = page.locator('mat-card').filter({ hasText: FORM_TITLE });
    await expect(updatedCard.locator('.status-chip.published')).toBeVisible({ timeout: 8_000 });
    console.log('[Phase 2] Form published');
  });

  // ── Phase 3: All 3 employees submit responses (API) ──────────────────────

  test('Phase 3 — all 3 employees submit their responses', async ({ page }) => {
    // Login as each employee sequentially and submit via page.request
    for (let i = 0; i < EMPLOYEE_COUNT; i++) {
      const emp = state.employees[i];
      await loginAs(page, emp.email);

      const token = await page.evaluate(() => localStorage.getItem('psytrack_token'));
      expect(token, `employee ${i + 1} token`).toBeTruthy();

      // Fetch the form to get the question IDs
      const formResp = await page.request.get(`${API_URL}/forms/${state.formId}`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      expect(formResp.status(), `GET /forms/${state.formId}`).toBeLessThan(300);
      const form = await formResp.json();
      const questions: Array<{ id: string; type: string }> = form.questions ?? [];

      // Build answers list — TEXT questions get textValue, others get numeric value
      const answers = questions.map((q: { id: string; type: string }) =>
        q.type === 'TEXT' || q.type === 'LONG_TEXT'
          ? { questionId: q.id, textValue: `Resposta do funcionário ${i + 1} — lifecycle multi test` }
          : { questionId: q.id, value: 60 + i * 5 },
      );

      const submitResp = await page.request.post(`${API_URL}/form-submissions`, {
        headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
        data: {
          formId: state.formId,
          employeeId: emp.id,
          answers,
        },
      });
      expect(
        submitResp.status(),
        `POST /form-submissions employee ${i + 1}: ${await submitResp.text()}`,
      ).toBe(201);
      console.log(`[Phase 3] Employee ${i + 1} (${emp.id}) submitted`);
    }
  });

  // ── Phase 4: Counselor batch-publishes evaluations (UI) ──────────────────

  test('Phase 4 — counselor batch-publishes all 3 evaluations', async ({ page }) => {
    await loginAs(page, state.counselorEmail);

    const token = await page.evaluate(() => localStorage.getItem('psytrack_token'));
    expect(token, 'counselor token').toBeTruthy();

    // Upsert a TherapistEvaluation (with closingCommentary) for each employee via API —
    // the batch-publish UI requires the draft to exist before publishing.
    for (const emp of state.employees) {
      const upsertResp = await page.request.put(
        `${API_URL}/therapist-evaluations/${state.formId}/${emp.id}`,
        {
          headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
          data: { closingCommentary: `Multi-test evaluation for employee ${emp.id}` },
        },
      );
      expect(
        upsertResp.status(),
        `PUT /therapist-evaluations employee ${emp.id}: ${await upsertResp.text()}`,
      ).toBeLessThan(300);
    }
    console.log('[Phase 4] All evaluations upserted, opening UI for batch publish');

    // Navigate to Avaliações Individuais tab
    await page.goto('/dashboard/counselor-reports');
    await page.waitForLoadState('networkidle');

    await page.locator('[role="tab"]').filter({ hasText: /avaliações individuais/i }).click();
    await page.waitForLoadState('networkidle');

    // Select the form from the dropdown (if one exists)
    const formSelect = page.locator('mat-select').first();
    const selectCount = await formSelect.count();
    if (selectCount > 0) {
      await formSelect.click({ force: true });
      const formOption = page.locator('mat-option').filter({ hasText: FORM_TITLE });
      if (await formOption.count() > 0) {
        await formOption.first().click();
      } else {
        await page.locator('mat-option').first().click();
      }
      await page.waitForLoadState('networkidle');
    }

    // Wait for member cards to appear
    await expect(page.locator('.member-card').first()).toBeVisible({ timeout: 15_000 });

    // Check all checkboxes for members who can be batch-published
    const checkboxes = page.locator('.member-card mat-checkbox');
    const checkboxCount = await checkboxes.count();
    expect(checkboxCount, 'at least 3 checkboxes visible').toBeGreaterThanOrEqual(EMPLOYEE_COUNT);

    for (let i = 0; i < checkboxCount; i++) {
      await checkboxes.nth(i).click();
    }

    // "Publish Selected (N)" button should appear in the batch action card
    const publishSelectedBtn = page.locator('button').filter({ hasText: /publish selected/i });
    await expect(publishSelectedBtn).toBeEnabled({ timeout: 5_000 });
    await publishSelectedBtn.click();

    // Wait for batch message confirming all published
    const batchMsg = page.locator('.batch-message');
    await expect(batchMsg).toBeVisible({ timeout: 15_000 });
    const msgText = await batchMsg.textContent();
    console.log(`[Phase 4] Batch message: "${msgText}"`);

    // Message should indicate all EMPLOYEE_COUNT published
    expect(
      msgText,
      `batch message should confirm ${EMPLOYEE_COUNT} published`,
    ).toMatch(new RegExp(`${EMPLOYEE_COUNT} de ${EMPLOYEE_COUNT}|${EMPLOYEE_COUNT}.*análise`));
  });

  // ── Phase 5: Counselor closes the form (API) ─────────────────────────────

  test('Phase 5 — counselor closes the form', async ({ page }) => {
    await loginAs(page, state.counselorEmail);

    const token = await page.evaluate(() => localStorage.getItem('psytrack_token'));
    expect(token, 'counselor token').toBeTruthy();

    const closeResp = await page.request.put(
      `${API_URL}/forms/${state.formId}/close`,
      { headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' } },
    );
    expect(closeResp.status(), `PUT /forms/${state.formId}/close: ${await closeResp.text()}`).toBeLessThan(300);
    console.log(`[Phase 5] Form ${state.formId} closed`);
  });

  // ── Phase 6: Counselor generates team report (UI) ────────────────────────

  test('Phase 6 — counselor generates the team report', async ({ page }) => {
    await loginAs(page, state.counselorEmail);

    await page.goto('/dashboard/counselor-reports');
    await page.waitForLoadState('networkidle');

    const formCard = page.locator('.form-readiness-card').filter({ hasText: FORM_TITLE });
    await expect(formCard).toBeVisible({ timeout: 12_000 });

    const generateBtn = formCard.locator('button').filter({ hasText: /generate report/i });
    await expect(generateBtn).toBeEnabled({ timeout: 10_000 });
    await generateBtn.click();

    const nameSection = page.locator('.report-name-input-section');
    await expect(nameSection).toBeVisible({ timeout: 5_000 });
    await nameSection.locator('button').filter({ hasText: /confirmar/i }).click();

    await page.waitForLoadState('networkidle');

    const reportRow = formCard.locator('tr[mat-row]').first();
    await expect(reportRow).toBeVisible({ timeout: 15_000 });

    const verBtn = formCard.locator('button').filter({ hasText: /ver dashboard/i });
    await verBtn.first().click();
    await page.waitForURL('**/report-viewer/**', { timeout: 12_000 });
    console.log('[Phase 6] Report generated and dashboard opened');
  });

  // ── Phase 7: Manager sees report with 3 respondents (UI) ─────────────────

  test('Phase 7 — manager sees report with correct respondent count', async ({ page }) => {
    await loginAs(page, state.managerEmail);

    await page.goto('/dashboard/manager-assessments');
    await page.waitForLoadState('networkidle');

    const reportSelect = page.locator('.mat-mdc-select-trigger').first();
    await reportSelect.click({ force: true });

    const option = page.locator('mat-option').filter({ hasText: new RegExp(FORM_TITLE, 'i') });
    if (await option.count() > 0) {
      await option.first().click();
    } else {
      await page.locator('mat-option').first().click();
    }
    await page.waitForLoadState('networkidle');

    // All 3 respondent rows should be visible in the table
    const rows = page.locator('tr[mat-row]');
    await expect(rows.first()).toBeVisible({ timeout: 8_000 });
    const rowCount = await rows.count();
    expect(rowCount, `table should have ${EMPLOYEE_COUNT} respondent rows`).toBeGreaterThanOrEqual(EMPLOYEE_COUNT);
    console.log(`[Phase 7] Manager sees ${rowCount} respondent rows`);
  });

  // ── Phase 8: All 3 employees see their result in My Reports ──────────────

  test('Phase 8 — all 3 employees see their result in my-reports', async ({ page }) => {
    for (let i = 0; i < EMPLOYEE_COUNT; i++) {
      await loginAs(page, state.employees[i].email);

      await page.goto('/dashboard/my-reports');
      await page.waitForLoadState('networkidle');

      const dataRow = page.locator('tr[mat-row]').first();
      await expect(dataRow, `employee ${i + 1} should see at least one result row`).toBeVisible({ timeout: 10_000 });
      console.log(`[Phase 8] Employee ${i + 1} sees their result row`);
    }
  });
});
