import { testApiHandler } from 'next-test-api-route-handler';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { prisma } from '@/lib/prisma';
import type { CompletionParams } from '@/lib/ai/types';
import { createTestUser, resetUserData, sessionFor } from '@/test/helpers';

function streamText(text: string) {
  return (async function* stream() {
    yield text;
  })();
}

function mockAI(capture: (params: CompletionParams) => void, text = 'ok') {
  vi.doMock('@/lib/ai', () => ({
    isAIConfigured: async () => true,
    getAIProvider: async () => ({
      name: 'Mock',
      getInfo: () => ({ name: 'Mock', model: 'mock-model', endpoint: 'mock://ai' }),
      createCompletion: async () => text,
      streamCompletion: (params: CompletionParams) => {
        capture(params);
        return streamText(text);
      },
      supportsVision: () => false,
    }),
  }));
  vi.doMock('@/lib/ai/rate-limit', () => ({ checkAIRateLimit: () => true }));
  vi.doMock('@/lib/ai/learning-context', () => ({
    buildLearningKnowledgeBase: async () =>
      '## 学生当前学习情况知识库\n- 薄弱知识点：数据库\n- 今日任务：完成选择题',
  }));
}

async function loadChatRoute(session: unknown, capture: (params: CompletionParams) => void) {
  vi.resetModules();
  vi.doMock('@/lib/auth', () => ({ auth: async () => session }));
  mockAI(capture);
  return import('@/app/api/ai/chat/route');
}

async function loadTodayPlanDiagnosisRoute(
  session: unknown,
  capture: (params: CompletionParams) => void
) {
  vi.resetModules();
  vi.doMock('@/lib/auth', () => ({ auth: async () => session }));
  mockAI(capture, 'plan-ok');
  return import('@/app/api/ai/today-plan-diagnosis/route');
}

afterEach(() => {
  vi.doUnmock('@/lib/auth');
  vi.doUnmock('@/lib/ai');
  vi.doUnmock('@/lib/ai/rate-limit');
  vi.doUnmock('@/lib/ai/learning-context');
});

describe('POST /api/ai/chat', () => {
  it('adds the learning knowledge base to the chat system prompt', async () => {
    let captured: CompletionParams | null = null;
    const appHandler = await loadChatRoute(
      { user: { id: 'chat-user', email: 'chat@example.com', name: 'Chat User' } },
      (params) => {
        captured = params;
      }
    );

    await testApiHandler({
      appHandler,
      async test({ fetch }) {
        const response = await fetch({
          method: 'POST',
          headers: { 'content-type': 'application/json' },
          body: JSON.stringify({ messages: [{ role: 'user', content: '我今天该复习什么？' }] }),
        });

        expect(response.status).toBe(200);
        await expect(response.text()).resolves.toBe('ok');
        expect(captured?.systemPrompt).toContain('学生当前学习情况知识库');
        expect(captured?.systemPrompt).toContain('薄弱知识点：数据库');
      },
    });
  });
});

describe('POST /api/ai/today-plan-diagnosis', () => {
  beforeEach(async () => {
    await resetUserData();
  });

  it('streams a diagnosis for the selected plan day', async () => {
    const user = await createTestUser({ id: 'plan-user', email: 'plan@example.com' });
    const plan = await prisma.studyPlan.create({
      data: {
        userId: user.id,
        title: '测试学习计划',
        content: '计划概览',
        targetExamDate: new Date('2026-05-16T00:00:00+08:00'),
        totalDays: 1,
        days: {
          create: {
            dayNumber: 1,
            date: new Date('2026-05-02T00:00:00+08:00'),
            tasks: ['复习数据库 ER 图', '完成选择题 10 道'],
          },
        },
      },
    });
    let captured: CompletionParams | null = null;
    const appHandler = await loadTodayPlanDiagnosisRoute(sessionFor(user), (params) => {
      captured = params;
    });

    await testApiHandler({
      appHandler,
      async test({ fetch }) {
        const response = await fetch({
          method: 'POST',
          headers: { 'content-type': 'application/json' },
          body: JSON.stringify({ planId: plan.id, dayNumber: 1 }),
        });

        expect(response.status).toBe(200);
        await expect(response.text()).resolves.toBe('plan-ok');
        expect(captured?.systemPrompt).toContain('今日任务');
        expect(captured?.userMessage).toContain('复习数据库 ER 图');
        expect(captured?.userMessage).toContain('学生当前学习情况知识库');
      },
    });
  });
});
