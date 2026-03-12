import { expect, test } from '@playwright/test';
import { loginByApi } from './helpers';

test('AI 出题页面支持生成、预览、保存与历史', async ({ page, request }) => {
  await loginByApi(page, request);

  await page.route('**/api/ai/providers', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 0,
        message: 'ok',
        data: [
          { provider: 'OPENAI', configured: true, statusMessage: '可用', models: [{ model: 'gpt-4.1-mini', displayName: 'gpt-4.1-mini', isDefault: true }] },
          { provider: 'ANTHROPIC', configured: true, statusMessage: '可用', models: [{ model: 'claude-3-5-sonnet-latest', displayName: 'claude-3-5-sonnet-latest', isDefault: true }] },
        ],
        timestamp: new Date().toISOString(),
      }),
    });
  });

  await page.route('**/api/ai/history', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 0, message: 'ok', data: [{ id: 1, provider: 'OPENAI', model: 'gpt-4.1-mini', questionType: 'MORNING_SINGLE', status: 'SUCCESS', createdAt: '2026-03-12 10:00:00' }], timestamp: new Date().toISOString() }),
    });
  });

  await page.route('**/api/ai/questions/generate', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 0,
        message: 'ok',
        data: {
          generationId: 8,
          provider: 'OPENAI',
          model: 'gpt-4.1-mini',
          questionType: 'MORNING_SINGLE',
          disclaimer: 'AI 生成题目仅供辅助学习，内容需人工确认后使用。',
          saveAllowed: true,
          validationErrors: [],
          drafts: [
            {
              draftId: 'd1',
              questionType: 'MORNING_SINGLE',
              title: '事务隔离级别判断',
              content: '某系统出现脏读，应该调整为哪种隔离级别？',
              options: [
                { key: 'A', content: '读未提交' },
                { key: 'B', content: '读已提交' },
                { key: 'C', content: '可重复读' },
                { key: 'D', content: '串行化' },
              ],
              correctAnswer: 'B',
              explanation: '读已提交可避免脏读。',
              knowledgePointIds: [1],
              difficulty: 'MEDIUM',
              provider: 'OPENAI',
              model: 'gpt-4.1-mini',
            },
          ],
        },
        timestamp: new Date().toISOString(),
      }),
    });
  });

  await page.route('**/api/ai/questions/save', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 0,
        message: 'ok',
        data: { generationId: 8, savedCount: 1, status: 'PENDING_REVIEW', questionIds: [100] },
        timestamp: new Date().toISOString(),
      }),
    });
  });

  await page.goto('/ai-questions');
  await expect(page.getByTestId('ai-question-page')).toBeVisible();
  await page.getByTestId('ai-generate').click();
  await expect(page.getByTestId('ai-result-list')).toContainText('题目 1 · AI 生成');

  await page.getByTestId('ai-save').click();
  await expect(page.getByTestId('ai-history-table')).toContainText('OPENAI');

  await page.getByTestId('ai-discard').click();
  await expect(page.getByTestId('ai-question-page')).toBeVisible();
});
