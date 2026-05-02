import { notFound } from "next/navigation";
import { prisma } from "@/lib/prisma";
import { CaseAnswerClient } from "@/components/case-answer-client";

export default async function CaseAnswerPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const question = await prisma.question.findFirst({
    where: { id, type: "CASE_ANALYSIS" },
    include: { knowledgePoint: { include: { parent: true } }, caseScenario: { include: { subQuestions: { orderBy: { subNumber: "asc" } } } } },
  });
  if (!question?.caseScenario) notFound();
  const caseQuestion = question as typeof question & { caseScenario: NonNullable<typeof question.caseScenario> };
  return <CaseAnswerClient question={caseQuestion} />;
}
