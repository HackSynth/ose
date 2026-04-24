"use client";

import { useEffect, useRef, useState } from "react";
import { Bot, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { MarkdownRenderer } from "@/components/markdown-renderer";
import { useAIStatus } from "@/components/ai-status-context";

export function AIDiagnosisButton() {
  const status = useAIStatus();
  const [loading, setLoading] = useState(false);
  const [content, setContent] = useState("");
  const controllerRef = useRef<AbortController | null>(null);

  useEffect(() => () => controllerRef.current?.abort(), []);

  async function run() {
    if (!status.configured) {
      setContent("AI 未配置，请先在个人中心填入 API Key。");
      return;
    }
    setLoading(true);
    setContent("");
    controllerRef.current?.abort();
    const controller = new AbortController();
    controllerRef.current = controller;
    try {
      const response = await fetch("/api/ai/diagnosis", { method: "POST", signal: controller.signal });
      if (!response.ok || !response.body) {
        const data = await response.json().catch(() => ({}));
        setContent(data.message || "AI 服务暂时不可用，请稍后再试。");
        return;
      }
      const reader = response.body.getReader();
      const decoder = new TextDecoder();
      while (true) {
        const { done, value } = await reader.read();
        if (done) break;
        setContent((prev) => prev + decoder.decode(value, { stream: true }));
      }
    } catch (error) {
      if ((error as { name?: string })?.name === "AbortError") return;
      setContent("网络异常，请稍后再试。");
    } finally {
      if (controllerRef.current === controller) controllerRef.current = null;
      setLoading(false);
    }
  }

  return <div><Button onClick={run} disabled={loading || !status.configured} title={!status.configured ? "AI 未配置，请先在个人中心填入 API Key" : undefined}>{loading ? <Loader2 className="h-4 w-4 animate-spin" /> : <Bot className="h-4 w-4" />}{loading ? "AI 正在分析..." : "AI 智能诊断"}</Button>{content ? <Card className="mt-5 bg-softYellow/60 p-6 hover:translate-y-0"><MarkdownRenderer content={content} /></Card> : null}</div>;
}
