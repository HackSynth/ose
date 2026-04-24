import { NextResponse } from "next/server";
import { auth } from "@/lib/auth";
import { prisma } from "@/lib/prisma";

export async function GET(_request: Request, { params }: { params: Promise<{ id: string }> }) {
  const session = await auth();
  if (!session?.user?.id) return NextResponse.json({ message: "请先登录" }, { status: 401 });
  const { id } = await params;
  const question = await prisma.question.findFirst({
    where: { id, type: "CASE_ANALYSIS" },
    include: { caseScenario: { include: { subQuestions: { orderBy: { subNumber: "asc" }, include: { userAnswers: { where: { userId: session.user.id }, orderBy: { createdAt: "desc" }, take: 1 } } } } } },
  });
  if (!question?.caseScenario) return NextResponse.json({ message: "案例题不存在" }, { status: 404 });
  const results = question.caseScenario.subQuestions.map((sub) => ({ subQuestionId: sub.id, subNumber: sub.subNumber, score: sub.userAnswers[0]?.score ?? null, answer: sub.userAnswers[0]?.answer ?? "", feedback: sub.userAnswers[0]?.feedback ?? "", referenceAnswer: sub.referenceAnswer, explanation: sub.explanation, maxScore: sub.score }));
  return NextResponse.json({ totalScore: results.reduce((sum, item) => sum + (item.score ?? 0), 0), maxScore: results.reduce((sum, item) => sum + item.maxScore, 0), results });
}
