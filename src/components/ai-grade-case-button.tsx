'use client';

import { useEffect, useRef, useState } from 'react';
import { Bot, Loader2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { CaseMethodologyButton } from '@/components/case-methodology-button';
import { ContinueAIChatButton } from '@/components/continue-ai-chat-button';
import { useAIStatus } from '@/components/ai-status-context';

type Grade = {
  subNumber: number;
  score: number;
  maxScore: number;
  feedback: string;
  correctParts: string;
  missingParts: string;
};
type Result = {
  subQuestions: Grade[];
  totalScore: number;
  totalMaxScore: number;
  overallFeedback: string;
};

function formatGradeResult(result: Result) {
  return [
    `AI 批改结果：${result.totalScore}/${result.totalMaxScore} 分`,
    '',
    ...result.subQuestions.flatMap((item) => [
      `## 子题 ${item.subNumber}：${item.score}/${item.maxScore} 分`,
      item.feedback,
      `做对的部分：${item.correctParts}`,
      `遗漏或问题：${item.missingParts}`,
      '',
    ]),
    `总体反馈：${result.overallFeedback}`,
  ].join('\n');
}

export function AIGradeCaseButton({
  caseScenarioId,
  userAnswers,
}: {
  caseScenarioId: string;
  userAnswers: Record<string, string>;
}) {
  const status = useAIStatus();
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<Result | null>(null);
  const [error, setError] = useState('');
  const controllerRef = useRef<AbortController | null>(null);

  useEffect(() => () => controllerRef.current?.abort(), []);

  async function grade() {
    setLoading(true);
    setError('');
    controllerRef.current?.abort();
    const controller = new AbortController();
    controllerRef.current = controller;
    try {
      const response = await fetch('/api/ai/grade-case', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ caseScenarioId, userAnswers }),
        signal: controller.signal,
      });
      const data = await response.json().catch(() => ({}));
      if (!response.ok) {
        setError(data.message || 'AI 服务暂时不可用，请稍后再试');
        return;
      }
      setResult(data);
    } catch (err) {
      if ((err as { name?: string })?.name === 'AbortError') return;
      setError('网络异常，请稍后再试');
    } finally {
      if (controllerRef.current === controller) controllerRef.current = null;
      setLoading(false);
    }
  }

  return (
    <div className="mt-6">
      <Button
        type="button"
        onClick={grade}
        disabled={!status.configured || loading}
        className="bg-gradient-to-r from-primary to-purple-500"
        title={
          !status.configured ? '请在环境变量中配置 AI 供应商的 API Key 以启用 AI 功能' : undefined
        }
      >
        {loading ? <Loader2 className="h-4 w-4 animate-spin" /> : <Bot className="h-4 w-4" />}
        {loading ? 'AI 阅卷老师正在批改...' : 'AI 智能批改'}
      </Button>
      {error ? (
        <p className="mt-3 rounded-2xl bg-red-50 px-4 py-3 text-sm font-black text-red-600">
          {error}
        </p>
      ) : null}
      {result ? (
        <Card className="mt-5 p-5 hover:translate-y-0">
          <div className="mb-4 flex justify-end">
            <ContinueAIChatButton
              title="AI 智能批改"
              messages={[
                {
                  role: 'user',
                  content: `请批改我的案例分析作答。案例 ID：${caseScenarioId}\n\n我的作答：\n${Object.entries(
                    userAnswers
                  )
                    .map(([key, value]) => `${key}. ${value}`)
                    .join('\n')}`,
                },
                { role: 'assistant', content: formatGradeResult(result) },
              ]}
            />
          </div>
          <h3 className="text-2xl font-black text-navy">
            AI 批改结果：{result.totalScore}/{result.totalMaxScore} 分
          </h3>
          <div className="mt-5 space-y-4">
            {result.subQuestions.map((item) => (
              <div key={item.subNumber} className="rounded-3xl bg-white p-4 shadow-sm">
                <p className="font-black text-navy">
                  子题 {item.subNumber}：{item.score}/{item.maxScore} 分
                </p>
                <p className="mt-2 font-semibold text-muted">{item.feedback}</p>
                <p className="mt-2 rounded-2xl bg-green-50 p-3 font-semibold text-green-700">
                  做对的部分：{item.correctParts}
                </p>
                <p className="mt-2 rounded-2xl bg-orange-50 p-3 font-semibold text-orange-700">
                  遗漏或问题：{item.missingParts}
                </p>
              </div>
            ))}
          </div>
          <p className="mt-5 rounded-3xl bg-primary-soft p-4 font-bold text-primary">
            {result.overallFeedback}
          </p>
          <CaseMethodologyButton caseScenarioId={caseScenarioId} />
        </Card>
      ) : null}
    </div>
  );
}
