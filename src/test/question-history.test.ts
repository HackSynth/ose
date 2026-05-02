import { testApiHandler } from 'next-test-api-route-handler';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { prisma } from '@/lib/prisma';
import { getQuestionHistory } from '@/lib/question-history';
import {
  createTestPracticeSession,
  createTestUser,
  resetUserData,
  sessionFor,
} from '@/test/helpers';

async function getOptions() {
  const correct = await prisma.questionOption.findFirstOrThrow({
    where: { questionId: 'test-question-1', isCorrect: true },
  });
  const wrong = await prisma.questionOption.findFirstOrThrow({
    where: { questionId: 'test-question-1', isCorrect: false },
  });
  return { correct, wrong };
}

async function loadStartRoute(session: unknown) {
  vi.resetModules();
  vi.doMock('@/lib/auth', () => ({ auth: async () => session }));
  return import('@/app/api/practice/start/route');
}

afterEach(() => {
  vi.doUnmock('@/lib/auth');
});

describe('getQuestionHistory', () => {
  beforeEach(resetUserData);

  it('returns empty map when no answers exist', async () => {
    const user = await createTestUser({ id: 'qh-u1', email: 'qh1@example.com' });
    const result = await getQuestionHistory(user.id, ['test-question-1']);
    expect(result.size).toBe(0);
  });

  it('aggregates attempts and correct count', async () => {
    const user = await createTestUser({ id: 'qh-u2', email: 'qh2@example.com' });
    const { correct, wrong } = await getOptions();
    const sess = await createTestPracticeSession(user.id);

    await prisma.userAnswer.createMany({
      data: [
        { userId: user.id, questionId: 'test-question-1', selectedOptionId: correct.id, practiceSessionId: sess.id, isCorrect: true, timeSpent: 10 },
        { userId: user.id, questionId: 'test-question-1', selectedOptionId: wrong.id, practiceSessionId: sess.id, isCorrect: false, timeSpent: 10 },
        { userId: user.id, questionId: 'test-question-1', selectedOptionId: correct.id, practiceSessionId: sess.id, isCorrect: true, timeSpent: 10 },
      ],
    });

    const result = await getQuestionHistory(user.id, ['test-question-1']);
    const entry = result.get('test-question-1');
    expect(entry).toBeDefined();
    expect(entry!.attempts).toBe(3);
    expect(entry!.correctCount).toBe(2);
  });

  it('reflects the latest answer for lastIsCorrect', async () => {
    const user = await createTestUser({ id: 'qh-u3', email: 'qh3@example.com' });
    const { correct, wrong } = await getOptions();
    const sess = await createTestPracticeSession(user.id);

    // Insert older correct answer, then newer wrong answer
    await prisma.userAnswer.create({
      data: { userId: user.id, questionId: 'test-question-1', selectedOptionId: correct.id, practiceSessionId: sess.id, isCorrect: true, timeSpent: 10, createdAt: new Date('2025-01-01') },
    });
    await prisma.userAnswer.create({
      data: { userId: user.id, questionId: 'test-question-1', selectedOptionId: wrong.id, practiceSessionId: sess.id, isCorrect: false, timeSpent: 10, createdAt: new Date('2025-01-02') },
    });

    const result = await getQuestionHistory(user.id, ['test-question-1']);
    const entry = result.get('test-question-1');
    expect(entry!.lastIsCorrect).toBe(false);
    expect(entry!.lastSelectedOptionLabel).toBe(wrong.label);
  });

  it('excludes the current session answers when excludeSessionId is provided', async () => {
    const user = await createTestUser({ id: 'qh-u4', email: 'qh4@example.com' });
    const { correct } = await getOptions();
    const sess1 = await createTestPracticeSession(user.id);
    const sess2 = await createTestPracticeSession(user.id);

    // Answer in session 1 (old) and session 2 (current, should be excluded)
    await prisma.userAnswer.create({
      data: { userId: user.id, questionId: 'test-question-1', selectedOptionId: correct.id, practiceSessionId: sess1.id, isCorrect: true, timeSpent: 10, createdAt: new Date('2025-01-01') },
    });
    await prisma.userAnswer.create({
      data: { userId: user.id, questionId: 'test-question-1', selectedOptionId: correct.id, practiceSessionId: sess2.id, isCorrect: true, timeSpent: 10, createdAt: new Date('2025-01-02') },
    });

    // Excluding sess2 → only sess1's answer counts
    const result = await getQuestionHistory(user.id, ['test-question-1'], sess2.id);
    const entry = result.get('test-question-1');
    expect(entry!.attempts).toBe(1);
  });

  it('returns empty map for empty questionIds', async () => {
    const user = await createTestUser({ id: 'qh-u5', email: 'qh5@example.com' });
    const result = await getQuestionHistory(user.id, []);
    expect(result.size).toBe(0);
  });
});

describe('POST /api/practice/start – filter param', () => {
  beforeEach(resetUserData);

  it('filter=all (default) returns questions regardless of prior answers', async () => {
    const user = await createTestUser({ id: 'qf-u1', email: 'qf1@example.com' });
    const { correct } = await getOptions();
    const sess = await createTestPracticeSession(user.id);
    await prisma.userAnswer.create({
      data: { userId: user.id, questionId: 'test-question-1', selectedOptionId: correct.id, practiceSessionId: sess.id, isCorrect: true, timeSpent: 10 },
    });
    const appHandler = await loadStartRoute(sessionFor(user));

    await testApiHandler({
      appHandler,
      async test({ fetch }) {
        const response = await fetch({
          method: 'POST',
          headers: { 'content-type': 'application/json' },
          body: JSON.stringify({ mode: 'sequential', limit: 5, filter: 'all' }),
        });
        expect(response.status).toBe(200);
        const body = await response.json();
        expect(body.questions.length).toBeGreaterThan(0);
      },
    });
  });

  it('filter=unanswered excludes previously answered questions', async () => {
    const user = await createTestUser({ id: 'qf-u2', email: 'qf2@example.com' });
    const { correct } = await getOptions();
    const sess = await createTestPracticeSession(user.id);
    await prisma.userAnswer.create({
      data: { userId: user.id, questionId: 'test-question-1', selectedOptionId: correct.id, practiceSessionId: sess.id, isCorrect: true, timeSpent: 10 },
    });
    const appHandler = await loadStartRoute(sessionFor(user));

    await testApiHandler({
      appHandler,
      async test({ fetch }) {
        // The only CHOICE question is test-question-1, which was answered → 404
        const response = await fetch({
          method: 'POST',
          headers: { 'content-type': 'application/json' },
          body: JSON.stringify({ mode: 'sequential', limit: 5, filter: 'unanswered' }),
        });
        expect(response.status).toBe(404);
      },
    });
  });

  it('filter=unanswered returns questions that have never been answered', async () => {
    const user = await createTestUser({ id: 'qf-u3', email: 'qf3@example.com' });
    const appHandler = await loadStartRoute(sessionFor(user));

    await testApiHandler({
      appHandler,
      async test({ fetch }) {
        const response = await fetch({
          method: 'POST',
          headers: { 'content-type': 'application/json' },
          body: JSON.stringify({ mode: 'sequential', limit: 5, filter: 'unanswered' }),
        });
        expect(response.status).toBe(200);
        const body = await response.json();
        expect(body.questions.length).toBeGreaterThan(0);
      },
    });
  });

  it('filter=wrong-only returns questions whose LATEST answer is wrong', async () => {
    const user = await createTestUser({ id: 'qf-u4', email: 'qf4@example.com' });
    const { wrong } = await getOptions();
    const sess = await createTestPracticeSession(user.id);
    // Latest answer is wrong
    await prisma.userAnswer.create({
      data: { userId: user.id, questionId: 'test-question-1', selectedOptionId: wrong.id, practiceSessionId: sess.id, isCorrect: false, timeSpent: 10 },
    });
    const appHandler = await loadStartRoute(sessionFor(user));

    await testApiHandler({
      appHandler,
      async test({ fetch }) {
        const response = await fetch({
          method: 'POST',
          headers: { 'content-type': 'application/json' },
          body: JSON.stringify({ mode: 'sequential', limit: 5, filter: 'wrong-only' }),
        });
        expect(response.status).toBe(200);
        const body = await response.json();
        expect(body.questions.some((q: { id: string }) => q.id === 'test-question-1')).toBe(true);
      },
    });
  });

  it('filter=wrong-only excludes questions whose LATEST answer is correct (even with prior wrong answers)', async () => {
    const user = await createTestUser({ id: 'qf-u5', email: 'qf5@example.com' });
    const { correct, wrong } = await getOptions();
    const sess = await createTestPracticeSession(user.id);

    // First answer wrong, then correct → latest is correct → should NOT appear in wrong-only
    await prisma.userAnswer.create({
      data: { userId: user.id, questionId: 'test-question-1', selectedOptionId: wrong.id, practiceSessionId: sess.id, isCorrect: false, timeSpent: 10, createdAt: new Date('2025-01-01') },
    });
    await prisma.userAnswer.create({
      data: { userId: user.id, questionId: 'test-question-1', selectedOptionId: correct.id, practiceSessionId: sess.id, isCorrect: true, timeSpent: 10, createdAt: new Date('2025-01-02') },
    });
    const appHandler = await loadStartRoute(sessionFor(user));

    await testApiHandler({
      appHandler,
      async test({ fetch }) {
        const response = await fetch({
          method: 'POST',
          headers: { 'content-type': 'application/json' },
          body: JSON.stringify({ mode: 'sequential', limit: 5, filter: 'wrong-only' }),
        });
        // Latest answer is correct → no wrong-only questions → 404
        expect(response.status).toBe(404);
      },
    });
  });

  it('filter=wrong-only with no prior answers returns 404', async () => {
    const user = await createTestUser({ id: 'qf-u6', email: 'qf6@example.com' });
    const appHandler = await loadStartRoute(sessionFor(user));

    await testApiHandler({
      appHandler,
      async test({ fetch }) {
        const response = await fetch({
          method: 'POST',
          headers: { 'content-type': 'application/json' },
          body: JSON.stringify({ mode: 'sequential', limit: 5, filter: 'wrong-only' }),
        });
        expect(response.status).toBe(404);
      },
    });
  });

  it('invalid filter value falls back to all', async () => {
    const user = await createTestUser({ id: 'qf-u7', email: 'qf7@example.com' });
    const appHandler = await loadStartRoute(sessionFor(user));

    await testApiHandler({
      appHandler,
      async test({ fetch }) {
        const response = await fetch({
          method: 'POST',
          headers: { 'content-type': 'application/json' },
          body: JSON.stringify({ mode: 'sequential', limit: 5, filter: 'invalid-filter' }),
        });
        expect(response.status).toBe(200);
      },
    });
  });
});
