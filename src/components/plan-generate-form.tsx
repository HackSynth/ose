"use client";

import { useState, type FormEvent } from "react";
import { useRouter } from "next/navigation";
import { Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

const preferences = ["侧重薄弱环节", "全面复习", "真题为主", "理论优先"];

export function PlanGenerateForm({ defaultDate }: { defaultDate: string }) {
  const router = useRouter();
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<string[]>(["侧重薄弱环节"]);
  const [error, setError] = useState("");

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (loading) return;
    const formData = new FormData(event.currentTarget);
    const targetDate = String(formData.get("targetDate") ?? "");
    if (!targetDate) {
      setError("请选择目标考试日期");
      return;
    }
    setLoading(true);
    setError("");
    try {
      const response = await fetch("/api/plan/generate", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ targetDate, dailyTime: formData.get("dailyTime"), preferences: selected }),
      });
      const data = await response.json().catch(() => ({}));
      if (!response.ok) {
        setError(data.message || "生成失败，请稍后再试");
        return;
      }
      router.push(`/plan/${data.planId}`);
    } catch {
      setError("网络异常，请稍后再试");
    } finally {
      setLoading(false);
    }
  }

  return (
    <form onSubmit={submit} className="space-y-5">
      <div className="grid gap-4 md:grid-cols-2">
        <label className="space-y-2"><Label>目标考试日期</Label><Input name="targetDate" type="date" defaultValue={defaultDate} required /></label>
        <label className="space-y-2"><Label>每日学习时间</Label><select name="dailyTime" className="ose-input w-full" defaultValue="1小时"><option>30分钟</option><option>1小时</option><option>2小时</option><option>3小时以上</option></select></label>
      </div>
      <div>
        <Label>学习偏好</Label>
        <div className="mt-3 flex flex-wrap gap-3">{preferences.map((item) => <button type="button" key={item} onClick={() => setSelected((prev) => prev.includes(item) ? prev.filter((x) => x !== item) : [...prev, item])} className={`rounded-full px-4 py-2 text-sm font-black ${selected.includes(item) ? "bg-primary text-white" : "bg-white text-muted"}`}>{item}</button>)}</div>
      </div>
      {error ? <p className="rounded-2xl bg-red-50 px-4 py-3 text-sm font-black text-red-600">{error}</p> : null}
      <Button size="lg" disabled={loading}>{loading ? <Loader2 className="h-4 w-4 animate-spin" /> : null}生成学习计划</Button>
    </form>
  );
}
