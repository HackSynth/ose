export { getDescendantTopicIds, visibilityFilter } from "@/lib/knowledge-stats";

export const questionSelect = {
  id: true,
  content: true,
  difficulty: true,
  year: true,
  session: true,
  questionNumber: true,
  explanation: true,
  knowledgePoint: {
    select: {
      id: true,
      name: true,
      parent: { select: { id: true, name: true } },
    },
  },
  options: {
    select: { id: true, label: true, content: true, isCorrect: true },
    orderBy: { label: "asc" as const },
  },
};

export function stripCorrectFlags<T extends { options: Array<{ isCorrect?: boolean }> }>(question: T) {
  return {
    ...question,
    options: question.options.map(({ isCorrect: _isCorrect, ...option }) => option),
  };
}

export function shuffle<T>(items: T[]) {
  const result = [...items];
  for (let i = result.length - 1; i > 0; i -= 1) {
    const j = Math.floor(Math.random() * (i + 1));
    [result[i], result[j]] = [result[j], result[i]];
  }
  return result;
}
