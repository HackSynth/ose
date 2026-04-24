export { clampInt } from "@/lib/validate";

export function parseAIJson<T>(content: string): T {
  const trimmed = content.trim().replace(/^```json\s*/i, "").replace(/^```\s*/i, "").replace(/```$/i, "").trim();
  // 1) Try raw parse first — handles well-formed responses of any shape.
  try {
    return JSON.parse(trimmed) as T;
  } catch {
    // fall through to bracket slicing
  }
  // 2) Slice the first object or array (whichever starts earlier).
  const braceStart = trimmed.indexOf("{");
  const bracketStart = trimmed.indexOf("[");
  const candidates: Array<{ start: number; endChar: string }> = [];
  if (braceStart !== -1) candidates.push({ start: braceStart, endChar: "}" });
  if (bracketStart !== -1) candidates.push({ start: bracketStart, endChar: "]" });
  candidates.sort((a, b) => a.start - b.start);
  for (const candidate of candidates) {
    const end = trimmed.lastIndexOf(candidate.endChar);
    if (end > candidate.start) {
      try {
        return JSON.parse(trimmed.slice(candidate.start, end + 1)) as T;
      } catch {
        continue;
      }
    }
  }
  throw new Error("AI 返回内容不是有效 JSON");
}
