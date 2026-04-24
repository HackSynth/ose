import { prisma } from "@/lib/prisma";
import type { Prisma } from "@prisma/client";

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

export async function getTopicAnswerStats(userId: string): Promise<Map<string, { total: number; correct: number }>> {
  const rows = await prisma.userAnswer.groupBy({
    by: ["questionId", "isCorrect"],
    where: { userId },
    _count: { _all: true },
  });
  if (rows.length === 0) return new Map();
  const questionIds = Array.from(new Set(rows.map((row) => row.questionId)));
  const questions = await prisma.question.findMany({
    where: { id: { in: questionIds } },
    select: { id: true, knowledgePointId: true },
  });
  const kpByQuestion = new Map(questions.map((question) => [question.id, question.knowledgePointId]));
  const stats = new Map<string, { total: number; correct: number }>();
  for (const row of rows) {
    const kpId = kpByQuestion.get(row.questionId);
    if (!kpId) continue;
    const bucket = stats.get(kpId) ?? { total: 0, correct: 0 };
    bucket.total += row._count._all;
    if (row.isCorrect) bucket.correct += row._count._all;
    stats.set(kpId, bucket);
  }
  return stats;
}

export function rollupStats(
  descendantsOf: (id: string) => string[],
  stats: Map<string, { total: number; correct: number }>,
  rootId: string,
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
