import { NextResponse } from "next/server";
import { auth } from "@/lib/auth";
import { prisma } from "@/lib/prisma";

export async function GET() {
  const session = await auth();
  if (!session?.user?.id) return NextResponse.json({ message: "请先登录" }, { status: 401 });
  const userId = session.user.id;
  const [exams, inProgress] = await Promise.all([
    prisma.exam.findMany({
      where: { OR: [{ createdByUserId: null }, { createdByUserId: userId }] },
      orderBy: { createdAt: "asc" },
      include: {
        _count: { select: { questions: true } },
        attempts: {
          where: { userId },
          orderBy: { startedAt: "desc" },
          select: { id: true, status: true, totalScore: true, startedAt: true, finishedAt: true },
        },
      },
    }),
    prisma.examAttempt.findFirst({ where: { userId, status: "IN_PROGRESS" }, orderBy: { startedAt: "desc" }, include: { exam: { select: { id: true, title: true } } } }),
  ]);
  return NextResponse.json({
    inProgress,
    exams: exams.map((exam) => {
      const completed = exam.attempts.filter((attempt) => attempt.status === "COMPLETED");
      const bestScore = completed.reduce((max, attempt) => Math.max(max, attempt.totalScore ?? 0), 0);
      return { id: exam.id, title: exam.title, type: exam.type, session: exam.session, timeLimit: exam.timeLimit, totalScore: exam.totalScore, questionCount: exam._count.questions, bestScore, lastAttempt: exam.attempts[0] ?? null };
    }),
  });
}
