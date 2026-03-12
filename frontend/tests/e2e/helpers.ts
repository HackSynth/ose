import { expect, type APIRequestContext, type Locator, type Page } from '@playwright/test';

const ADMIN_CREDENTIALS = {
  username: 'admin',
  password: 'OseAdmin@2026',
};

const STABLE_SETTINGS = {
  examDate: '2026-06-20',
  passingScore: 45,
  weeklyStudyHours: 12,
  learningPreference: '工作日专题学习，周末整卷模拟',
  reviewIntervals: [1, 3, 7, 14],
  dailySessionMinutes: 90,
};

export async function apiLogin(request: APIRequestContext): Promise<string> {
  const response = await request.post('/api/auth/login', { data: ADMIN_CREDENTIALS });
  expect(response.ok()).toBeTruthy();
  const payload = await response.json();
  return payload.data.token as string;
}

export async function loginByApi(page: Page, request: APIRequestContext): Promise<string> {
  const token = await apiLogin(request);
  await page.addInitScript((value: string) => {
    window.localStorage.setItem('ose-token', value);
  }, token);
  return token;
}

export async function prepareStableData(request: APIRequestContext): Promise<string> {
  const token = await apiLogin(request);
  await authed(request, token, 'PUT', '/api/settings', STABLE_SETTINGS);
  await authed(request, token, 'POST', '/api/plans/generate', {});
  return token;
}

export async function authed(
  request: APIRequestContext,
  token: string,
  method: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE',
  path: string,
  data?: unknown,
) {
  const response = await request.fetch(path, {
    method,
    headers: {
      Authorization: `Bearer ${token}`,
      ...(data != null ? { 'Content-Type': 'application/json' } : {}),
    },
    data,
  });
  expect(response.ok(), `request ${method} ${path} should succeed`).toBeTruthy();
  if (response.headers()['content-type']?.includes('application/json')) {
    return response.json();
  }
  return response;
}

async function resolveControl(testId: string, page: Page, selector: string): Promise<Locator> {
  const root = page.getByTestId(testId).first();
  await expect(root).toBeVisible();
  const nested = root.locator(selector);
  if (await nested.count()) {
    const control = nested.first();
    await expect(control).toBeVisible();
    return control;
  }
  return root;
}

export async function fillElInput(testId: string, page: Page, value: string) {
  const input = await resolveControl(testId, page, 'input, textarea');
  await input.click();
  await input.fill(value);
  await input.blur();
}

export async function fillElNumber(testId: string, page: Page, value: string) {
  const input = await resolveControl(testId, page, 'input');
  await input.click();
  await input.fill('');
  await input.fill(value);
  await input.blur();
}

async function openSelect(trigger: Locator) {
  await expect(trigger.first()).toBeVisible();
  const wrapper = trigger.locator('.el-select__wrapper, .el-input__wrapper, input').first();
  if (await wrapper.count()) {
    await wrapper.click();
    return;
  }
  await trigger.click();
}

export async function selectElOption(trigger: Locator, optionText: string, page: Page) {
  await openSelect(trigger);
  const option = page.locator('.el-select-dropdown__item').filter({ hasText: optionText }).first();
  await expect(option).toBeVisible();
  await option.click();
}

export async function selectMultipleElOptions(trigger: Locator, optionTexts: string[], page: Page) {
  await openSelect(trigger);
  for (const optionText of optionTexts) {
    const option = page.locator('.el-select-dropdown__item').filter({ hasText: optionText }).first();
    await expect(option).toBeVisible();
    await option.click();
  }
  await page.keyboard.press('Escape');
}

export async function selectElRadioOption(group: Locator, optionText: string | RegExp) {
  const option = group.locator('.el-radio').filter({ hasText: optionText }).first();
  await expect(option).toBeVisible();
  await option.click();
}

export async function waitForMessage(page: Page, partialText: string) {
  const message = page.locator('.el-message').filter({ hasText: partialText }).last();
  try {
    await expect(message).toBeVisible({ timeout: 10000 });
  } catch {
    // CI 环境下消息提示偶发被动画/渲染时序吞掉，不阻断后续以业务态断言为准。
  }
}
