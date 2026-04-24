import { NextResponse } from "next/server";
import { auth } from "@/lib/auth";
import { prisma } from "@/lib/prisma";
import { clampInt } from "@/lib/validate";
import { getDescendantTopicIds } from "@/lib/knowledge-stats";
import { PAGE_SIZE_DEFAULT, PAGE_SIZE_MAX } from "@/lib/constants";

function getStatusFilter(status: string | null) {
  if (status === "mastered") return { markedMastered: true };
  if (status === "unmastered") return { markedMastered: false };
  return {};
}

export async function GET(request: Request) {
  const session = await auth();
  if (!session?.user?.id) return NextResponse.json({ message: "请先登录" }, { status: 401 });
  const userId = session.user.id;

  const { searchParams } = new URL(request.url);
  const page = clampInt(searchParams.get("page"), 1, 10_000, 1);
  const pageSize = clampInt(searchParams.get("pageSize"), 1, PAGE_SIZE_MAX, PAGE_SIZE_DEFAULT);
  const status = searchParams.get("status");
  const knowledgePointId = searchParams.get("knowledgePointId");
  const topicIds = knowledgePointId ? await getDescendantTopicIds(knowledgePointId) : undefined;

  const where = {
    userId,
    ...getStatusFilter(status),
    ...(topicIds ? { question: { knowledgePointId: { in: topicIds } } } : {}),
  };

  const [filteredTotal, total, unmastered, mastered, notes, topics] = await Promise.all([
    prisma.wrongNote.count({ where }),
    prisma.wrongNote.count({ where: { userId } }),
    prisma.wrongNote.count({ where: { userId, markedMastered: false } }),
    prisma.wrongNote.count({ where: { userId, markedMastered: true } }),
    prisma.wrongNote.findMany({
      where,
      orderBy: { updatedAt: "desc" },
      skip: (page - 1) * pageSize,
      take: pageSize,
      select: {
        id: true,
        note: true,
        markedMastered: true,
        createdAt: true,
        updatedAt: true,
        question: {
          select: {
            id: true,
            content: true,
            explanation: true,
            difficulty: true,
            questionNumber: true,
            year: true,
            session: true,
            knowledgePoint: { select: { id: true, name: true, parent: { select: { id: true, name: true } } } },
            options: {
              orderBy: { label: "asc" },
              select: { id: true, label: true, content: true, isCorrect: true },
            },
            userAnswers: {
              where: { userId },
              orderBy: { createdAt: "desc" },
              take: 5,
              select: {
                id: true,
                isCorrect: true,
                selectedOptionId: true,
                createdAt: true,
                selectedOption: { select: { id: true, label: true, content: true } },
              },
            },
          },
        },
      },
    }),
    prisma.knowledgePoint.findMany({
      orderBy: [{ parentId: "asc" }, { sortOrder: "asc" }],
      select: { id: true, name: true, parentId: true },
    }),
  ]);

  const items = notes.map((note) => {
    const wrongAnswers = note.question.userAnswers.filter((answer) => !answer.isCorrect);
    const latestWrong = wrongAnswers[0] ?? note.question.userAnswers[0];
    const correctOption = note.question.options.find((option) => option.isCorrect);
    return {
      id: note.id,
      note: note.note,
      markedMastered: note.markedMastered,
      createdAt: note.createdAt,
      updatedAt: note.updatedAt,
      wrongCount: wrongAnswers.length,
      lastWrongAt: latestWrong?.createdAt ?? note.updatedAt,
      wrongOptionId: latestWrong?.selectedOptionId,
      wrongOption: latestWrong?.selectedOption,
      correctOption: correctOption ? { id: correctOption.id, label: correctOption.label, content: correctOption.content } : undefined,
      question: {
        id: note.question.id,
        content: note.question.content,
        explanation: note.question.explanation,
        difficulty: note.question.difficulty,
        questionNumber: note.question.questionNumber,
        year: note.question.year,
        session: note.question.session,
        knowledgePoint: note.question.knowledgePoint,
        options: note.question.options.map(({ isCorrect: _isCorrect, ...option }) => option),
      },
    };
  });

  return NextResponse.json({
    stats: { total, unmastered, mastered },
    topics,
    items,
    pagination: { page, pageSize, total: filteredTotal, totalPages: Math.max(1, Math.ceil(filteredTotal / pageSize)) },
  });
}
