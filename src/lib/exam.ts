export function scoreTextAnswer(answer: string, reference: string, maxScore: number) {
  const trimmed = answer.trim();
  if (!trimmed) return 0;
  const normalized = trimmed.toLowerCase();
  const keywords = Array.from(new Set(
    reference
      .split(/[、，,；;。\s()（）=：:]+/)
      .map((word) => word.trim().toLowerCase())
      .filter((word) => word.length >= 2),
  )).slice(0, 12);
  if (!keywords.length) return 0;
  const hits = keywords.filter((word) => normalized.includes(word)).length;
  if (hits === 0) return 0;
  return Math.min(maxScore, Math.round((hits / keywords.length) * maxScore));
}

export function formatDuration(seconds: number) {
  const minutes = Math.floor(seconds / 60);
  const rest = seconds % 60;
  return `${minutes}分${rest}秒`;
}
