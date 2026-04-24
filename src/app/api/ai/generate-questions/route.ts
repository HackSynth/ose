import { NextResponse } from "next/server";
import { auth } from "@/lib/auth";
import { getAIProvider, isAIConfigured } from "@/lib/ai";
import { checkAIRateLimit } from "@/lib/ai/rate-limit";
import { createAIErrorResponse } from "@/lib/ai/utils";
import { parseAIJson, clampInt } from "@/lib/ai/json";
import { buildGenerateChoiceUserMessage, GENERATE_CHOICE_SYSTEM_PROMPT } from "@/lib/ai/prompts";
import { findBestKnowledgePoint, normalizeChoiceQuestion, type ChoiceAIQuestion } from "@/lib/ai/generation";
import { prisma } from "@/lib/prisma";

type ChoiceResponse = { questions: ChoiceAIQuestion[] };

export async function POST(request: Request) {
  try {
    const session = await auth();
    if (!session?.user?.id) return NextResponse.json({ message: "请先登录" }, { status: 401 });
    const userId = session.user.id;
    if (!(await isAIConfigured(userId))) return NextResponse.json({ message: "请在个人中心填入 API Key 或设置环境变量以启用 AI" }, { status: 503 });
    if (!checkAIRateLimit(userId)) return NextResponse.json({ message: "AI 调用太频繁啦，请稍后再试" }, { status: 429 });

    const body = await request.json().catch(() => ({}));
    const difficulty = clampInt(body.difficulty, 1, 5, 3);
    const count = clampInt(body.count, 1, 20, 5);
    const knowledgePointIds = Array.isArray(body.knowledgePointIds) ? body.knowledgePointIds.map(String).filter(Boolean) : [];
    const knowledgePoints = knowledgePointIds.length
      ? await prisma.knowledgePoint.findMany({ where: { id: { in: knowledgePointIds } } })
      : await prisma.knowledgePoint.findMany({ take: 8, orderBy: { sortOrder: "asc" } });
    const existing = await prisma.question.findMany({ where: knowledgePointIds.length ? { knowledgePointId: { in: knowledgePointIds } } : undefined, take: 10, orderBy: { questionNumber: "desc" }, select: { content: true } });
    const provider = await getAIProvider(userId);
    const raw = await provider.createCompletion({
      systemPrompt: GENERATE_CHOICE_SYSTEM_PROMPT,
      userMessage: buildGenerateChoiceUserMessage({ count, difficulty, knowledgePoints: knowledgePoints.map((item) => item.name).join("、") || "全部", existingQuestionSummaries: existing.map((item) => `- ${item.content}`).join("\n") || "无" }),
      maxTokens: Math.min(6000, 900 + count * 700),
      temperature: 0.35,
    });
    const parsed = parseAIJson<ChoiceResponse>(raw);
    const questions = (parsed.questions ?? []).slice(0, count).map((question) => normalizeChoiceQuestion(question, difficulty));
    if (!questions.length) return NextResponse.json({ message: "AI 没有返回有效题目" }, { status: 502 });

    const providerName = provider.getInfo().name;

    // Resolve all knowledge-point names up-front (parallel) — avoids N+1 lookups inside the transaction.
    const resolvedKps = await Promise.all(
      questions.map((question) => findBestKnowledgePoint(question.knowledgePointName, knowledgePointIds)),
    );

    const createdIds = await prisma.$transaction(async (tx) => {
      const aggregate = await tx.question.aggregate({ where: { year: 2099 }, _max: { questionNumber: true } });
      let questionNumber = (aggregate._max.questionNumber ?? 0) + 1;
      // createMany does not support nested writes; create questions sequentially but only one write per
      // question (vs. the previous pattern that also looked up a knowledge point per iteration).
      const ids: string[] = [];
      for (let i = 0; i < questions.length; i += 1) {
        const question = questions[i];
        const knowledgePoint = resolvedKps[i];
        const created = await tx.question.create({
          data: {
            content: question.content,
            type: "CHOICE",
            difficulty,
            year: 2099,
            session: "AM",
            questionNumber: questionNumber++,
            explanation: question.explanation,
            isAIGenerated: true,
            aiGeneratedBy: providerName,
            createdByUserId: userId,
            knowledgePointId: knowledgePoint.id,
            options: { create: question.options.map((option) => ({ label: option.label, content: option.content, isCorrect: option.isCorrect })) },
          },
          select: { id: true },
        });
        ids.push(created.id);
      }
      await tx.aIQuestionGeneration.create({
        data: { userId, type: "CHOICE", knowledgePointNames: knowledgePoints.map((item) => item.name).join("、") || "全部", difficulty, count: ids.length, questionIds: ids },
      });
      return ids;
    });

    return NextResponse.json({ questionIds: createdIds });
  } catch (error) {
    if (error && typeof error === "object" && "code" in error && (error as { code?: string }).code === "P2002") {
      return NextResponse.json({ message: "题目编号冲突，请稍后重试" }, { status: 409 });
    }
    return createAIErrorResponse(error);
  }
}
