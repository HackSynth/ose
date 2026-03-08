import { defineConfig } from '@playwright/test';

const executablePath = process.env.PLAYWRIGHT_EXECUTABLE_PATH;
const extraArgs = (process.env.PLAYWRIGHT_LAUNCH_ARGS || '')
  .split(' ')
  .map((item) => item.trim())
  .filter(Boolean);
const reportDir = process.env.PLAYWRIGHT_REPORT_DIR || '/tmp/ose-playwright-report';
const outputDir = process.env.PLAYWRIGHT_OUTPUT_DIR || '/tmp/ose-playwright-results';
const browserName = (process.env.PLAYWRIGHT_BROWSER || 'chromium') as 'chromium' | 'firefox' | 'webkit';

export default defineConfig({
  testDir: './tests/e2e',
  outputDir,
  timeout: 60_000,
  expect: {
    timeout: 10_000,
  },
  fullyParallel: false,
  workers: 1,
  retries: process.env.CI ? 1 : 0,
  reporter: [
    ['list'],
    ['html', { open: 'never', outputFolder: reportDir }],
  ],
  use: {
    baseURL: process.env.PLAYWRIGHT_BASE_URL || 'http://127.0.0.1',
    acceptDownloads: true,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    viewport: { width: 1440, height: 900 },
    launchOptions: {
      ...(executablePath ? { executablePath } : {}),
      ...(extraArgs.length ? { args: extraArgs } : {}),
    },
  },
  projects: [
    {
      name: browserName,
      use: {
        browserName,
      },
    },
  ],
});
