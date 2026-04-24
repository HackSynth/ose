import { NextResponse } from "next/server";
import { auth } from "@/lib/auth";
import { prisma } from "@/lib/prisma";

export async function GET(request: Request) {
  const session = await auth();
  if (!session?.user?.id) return NextResponse.json({ message: "未登录" }, { status: 401 });

  const { searchParams } = new URL(request.url);
  const sessionId = searchParams.get("sessionId");
  if (!sessionId) return NextResponse.json({ message: "缺少 sessionId" }, { status: 400 });

  const practiceSession = await prisma.practiceSession.findFirst({
    where: { id: sessionId, userId: session.user.id },
    include: {
      answers: { include: { question: { include: { knowledgePoint: { include: { parent: true } } } } } },
    },
  });

  if (!practiceSession) return NextResponse.json({ message: "练习不存在" }, { status: 404 });

  const correct = practiceSession.answers.filter((answer) => answer.isCorrect).length;
  const totalTime = practiceSession.answers.reduce((sum, answer) => sum + answer.timeSpent, 0);
  const weakMap = new Map<string, { name: string; wrong: number }>();
  practiceSession.answers.filter((answer) => !answer.isCorrect).forEach((answer) => {
    const parent = answer.question.knowledgePoint.parent;
    const id = parent?.id ?? answer.question.knowledgePoint.id;
    const name = parent?.name ?? answer.question.knowledgePoint.name;
    const current = weakMap.get(id) ?? { name, wrong: 0 };
    current.wrong += 1;
    weakMap.set(id, current);
  });

  return NextResponse.json({
    mode: practiceSession.mode,
    total: practiceSession.total,
    answered: practiceSession.answers.length,
    correct,
    accuracy: practiceSession.answers.length === 0 ? 0 : Math.round((correct / practiceSession.answers.length) * 100),
    timeSpent: totalTime,
    weakTopics: Array.from(weakMap.values()).sort((a, b) => b.wrong - a.wrong).slice(0, 5),
  });
}



