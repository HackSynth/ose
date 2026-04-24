import { NextResponse } from "next/server";
import { auth } from "@/lib/auth";
import { getAIProvider, isAIConfigured } from "@/lib/ai";
import { checkAIRateLimit } from "@/lib/ai/rate-limit";
import { createAIErrorResponse, streamText } from "@/lib/ai/utils";
import { EXPLAIN_SYSTEM_PROMPT, buildExplainUserMessage } from "@/lib/ai/prompts";
import { prisma } from "@/lib/prisma";

export async function POST(request: Request) {
  try {
    const session = await auth();
    if (!session?.user?.id) return NextResponse.json({ message: "请先登录" }, { status: 401 });
    const userId = session.user.id;
    if (!(await isAIConfigured(userId))) return NextResponse.json({ message: "请在个人中心填入 API Key 或设置环境变量以启用 AI" }, { status: 503 });
    if (!checkAIRateLimit(userId)) return NextResponse.json({ message: "AI 调用太频繁啦，请稍后再试" }, { status: 429 });
    const body = await request.json().catch(() => ({}));
    const questionId = String(body.questionId ?? "");
    const userAnswerOptionId = body.userAnswerOptionId ? String(body.userAnswerOptionId) : "";
    const question = await prisma.question.findUnique({ where: { id: questionId }, include: { options: { orderBy: { label: "asc" } } } });
    if (!question) return NextResponse.json({ message: "题目不存在" }, { status: 404 });
    const selected = question.options.find((option) => option.id === userAnswerOptionId);
    const userMessage = buildExplainUserMessage(question, selected?.label ?? "未选择", Boolean(selected?.isCorrect));
    const provider = await getAIProvider(userId);
    return streamText(provider.streamCompletion({ systemPrompt: EXPLAIN_SYSTEM_PROMPT, userMessage, maxTokens: 1600, temperature: 0.2 }));
  } catch (error) {
    return createAIErrorResponse(error);
  }
}
