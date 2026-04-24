import { NextResponse } from "next/server";
import { auth } from "@/lib/auth";
import { prisma } from "@/lib/prisma";
import { questionSelect, stripCorrectFlags } from "@/lib/practice";

export async function GET(_request: Request, { params }: { params: Promise<{ id: string }> }) {
  const session = await auth();
  if (!session?.user?.id) return NextResponse.json({ message: "未登录" }, { status: 401 });
  const { id } = await params;

  const practiceSession = await prisma.practiceSession.findFirst({
    where: { id, userId: session.user.id },
    include: {
      questions: {
        orderBy: { order: "asc" },
        include: { question: { select: { ...questionSelect, options: { select: { id: true, label: true, content: true, isCorrect: true }, orderBy: { label: "asc" } } } } },
      },
      answers: { select: { questionId: true, selectedOptionId: true, isCorrect: true } },
    },
  });

  if (!practiceSession) return NextResponse.json({ message: "练习会话不存在" }, { status: 404 });

  const questions = practiceSession.questions.map((entry) => stripCorrectFlags(entry.question));
  const questionMap = new Map(practiceSession.questions.map((entry) => [entry.question.id, entry.question]));
  const results: Record<string, { isCorrect: boolean; selectedOptionId: string; correctOptionId: string; explanation: string; options: Array<{ id: string; label: string; content: string; isCorrect: boolean }> }> = {};
  for (const answer of practiceSession.answers) {
    const question = questionMap.get(answer.questionId);
    if (!question) continue;
    results[answer.questionId] = {
      isCorrect: answer.isCorrect,
      selectedOptionId: answer.selectedOptionId,
      correctOptionId: question.options.find((option) => option.isCorrect)?.id ?? "",
      explanation: question.explanation,
      options: question.options,
    };
  }

  return NextResponse.json({
    sessionId: practiceSession.id,
    mode: practiceSession.mode,
    total: practiceSession.total,
    completedAt: practiceSession.completedAt,
    questions,
    results,
  });
}
