import { NextResponse } from "next/server";
import { auth } from "@/lib/auth";
import { prisma } from "@/lib/prisma";
import { questionSelect, stripCorrectFlags } from "@/lib/practice";

export async function POST() {
  const session = await auth();
  if (!session?.user?.id) return NextResponse.json({ message: "请先登录" }, { status: 401 });
  const userId = session.user.id;

  const fiveMinutesAgo = new Date(Date.now() - 5 * 60_000);
  const recentActive = await prisma.practiceSession.findFirst({
    where: { userId, mode: "wrong-note-retry", completedAt: null, startedAt: { gte: fiveMinutesAgo } },
    orderBy: { startedAt: "desc" },
    include: { questions: { orderBy: { order: "asc" }, include: { question: { select: questionSelect } } } },
  });
  if (recentActive) {
    return NextResponse.json({
      sessionId: recentActive.id,
      questions: recentActive.questions.map((entry) => stripCorrectFlags(entry.question)),
      message: "继续之前的错题重练",
    });
  }

  const notes = await prisma.wrongNote.findMany({
    where: { userId, markedMastered: false },
    orderBy: { updatedAt: "desc" },
    select: { questionId: true },
  });
  if (!notes.length) return NextResponse.json({ message: "暂无未掌握错题" }, { status: 404 });

  const questionIds = notes.map((note) => note.questionId);
  const questions = await prisma.question.findMany({ where: { id: { in: questionIds } }, select: questionSelect });
  const ordered = questionIds.map((id) => questions.find((question) => question.id === id)).filter((question): question is NonNullable<typeof question> => Boolean(question));

  const practiceSession = await prisma.practiceSession.create({
    data: {
      userId,
      mode: "wrong-note-retry",
      total: ordered.length,
      questions: { create: ordered.map((question, index) => ({ questionId: question.id, order: index + 1 })) },
    },
  });

  return NextResponse.json({ sessionId: practiceSession.id, questions: ordered.map(stripCorrectFlags), message: "错题重练已开始" });
}
