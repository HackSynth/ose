import { NextResponse } from "next/server";
import { auth } from "@/lib/auth";
import { prisma } from "@/lib/prisma";
import { getChinaDateKey, getRecentDateKeys } from "@/lib/stats";

export async function GET() {
  const session = await auth();
  if (!session?.user?.id) return NextResponse.json({ message: "请先登录" }, { status: 401 });
  const keys = getRecentDateKeys(90);
  const start = new Date(`${keys[0]}T00:00:00+08:00`);
  const answers = await prisma.userAnswer.findMany({ where: { userId: session.user.id, createdAt: { gte: start } }, select: { createdAt: true } });
  const map = new Map(keys.map((key) => [key, 0]));
  answers.forEach((answer) => {
    const key = getChinaDateKey(answer.createdAt);
    map.set(key, (map.get(key) ?? 0) + 1);
  });
  return NextResponse.json({ days: keys.map((date) => ({ date, count: map.get(date) ?? 0 })) });
}
