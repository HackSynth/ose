import { NextResponse } from "next/server";
import { auth } from "@/lib/auth";
import { prisma } from "@/lib/prisma";
import { getContinuousDays, getLongestStreak } from "@/lib/stats";

export async function GET() {
  const session = await auth();
  if (!session?.user?.id) return NextResponse.json({ message: "请先登录" }, { status: 401 });
  const answers = await prisma.userAnswer.findMany({ where: { userId: session.user.id }, select: { isCorrect: true, createdAt: true } });
  const total = answers.length;
  const correct = answers.filter((answer) => answer.isCorrect).length;
  return NextResponse.json({
    totalAnswers: total,
    accuracy: total ? Math.round((correct / total) * 100) : 0,
    studyDays: new Set(answers.map((answer) => answer.createdAt.toDateString())).size,
    currentStreak: getContinuousDays(answers.map((answer) => answer.createdAt)),
    longestStreak: getLongestStreak(answers.map((answer) => answer.createdAt)),
  });
}
