import { getAIProvider, isAIConfigured } from "@/lib/ai";
import { cleanJsonText } from "@/lib/ai/utils";
import { GRADE_CASE_SYSTEM_PROMPT, buildGradeCaseUserMessage } from "@/lib/ai/prompts";
import { scoreTextAnswer } from "@/lib/exam";

export type CaseSubLike = { id: string; subNumber: number; content: string; referenceAnswer: string; score: number };
export type GradedSub = { subId: string; score: number; feedback: string; source: "ai" | "keyword" };

export async function gradeCaseWithAI(
  scenario: { background: string },
  subQuestions: CaseSubLike[],
  answers: Record<string, string>,
  userId?: string | null,
): Promise<GradedSub[] | null> {
  if (!(await isAIConfigured(userId))) return null;
  try {
    const bySubNumber: Record<string, string> = Object.fromEntries(
      subQuestions.map((sub) => [String(sub.subNumber), (answers[sub.id] ?? "").slice(0, 4000)]),
    );
    const provider = await getAIProvider(userId);
    const completion = await provider.createCompletion({
      systemPrompt: GRADE_CASE_SYSTEM_PROMPT,
      userMessage: buildGradeCaseUserMessage(scenario, subQuestions, bySubNumber),
      maxTokens: 2000,
      temperature: 0.2,
    });
    const parsed = JSON.parse(cleanJsonText(completion)) as unknown;
    if (!parsed || typeof parsed !== "object" || !Array.isArray((parsed as { subQuestions?: unknown }).subQuestions)) {
      return null;
    }
    const items = (parsed as { subQuestions: Array<{ subNumber?: number; score?: number; feedback?: string; correctParts?: string; missingParts?: string }> }).subQuestions;
    const results: GradedSub[] = [];
    for (const sub of subQuestions) {
      const grade = items.find((item) => item.subNumber === sub.subNumber);
      if (!grade || typeof grade.score !== "number") {
        results.push({ subId: sub.id, score: scoreTextAnswer(answers[sub.id] ?? "", sub.referenceAnswer, sub.score), feedback: "AI 未能返回该子题评分，已回落到关键词评分。", source: "keyword" });
        continue;
      }
      const clamped = Math.max(0, Math.min(sub.score, Math.round(grade.score)));
      const feedbackParts = [grade.feedback, grade.correctParts ? `✓ ${grade.correctParts}` : "", grade.missingParts ? `✗ ${grade.missingParts}` : ""].filter(Boolean);
      results.push({ subId: sub.id, score: clamped, feedback: feedbackParts.join(" · ") || "AI 已完成批改。", source: "ai" });
    }
    return results;
  } catch {
    return null;
  }
}

export function gradeCaseLocal(subQuestions: CaseSubLike[], answers: Record<string, string>): GradedSub[] {
  return subQuestions.map((sub) => {
    const score = scoreTextAnswer(answers[sub.id] ?? "", sub.referenceAnswer, sub.score);
    const feedback = score >= sub.score * 0.8 ? "关键词覆盖较完整，答案接近参考答案。" : score > 0 ? "命中部分关键词，可继续补充关键概念。" : "暂未命中核心关键词，建议对照参考答案复盘。";
    return { subId: sub.id, score, feedback, source: "keyword" };
  });
}
