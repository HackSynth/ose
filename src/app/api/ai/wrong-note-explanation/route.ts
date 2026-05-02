import { NextResponse } from 'next/server';

import { auth } from '@/lib/auth';
import { createAIErrorResponse } from '@/lib/ai/utils';
import {
  getLatestWrongNoteExplanationGeneration,
  prepareAndRunWrongNoteExplanation,
  serializeExplanationGeneration,
} from '@/lib/ai/wrong-note-explanation';

export async function GET(request: Request) {
  const session = await auth();
  if (!session?.user?.id) return NextResponse.json({ message: '请先登录' }, { status: 401 });
  const userId = session.user.id;
  const { searchParams } = new URL(request.url);
  const wrongNoteId = String(searchParams.get('wrongNoteId') ?? '');
  if (!wrongNoteId) return NextResponse.json({ message: '参数不完整' }, { status: 400 });

  try {
    const generation = await getLatestWrongNoteExplanationGeneration(userId, wrongNoteId);
    return NextResponse.json({
      generation: generation ? serializeExplanationGeneration(generation) : null,
    });
  } catch (error) {
    return createAIErrorResponse(error);
  }
}

export async function POST(request: Request) {
  const session = await auth();
  if (!session?.user?.id) return NextResponse.json({ message: '请先登录' }, { status: 401 });
  const userId = session.user.id;
  const body = await request.json().catch(() => ({}));
  const wrongNoteId = String((body as { wrongNoteId?: unknown }).wrongNoteId ?? '');
  const force = Boolean((body as { force?: unknown }).force);
  if (!wrongNoteId) return NextResponse.json({ message: '参数不完整' }, { status: 400 });

  try {
    const { generation, reused } = await prepareAndRunWrongNoteExplanation({
      userId,
      wrongNoteId,
      force,
    });
    return NextResponse.json({ generation: serializeExplanationGeneration(generation), reused });
  } catch (error) {
    const withGeneration = error as { generation?: unknown };
    if (withGeneration.generation) {
      return NextResponse.json(
        {
          message:
            error instanceof Error ? error.message : 'AI 深度讲解生成失败，请稍后重试',
          generation: serializeExplanationGeneration(
            withGeneration.generation as Parameters<typeof serializeExplanationGeneration>[0]
          ),
        },
        { status: 502 }
      );
    }
    return createAIErrorResponse(error);
  }
}
