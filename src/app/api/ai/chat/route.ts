import { NextResponse } from "next/server";
import { auth } from "@/lib/auth";
import { getAIProvider, isAIConfigured } from "@/lib/ai";
import { checkAIRateLimit } from "@/lib/ai/rate-limit";
import { createAIErrorResponse, streamText } from "@/lib/ai/utils";
import { CHAT_SYSTEM_PROMPT } from "@/lib/ai/prompts";
import type { AIMessage } from "@/lib/ai/types";

const MAX_MESSAGE_CHARS = 4000;

export async function POST(request: Request) {
  try {
    const session = await auth();
    if (!session?.user?.id) return NextResponse.json({ message: "请先登录" }, { status: 401 });
    const userId = session.user.id;
    if (!(await isAIConfigured(userId))) return NextResponse.json({ message: "请在个人中心填入 API Key 或设置环境变量以启用 AI" }, { status: 503 });
    if (!checkAIRateLimit(userId)) return NextResponse.json({ message: "AI 调用太频繁啦，请稍后再试" }, { status: 429 });
    const body = await request.json().catch(() => ({}));
    const rawMessages: unknown[] = Array.isArray(body.messages) ? body.messages : [];
    const messages: AIMessage[] = rawMessages
      .slice(-20)
      .filter((message: unknown): message is AIMessage =>
        typeof message === "object" &&
        message !== null &&
        "role" in message &&
        "content" in message &&
        ["user", "assistant"].includes(String((message as { role: unknown }).role)) &&
        typeof (message as { content: unknown }).content === "string",
      )
      .map((message) => ({ ...message, content: message.content.slice(0, MAX_MESSAGE_CHARS) }));
    if (!messages.length) return NextResponse.json({ message: "请输入问题" }, { status: 400 });
    const provider = await getAIProvider(userId);
    return streamText(provider.streamCompletion({ systemPrompt: CHAT_SYSTEM_PROMPT, userMessage: messages[messages.length - 1].content, messages, maxTokens: 1400, temperature: 0.4 }));
  } catch (error) {
    return createAIErrorResponse(error);
  }
}
