'use client';

import { useRouter } from 'next/navigation';
import { useState, type FormEvent } from 'react';
import { Loader2, Wand2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { OSESelect } from '@/components/ose-select';
import { showToast } from '@/lib/toast-client';

type Exam = {
  id: string;
  title: string;
  session: 'AM' | 'PM' | 'FULL';
  timeLimit: number;
  totalScore: number;
  questionCount: number;
  bestScore: number;
  lastAttempt?: { id: string; status: string; totalScore?: number | null } | null;
};

export function ExamActions({ exam }: { exam: Exam }) {
  const router = useRouter();
  const [loading, setLoading] = useState(false);
  async function start() {
    if (
      !confirm(
        `即将开始《${exam.title}》，时间限制 ${exam.timeLimit} 分钟。开始后会正式计时，确定继续？`
      )
    )
      return;
    setLoading(true);
    try {
      const response = await fetch(`/api/exam/${exam.id}/start`, { method: 'POST' });
      const data = await response.json().catch(() => ({}));
      if (!response.ok) {
        showToast({ title: '开始考试失败', description: data.message || '请稍后重试' });
        return;
      }
      router.push(`/exam/${data.attemptId}`);
    } catch {
      showToast({ title: '网络异常', description: '请稍后再试' });
    } finally {
      setLoading(false);
    }
  }
  return (
    <Button onClick={start} disabled={loading}>
      {loading ? <Loader2 className="h-4 w-4 animate-spin" /> : null}
      {exam.lastAttempt?.status === 'COMPLETED' ? '重新考试' : '开始考试'}
    </Button>
  );
}

export function GenerateExamButton() {
  const router = useRouter();
  const [open, setOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [sessionType, setSessionType] = useState<'AM' | 'PM' | 'FULL'>('AM');
  const [count, setCount] = useState(20);
  const [error, setError] = useState('');

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError('');
    if (!Number.isInteger(count) || count < 1 || count > 100) {
      setError('题数需在 1-100 之间');
      return;
    }
    setLoading(true);
    try {
      const response = await fetch('/api/exam/generate', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ session: sessionType, questionCount: count }),
      });
      const data = await response.json().catch(() => ({}));
      if (!response.ok) {
        setError(data.message || '组卷失败');
        return;
      }
      setOpen(false);
      router.refresh();
    } catch {
      setError('网络异常，请稍后再试');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="relative">
      <Button variant="secondary" onClick={() => setOpen((v) => !v)}>
        <Wand2 className="h-4 w-4" />
        智能组卷
      </Button>
      {open ? (
        <form
          onSubmit={submit}
          className="absolute right-0 z-20 mt-3 w-80 space-y-4 rounded-3xl border border-white/80 bg-white p-5 shadow-lift"
        >
          <div className="space-y-2">
            <Label>组卷类型</Label>
            <OSESelect
              value={sessionType}
              options={[
                { value: 'AM', label: '上午 (AM)' },
                { value: 'PM', label: '下午 (PM)' },
                { value: 'FULL', label: '全真 (FULL)' },
              ]}
              onChange={(nextValue) => {
                const value = nextValue as 'AM' | 'PM' | 'FULL';
                setSessionType(value);
                setCount(value === 'PM' ? 4 : 20);
              }}
            />
          </div>
          <div className="space-y-2">
            <Label>题数（1-100）</Label>
            <Input
              type="number"
              min={1}
              max={100}
              value={count}
              onChange={(event) => setCount(Number(event.target.value))}
            />
          </div>
          {error ? (
            <p className="rounded-2xl bg-red-50 px-3 py-2 text-sm font-black text-red-600">
              {error}
            </p>
          ) : null}
          <div className="flex gap-2">
            <Button type="submit" disabled={loading}>
              {loading ? <Loader2 className="h-4 w-4 animate-spin" /> : null}确定
            </Button>
            <Button type="button" variant="secondary" onClick={() => setOpen(false)}>
              取消
            </Button>
          </div>
        </form>
      ) : null}
    </div>
  );
}
