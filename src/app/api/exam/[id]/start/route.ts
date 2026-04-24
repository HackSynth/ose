import { NextResponse } from "next/server";
import { auth } from "@/lib/auth";
import { prisma } from "@/lib/prisma";

export async function POST(_request: Request, { params }: { params: Promise<{ id: string }> }) {
  const session = await auth();
  if (!session?.user?.id) return NextResponse.json({ message: "请先登录" }, { status: 401 });
  const userId = session.user.id;
  const { id: examId } = await params;

  const exam = await prisma.exam.findUnique({ where: { id: examId }, select: { id: true, createdByUserId: true } });
  if (!exam) return NextResponse.json({ message: "试卷不存在" }, { status: 404 });
  if (exam.createdByUserId && exam.createdByUserId !== userId) {
    return NextResponse.json({ message: "无权访问该试卷" }, { status: 403 });
  }

  const existing = await prisma.examAttempt.findFirst({ where: { userId, examId, status: "IN_PROGRESS" }, orderBy: { startedAt: "desc" } });
  if (existing) return NextResponse.json({ attemptId: existing.id, resumed: true });

  const attempt = await prisma.examAttempt.create({ data: { userId, examId } });
  return NextResponse.json({ attemptId: attempt.id });
}
