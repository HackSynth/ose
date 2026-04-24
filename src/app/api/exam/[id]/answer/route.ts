import { NextResponse } from "next/server";
import { auth } from "@/lib/auth";
import { prisma } from "@/lib/prisma";
import { clampInt } from "@/lib/validate";

function sanitizeCaseAnswers(value: unknown): Record<string, string> | undefined {
  if (!value || typeof value !== "object") return undefined;
  const result: Record<string, string> = {};
  let size = 0;
  for (const [k, v] of Object.entries(value as Record<string, unknown>)) {
    if (typeof k !== "string" || typeof v !== "string") continue;
    const trimmed = v.slice(0, 4000);
    size += trimmed.length;
    if (size > 20_000) break;
    result[k] = trimmed;
  }
  return Object.keys(result).length ? result : undefined;
}

export async function POST(request: Request, { params }: { params: Promise<{ id: string }> }) {
  const session = await auth();
  if (!session?.user?.id) return NextResponse.json({ message: "请先登录" }, { status: 401 });
  const userId = session.user.id;
  const { id: attemptId } = await params;

  const body = await request.json().catch(() => ({}));
  const questionId = String(body.questionId ?? "");
  const selectedOptionId = body.selectedOptionId ? String(body.selectedOptionId) : null;
  const caseAnswers = sanitizeCaseAnswers(body.caseAnswers);
  const timeSpent = clampInt(body.timeSpent, 0, 24 * 3600, 0);

  if (!questionId) return NextResponse.json({ message: "参数不完整" }, { status: 400 });

  const attempt = await prisma.examAttempt.findFirst({
    where: { id: attemptId, userId, status: "IN_PROGRESS" },
    select: { id: true, examId: true },
  });
  if (!attempt) return NextResponse.json({ message: "考试不存在或已结束" }, { status: 404 });

  const examQuestion = await prisma.examQuestion.findFirst({
    where: { examId: attempt.examId, questionId },
    select: { id: true },
  });
  if (!examQuestion) return NextResponse.json({ message: "题目不属于该试卷" }, { status: 404 });

  let isCorrect: boolean | null = null;
  if (selectedOptionId) {
    const option = await prisma.questionOption.findUnique({ where: { id: selectedOptionId }, select: { questionId: true, isCorrect: true } });
    if (!option || option.questionId !== questionId) {
      return NextResponse.json({ message: "选项不属于该题目" }, { status: 400 });
    }
    isCorrect = option.isCorrect;
  }

  await prisma.examAnswer.upsert({
    where: { examAttemptId_questionId: { examAttemptId: attemptId, questionId } },
    update: { selectedOptionId, caseAnswers, isCorrect, timeSpent },
    create: { examAttemptId: attemptId, questionId, selectedOptionId, caseAnswers, isCorrect, timeSpent },
  });
  return NextResponse.json({ message: "答案已保存" });
}
