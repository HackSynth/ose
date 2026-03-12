import { expect, test } from '@playwright/test';
import { fillElInput, loginByApi, waitForMessage } from './helpers';

test('AI 配置页支持测试连接、保存配置并驱动 AI 出题链路', async ({ page, request }) => {
  await loginByApi(page, request);

  const state = {
    openai: {
      enabled: false,
      configured: false,
      maskedKey: null as string | null,
      storedMaskedKey: null as string | null,
      baseUrl: 'https://api.openai.com',
      defaultModel: 'gpt-4.1-mini',
      timeoutMs: 10000,
      maxRetries: 1,
      temperature: 0.2,
      configSource: 'ENV_FALLBACK',
      healthStatus: 'UNKNOWN',
      healthMessage: '尚未测试',
      editable: true,
      keyManagedByEnv: true,
      hasStoredApiKey: false,
    },
  };

  const aiSettingsResponse = () => ({
    code: 0,
    message: 'ok',
    data: {
      configMode: 'HYBRID',
      encryptionKeyConfigured: true,
      databaseConfigWritable: true,
      providers: [
        {
          provider: 'OPENAI',
          ...state.openai,
        },
        {
          provider: 'ANTHROPIC',
          enabled: false,
          configured: false,
          maskedKey: null,
          storedMaskedKey: null,
          baseUrl: 'https://api.anthropic.com',
          defaultModel: 'claude-3-5-sonnet-latest',
          timeoutMs: 12000,
          maxRetries: 1,
          temperature: 0.2,
          configSource: 'UNAVAILABLE',
          healthStatus: 'UNAVAILABLE',
          healthMessage: '未配置',
          editable: true,
          keyManagedByEnv: false,
          hasStoredApiKey: false,
        },
      ],
    },
    timestamp: new Date().toISOString(),
  });

  await page.route('**/api/ai/settings', async (route) => {
    if (route.request().method() === 'GET') {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(aiSettingsResponse()),
      });
      return;
    }
    await route.fallback();
  });

  await page.route('**/api/ai/settings/OPENAI/models', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 0,
        message: 'ok',
        data: {
          provider: 'OPENAI',
          suggestedDefaultModel: 'gpt-4.1-mini',
          models: [
            { model: 'gpt-4.1-mini', displayName: 'gpt-4.1-mini', isDefault: true },
            { model: 'gpt-4.1', displayName: 'gpt-4.1', isDefault: false },
          ],
        },
        timestamp: new Date().toISOString(),
      }),
    });
  });

  await page.route('**/api/ai/settings/ANTHROPIC/models', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 0,
        message: 'ok',
        data: {
          provider: 'ANTHROPIC',
          suggestedDefaultModel: 'claude-3-5-sonnet-latest',
          models: [
            { model: 'claude-3-5-sonnet-latest', displayName: 'claude-3-5-sonnet-latest', isDefault: true },
          ],
        },
        timestamp: new Date().toISOString(),
      }),
    });
  });

  await page.route('**/api/ai/settings/OPENAI/test', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 0,
        message: 'ok',
        data: {
          success: true,
          provider: 'OPENAI',
          model: 'gpt-4.1-mini',
          latencyMs: 120,
          message: 'OpenAI 连通性测试通过',
          configSource: 'DB',
        },
        timestamp: new Date().toISOString(),
      }),
    });
  });

  await page.route('**/api/ai/settings/OPENAI', async (route) => {
    if (route.request().method() !== 'PUT') {
      await route.fallback();
      return;
    }
    const payload = await route.request().postDataJSON();
    state.openai.enabled = Boolean(payload.enabled);
    state.openai.configured = true;
    state.openai.maskedKey = 'sk-***9876';
    state.openai.storedMaskedKey = 'sk-***9876';
    state.openai.baseUrl = payload.baseUrl;
    state.openai.defaultModel = payload.defaultModel;
    state.openai.timeoutMs = payload.timeoutMs;
    state.openai.maxRetries = payload.maxRetries;
    state.openai.temperature = payload.temperature;
    state.openai.configSource = 'DB';
    state.openai.healthStatus = 'SUCCESS';
    state.openai.healthMessage = '最近测试通过';
    state.openai.keyManagedByEnv = false;
    state.openai.hasStoredApiKey = true;

    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 0,
        message: 'ok',
        data: {
          provider: 'OPENAI',
          ...state.openai,
        },
        timestamp: new Date().toISOString(),
      }),
    });
  });

  await page.route('**/api/ai/providers', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 0,
        message: 'ok',
        data: [
          {
            provider: 'OPENAI',
            configured: true,
            statusMessage: '可用',
            models: [{ model: 'gpt-4.1-mini', displayName: 'gpt-4.1-mini', isDefault: true }],
          },
          {
            provider: 'ANTHROPIC',
            configured: false,
            statusMessage: '未配置 API Key',
            models: [{ model: 'claude-3-5-sonnet-latest', displayName: 'claude-3-5-sonnet-latest', isDefault: true }],
          },
        ],
        timestamp: new Date().toISOString(),
      }),
    });
  });

  await page.route('**/api/ai/history', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 0,
        message: 'ok',
        data: [],
        timestamp: new Date().toISOString(),
      }),
    });
  });

  await page.route('**/api/knowledge-points/tree', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 0,
        message: 'ok',
        data: [{ id: 1, code: 'DB.TXN', name: '事务管理', children: [] }],
        timestamp: new Date().toISOString(),
      }),
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
          generationId: 18,
          provider: 'OPENAI',
          model: 'gpt-4.1-mini',
          questionType: 'MORNING_SINGLE',
          disclaimer: 'AI 生成题目仅供辅助学习，内容需人工确认后使用。',
          saveAllowed: true,
          validationErrors: [],
          drafts: [
            {
              draftId: 'draft-1',
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

  await page.goto('/ai-settings');
  await expect(page.getByTestId('ai-settings-page')).toBeVisible();
  await fillElInput('ai-settings-key-OPENAI', page, 'sk-test-9876');
  await page.getByTestId('ai-settings-enabled-OPENAI').click();
  await page.getByTestId('ai-settings-test-OPENAI').click();
  await expect(page.getByTestId('ai-settings-test-result-OPENAI')).toContainText('OpenAI 连通性测试通过');

  await page.getByTestId('ai-settings-save-OPENAI').click();
  await waitForMessage(page, '配置已保存');
  await expect(page.getByTestId('ai-settings-source-OPENAI')).toContainText('DB');
  await expect(page.getByTestId('ai-settings-mask-OPENAI')).toContainText('sk-***9876');

  await page.goto('/ai-questions');
  await expect(page.getByTestId('ai-question-page')).toBeVisible();
  await page.getByTestId('ai-generate').click();
  await expect(page.getByTestId('ai-result-list')).toContainText('题目 1 · AI 生成');
});
