import { notFound, redirect } from "next/navigation";
import { auth } from "@/lib/auth";
import { prisma } from "@/lib/prisma";
import { ExamAttemptClient } from "@/components/exam-attempt-client";

export default async function ExamAttemptPage({ params }: { params: Promise<{ attemptId: string }> }) {
  const session = await auth();
  if (!session?.user?.id) redirect("/login");
  const { attemptId } = await params;
  const attempt = await prisma.examAttempt.findFirst({
    where: { id: attemptId, userId: session.user.id },
    include: { exam: { include: { questions: { orderBy: { orderNumber: "asc" }, include: { question: { include: { options: { orderBy: { label: "asc" } }, caseScenario: { include: { subQuestions: { orderBy: { subNumber: "asc" } } } } } } } } } }, answers: true },
  });
  if (!attempt) notFound();
  if (attempt.status === "COMPLETED") redirect(`/exam/${attempt.id}/result`);
  return <ExamAttemptClient attempt={attempt} />;
}
