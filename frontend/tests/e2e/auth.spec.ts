import { expect, test } from '@playwright/test';
import { fillElInput } from './helpers';

test('管理员可以通过登录页登录', async ({ page }) => {
  await page.goto('/login');

  await expect(page.getByTestId('login-page')).toBeVisible();
  await fillElInput('login-username', page, 'admin');
  await fillElInput('login-password', page, 'OseAdmin@2026');
  await page.getByTestId('login-submit').click();

  await expect(page).toHaveURL(/\/dashboard$/);
  await expect(page.getByTestId('dashboard-page')).toBeVisible();
  await expect(page.getByTestId('current-user')).toContainText('备考管理员');
});
