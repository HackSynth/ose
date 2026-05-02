import Link from 'next/link';
import { notFound } from 'next/navigation';
import { ArrowLeft, FileQuestion } from 'lucide-react';
import { prisma } from '@/lib/prisma';
import { auth } from '@/lib/auth';
import { getDescendantTopicIds } from '@/lib/knowledge-stats';
import { getQuestionHistory, type QuestionHistoryEntry } from '@/lib/question-history';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { StartPracticeButton } from '@/components/start-practice-button';

export default async function TopicPracticePage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const [session, topic] = await Promise.all([
    auth(),
    prisma.knowledgePoint.findUnique({
      where: { id },
      include: { children: { orderBy: { sortOrder: 'asc' } } },
    }),
  ]);
  if (!topic) notFound();

  const userId = session?.user?.id;
  const topicIds = await getDescendantTopicIds(id);
  const questions = await prisma.question.findMany({
    where: { knowledgePointId: { in: topicIds } },
    orderBy: [{ year: 'desc' }, { questionNumber: 'asc' }],
    select: {
      id: true,
      content: true,
      questionNumber: true,
      knowledgePoint: { select: { name: true, parent: { select: { name: true } } } },
    },
  });

  const history: Map<string, QuestionHistoryEntry> = userId
    ? await getQuestionHistory(userId, questions.map((q) => q.id))
    : new Map();

  return (
    <main className="mx-auto mt-6 max-w-6xl space-y-6 md:mt-8">
      <Button asChild variant="secondary">
        <Link href="/practice">
          <ArrowLeft className="h-4 w-4" />
          返回练习入口
        </Link>
      </Button>
      <Card className="p-5 sm:p-8">
        <p className="mb-3 text-sm font-black text-primary">Topic Practice</p>
        <h1 className="text-3xl font-black text-navy sm:text-4xl">{topic.name}</h1>
        <p className="mt-3 font-semibold text-muted">
          共 {questions.length} 道题，覆盖 {topic.children.length || 1} 个子知识点。
        </p>
        <div className="mt-6">
          <StartPracticeButton
            payload={{ mode: 'topic', topicId: id, limit: questions.length || 20 }}
          >
            开始本知识点练习
          </StartPracticeButton>
        </div>
      </Card>
      <div className="grid gap-4">
        {questions.map((question) => {
          const h = history.get(question.id);
          return (
            <Card key={question.id} className="p-5 hover:translate-y-0">
              <div className="flex flex-wrap items-start justify-between gap-4">
                <div className="min-w-0 flex-1">
                  <div className="mb-2 flex flex-wrap items-center gap-2">
                    <p className="text-sm font-black text-muted">
                      2023 上午 · 第 {question.questionNumber} 题 ·{' '}
                      {question.knowledgePoint.parent?.name ?? question.knowledgePoint.name} /{' '}
                      {question.knowledgePoint.name}
                    </p>
                    {h ? (
                      <span
                        className={`rounded-full px-2 py-0.5 text-xs font-bold ${
                          h.lastIsCorrect
                            ? 'bg-green-50 text-green-700'
                            : 'bg-red-50 text-red-600'
                        }`}
                      >
                        做过 {h.attempts} 次 · 上次 {h.lastIsCorrect ? '✓' : '✗'}
                      </span>
                    ) : (
                      <span className="rounded-full bg-muted/10 px-2 py-0.5 text-xs font-bold text-muted">
                        未做过
                      </span>
                    )}
                  </div>
                  <h2 className="text-lg font-black leading-relaxed text-navy">{question.content}</h2>
                </div>
                <FileQuestion className="h-7 w-7 shrink-0 text-primary" />
              </div>
            </Card>
          );
        })}
      </div>
    </main>
  );
}
