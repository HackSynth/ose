import { NextResponse } from "next/server";
import { auth } from "@/lib/auth";
import { getAIProvider, isAIConfigured } from "@/lib/ai";
import { checkAIRateLimit } from "@/lib/ai/rate-limit";
import { createAIErrorResponse, streamText } from "@/lib/ai/utils";
import { CASE_METHODOLOGY_SYSTEM_PROMPT } from "@/lib/ai/prompts";
import { prisma } from "@/lib/prisma";

export async function POST(request: Request) {
  try {
    const session = await auth();
    if (!session?.user?.id) return NextResponse.json({ message: "请先登录" }, { status: 401 });
    const userId = session.user.id;
    if (!(await isAIConfigured(userId))) return NextResponse.json({ message: "请在个人中心填入 API Key 或设置环境变量以启用 AI" }, { status: 503 });
    if (!checkAIRateLimit(userId)) return NextResponse.json({ message: "AI 调用太频繁啦，请稍后再试" }, { status: 429 });
    const body = await request.json().catch(() => ({}));
    const caseScenarioId = String(body.caseScenarioId ?? "");
    const scenario = await prisma.caseScenario.findUnique({ where: { id: caseScenarioId }, include: { question: { include: { knowledgePoint: true } }, subQuestions: { orderBy: { subNumber: "asc" } } } });
    if (!scenario) return NextResponse.json({ message: "案例题不存在" }, { status: 404 });
    const userMessage = `知识点：${scenario.question.knowledgePoint.name}\n案例背景：${scenario.background}\n\n子题：\n${scenario.subQuestions.map((sub) => `${sub.subNumber}. ${sub.content}\n参考答案：${sub.referenceAnswer}\n解析：${sub.explanation}`).join("\n\n")}`;
    const provider = await getAIProvider(userId);
    return streamText(provider.streamCompletion({ systemPrompt: CASE_METHODOLOGY_SYSTEM_PROMPT, userMessage, maxTokens: 2200, temperature: 0.25 }));
  } catch (error) {
    return createAIErrorResponse(error);
  }
}
