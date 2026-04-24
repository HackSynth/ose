import { NextResponse } from "next/server";
import { auth } from "@/lib/auth";
import { getAIProvider, isAIConfigured } from "@/lib/ai";
import { checkAIRateLimit } from "@/lib/ai/rate-limit";
import { createAIErrorResponse } from "@/lib/ai/utils";
import { parseAIJson, clampInt } from "@/lib/ai/json";
import { buildGenerateCaseUserMessage, GENERATE_CASE_SYSTEM_PROMPT } from "@/lib/ai/prompts";
import { findBestKnowledgePoint, type CaseAIQuestion } from "@/lib/ai/generation";
import { prisma } from "@/lib/prisma";

const answerTypes = new Set(["FILL_BLANK", "SHORT_ANSWER", "DIAGRAM_FILL"]);

export async function POST(request: Request) {
  try {
    const session = await auth();
    if (!session?.user?.id) return NextResponse.json({ message: "请先登录" }, { status: 401 });
    const userId = session.user.id;
    if (!(await isAIConfigured(userId))) return NextResponse.json({ message: "请在个人中心填入 API Key 或设置环境变量以启用 AI" }, { status: 503 });
    if (!checkAIRateLimit(userId)) return NextResponse.json({ message: "AI 调用太频繁啦，请稍后再试" }, { status: 429 });

    const body = await request.json().catch(() => ({}));
    const difficulty = clampInt(body.difficulty, 1, 5, 3);
    const caseType = String(body.caseType || "随机").slice(0, 40);
    const provider = await getAIProvider(userId);
    const raw = await provider.createCompletion({ systemPrompt: GENERATE_CASE_SYSTEM_PROMPT, userMessage: buildGenerateCaseUserMessage({ caseType, difficulty }), maxTokens: 4500, temperature: 0.35 });
    const parsed = parseAIJson<CaseAIQuestion>(raw);
    if (!parsed.background || !parsed.subQuestions?.length) return NextResponse.json({ message: "AI 没有返回有效案例题" }, { status: 502 });

    const knowledgePoint = await findBestKnowledgePoint(parsed.knowledgePointName);
    const providerName = provider.getInfo().name;

    const questionId = await prisma.$transaction(async (tx) => {
      const aggregate = await tx.question.aggregate({ where: { year: 2099 }, _max: { questionNumber: true } });
      const questionNumber = (aggregate._max.questionNumber ?? 0) + 1;
      const question = await tx.question.create({
        data: {
          content: `AI 案例分析：${caseType}`,
          type: "CASE_ANALYSIS",
          difficulty,
          year: 2099,
          session: "PM",
          questionNumber,
          explanation: "请完成各子题后查看参考答案与解析。",
          isAIGenerated: true,
          aiGeneratedBy: providerName,
          createdByUserId: userId,
          knowledgePointId: knowledgePoint.id,
          caseScenario: {
            create: {
              background: parsed.background,
              subQuestions: {
                create: parsed.subQuestions.slice(0, 5).map((subQuestion, index) => ({
                  subNumber: subQuestion.subNumber || index + 1,
                  content: subQuestion.content,
                  answerType: answerTypes.has(subQuestion.answerType) ? subQuestion.answerType : "SHORT_ANSWER",
                  referenceAnswer: subQuestion.referenceAnswer,
                  score: subQuestion.score || 3,
                  explanation: subQuestion.explanation,
                })),
              },
            },
          },
        },
      });
      await tx.aIQuestionGeneration.create({ data: { userId, type: "CASE_ANALYSIS", knowledgePointNames: knowledgePoint.name, difficulty, count: 1, caseType, questionIds: [question.id] } });
      return question.id;
    });

    return NextResponse.json({ questionId });
  } catch (error) {
    if (error && typeof error === "object" && "code" in error && (error as { code?: string }).code === "P2002") {
      return NextResponse.json({ message: "题目编号冲突，请稍后重试" }, { status: 409 });
    }
    return createAIErrorResponse(error);
  }
}
