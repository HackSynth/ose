import { NextResponse } from "next/server";
import { auth } from "@/lib/auth";
import { prisma } from "@/lib/prisma";
import { visibilityFilter } from "@/lib/knowledge-stats";
import { EXAM_SCORE_CASE } from "@/lib/constants";

export async function GET() {
  const session = await auth();
  if (!session?.user?.id) return NextResponse.json({ message: "请先登录" }, { status: 401 });
  const userId = session.user.id;

  const [cases, answeredRows] = await Promise.all([
    prisma.question.findMany({
      where: { type: "CASE_ANALYSIS", ...visibilityFilter(userId) },
      orderBy: [{ year: "desc" }, { questionNumber: "asc" }],
      select: {
        id: true,
        content: true,
        questionNumber: true,
        year: true,
        difficulty: true,
        knowledgePoint: { select: { name: true, parent: { select: { name: true } } } },
        caseScenario: {
          select: {
            subQuestions: { select: { id: true, score: true } },
          },
        },
      },
    }),
    prisma.userCaseAnswer.findMany({ where: { userId }, select: { caseSubQuestionId: true } }),
  ]);

  const answered = new Set(answeredRows.map((row) => row.caseSubQuestionId));
  return NextResponse.json({
    cases: cases.map((item) => ({
      id: item.id,
      title: item.content,
      number: item.questionNumber,
      year: item.year,
      difficulty: item.difficulty,
      tag: item.knowledgePoint.name,
      parentTag: item.knowledgePoint.parent?.name,
      score: item.caseScenario?.subQuestions.reduce((sum, sub) => sum + sub.score, 0) ?? EXAM_SCORE_CASE,
      answered: Boolean(item.caseScenario?.subQuestions.some((sub) => answered.has(sub.id))),
    })),
  });
}
