import { NextResponse } from "next/server";
import { auth } from "@/lib/auth";
import { prisma } from "@/lib/prisma";
import { visibilityFilter } from "@/lib/knowledge-stats";
import { clampInt } from "@/lib/validate";
import type { Prisma, QuestionType } from "@prisma/client";
import {
  EXAM_DEFAULT_AM_COUNT,
  EXAM_DEFAULT_PM_COUNT,
  EXAM_DEFAULT_TIME_LIMIT_MIN,
  EXAM_SCORE_CASE,
  EXAM_SCORE_CHOICE,
} from "@/lib/constants";

const ALLOWED_SESSIONS = new Set(["AM", "PM", "FULL"] as const);
type ExamSession = "AM" | "PM" | "FULL";

export async function POST(request: Request) {
  const session = await auth();
  if (!session?.user?.id) return NextResponse.json({ message: "请先登录" }, { status: 401 });
  const userId = session.user.id;
  const body = await request.json().catch(() => ({}));

  const sessionInput = String(body.session ?? "AM");
  if (!ALLOWED_SESSIONS.has(sessionInput as ExamSession)) {
    return NextResponse.json({ message: "session 参数不合法" }, { status: 400 });
  }
  const examSession = sessionInput as ExamSession;

  const defaultCount = examSession === "PM" ? EXAM_DEFAULT_PM_COUNT : EXAM_DEFAULT_AM_COUNT;
  const questionCount = clampInt(body.questionCount, 1, 100, defaultCount);

  const knowledgePointIds = Array.isArray(body.knowledgePointIds) ? body.knowledgePointIds.map(String).filter(Boolean) : undefined;
  const typeFilter: QuestionType | undefined = examSession === "PM" ? "CASE_ANALYSIS" : examSession === "AM" ? "CHOICE" : undefined;

  const correctlyAnswered = await prisma.userAnswer.findMany({
    where: { userId, isCorrect: true },
    select: { questionId: true },
    distinct: ["questionId"],
  });
  const masteredIds = new Set(correctlyAnswered.map((row) => row.questionId));

  const baseWhere: Prisma.QuestionWhereInput = {
    ...(typeFilter ? { type: typeFilter } : {}),
    ...(knowledgePointIds?.length ? { knowledgePointId: { in: knowledgePointIds } } : {}),
    ...visibilityFilter(userId),
  };

  // Prefer wrong/unseen questions first. Do it as two queries so we don't load the whole bank.
  const unseenOrWrong = masteredIds.size
    ? await prisma.question.findMany({
        where: { ...baseWhere, NOT: { id: { in: Array.from(masteredIds) } } },
        select: { id: true, type: true },
        take: questionCount,
      })
    : await prisma.question.findMany({ where: baseWhere, select: { id: true, type: true }, take: questionCount });

  let selected = unseenOrWrong;
  if (selected.length < questionCount && masteredIds.size) {
    const filler = await prisma.question.findMany({
      where: { ...baseWhere, id: { in: Array.from(masteredIds) } },
      select: { id: true, type: true },
      take: questionCount - selected.length,
    });
    selected = [...selected, ...filler];
  }

  if (!selected.length) return NextResponse.json({ message: "题库中暂无可组卷题目" }, { status: 404 });

  const totalScore = selected.reduce((sum, q) => sum + (q.type === "CHOICE" ? EXAM_SCORE_CHOICE : EXAM_SCORE_CASE), 0);
  const exam = await prisma.exam.create({
    data: {
      title: `智能组卷·${examSession === "AM" ? "上午" : examSession === "PM" ? "下午" : "全真"}·${new Date().toLocaleDateString("zh-CN")}`,
      type: "MOCK",
      session: examSession,
      timeLimit: EXAM_DEFAULT_TIME_LIMIT_MIN,
      totalScore,
      createdByUserId: userId,
      questions: { create: selected.map((q, index) => ({ questionId: q.id, orderNumber: index + 1 })) },
    },
  });
  return NextResponse.json({ exam });
}
