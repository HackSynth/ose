import { NextResponse } from "next/server";
import { auth } from "@/lib/auth";
import { getUserAnalysis } from "@/lib/analysis";

export async function GET() {
  const session = await auth();
  if (!session?.user?.id) return NextResponse.json({ message: "请先登录" }, { status: 401 });
  return NextResponse.json(await getUserAnalysis(session.user.id));
}
