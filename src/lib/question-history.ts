import { prisma } from '@/lib/prisma';
import type { Prisma } from '@prisma/client';

export type QuestionHistoryEntry = {
  attempts: number;
  correctCount: number;
  lastIsCorrect: boolean;
  lastSelectedOptionLabel: string;
  lastAt: Date;
};

export async function getQuestionHistory(
  userId: string,
  questionIds: string[],
  excludeSessionId?: string,
): Promise<Map<string, QuestionHistoryEntry>> {
  if (questionIds.length === 0) return new Map();

  const baseWhere: Prisma.UserAnswerWhereInput = {
    userId,
    questionId: { in: questionIds },
  };

  if (excludeSessionId) {
    baseWhere.OR = [
      { practiceSessionId: null },
      { practiceSessionId: { not: excludeSessionId } },
    ];
  }

  const [grouped, latestAnswers] = await Promise.all([
    prisma.userAnswer.groupBy({
      by: ['questionId', 'isCorrect'],
      where: baseWhere,
      _count: { id: true },
    }),
    prisma.userAnswer.findMany({
      where: baseWhere,
      orderBy: { createdAt: 'desc' },
      distinct: ['questionId'],
      select: {
        questionId: true,
        isCorrect: true,
        createdAt: true,
        selectedOption: { select: { label: true } },
      },
    }),
  ]);

  const attemptsMap = new Map<string, { attempts: number; correctCount: number }>();
  for (const row of grouped) {
    const current = attemptsMap.get(row.questionId) ?? { attempts: 0, correctCount: 0 };
    current.attempts += row._count.id;
    if (row.isCorrect) current.correctCount += row._count.id;
    attemptsMap.set(row.questionId, current);
  }

  const result = new Map<string, QuestionHistoryEntry>();
  for (const answer of latestAnswers) {
    const stats = attemptsMap.get(answer.questionId) ?? { attempts: 0, correctCount: 0 };
    result.set(answer.questionId, {
      attempts: stats.attempts,
      correctCount: stats.correctCount,
      lastIsCorrect: answer.isCorrect,
      lastSelectedOptionLabel: answer.selectedOption.label,
      lastAt: answer.createdAt,
    });
  }

  return result;
}
