import { NextResponse } from "next/server";
import { auth } from "@/lib/auth";
import { prisma } from "@/lib/prisma";
import { clampInt } from "@/lib/validate";
import { PAGE_SIZE_DEFAULT, PAGE_SIZE_MAX } from "@/lib/constants";

export async function GET(request: Request) {
  const session = await auth();
  if (!session?.user?.id) return NextResponse.json({ message: "请先登录" }, { status: 401 });
  const userId = session.user.id;
  const { searchParams } = new URL(request.url);
  const page = clampInt(searchParams.get("page"), 1, 10_000, 1);
  const pageSize = clampInt(searchParams.get("pageSize"), 1, PAGE_SIZE_MAX, PAGE_SIZE_DEFAULT);

  const [plans, total] = await Promise.all([
    prisma.studyPlan.findMany({
      where: { userId },
      orderBy: { createdAt: "desc" },
      skip: (page - 1) * pageSize,
      take: pageSize,
      select: {
        id: true,
        title: true,
        status: true,
        targetExamDate: true,
        totalDays: true,
        createdAt: true,
        _count: { select: { days: true } },
        days: { select: { id: true, completed: true } },
      },
    }),
    prisma.studyPlan.count({ where: { userId } }),
  ]);
  return NextResponse.json({
    plans,
    pagination: { page, pageSize, total, totalPages: Math.max(1, Math.ceil(total / pageSize)) },
  });
}
