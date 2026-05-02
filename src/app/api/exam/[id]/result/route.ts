import { NextResponse } from "next/server";
import { auth } from "@/lib/auth";
import { prisma } from "@/lib/prisma";

export async function GET(_request: Request, { params }: { params: Promise<{ id: string }> }) {
  const session = await auth();
  if (!session?.user?.id) return NextResponse.json({ message: "请先登录" }, { status: 401 });
  const { id: attemptId } = await params;
  const attempt = await prisma.examAttempt.findFirst({
    where: { id: attemptId, userId: session.user.id },
    include: {
      exam: { include: { questions: { orderBy: { orderNumber: "asc" }, include: { question: { include: { options: { orderBy: { label: "asc" } }, knowledgePoint: { include: { parent: true } }, caseScenario: { include: { subQuestions: { orderBy: { subNumber: "asc" } } } } } } } } } },
      answers: { include: { selectedOption: true } },
    },
  });
  if (!attempt) return NextResponse.json({ message: "考试不存在" }, { status: 404 });
  const previous = await prisma.examAttempt.findFirst({ where: { userId: session.user.id, examId: attempt.examId, status: "COMPLETED", startedAt: { lt: attempt.startedAt } }, orderBy: { startedAt: "desc" } });
  return NextResponse.json({ attempt, previousScore: previous?.totalScore ?? null });
}


