import { NextResponse } from "next/server";
import crypto from "node:crypto";
import bcrypt from "bcryptjs";
import { prisma } from "@/lib/prisma";
import { isValidEmail } from "@/lib/validate";

const TOKEN_TTL_MINUTES = 30;
const MIN_PASSWORD = 6;
const MAX_PASSWORD = 128;

function hashToken(token: string) {
  return crypto.createHash("sha256").update(token).digest("hex");
}

export async function POST(request: Request) {
  const body = await request.json().catch(() => ({}));
  const mode = String(body.mode ?? "request");

  if (mode === "request") {
    const email = String(body.email ?? "").trim().toLowerCase();
    if (!isValidEmail(email)) {
      return NextResponse.json({ message: "邮箱格式不正确" }, { status: 400 });
    }
    const user = await prisma.user.findUnique({ where: { email }, select: { id: true, email: true } });
    // Always respond success to avoid leaking account existence.
    if (user) {
      const rawToken = crypto.randomBytes(32).toString("hex");
      const expiresAt = new Date(Date.now() + TOKEN_TTL_MINUTES * 60_000);
      await prisma.passwordResetToken.create({
        data: { userId: user.id, tokenHash: hashToken(rawToken), expiresAt },
      });
      const origin = request.headers.get("origin") ?? process.env.APP_URL ?? "http://localhost:3000";
      const resetUrl = `${origin}/reset-password/${rawToken}`;
      // Personal-use app: no email infra. The reset URL is emitted to the server
      // console so the operator can copy it out. Replace with email delivery
      // when needed.
      console.log(`[password-reset] ${user.email} → ${resetUrl} (expires ${expiresAt.toISOString()})`);
    }
    return NextResponse.json({
      message: "如果该邮箱已注册，我们已生成重置链接。请在服务器日志中查看该链接（个人部署场景）。",
    });
  }

  if (mode === "confirm") {
    const token = String(body.token ?? "");
    const newPassword = String(body.newPassword ?? "");
    if (!token) return NextResponse.json({ message: "重置令牌不能为空" }, { status: 400 });
    if (newPassword.length < MIN_PASSWORD || newPassword.length > MAX_PASSWORD) {
      return NextResponse.json({ message: `新密码长度需在 ${MIN_PASSWORD}-${MAX_PASSWORD} 之间` }, { status: 400 });
    }
    const record = await prisma.passwordResetToken.findUnique({ where: { tokenHash: hashToken(token) } });
    if (!record || record.usedAt || record.expiresAt.getTime() < Date.now()) {
      return NextResponse.json({ message: "重置链接无效或已过期" }, { status: 400 });
    }
    const hashed = await bcrypt.hash(newPassword, 12);
    await prisma.$transaction([
      prisma.user.update({ where: { id: record.userId }, data: { password: hashed } }),
      prisma.passwordResetToken.update({ where: { id: record.id }, data: { usedAt: new Date() } }),
      prisma.passwordResetToken.deleteMany({ where: { userId: record.userId, usedAt: null, id: { not: record.id } } }),
    ]);
    return NextResponse.json({ ok: true });
  }

  return NextResponse.json({ message: "不支持的操作" }, { status: 400 });
}
