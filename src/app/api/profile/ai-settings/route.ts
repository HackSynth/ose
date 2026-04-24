import { NextResponse } from "next/server";
import { auth } from "@/lib/auth";
import { prisma } from "@/lib/prisma";

const ALLOWED_PROVIDERS = new Set(["claude", "openai", "gemini", "custom"]);

function maskApiKey(key: string | null | undefined) {
  if (!key) return null;
  if (key.length <= 6) return "••••";
  return `${key.slice(0, 3)}…${key.slice(-4)}`;
}

export async function GET() {
  const session = await auth();
  if (!session?.user?.id) return NextResponse.json({ message: "请先登录" }, { status: 401 });
  const settings = await prisma.userAISettings.findUnique({ where: { userId: session.user.id } });
  return NextResponse.json({
    provider: settings?.provider ?? null,
    model: settings?.model ?? null,
    baseUrl: settings?.baseUrl ?? null,
    apiKeyMasked: maskApiKey(settings?.apiKey),
    hasApiKey: Boolean(settings?.apiKey),
    updatedAt: settings?.updatedAt ?? null,
  });
}

export async function PUT(request: Request) {
  const session = await auth();
  if (!session?.user?.id) return NextResponse.json({ message: "请先登录" }, { status: 401 });
  const body = await request.json().catch(() => ({}));
  const provider = String(body.provider ?? "").toLowerCase();
  if (provider && !ALLOWED_PROVIDERS.has(provider)) {
    return NextResponse.json({ message: "不支持的 provider" }, { status: 400 });
  }
  const model = typeof body.model === "string" ? body.model.trim().slice(0, 200) : "";
  const baseUrl = typeof body.baseUrl === "string" ? body.baseUrl.trim().slice(0, 500) : "";
  const apiKeyRaw = typeof body.apiKey === "string" ? body.apiKey : "";
  const clearApiKey = body.apiKey === null;

  if (provider === "custom" && !baseUrl) {
    return NextResponse.json({ message: "custom 供应商必须填写 Base URL" }, { status: 400 });
  }

  const existing = await prisma.userAISettings.findUnique({ where: { userId: session.user.id } });
  const nextApiKey = clearApiKey ? null : apiKeyRaw ? apiKeyRaw.trim().slice(0, 500) : (existing?.apiKey ?? null);

  await prisma.userAISettings.upsert({
    where: { userId: session.user.id },
    update: { provider: provider || null, model: model || null, baseUrl: baseUrl || null, apiKey: nextApiKey },
    create: { userId: session.user.id, provider: provider || null, model: model || null, baseUrl: baseUrl || null, apiKey: nextApiKey },
  });
  return NextResponse.json({ ok: true });
}

export async function DELETE() {
  const session = await auth();
  if (!session?.user?.id) return NextResponse.json({ message: "请先登录" }, { status: 401 });
  await prisma.userAISettings.deleteMany({ where: { userId: session.user.id } });
  return NextResponse.json({ ok: true });
}
