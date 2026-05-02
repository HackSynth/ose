'use client';

import { useCallback, useEffect, useRef, useState } from 'react';
import Link from 'next/link';
import { Lightbulb, Loader2, RefreshCw } from 'lucide-react';

import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { ContinueAIChatButton } from '@/components/continue-ai-chat-button';
import { MarkdownRenderer } from '@/components/markdown-renderer';
import { VariantQuestions } from '@/components/variant-questions';
import { useAIStatus } from '@/components/ai-status-context';
import { showToast } from '@/lib/toast-client';
import { cn } from '@/lib/utils';

type ExplanationStatus = 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED';

type ExplanationGeneration = {
  id: string;
  status: ExplanationStatus;
  content?: string | null;
  errorMessage: string | null;
  provider?: string;
  model?: string;
  createdAt?: string;
  updatedAt?: string;
  completedAt?: string | null;
};

type AIWrongNoteExplanationButtonProps = {
  wrongNoteId: string;
  questionId: string;
  userAnswerOptionId?: string;
  initialGeneration?: ExplanationGeneration | null;
  onGenerationChange?: (generation: ExplanationGeneration | null) => void;
};

function isActiveStatus(status: ExplanationStatus | undefined) {
  return status === 'PENDING' || status === 'RUNNING';
}

export function AIWrongNoteExplanationButton({
  wrongNoteId,
  questionId,
  userAnswerOptionId,
  initialGeneration,
  onGenerationChange,
}: AIWrongNoteExplanationButtonProps) {
  const aiStatus = useAIStatus();
  const [generation, setGeneration] = useState<ExplanationGeneration | null>(
    initialGeneration ?? null
  );
  const [loading, setLoading] = useState(!initialGeneration);
  const [submitting, setSubmitting] = useState(false);
  const controllerRef = useRef<AbortController | null>(null);
  const hasInitialRef = useRef(Boolean(initialGeneration));
  const initialKey = initialGeneration
    ? `${initialGeneration.id}:${initialGeneration.status}`
    : '';

  useEffect(() => {
    if (initialGeneration) setGeneration(initialGeneration);
  }, [initialGeneration, initialKey]);

  const load = useCallback(
    async (options?: { silent?: boolean }) => {
      controllerRef.current?.abort();
      const controller = new AbortController();
      controllerRef.current = controller;
      if (!options?.silent) setLoading(true);
      try {
        const response = await fetch(
          `/api/ai/wrong-note-explanation?wrongNoteId=${encodeURIComponent(wrongNoteId)}`,
          { cache: 'no-store', signal: controller.signal }
        );
        const data = await response.json().catch(() => ({}));
        if (!response.ok) return;
        const next = ((data as { generation?: ExplanationGeneration | null }).generation ??
          null) as ExplanationGeneration | null;
        setGeneration((current) => {
          if (next) return next;
          if (current?.status === 'COMPLETED' && current.content) return current;
          return null;
        });
        if (next) onGenerationChange?.(next);
      } catch (error) {
        if ((error as { name?: string })?.name === 'AbortError') return;
      } finally {
        if (controllerRef.current === controller) controllerRef.current = null;
        if (!options?.silent) setLoading(false);
      }
    },
    [onGenerationChange, wrongNoteId]
  );

  useEffect(() => {
    load({ silent: hasInitialRef.current });
    return () => controllerRef.current?.abort();
  }, [load]);

  useEffect(() => {
    if (!isActiveStatus(generation?.status)) return;
    const timer = window.setInterval(() => {
      void load({ silent: true });
    }, 2200);
    return () => window.clearInterval(timer);
  }, [generation?.status, load]);

  async function generate(force = false) {
    if (!aiStatus.configured) return;
    setSubmitting(true);
    try {
      const response = await fetch('/api/ai/wrong-note-explanation', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ wrongNoteId, force }),
      });
      const data = await response.json().catch(() => ({}));
      if (!response.ok) {
        const msg = (data as { message?: string }).message || 'AI 深度讲解生成失败';
        showToast({ title: 'AI 深度讲解失败', description: msg });
        if ((data as { generation?: ExplanationGeneration }).generation) {
          const next = (data as { generation: ExplanationGeneration }).generation;
          setGeneration(next);
          onGenerationChange?.(next);
        }
        return;
      }
      const next = (data as { generation: ExplanationGeneration }).generation;
      setGeneration(next);
      onGenerationChange?.(next);
      if (next.status === 'COMPLETED') {
        showToast({ title: force ? 'AI 讲解已重新生成' : 'AI 深度讲解已生成' });
      }
    } catch {
      showToast({ title: '网络异常', description: '请稍后再试' });
    } finally {
      setSubmitting(false);
    }
  }

  const active = isActiveStatus(generation?.status);
  const completed = generation?.status === 'COMPLETED' && Boolean(generation?.content);
  const failed = generation?.status === 'FAILED';
  const disabled = loading || submitting || active || !aiStatus.configured;

  return (
    <div className="mt-5">
      <div className="flex flex-wrap items-center gap-3">
        <Button
          type="button"
          variant={completed ? 'secondary' : 'default'}
          onClick={() => generate(completed || failed)}
          disabled={disabled}
          title={!aiStatus.configured ? '请先配置文本 AI，生图 AI 不会启用文字讲解' : undefined}
        >
          {loading || submitting || active ? (
            <Loader2 className="h-4 w-4 animate-spin" />
          ) : completed || failed ? (
            <RefreshCw className="h-4 w-4" />
          ) : (
            <Lightbulb className="h-4 w-4" />
          )}
          {loading
            ? '加载讲解...'
            : submitting
              ? 'AI 正在生成讲解...'
              : active
                ? 'AI 深度讲解生成中...'
                : completed
                  ? '重新生成 AI 讲解'
                  : failed
                    ? '重试 AI 讲解'
                    : aiStatus.configured
                      ? 'AI 深度讲解'
                      : '未配置文本 AI'}
        </Button>
        {!aiStatus.configured ? (
          <Button asChild variant="ghost">
            <Link href="/profile">去配置</Link>
          </Button>
        ) : null}
        {generation?.status ? (
          <span
            className={cn(
              'rounded-full px-3 py-1 text-xs font-black',
              generation.status === 'COMPLETED' && 'bg-green-100 text-green-700',
              active && 'bg-amber-100 text-amber-700',
              generation.status === 'FAILED' && 'bg-red-100 text-red-600'
            )}
          >
            {generation.status === 'PENDING'
              ? 'AI 讲解排队中'
              : generation.status === 'RUNNING'
                ? 'AI 讲解生成中'
                : generation.status === 'COMPLETED'
                  ? 'AI 讲解已生成'
                  : 'AI 讲解生成失败'}
          </span>
        ) : null}
      </div>

      {!aiStatus.configured ? (
        <p className="mt-3 rounded-2xl bg-softYellow px-4 py-3 text-sm font-bold text-muted">
          AI 深度讲解需要配置文本 AI；生图 AI 只用于错题讲解图。
        </p>
      ) : null}

      {failed && generation.errorMessage ? (
        <p className="mt-3 rounded-2xl bg-red-50 px-4 py-3 text-sm font-black text-red-600">
          {generation.errorMessage}
        </p>
      ) : null}

      {completed && generation.content ? (
        <Card className="mt-4 bg-softYellow/60 p-5 hover:translate-y-0">
          <div className="mb-4 flex justify-end">
            <ContinueAIChatButton
              title="AI 深度讲解"
              messages={[
                {
                  role: 'user',
                  content: `请对这道题进行 AI 深度讲解。题目 ID：${questionId}${userAnswerOptionId ? `，我的选项 ID：${userAnswerOptionId}` : ''}`,
                },
                { role: 'assistant', content: generation.content ?? '' },
              ]}
            />
          </div>
          <MarkdownRenderer content={generation.content ?? ''} />
          <VariantQuestions questionId={questionId} />
        </Card>
      ) : null}
    </div>
  );
}
