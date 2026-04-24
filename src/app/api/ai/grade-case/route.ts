import { NextResponse } from "next/server";
import { auth } from "@/lib/auth";
import { getAIProvider, isAIConfigured } from "@/lib/ai";
import { checkAIRateLimit } from "@/lib/ai/rate-limit";
import { cleanJsonText, createAIErrorResponse } from "@/lib/ai/utils";
import { GRADE_CASE_SYSTEM_PROMPT, buildGradeCaseUserMessage } from "@/lib/ai/prompts";
import { prisma } from "@/lib/prisma";

type AIGradeResult = {
  subQuestions: Array<{ subNumber: number; score: number; maxScore: number; feedback: string; correctParts: string; missingParts: string }>;
  totalScore: number;
  totalMaxScore: number;
  overallFeedback: string;
};

function isValidGradeResult(value: unknown): value is AIGradeResult {
  if (!value || typeof value !== "object") return false;
  const v = value as Partial<AIGradeResult>;
  return Array.isArray(v.subQuestions) && typeof v.overallFeedback === "string";
}

export async function POST(request: Request) {
  try {
    const session = await auth();
    if (!session?.user?.id) return NextResponse.json({ message: "请先登录" }, { status: 401 });
    const userId = session.user.id;
    if (!(await isAIConfigured(userId))) return NextResponse.json({ message: "请在个人中心填入 API Key 或设置环境变量以启用 AI" }, { status: 503 });
    if (!checkAIRateLimit(userId)) return NextResponse.json({ message: "AI 调用太频繁啦，请稍后再试" }, { status: 429 });

    const body = await request.json().catch(() => ({}));
    const caseScenarioId = String(body.caseScenarioId ?? "");
    const rawAnswers = (body.userAnswers && typeof body.userAnswers === "object") ? (body.userAnswers as Record<string, unknown>) : {};
    if (!caseScenarioId) return NextResponse.json({ message: "参数不完整" }, { status: 400 });

    const scenario = await prisma.caseScenario.findUnique({
      where: { id: caseScenarioId },
      include: { subQuestions: { orderBy: { subNumber: "asc" } } },
    });
    if (!scenario) return NextResponse.json({ message: "案例题不存在" }, { status: 404 });

    const bySubNumber: Record<string, string> = Object.fromEntries(
      scenario.subQuestions.map((subQuestion) => {
        const bySubId = rawAnswers[subQuestion.id];
        const byNumber = rawAnswers[String(subQuestion.subNumber)];
        const value = typeof bySubId === "string" ? bySubId : typeof byNumber === "string" ? byNumber : "";
        return [String(subQuestion.subNumber), value.slice(0, 4000)];
      }),
    );

    const provider = await getAIProvider(userId);
    const completion = await provider.createCompletion({
      systemPrompt: GRADE_CASE_SYSTEM_PROMPT,
      userMessage: buildGradeCaseUserMessage(scenario, scenario.subQuestions, bySubNumber),
      maxTokens: 2000,
      temperature: 0.2,
    });

    let parsed: AIGradeResult;
    try {
      const raw = JSON.parse(cleanJsonText(completion)) as unknown;
      if (!isValidGradeResult(raw)) throw new Error("shape");
      parsed = raw;
    } catch {
      return NextResponse.json({ message: "AI 批改结果解析失败，请稍后再试" }, { status: 502 });
    }

    await prisma.$transaction(async (tx) => {
      for (const subQuestion of scenario.subQuestions) {
        const grade = parsed.subQuestions.find((item) => item.subNumber === subQuestion.subNumber);
        const rawScore = typeof grade?.score === "number" ? grade.score : null;
        const clampedScore = rawScore === null ? null : Math.max(0, Math.min(subQuestion.score, Math.round(rawScore)));
        await tx.userCaseAnswer.upsert({
          where: { userId_caseSubQuestionId: { userId, caseSubQuestionId: subQuestion.id } },
          update: { answer: bySubNumber[String(subQuestion.subNumber)] ?? "", score: clampedScore, feedback: grade ? JSON.stringify(grade) : parsed.overallFeedback },
          create: { userId, caseSubQuestionId: subQuestion.id, answer: bySubNumber[String(subQuestion.subNumber)] ?? "", score: clampedScore, feedback: grade ? JSON.stringify(grade) : parsed.overallFeedback },
        });
      }
    });
    return NextResponse.json(parsed);
  } catch (error) {
    return createAIErrorResponse(error);
  }
}
