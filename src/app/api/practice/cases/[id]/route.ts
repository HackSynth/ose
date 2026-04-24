import { NextResponse } from "next/server";
import { auth } from "@/lib/auth";
import { prisma } from "@/lib/prisma";

export async function GET(_request: Request, { params }: { params: Promise<{ id: string }> }) {
  const session = await auth();
  if (!session?.user?.id) return NextResponse.json({ message: "请先登录" }, { status: 401 });
  const { id } = await params;
  const question = await prisma.question.findFirst({
    where: { id, type: "CASE_ANALYSIS" },
    include: {
      knowledgePoint: { include: { parent: true } },
      caseScenario: { include: { subQuestions: { orderBy: { subNumber: "asc" } } } },
    },
  });
  if (!question?.caseScenario) return NextResponse.json({ message: "案例题不存在" }, { status: 404 });
  return NextResponse.json({ question });
}
