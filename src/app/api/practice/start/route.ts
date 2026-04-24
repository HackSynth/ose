import { NextResponse } from "next/server";
import { auth } from "@/lib/auth";
import { prisma } from "@/lib/prisma";
import { questionSelect, shuffle, stripCorrectFlags } from "@/lib/practice";
import { getDescendantTopicIds, visibilityFilter } from "@/lib/knowledge-stats";
import { clampInt } from "@/lib/validate";
import { PRACTICE_AI_MAX_IDS, PRACTICE_DEFAULT_LIMIT, PRACTICE_MAX_LIMIT } from "@/lib/constants";
import type { Prisma } from "@prisma/client";

const ALLOWED_MODES = new Set(["random", "topic", "sequential", "ai"]);

export async function POST(request: Request) {
  const session = await auth();
  if (!session?.user?.id) return NextResponse.json({ message: "未登录" }, { status: 401 });
  const userId = session.user.id;

  const body = await request.json().catch(() => ({}));
  const mode = String(body.mode ?? "random");
  if (!ALLOWED_MODES.has(mode)) return NextResponse.json({ message: "mode 参数不合法" }, { status: 400 });

  const limit = clampInt(body.limit, 1, PRACTICE_MAX_LIMIT, PRACTICE_DEFAULT_LIMIT);
  const topicId = body.topicId ? String(body.topicId) : undefined;
  const questionIds: string[] = Array.isArray(body.questionIds)
    ? body.questionIds.map(String).filter(Boolean).slice(0, PRACTICE_AI_MAX_IDS)
    : [];

  const baseWhere = visibilityFilter(userId);
  let where: Prisma.QuestionWhereInput = { ...baseWhere, type: "CHOICE" };
  let orderBy: { questionNumber: "asc" } | undefined;
  let take: number | undefined = limit;

  if (mode === "ai") {
    if (questionIds.length === 0) return NextResponse.json({ message: "缺少 AI 生成题目" }, { status: 400 });
    where = { ...baseWhere, type: "CHOICE", id: { in: questionIds } };
    take = undefined;
  } else if (mode === "topic") {
    if (!topicId) return NextResponse.json({ message: "缺少知识点" }, { status: 400 });
    const topicIds = await getDescendantTopicIds(topicId);
    where = { ...baseWhere, type: "CHOICE", knowledgePointId: { in: topicIds } };
    orderBy = { questionNumber: "asc" };
  } else if (mode === "sequential") {
    orderBy = { questionNumber: "asc" };
  }

  let selected;
  if (mode === "random") {
    // Two-phase random: fetch ids only, shuffle in memory, then detail-load the picked slice.
    // For SQLite, ORDER BY RANDOM() on Question is ok at moderate size but still scans; this
    // approach scales better and keeps payload minimal.
    const ids = await prisma.question.findMany({ where, select: { id: true } });
    const pickedIds = shuffle(ids).slice(0, limit).map((row) => row.id);
    const rows = pickedIds.length
      ? await prisma.question.findMany({ where: { id: { in: pickedIds } }, select: questionSelect })
      : [];
    const byId = new Map(rows.map((row) => [row.id, row]));
    selected = pickedIds.map((id) => byId.get(id)).filter((row): row is NonNullable<typeof row> => Boolean(row));
  } else {
    const questions = await prisma.question.findMany({ where, orderBy, select: questionSelect, ...(take ? { take } : {}) });
    selected = mode === "ai"
      ? questionIds.map((id) => questions.find((question) => question.id === id)).filter((question): question is NonNullable<typeof question> => Boolean(question))
      : questions;
  }

  if (selected.length === 0) {
    return NextResponse.json({ message: "暂无可练习题目" }, { status: 404 });
  }

  const practiceSession = await prisma.practiceSession.create({
    data: {
      userId,
      mode,
      topicId,
      total: selected.length,
      questions: {
        create: selected.map((question, index) => ({ questionId: question.id, order: index + 1 })),
      },
    },
  });

  return NextResponse.json({
    sessionId: practiceSession.id,
    questions: selected.map(stripCorrectFlags),
  });
}
