import { NextResponse } from 'next/server';
import { auth } from '@/lib/auth';
import { prisma } from '@/lib/prisma';
import { AI_CHAT_MAX_MESSAGES } from '@/lib/constants';
import type { AIMessage } from '@/lib/ai/types';

const MAX_TITLE_CHARS = 48;
const MAX_MESSAGE_CHARS = 4000;

function normalizeMessages(value: unknown): AIMessage[] {
  if (!Array.isArray(value)) return [];
  return value
    .slice(-AI_CHAT_MAX_MESSAGES)
    .filter(
      (message: unknown): message is AIMessage =>
        typeof message === 'object' &&
        message !== null &&
        'role' in message &&
        'content' in message &&
        ['user', 'assistant'].includes(String((message as { role: unknown }).role)) &&
        typeof (message as { content: unknown }).content === 'string'
    )
    .map((message) => ({
      role: message.role,
      content: message.content.slice(0, MAX_MESSAGE_CHARS),
    }));
}

function normalizeTitle(value: unknown, messages: AIMessage[]) {
  const explicit = typeof value === 'string' ? value.trim() : '';
  const fallback = messages.find((message) => message.role === 'user')?.content.trim();
  return (explicit || fallback || '新会话').replace(/\s+/g, ' ').slice(0, MAX_TITLE_CHARS);
}

export async function GET(_request: Request, { params }: { params: Promise<{ id: string }> }) {
  const session = await auth();
  if (!session?.user?.id) return NextResponse.json({ message: '请先登录' }, { status: 401 });
  const { id } = await params;

  const chatSession = await prisma.aIChatSession.findFirst({
    where: { id, userId: session.user.id },
    select: { id: true, title: true, messages: true, createdAt: true, updatedAt: true },
  });
  if (!chatSession) return NextResponse.json({ message: '会话不存在' }, { status: 404 });

  return NextResponse.json({ session: chatSession });
}

export async function PATCH(request: Request, { params }: { params: Promise<{ id: string }> }) {
  const session = await auth();
  if (!session?.user?.id) return NextResponse.json({ message: '请先登录' }, { status: 401 });
  const { id } = await params;

  const body = await request.json().catch(() => ({}));
  const messages = normalizeMessages((body as { messages?: unknown }).messages);
  const title = normalizeTitle((body as { title?: unknown }).title, messages);

  const result = await prisma.aIChatSession.updateMany({
    where: { id, userId: session.user.id },
    data: { title, messages },
  });
  if (!result.count) return NextResponse.json({ message: '会话不存在' }, { status: 404 });

  const updated = await prisma.aIChatSession.findUnique({
    where: { id },
    select: { id: true, title: true, messages: true, createdAt: true, updatedAt: true },
  });
  return NextResponse.json({ session: updated });
}

export async function DELETE(_request: Request, { params }: { params: Promise<{ id: string }> }) {
  const session = await auth();
  if (!session?.user?.id) return NextResponse.json({ message: '请先登录' }, { status: 401 });
  const { id } = await params;

  const result = await prisma.aIChatSession.deleteMany({ where: { id, userId: session.user.id } });
  if (!result.count) return NextResponse.json({ message: '会话不存在' }, { status: 404 });

  return NextResponse.json({ ok: true });
}
