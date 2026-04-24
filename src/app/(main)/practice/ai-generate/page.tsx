import { redirect } from "next/navigation";
import { auth } from "@/lib/auth";
import { prisma } from "@/lib/prisma";
import { getUserAnalysis } from "@/lib/analysis";
import { AIGenerateClient } from "@/components/ai-generate-client";

export default async function AIGeneratePage() {
  const session = await auth();
  if (!session?.user?.id) redirect("/login");
  const userId = session.user.id;
  const [roots, history, analysis] = await Promise.all([
    prisma.knowledgePoint.findMany({ where: { parentId: null }, orderBy: { sortOrder: "asc" }, include: { children: { orderBy: { sortOrder: "asc" } } } }),
    prisma.aIQuestionGeneration.findMany({ where: { userId }, orderBy: { createdAt: "desc" }, take: 12 }),
    getUserAnalysis(userId),
  ]);
  const weakIds = analysis.knowledgePoints.flatMap((kp) => kp.children?.length ? kp.children : [kp]).filter((kp) => kp.status === "危险" || kp.status === "薄弱").sort((a, b) => a.mastery - b.mastery).slice(0, 3).map((kp) => kp.id);
  return <AIGenerateClient knowledgeTree={roots} history={history.map((item) => ({ ...item, createdAt: item.createdAt.toISOString() }))} weakIds={weakIds} />;
}
