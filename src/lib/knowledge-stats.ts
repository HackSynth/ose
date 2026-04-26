import { prisma } from '@/lib/prisma';
import type { Prisma } from '@prisma/client';

export type KnowledgeNode = { id: string; parentId: string | null };

export async function loadKnowledgeTree(): Promise<{
  nodes: KnowledgeNode[];
  descendantsOf: (id: string) => string[];
}> {
  const nodes = await prisma.knowledgePoint.findMany({ select: { id: true, parentId: true } });
  const childrenByParent = new Map<string, string[]>();
  for (const node of nodes) {
    if (!node.parentId) continue;
    const list = childrenByParent.get(node.parentId) ?? [];
    list.push(node.id);
    childrenByParent.set(node.parentId, list);
  }
  const cache = new Map<string, string[]>();
  function collect(id: string): string[] {
    const cached = cache.get(id);
    if (cached) return cached;
    const result = [id];
    for (const child of childrenByParent.get(id) ?? []) result.push(...collect(child));
    cache.set(id, result);
    return result;
  }
  return { nodes, descendantsOf: collect };
}

export async function getDescendantTopicIds(topicId: string): Promise<string[]> {
  const { descendantsOf } = await loadKnowledgeTree();
  return descendantsOf(topicId);
}

export function visibilityFilter(userId: string): Prisma.QuestionWhereInput {
  return { OR: [{ createdByUserId: null }, { createdByUserId: userId }] };
}

export type TopicAnswerStat = { knowledgePointId: string; total: number; correct: number };

export type ChoiceAnswerEvent = {
  id: string;
  questionId: string;
  knowledgePointId: string;
  parentId: string | null;
  isCorrect: boolean;
  timeSpent: number;
  createdAt: Date;
};

export async function getChoiceAnswerEvents(userId: string): Promise<ChoiceAnswerEvent[]> {
  const [practiceAnswers, examAnswers] = await Promise.all([
    prisma.userAnswer.findMany({
      where: { userId },
      select: {
        id: true,
        questionId: true,
        isCorrect: true,
        timeSpent: true,
        createdAt: true,
        question: {
          select: {
            knowledgePointId: true,
            knowledgePoint: { select: { parentId: true } },
          },
        },
      },
    }),
    prisma.examAnswer.findMany({
      where: {
        isCorrect: { not: null },
        examAttempt: { userId, status: 'COMPLETED' },
      },
      select: {
        id: true,
        questionId: true,
        isCorrect: true,
        timeSpent: true,
        examAttempt: { select: { finishedAt: true, startedAt: true } },
        question: {
          select: {
            knowledgePointId: true,
            knowledgePoint: { select: { parentId: true } },
          },
        },
      },
    }),
  ]);

  return [
    ...practiceAnswers.map((answer) => ({
      id: `practice-${answer.id}`,
      questionId: answer.questionId,
      knowledgePointId: answer.question.knowledgePointId,
      parentId: answer.question.knowledgePoint.parentId,
      isCorrect: answer.isCorrect,
      timeSpent: answer.timeSpent,
      createdAt: answer.createdAt,
    })),
    ...examAnswers.map((answer) => ({
      id: `exam-${answer.id}`,
      questionId: answer.questionId,
      knowledgePointId: answer.question.knowledgePointId,
      parentId: answer.question.knowledgePoint.parentId,
      isCorrect: Boolean(answer.isCorrect),
      timeSpent: answer.timeSpent,
      createdAt: answer.examAttempt.finishedAt ?? answer.examAttempt.startedAt,
    })),
  ].sort((a, b) => a.createdAt.getTime() - b.createdAt.getTime());
}

export async function getTopicAnswerStats(
  userId: string
): Promise<Map<string, { total: number; correct: number }>> {
  const rows = await getChoiceAnswerEvents(userId);
  const stats = new Map<string, { total: number; correct: number }>();
  for (const row of rows) {
    const bucket = stats.get(row.knowledgePointId) ?? { total: 0, correct: 0 };
    bucket.total += 1;
    if (row.isCorrect) bucket.correct += 1;
    stats.set(row.knowledgePointId, bucket);
  }
  return stats;
}

export function rollupStats(
  descendantsOf: (id: string) => string[],
  stats: Map<string, { total: number; correct: number }>,
  rootId: string
) {
  let total = 0;
  let correct = 0;
  for (const id of descendantsOf(rootId)) {
    const bucket = stats.get(id);
    if (!bucket) continue;
    total += bucket.total;
    correct += bucket.correct;
  }
  const accuracy = total === 0 ? 0 : Math.round((correct / total) * 100);
  return { total, correct, accuracy };
}
