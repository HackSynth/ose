import { NextResponse } from "next/server";
import { auth } from "@/lib/auth";
import { prisma } from "@/lib/prisma";
import { gradeCaseLocal, gradeCaseWithAI } from "@/lib/grade";

export async function POST(request: Request, { params }: { params: Promise<{ id: string }> }) {
  const session = await auth();
  if (!session?.user?.id) return NextResponse.json({ message: "请先登录" }, { status: 401 });
  const userId = session.user.id;
  const { id } = await params;

  const body = await request.json().catch(() => ({}));
  const answers = Array.isArray(body.answers) ? (body.answers as Array<{ caseSubQuestionId?: unknown; answer?: unknown }>) : [];
  if (!answers.length) return NextResponse.json({ message: "请填写答案" }, { status: 400 });

  const question = await prisma.question.findFirst({ where: { id, type: "CASE_ANALYSIS" }, include: { caseScenario: { include: { subQuestions: true } } } });
  if (!question?.caseScenario) return NextResponse.json({ message: "案例题不存在" }, { status: 404 });

  const validSubIds = new Set(question.caseScenario.subQuestions.map((s) => s.id));
  const answerMap = new Map<string, string>();
  for (const item of answers) {
    const subId = typeof item?.caseSubQuestionId === "string" ? item.caseSubQuestionId : "";
    if (!validSubIds.has(subId)) continue;
    const raw = typeof item?.answer === "string" ? item.answer : "";
    answerMap.set(subId, raw.slice(0, 4000).trim());
  }

  const subs = question.caseScenario.subQuestions;
  const fullAnswerMap: Record<string, string> = Object.fromEntries(subs.map((sub) => [sub.id, answerMap.get(sub.id) ?? ""]));
  const aiGraded = await gradeCaseWithAI({ background: question.caseScenario.background }, subs, fullAnswerMap, userId);
  const graded = aiGraded ?? gradeCaseLocal(subs, fullAnswerMap);
  const gradeBySubId = new Map(graded.map((item) => [item.subId, item]));

  const results = await prisma.$transaction(async (tx) => {
    const out: Array<{ id: string; caseSubQuestionId: string; subNumber: number; answer: string; score: number; maxScore: number; feedback: string; referenceAnswer: string; explanation: string; source: "ai" | "keyword" }> = [];
    for (const subQuestion of subs) {
      const userAnswer = fullAnswerMap[subQuestion.id];
      const grade = gradeBySubId.get(subQuestion.id) ?? { score: 0, feedback: "暂未评分", source: "keyword" as const };
      const saved = await tx.userCaseAnswer.upsert({
        where: { userId_caseSubQuestionId: { userId, caseSubQuestionId: subQuestion.id } },
        update: { answer: userAnswer, score: grade.score, feedback: grade.feedback },
        create: { userId, caseSubQuestionId: subQuestion.id, answer: userAnswer, score: grade.score, feedback: grade.feedback },
      });
      out.push({ id: saved.id, caseSubQuestionId: subQuestion.id, subNumber: subQuestion.subNumber, answer: userAnswer, score: grade.score, maxScore: subQuestion.score, feedback: grade.feedback, referenceAnswer: subQuestion.referenceAnswer, explanation: subQuestion.explanation, source: grade.source });
    }
    return out;
  });

  return NextResponse.json({
    totalScore: results.reduce((sum, item) => sum + item.score, 0),
    maxScore: results.reduce((sum, item) => sum + item.maxScore, 0),
    gradedBy: aiGraded ? "ai" : "keyword",
    results,
  });
}
