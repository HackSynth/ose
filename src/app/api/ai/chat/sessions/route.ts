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
  const fallback = messages.find((message) => message.role === 'user')?.content.trim() || '新会话';
  return (explicit || fallback).replace(/\s+/g, ' ').slice(0, MAX_TITLE_CHARS);
}

export async function GET() {
  const session = await auth();
  if (!session?.user?.id) return NextResponse.json({ message: '请先登录' }, { status: 401 });

  const sessions = await prisma.aIChatSession.findMany({
    where: { userId: session.user.id },
    orderBy: { updatedAt: 'desc' },
    take: 20,
    select: { id: true, title: true, messages: true, createdAt: true, updatedAt: true },
  });

  return NextResponse.json({ sessions });
}

export async function POST(request: Request) {
  const session = await auth();
  if (!session?.user?.id) return NextResponse.json({ message: '请先登录' }, { status: 401 });

  const body = await request.json().catch(() => ({}));
  const messages = normalizeMessages((body as { messages?: unknown }).messages);
  const title = normalizeTitle((body as { title?: unknown }).title, messages);
  const created = await prisma.aIChatSession.create({
    data: { userId: session.user.id, title, messages },
    select: { id: true, title: true, messages: true, createdAt: true, updatedAt: true },
  });

  return NextResponse.json({ session: created }, { status: 201 });
}
