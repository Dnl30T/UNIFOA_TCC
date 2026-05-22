import { chromium, FullConfig } from '@playwright/test';
import * as fs from 'fs';
import * as path from 'path';

interface Credential {
  email: string;
  password: string;
  role: string;
  redirectPath: string;
}

const CREDENTIALS: Credential[] = [
  {
    email: 'alice.manager@psytrack.dev',
    password: 'Senha@123',
    role: 'manager',
    redirectPath: '/dashboard',
  },
  {
    email: 'carol.counselor@psytrack.dev',
    password: 'Senha@123',
    role: 'counselor',
    redirectPath: '/dashboard',
  },
  {
    email: 'eve.emp@psytrack.dev',
    password: 'Senha@123',
    role: 'employee',
    redirectPath: '/dashboard',
  },
];

export default async function globalSetup(_config: FullConfig) {
  const authDir = path.join(process.cwd(), 'e2e', '.auth');
  if (!fs.existsSync(authDir)) {
    fs.mkdirSync(authDir, { recursive: true });
  }

  const browser = await chromium.launch();

  for (const cred of CREDENTIALS) {
    const authFile = path.join(authDir, `${cred.role}.json`);

    // Reuse existing auth state if fresh enough (< 30 min old)
    if (fs.existsSync(authFile)) {
      const stat = fs.statSync(authFile);
      const ageMs = Date.now() - stat.mtimeMs;
      if (ageMs < 30 * 60 * 1000) {
        console.log(`[global-setup] Reusing cached auth for ${cred.role}`);
        continue;
      }
    }

    console.log(`[global-setup] Logging in as ${cred.role} (${cred.email})`);
    const context = await browser.newContext();
    const page = await context.newPage();

    await page.goto('http://localhost:4200/auth/login');
    await page.waitForLoadState('networkidle');
    await page.locator('input[formcontrolname="email"]').fill(cred.email);
    await page.locator('input[formcontrolname="password"]').fill(cred.password);
    await page.locator('button[type="submit"]').click();

    // Wait until we land on the dashboard
    await page.waitForURL('**/dashboard**', { timeout: 15_000 });

    await context.storageState({ path: authFile });
    await context.close();
    console.log(`[global-setup] Saved auth state for ${cred.role}`);
  }

  await browser.close();
}
