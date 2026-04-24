import { NextResponse } from "next/server";
import { auth } from "@/lib/auth";
import { prisma } from "@/lib/prisma";
import { getTopicAnswerStats, loadKnowledgeTree, rollupStats } from "@/lib/knowledge-stats";

export async function GET() {
  const session = await auth();
  if (!session?.user?.id) return NextResponse.json({ message: "未登录" }, { status: 401 });

  const [roots, topicStats, tree] = await Promise.all([
    prisma.knowledgePoint.findMany({
      where: { parentId: null },
      orderBy: { sortOrder: "asc" },
      include: {
        children: {
          orderBy: { sortOrder: "asc" },
          include: { _count: { select: { questions: true } } },
        },
      },
    }),
    getTopicAnswerStats(session.user.id),
    loadKnowledgeTree(),
  ]);

  const topics = roots.map((root) => {
    const stat = rollupStats(tree.descendantsOf, topicStats, root.id);
    const totalQuestions = root.children.reduce((sum, child) => sum + child._count.questions, 0);
    return {
      id: root.id,
      name: root.name,
      description: root.description,
      questionCount: totalQuestions,
      doneCount: stat.total,
      accuracy: stat.accuracy,
      children: root.children.map((child) => ({ id: child.id, name: child.name, questionCount: child._count.questions })),
    };
  });

  return NextResponse.json({ topics });
}
