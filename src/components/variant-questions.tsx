"use client";

import { useState } from "react";
import { Loader2, Sparkles, CheckCircle2, XCircle } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { cn } from "@/lib/utils";

type Variant = { content: string; explanation: string; options: Array<{ label: string; content: string; isCorrect: boolean }> };

export function VariantQuestions({ questionId }: { questionId: string }) {
  const [loading, setLoading] = useState(false);
  const [variants, setVariants] = useState<Variant[]>([]);
  const [selected, setSelected] = useState<Record<number, string>>({});
  const [error, setError] = useState("");

  async function load() {
    if (loading) return;
    setLoading(true);
    setError("");
    try {
      const response = await fetch("/api/ai/variant-questions", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ questionId }) });
      const data = await response.json().catch(() => ({}));
      if (!response.ok) {
        setError(data.message || "生成变体题失败");
        return;
      }
      setVariants(data.variants ?? []);
    } catch {
      setError("网络异常，请稍后再试");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="mt-5">
      <Button type="button" variant="secondary" onClick={load} disabled={loading}>
        {loading ? <Loader2 className="h-4 w-4 animate-spin" /> : <Sparkles className="h-4 w-4" />}
        {loading ? "正在生成变体题..." : "举一反三"}
      </Button>
      {error ? <p className="mt-3 rounded-2xl bg-red-50 px-4 py-3 text-sm font-black text-red-600">{error}</p> : null}
      {variants.length ? <div className="mt-4 space-y-4">{variants.map((variant, index) => { const answer = selected[index]; const correct = variant.options.find((option) => option.isCorrect); return <Card key={index} className="bg-primary-soft/50 p-5 hover:translate-y-0"><p className="font-black text-navy">变体题 {index + 1}</p><h3 className="mt-2 font-black leading-7 text-navy">{variant.content}</h3><div className="mt-4 grid gap-2">{variant.options.map((option) => { const picked = answer === option.label; const show = Boolean(answer); return <button key={option.label} type="button" disabled={show} onClick={() => setSelected((prev) => ({ ...prev, [index]: option.label }))} className={cn("rounded-2xl border bg-white p-3 text-left font-bold transition", picked && !show && "border-primary", show && option.isCorrect && "border-green-300 bg-green-50 text-green-700", show && picked && !option.isCorrect && "border-red-300 bg-red-50 text-red-600")}>{option.label}. {option.content}</button>; })}</div>{answer ? <div className="mt-4 rounded-2xl bg-white p-4 font-semibold text-muted"><p className="mb-2 flex items-center gap-2 font-black text-navy">{answer === correct?.label ? <CheckCircle2 className="h-5 w-5 text-green-600" /> : <XCircle className="h-5 w-5 text-red-500" />}{answer === correct?.label ? "答对啦" : `正确答案：${correct?.label}`}</p>{variant.explanation}</div> : null}</Card>; })}</div> : null}
    </div>
  );
}
