'use client';

import { useEffect, useRef, useState } from 'react';
import { Bot, Loader2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { ContinueAIChatButton } from '@/components/continue-ai-chat-button';
import { MarkdownRenderer } from '@/components/markdown-renderer';
import { useAIStatus } from '@/components/ai-status-context';

type TodayPlanDiagnosisButtonProps = {
  planId: string;
  planTitle: string;
  dayNumber: number;
  date: string;
};

export function TodayPlanDiagnosisButton({
  planId,
  planTitle,
  dayNumber,
  date,
}: TodayPlanDiagnosisButtonProps) {
  const status = useAIStatus();
  const [loading, setLoading] = useState(false);
  const [content, setContent] = useState('');
  const controllerRef = useRef<AbortController | null>(null);

  useEffect(() => () => controllerRef.current?.abort(), []);

  async function run() {
    if (!status.configured || loading) return;
    setLoading(true);
    setContent('');
    controllerRef.current?.abort();
    const controller = new AbortController();
    controllerRef.current = controller;

    try {
      const response = await fetch('/api/ai/today-plan-diagnosis', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ planId, dayNumber }),
        signal: controller.signal,
      });
      if (!response.ok || !response.body) {
        const data = (await response.json().catch(() => ({}))) as { message?: string };
        setContent(data.message || 'AI 服务暂时不可用，请稍后再试。');
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
      if ((error as { name?: string })?.name === 'AbortError') return;
      setContent('网络异常，请稍后再试。');
    } finally {
      if (controllerRef.current === controller) controllerRef.current = null;
      setLoading(false);
    }
  }

  return (
    <>
      <Button
        type="button"
        variant="secondary"
        onClick={run}
        disabled={loading || !status.configured}
        title={!status.configured ? 'AI 未配置，请先在个人中心填入 API Key' : undefined}
      >
        {loading ? <Loader2 className="h-4 w-4 animate-spin" /> : <Bot className="h-4 w-4" />}
        {loading ? '诊断中...' : '检测今日任务'}
      </Button>
      {content ? (
        <div className="basis-full rounded-[1.25rem] bg-white/75 p-5 shadow-soft">
          <div className="mb-4 flex justify-end">
            <ContinueAIChatButton
              title="今日任务诊断"
              messages={[
                {
                  role: 'user',
                  content: `请继续分析学习计划《${planTitle}》第 ${dayNumber} 天（${date}）的今日任务。`,
                },
                { role: 'assistant', content },
              ]}
            />
          </div>
          <MarkdownRenderer content={content} />
        </div>
      ) : null}
    </>
  );
}
