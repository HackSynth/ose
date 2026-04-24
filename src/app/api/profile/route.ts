import { NextResponse } from "next/server";
import { auth } from "@/lib/auth";
import { prisma } from "@/lib/prisma";

export async function GET() {
  const session = await auth();
  if (!session?.user?.id) return NextResponse.json({ message: "请先登录" }, { status: 401 });
  const user = await prisma.user.findUnique({ where: { id: session.user.id }, select: { id: true, name: true, email: true, createdAt: true } });
  return NextResponse.json({ user });
}

export async function PATCH(request: Request) {
  const session = await auth();
  if (!session?.user?.id) return NextResponse.json({ message: "请先登录" }, { status: 401 });
  const body = await request.json().catch(() => ({}));
  const name = String(body.name ?? "").trim().slice(0, 50);
  if (!name) return NextResponse.json({ message: "用户名不能为空" }, { status: 400 });
  const user = await prisma.user.update({ where: { id: session.user.id }, data: { name }, select: { id: true, name: true, email: true, createdAt: true } });
  return NextResponse.json({ user, message: "用户名已更新" });
}
