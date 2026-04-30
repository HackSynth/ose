'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import Link from 'next/link';
import {
  AlertTriangle,
  CheckCircle2,
  Clock3,
  ExternalLink,
  ImageIcon,
  Loader2,
  RefreshCw,
  Rows3,
  Trash2,
  XCircle,
  type LucideIcon,
} from 'lucide-react';

import { OSESelect } from '@/components/ose-select';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { AI_IMAGE_STYLE_OPTIONS } from '@/lib/ai/image-types';
import { showToast } from '@/lib/toast-client';
import { cn } from '@/lib/utils';

type TaskStatus = 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED';
type ProcessState = 'QUEUED' | 'RUNNING' | null;

type QueueStats = {
  queued: number;
  running: number;
  concurrency: number;
  queuedIds: string[];
  runningIds: string[];
};

type TaskItem = {
  id: string;
  kind: string;
  wrongNoteId: string | null;
  questionId: string;
  questionContent: string;
  knowledgePoint: string;
  status: TaskStatus;
  processState: ProcessState;
  provider: string;
  model: string;
  promptProvider: string;
  promptModel: string;
  imageSize: string;
  imageQuality: string;
  imageOutputFormat: string;
  imageStyle: string;
  sourceImagePath: string | null;
  imageUrl: string | null;
  errorMessage: string | null;
  createdAt: string;
  updatedAt: string;
  completedAt: string | null;
};

type TaskQueueResponse = {
  queue: QueueStats;
  counts: Record<TaskStatus, number>;
  items: TaskItem[];
  pagination: {
    page: number;
    pageSize: number;
    total: number;
    totalPages: number;
  };
};

const STATUS_OPTIONS = [
  { value: 'all', label: '全部任务' },
  { value: 'PENDING', label: '待处理' },
  { value: 'RUNNING', label: '生成中' },
  { value: 'FAILED', label: '失败' },
  { value: 'COMPLETED', label: '已完成' },
];

const statusMeta: Record<TaskStatus, { label: string; className: string; icon: LucideIcon }> = {
  PENDING: {
    label: '待处理',
    className: 'bg-amber-100 text-amber-800',
    icon: Clock3,
  },
  RUNNING: {
    label: '生成中',
    className: 'bg-blue-100 text-blue-800',
    icon: Loader2,
  },
  COMPLETED: {
    label: '已完成',
    className: 'bg-green-100 text-green-800',
    icon: CheckCircle2,
  },
  FAILED: {
    label: '失败',
    className: 'bg-red-100 text-red-700',
    icon: XCircle,
  },
};

function formatDate(value: string | null | undefined) {
  if (!value) return '-';
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value));
}

function formatDuration(start: string, end?: string | null) {
  const startTime = new Date(start).getTime();
  const endTime = end ? new Date(end).getTime() : Date.now();
  if (!Number.isFinite(startTime) || !Number.isFinite(endTime)) return '-';
  const seconds = Math.max(0, Math.round((endTime - startTime) / 1000));
  if (seconds < 60) return `${seconds}s`;
  const minutes = Math.floor(seconds / 60);
  const rest = seconds % 60;
  if (minutes < 60) return `${minutes}m ${rest}s`;
  const hours = Math.floor(minutes / 60);
  return `${hours}h ${minutes % 60}m`;
}

function imageStyleLabel(style: string) {
  return AI_IMAGE_STYLE_OPTIONS.find((option) => option.value === style)?.label ?? style;
}

function compactText(value: string, maxLength = 96) {
  const text = value
    .replace(/!\[[^\]]*]\([^)]*\)/g, '[图片]')
    .replace(/\s+/g, ' ')
    .trim();
  return text.length > maxLength ? `${text.slice(0, maxLength)}...` : text;
}

function statusLabel(task: TaskItem) {
  if (task.processState === 'QUEUED') return '进程队列中';
  if (task.processState === 'RUNNING') return '当前运行中';
  return statusMeta[task.status].label;
}

function taskEndTime(task: TaskItem) {
  if (task.completedAt) return task.completedAt;
  if (task.status === 'FAILED') return task.updatedAt;
  return null;
}

function StatusBadge({ task }: { task: TaskItem }) {
  const meta = statusMeta[task.status];
  const Icon = task.processState === 'RUNNING' ? Loader2 : meta.icon;
  return (
    <span
      className={cn(
        'inline-flex w-fit items-center gap-1.5 rounded-full px-3 py-1 text-xs font-black',
        meta.className
      )}
    >
      <Icon className={cn('h-3.5 w-3.5', task.processState === 'RUNNING' && 'animate-spin')} />
      {statusLabel(task)}
    </span>
  );
}

function StatTile({ label, value, tone }: { label: string; value: string | number; tone: string }) {
  return (
    <Card className={cn('p-5 hover:translate-y-0', tone)}>
      <p className="text-xs font-black text-muted">{label}</p>
      <p className="mt-2 text-3xl font-black text-navy">{value}</p>
    </Card>
  );
}

export function AITaskQueueClient() {
  const [data, setData] = useState<TaskQueueResponse | null>(null);
  const [status, setStatus] = useState('all');
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [clearingFailed, setClearingFailed] = useState(false);
  const [message, setMessage] = useState('');

  const activeCount = (data?.queue.queued ?? 0) + (data?.queue.running ?? 0);
  const shouldPoll =
    activeCount > 0 || Boolean(data?.counts.PENDING) || Boolean(data?.counts.RUNNING);

  const load = useCallback(
    async (silent = false) => {
      if (silent) setRefreshing(true);
      else setLoading(true);
      setMessage('');
      try {
        const params = new URLSearchParams({
          page: String(page),
          pageSize: '30',
          ...(status !== 'all' ? { status } : {}),
        });
        const response = await fetch(`/api/ai/task-queue?${params.toString()}`, {
          cache: 'no-store',
        });
        const json = (await response.json().catch(() => ({}))) as
          | TaskQueueResponse
          | { message?: string };
        if (!response.ok) {
          setMessage(('message' in json && json.message) || '任务队列加载失败');
          return;
        }
        setData(json as TaskQueueResponse);
      } catch {
        setMessage('网络异常，任务队列加载失败');
      } finally {
        setLoading(false);
        setRefreshing(false);
      }
    },
    [page, status]
  );

  useEffect(() => {
    void load(false);
  }, [load]);

  useEffect(() => {
    if (!shouldPoll) return;
    const timer = window.setInterval(() => void load(true), 4000);
    return () => window.clearInterval(timer);
  }, [load, shouldPoll]);

  async function clearFailedTasks() {
    const failedCount = data?.counts.FAILED ?? 0;
    if (!failedCount) return;
    const confirmed = window.confirm(
      `确认清除 ${failedCount} 个失败任务？此操作不会删除已生成图片。`
    );
    if (!confirmed) return;

    setClearingFailed(true);
    setMessage('');
    try {
      const response = await fetch('/api/ai/task-queue', { method: 'DELETE' });
      const json = (await response.json().catch(() => ({}))) as {
        deleted?: number;
        message?: string;
      };
      if (!response.ok) {
        const nextMessage = json.message || '失败任务清除失败';
        setMessage(nextMessage);
        showToast({ title: '清除失败', description: nextMessage });
        return;
      }
      showToast({ title: '已清除失败任务', description: `已清除 ${json.deleted ?? 0} 个任务` });
      if (page === 1) await load(true);
      else setPage(1);
    } catch {
      setMessage('网络异常，失败任务清除失败');
      showToast({ title: '网络异常', description: '失败任务清除失败' });
    } finally {
      setClearingFailed(false);
    }
  }

  const summary = useMemo(() => {
    const counts = data?.counts ?? { PENDING: 0, RUNNING: 0, COMPLETED: 0, FAILED: 0 };
    return [
      { label: '进程排队', value: data?.queue.queued ?? 0, tone: 'bg-softYellow' },
      {
        label: '当前运行',
        value: `${data?.queue.running ?? 0}/${data?.queue.concurrency ?? 0}`,
        tone: 'bg-softBlue',
      },
      { label: '失败任务', value: counts.FAILED, tone: 'bg-softRose' },
      { label: '已完成', value: counts.COMPLETED, tone: 'bg-softGreen' },
    ];
  }, [data]);

  const items = data?.items ?? [];

  return (
    <main className="mx-auto mt-6 max-w-7xl space-y-6 md:mt-8 md:space-y-8">
      <section className="rounded-[1.5rem] bg-white/90 p-6 shadow-soft sm:rounded-[2rem] sm:p-8">
        <div className="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
          <div className="min-w-0">
            <p className="mb-3 text-sm font-black text-primary">Task Queue</p>
            <h1 className="text-3xl font-black text-navy sm:text-4xl md:text-5xl">任务队列</h1>
            <p className="mt-3 font-semibold text-muted">错题讲解图生成任务状态</p>
          </div>
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
            <div className="w-full sm:w-44">
              <OSESelect
                value={status}
                options={STATUS_OPTIONS}
                onChange={(value) => {
                  setPage(1);
                  setStatus(value);
                }}
              />
            </div>
            <Button
              type="button"
              variant="secondary"
              onClick={() => load(true)}
              disabled={refreshing || clearingFailed}
            >
              {refreshing ? (
                <Loader2 className="h-4 w-4 animate-spin" />
              ) : (
                <RefreshCw className="h-4 w-4" />
              )}
              刷新
            </Button>
            <Button
              type="button"
              variant="secondary"
              onClick={clearFailedTasks}
              disabled={clearingFailed || loading || !(data?.counts.FAILED ?? 0)}
              className="bg-red-50 text-red-700 shadow-none hover:bg-red-100"
            >
              {clearingFailed ? (
                <Loader2 className="h-4 w-4 animate-spin" />
              ) : (
                <Trash2 className="h-4 w-4" />
              )}
              清除失败任务
            </Button>
          </div>
        </div>
      </section>

      <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {summary.map((item) => (
          <StatTile key={item.label} label={item.label} value={item.value} tone={item.tone} />
        ))}
      </section>

      <Card className="overflow-hidden hover:translate-y-0">
        <div className="flex flex-col gap-3 border-b border-orange-100 px-5 py-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex items-center gap-2 font-black text-navy">
            <Rows3 className="h-5 w-5 text-primary" />
            最近任务
          </div>
          {data ? (
            <p className="text-sm font-bold text-muted">
              第 {data.pagination.page}/{data.pagination.totalPages} 页 · 共 {data.pagination.total}{' '}
              个任务
            </p>
          ) : null}
        </div>

        {message ? (
          <div className="m-5 flex items-center gap-2 rounded-2xl bg-red-50 px-4 py-3 text-sm font-black text-red-700">
            <AlertTriangle className="h-4 w-4" />
            {message}
          </div>
        ) : null}

        {loading ? (
          <div className="flex min-h-64 items-center justify-center gap-3 font-black text-muted">
            <Loader2 className="h-5 w-5 animate-spin text-primary" />
            加载任务队列...
          </div>
        ) : items.length ? (
          <div className="overflow-x-auto">
            <div className="hidden min-w-[1040px] grid-cols-[150px_1fr_132px_160px_132px_120px] gap-4 bg-warm px-5 py-3 text-xs font-black text-muted md:grid">
              <span>状态</span>
              <span>任务</span>
              <span>风格</span>
              <span>模型</span>
              <span>时间</span>
              <span>结果</span>
            </div>
            <div className="min-w-full divide-y divide-orange-100 md:min-w-[1040px]">
              {items.map((task) => (
                <div
                  key={task.id}
                  className="grid gap-4 px-5 py-4 md:grid-cols-[150px_1fr_132px_160px_132px_120px] md:items-start"
                >
                  <div className="flex flex-wrap items-center gap-2 md:block">
                    <StatusBadge task={task} />
                    <p className="mt-2 font-mono text-[11px] font-bold text-muted md:block">
                      {task.id.slice(0, 10)}
                    </p>
                  </div>

                  <div className="min-w-0">
                    <p className="text-xs font-black text-primary">{task.knowledgePoint}</p>
                    <p className="mt-1 font-bold leading-6 text-navy">
                      {compactText(task.questionContent)}
                    </p>
                    {task.errorMessage ? (
                      <p className="mt-2 rounded-2xl bg-red-50 px-3 py-2 text-xs font-bold leading-5 text-red-700">
                        {task.errorMessage}
                      </p>
                    ) : null}
                  </div>

                  <div className="text-sm font-black text-navy">
                    <span className="md:hidden">风格：</span>
                    {imageStyleLabel(task.imageStyle)}
                    <p className="mt-1 text-xs font-bold text-muted">{task.imageSize}</p>
                  </div>

                  <div className="text-sm font-black text-navy">
                    <span className="md:hidden">模型：</span>
                    {task.provider} / {task.model}
                    <p className="mt-1 text-xs font-bold text-muted">
                      提示词：{task.promptProvider} / {task.promptModel}
                    </p>
                  </div>

                  <div className="text-sm font-bold text-muted">
                    <p>
                      <span className="md:hidden">创建：</span>
                      {formatDate(task.createdAt)}
                    </p>
                    <p className="mt-1">耗时 {formatDuration(task.createdAt, taskEndTime(task))}</p>
                    <p className="mt-1">更新 {formatDate(task.updatedAt)}</p>
                  </div>

                  <div className="flex flex-wrap gap-2">
                    {task.imageUrl ? (
                      <Button asChild variant="secondary" size="sm">
                        <a href={task.imageUrl} target="_blank" rel="noreferrer">
                          <ImageIcon className="h-4 w-4" />
                          图片
                        </a>
                      </Button>
                    ) : null}
                    {task.wrongNoteId ? (
                      <Button asChild variant="ghost" size="sm">
                        <Link href="/wrong-notes">
                          <ExternalLink className="h-4 w-4" />
                          错题
                        </Link>
                      </Button>
                    ) : null}
                  </div>
                </div>
              ))}
            </div>
          </div>
        ) : (
          <div className="flex min-h-64 flex-col items-center justify-center gap-3 px-5 text-center">
            <Rows3 className="h-8 w-8 text-primary" />
            <p className="font-black text-navy">暂无任务</p>
          </div>
        )}

        {data && data.pagination.totalPages > 1 ? (
          <div className="flex items-center justify-between border-t border-orange-100 px-5 py-4">
            <Button
              type="button"
              variant="secondary"
              disabled={page <= 1}
              onClick={() => setPage((value) => Math.max(1, value - 1))}
            >
              上一页
            </Button>
            <span className="text-sm font-black text-muted">
              {data.pagination.page}/{data.pagination.totalPages}
            </span>
            <Button
              type="button"
              variant="secondary"
              disabled={page >= data.pagination.totalPages}
              onClick={() => setPage((value) => value + 1)}
            >
              下一页
            </Button>
          </div>
        ) : null}
      </Card>
    </main>
  );
}
