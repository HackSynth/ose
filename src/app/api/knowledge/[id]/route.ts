import { NextResponse } from "next/server";
import { auth } from "@/lib/auth";
import { prisma } from "@/lib/prisma";
import { getDescendantTopicIds, visibilityFilter } from "@/lib/knowledge-stats";

export async function GET(_request: Request, { params }: { params: Promise<{ id: string }> }) {
  const session = await auth();
  if (!session?.user?.id) return NextResponse.json({ message: "请先登录" }, { status: 401 });
  const userId = session.user.id;
  const { id } = await params;
  const topic = await prisma.knowledgePoint.findUnique({ where: { id }, include: { parent: true, children: { orderBy: { sortOrder: "asc" } } } });
  if (!topic) return NextResponse.json({ message: "知识点不存在" }, { status: 404 });
  const ids = await getDescendantTopicIds(id);
  const [questions, answerTotal, answerCorrect, wrongCount] = await Promise.all([
    prisma.question.findMany({
      where: { knowledgePointId: { in: ids }, ...visibilityFilter(userId) },
      orderBy: [{ type: "asc" }, { questionNumber: "asc" }],
      select: {
        id: true,
        type: true,
        content: true,
        questionNumber: true,
        year: true,
        session: true,
        difficulty: true,
        knowledgePoint: { select: { id: true, name: true, parent: { select: { id: true, name: true } } } },
        caseScenario: { select: { background: true, subQuestions: { select: { id: true, subNumber: true, content: true, score: true } } } },
      },
    }),
    prisma.userAnswer.count({ where: { userId, question: { knowledgePointId: { in: ids } } } }),
    prisma.userAnswer.count({ where: { userId, isCorrect: true, question: { knowledgePointId: { in: ids } } } }),
    prisma.wrongNote.count({ where: { userId, question: { knowledgePointId: { in: ids } } } }),
  ]);
  return NextResponse.json({
    topic,
    questions,
    stats: {
      done: answerTotal,
      accuracy: answerTotal ? Math.round((answerCorrect / answerTotal) * 100) : 0,
      wrongCount,
    },
  });
}
