import { NextResponse } from 'next/server';
import { auth } from '@/lib/auth';
import { prisma } from '@/lib/prisma';
import { getQuestionHistory } from '@/lib/question-history';

export async function GET(request: Request) {
  const session = await auth();
  if (!session?.user?.id) return NextResponse.json({ message: '未登录' }, { status: 401 });
  const userId = session.user.id;

  const { searchParams } = new URL(request.url);
  const sessionId = searchParams.get('sessionId');
  if (!sessionId) return NextResponse.json({ message: '缺少 sessionId' }, { status: 400 });

  const practiceSession = await prisma.practiceSession.findFirst({
    where: { id: sessionId, userId },
    select: {
      mode: true,
      questions: { select: { questionId: true } },
    },
  });

  if (!practiceSession) return NextResponse.json({ message: '会话不存在' }, { status: 404 });

  const questionIds = practiceSession.questions.map((q) => q.questionId);
  const historyMap = await getQuestionHistory(userId, questionIds, sessionId);

  const history: Record<string, {
    attempts: number;
    correctCount: number;
    lastIsCorrect: boolean;
    lastSelectedOptionLabel: string;
  }> = {};

  for (const [questionId, entry] of historyMap) {
    history[questionId] = {
      attempts: entry.attempts,
      correctCount: entry.correctCount,
      lastIsCorrect: entry.lastIsCorrect,
      lastSelectedOptionLabel: entry.lastSelectedOptionLabel,
    };
  }

  return NextResponse.json({ mode: practiceSession.mode, history });
}
