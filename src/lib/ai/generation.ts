import { prisma } from "@/lib/prisma";

export type ChoiceAIQuestion = {
  content: string;
  difficulty: number;
  explanation: string;
  knowledgePointName: string;
  options: Array<{ label: string; content: string; isCorrect: boolean }>;
};

export type CaseAIQuestion = {
  background: string;
  difficulty: number;
  knowledgePointName: string;
  subQuestions: Array<{ subNumber: number; content: string; answerType: "FILL_BLANK" | "SHORT_ANSWER" | "DIAGRAM_FILL"; referenceAnswer: string; score: number; explanation: string }>;
};

export async function findBestKnowledgePoint(name: string, fallbackIds?: string[]) {
  const normalized = name.trim();
  const exact = normalized ? await prisma.knowledgePoint.findFirst({ where: { name: normalized } }) : null;
  if (exact) return exact;

  const fuzzy = normalized ? await prisma.knowledgePoint.findFirst({ where: { name: { contains: normalized } } }) : null;
  if (fuzzy) return fuzzy;

  if (fallbackIds?.length) {
    const fallback = await prisma.knowledgePoint.findFirst({ where: { id: { in: fallbackIds } } });
    if (fallback) return fallback;
  }

  const first = await prisma.knowledgePoint.findFirst({ orderBy: { sortOrder: "asc" } });
  if (!first) throw new Error("暂无知识点，无法保存 AI 题目");
  return first;
}

export function normalizeChoiceQuestion(question: ChoiceAIQuestion, fallbackDifficulty: number) {
  const options = ["A", "B", "C", "D"].map((label) => {
    const option = question.options?.find((item) => item.label === label);
    return { label, content: option?.content || `${label} 选项`, isCorrect: Boolean(option?.isCorrect) };
  });
  const correctCount = options.filter((option) => option.isCorrect).length;
  const normalizedOptions = correctCount === 1 ? options : options.map((option, index) => ({ ...option, isCorrect: index === 0 }));
  return { ...question, difficulty: question.difficulty || fallbackDifficulty, options: normalizedOptions };
}

export async function nextAIQuestionNumber() {
  const aggregate = await prisma.question.aggregate({ where: { year: 2099 }, _max: { questionNumber: true } });
  return (aggregate._max.questionNumber ?? 0) + 1;
}
