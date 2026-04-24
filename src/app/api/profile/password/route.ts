import { NextResponse } from "next/server";
import bcrypt from "bcryptjs";
import { auth } from "@/lib/auth";
import { prisma } from "@/lib/prisma";

export async function PATCH(request: Request) {
  const session = await auth();
  if (!session?.user?.id) return NextResponse.json({ message: "请先登录" }, { status: 401 });
  const body = await request.json().catch(() => ({}));
  const oldPassword = String(body.oldPassword ?? "");
  const newPassword = String(body.newPassword ?? "");
  if (newPassword.length < 6 || newPassword.length > 128) {
    return NextResponse.json({ message: "新密码长度需在 6-128 位之间" }, { status: 400 });
  }
  const user = await prisma.user.findUnique({ where: { id: session.user.id }, select: { id: true, password: true } });
  if (!user) return NextResponse.json({ message: "用户不存在" }, { status: 404 });
  const valid = await bcrypt.compare(oldPassword, user.password);
  if (!valid) return NextResponse.json({ message: "旧密码不正确" }, { status: 400 });
  await prisma.user.update({ where: { id: user.id }, data: { password: await bcrypt.hash(newPassword, 12) } });
  return NextResponse.json({ message: "密码已更新" });
}
