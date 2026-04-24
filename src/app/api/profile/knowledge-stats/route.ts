import { NextResponse } from "next/server";
import { auth } from "@/lib/auth";
import { prisma } from "@/lib/prisma";

export async function GET() {
  const session = await auth();
  if (!session?.user?.id) return NextResponse.json({ message: "请先登录" }, { status: 401 });
  const roots = await prisma.knowledgePoint.findMany({ where: { parentId: null }, orderBy: { sortOrder: "asc" }, include: { children: { select: { id: true } } } });
  const answers = await prisma.userAnswer.findMany({ where: { userId: session.user.id }, select: { isCorrect: true, question: { select: { knowledgePointId: true, knowledgePoint: { select: { parentId: true } } } } } });
  const stats = roots.map((root) => {
    const childIds = root.children.map((child) => child.id);
    const rows = answers.filter((answer) => answer.question.knowledgePointId === root.id || answer.question.knowledgePoint.parentId === root.id || childIds.includes(answer.question.knowledgePointId));
    const correct = rows.filter((answer) => answer.isCorrect).length;
    return { id: root.id, name: root.name, total: rows.length, accuracy: rows.length ? Math.round((correct / rows.length) * 100) : 0 };
  });
  return NextResponse.json({ stats });
}
