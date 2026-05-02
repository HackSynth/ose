import { NextResponse } from 'next/server';
import { auth } from '@/lib/auth';
import { prisma } from '@/lib/prisma';
import { getChoiceAnswerEvents } from '@/lib/knowledge-stats';

export async function GET() {
  const session = await auth();
  if (!session?.user?.id) return NextResponse.json({ message: '请先登录' }, { status: 401 });
  const roots = await prisma.knowledgePoint.findMany({
    where: { parentId: null },
    orderBy: { sortOrder: 'asc' },
    include: {
      children: {
        orderBy: { sortOrder: 'asc' },
        include: { questions: { select: { id: true, type: true } } },
      },
      questions: { select: { id: true, type: true } },
    },
  });
  const answers = await getChoiceAnswerEvents(session.user.id);
  const caseAnswers = await prisma.userCaseAnswer.findMany({
    where: { userId: session.user.id },
    select: {
      score: true,
      caseSubQuestion: {
        select: {
          score: true,
          caseScenario: {
            select: {
              question: {
                select: { knowledgePointId: true, knowledgePoint: { select: { parentId: true } } },
              },
            },
          },
        },
      },
    },
  });

  const summarize = (ids: string[]) => {
    const choiceRows = answers.filter(
      (a) => ids.includes(a.knowledgePointId) || (a.parentId ? ids.includes(a.parentId) : false)
    );
    const caseRows = caseAnswers.filter(
      (a) =>
        ids.includes(a.caseSubQuestion.caseScenario.question.knowledgePointId) ||
        (a.caseSubQuestion.caseScenario.question.knowledgePoint.parentId
          ? ids.includes(a.caseSubQuestion.caseScenario.question.knowledgePoint.parentId)
          : false)
    );
    const correct =
      choiceRows.filter((a) => a.isCorrect).length +
      caseRows.filter((a) => (a.score ?? 0) >= a.caseSubQuestion.score * 0.8).length;
    const done = choiceRows.length + caseRows.length;
    const accuracy = done ? Math.round((correct / done) * 100) : 0;
    return { done, accuracy, status: done === 0 ? '未学习' : accuracy > 80 ? '已掌握' : '学习中' };
  };

  const tree = roots.map((root) => {
    const childIds = root.children.map((child) => child.id);
    const ids = [root.id, ...childIds];
    const questionCount =
      root.questions.length + root.children.reduce((sum, child) => sum + child.questions.length, 0);
    return {
      id: root.id,
      name: root.name,
      description: root.description,
      questionCount,
      ...summarize(ids),
      children: root.children.map((child) => ({
        id: child.id,
        name: child.name,
        description: child.description,
        questionCount: child.questions.length,
        ...summarize([child.id]),
      })),
    };
  });
  return NextResponse.json({ tree });
}
