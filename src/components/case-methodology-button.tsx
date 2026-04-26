'use client';

import { useEffect, useRef, useState } from 'react';
import { Bot, Loader2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { ContinueAIChatButton } from '@/components/continue-ai-chat-button';
import { MarkdownRenderer } from '@/components/markdown-renderer';

export function CaseMethodologyButton({ caseScenarioId }: { caseScenarioId: string }) {
  const [loading, setLoading] = useState(false);
  const [content, setContent] = useState('');
  const [error, setError] = useState('');
  const controllerRef = useRef<AbortController | null>(null);

  useEffect(() => () => controllerRef.current?.abort(), []);

  async function load() {
    setLoading(true);
    setContent('');
    setError('');
    controllerRef.current?.abort();
    const controller = new AbortController();
    controllerRef.current = controller;
    try {
      const response = await fetch('/api/ai/case-methodology', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ caseScenarioId }),
        signal: controller.signal,
      });
      if (!response.ok || !response.body) {
        const data = await response.json().catch(() => ({}));
        setError(data.message || 'AI 服务暂时不可用，请稍后再试');
        return;
      }
      const reader = response.body.getReader();
      const decoder = new TextDecoder();
      while (true) {
        const { done, value } = await reader.read();
        if (done) break;
        setContent((prev) => prev + decoder.decode(value, { stream: true }));
      }
    } catch (err) {
      if ((err as { name?: string })?.name === 'AbortError') return;
      setError('网络异常，请稍后再试');
    } finally {
      if (controllerRef.current === controller) controllerRef.current = null;
      setLoading(false);
    }
  }

  return (
    <div className="mt-5">
      <Button type="button" variant="secondary" onClick={load} disabled={loading}>
        {loading ? <Loader2 className="h-4 w-4 animate-spin" /> : <Bot className="h-4 w-4" />}
        {loading ? 'AI 正在梳理解题思路...' : 'AI 解题思路'}
      </Button>
      {error ? (
        <p className="mt-3 rounded-2xl bg-red-50 px-4 py-3 text-sm font-black text-red-600">
          {error}
        </p>
      ) : null}
      {content ? (
        <Card className="mt-4 bg-softYellow/60 p-5 hover:translate-y-0">
          <div className="mb-4 flex justify-end">
            <ContinueAIChatButton
              title="AI 解题思路"
              messages={[
                {
                  role: 'user',
                  content: `请梳理这道案例分析题的解题思路。案例 ID：${caseScenarioId}`,
                },
                { role: 'assistant', content },
              ]}
            />
          </div>
          <MarkdownRenderer content={content} />
        </Card>
      ) : null}
    </div>
  );
}
