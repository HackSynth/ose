/**
 * Seed 51CTO 软件设计师 historical exams into the OSE database.
 *
 * Source: data/51cto-seed.json (produced by scripts/convert-51cto-to-seed.py
 *         from raw scrapes in data/51cto-exams/)
 *
 * Usage:
 *   tsx src/prisma/seed-51cto.ts
 *
 * Idempotent — every record is upserted by a stable key (year/session/qn for
 * Question, sourceId for Exam, etc.).
 */

import {
  PrismaClient,
  ExamSession,
  ExamSessionType,
  ExamType,
  QuestionType,
  CaseAnswerType,
} from '@prisma/client';
import * as fs from 'node:fs';
import * as path from 'node:path';
import { allKnowledgePoints, FALLBACK_KP } from './knowledge-tree';

const prisma = new PrismaClient();

const SEED_PATH = path.resolve(process.cwd(), 'data/51cto-seed.json');

type SeedOption = {
  label: string;
  content: string;
  isCorrect: boolean;
};

type SeedSubQuestion = {
  sourceQuestionId: number;
  subNumber: number;
  content: string;
  answerType: keyof typeof CaseAnswerType;
  referenceAnswer: string;
  score: number;
  explanation: string;
  showTypeName: string;
};

type SeedQuestion = {
  sourceQuestionId?: number;
  questionNumber: number;
  type: keyof typeof QuestionType;
  content: string;
  difficulty: number;
  explanation: string;
  knowledgePointId: string;
  score?: number;
  showTypeName?: string;
  options?: SeedOption[];
  rawAnswer?: string[];
  caseScenario?: {
    background: string;
    figures: unknown;
    subQuestions: SeedSubQuestion[];
  };
};

type SeedExam = {
  sourceId: number;
  title: string;
  type: keyof typeof ExamType;
  session: keyof typeof ExamSession;
  year: number;
  month: number;
  kind: 'CHOICE' | 'CASE';
  timeLimit: number;
  totalScore: number;
  totalQuestions: number;
  questions: SeedQuestion[];
};

type SeedPayload = {
  metadata: Record<string, unknown>;
  knowledgePoints: Array<{
    id: string;
    name: string;
    sortOrder: number;
    description?: string;
  }>;
  exams: SeedExam[];
};

async function main() {
  if (!fs.existsSync(SEED_PATH)) {
    throw new Error(
      `Seed file not found: ${SEED_PATH}\n` + `Run: python scripts/convert-51cto-to-seed.py`
    );
  }
  const payload = JSON.parse(fs.readFileSync(SEED_PATH, 'utf-8')) as SeedPayload;

  console.log(
    `Loading 51CTO seed: ${payload.exams.length} exams, ` +
      `${payload.metadata.scopedQuestionRecordCount} question rows`
  );

  // 1. Knowledge points — upsert full tree (parents + leaves) plus the
  //    placeholder used for any unclassified question.
  console.log('Upserting knowledge tree...');
  for (const kp of allKnowledgePoints()) {
    await prisma.knowledgePoint.upsert({
      where: { id: kp.id },
      update: {
        name: kp.name,
        parentId: kp.parentId,
        sortOrder: kp.sortOrder,
        description: kp.description,
      },
      create: {
        id: kp.id,
        name: kp.name,
        parentId: kp.parentId,
        sortOrder: kp.sortOrder,
        description: kp.description,
      },
    });
  }
  await prisma.knowledgePoint.upsert({
    where: { id: FALLBACK_KP.id },
    update: {
      name: FALLBACK_KP.name,
      sortOrder: FALLBACK_KP.sortOrder,
      description: FALLBACK_KP.description,
    },
    create: {
      id: FALLBACK_KP.id,
      name: FALLBACK_KP.name,
      sortOrder: FALLBACK_KP.sortOrder,
      description: FALLBACK_KP.description,
    },
  });

  // 2. Exams + questions
  let examIdx = 0;
  for (const seedExam of payload.exams) {
    examIdx += 1;
    const examId = `exam-51cto-${seedExam.sourceId}`;
    const sessionType = (seedExam.session === 'AM' ? 'AM' : 'PM') as keyof typeof ExamSessionType;

    console.log(
      `[${examIdx}/${payload.exams.length}] ${seedExam.title} ` +
        `(${seedExam.questions.length} ${seedExam.kind === 'CHOICE' ? 'choice' : 'scenarios'})`
    );

    const exam = await prisma.exam.upsert({
      where: { id: examId },
      update: {
        title: seedExam.title,
        type: ExamType[seedExam.type],
        session: ExamSessionType[sessionType],
        timeLimit: seedExam.timeLimit,
        totalScore: seedExam.totalScore,
      },
      create: {
        id: examId,
        title: seedExam.title,
        type: ExamType[seedExam.type],
        session: ExamSessionType[sessionType],
        timeLimit: seedExam.timeLimit,
        totalScore: seedExam.totalScore,
      },
    });

    // Drop and recreate ExamQuestion links so reordering is consistent.
    await prisma.examQuestion.deleteMany({ where: { examId: exam.id } });

    for (const [qIdx, sq] of seedExam.questions.entries()) {
      const session = ExamSession[seedExam.session];

      // Upsert the Question
      const question = await prisma.question.upsert({
        where: {
          year_session_questionNumber: {
            year: seedExam.year,
            session,
            questionNumber: sq.questionNumber,
          },
        },
        update: {
          content: sq.content,
          type: QuestionType[sq.type],
          difficulty: sq.difficulty,
          explanation: sq.explanation,
          knowledgePointId: sq.knowledgePointId,
        },
        create: {
          content: sq.content,
          type: QuestionType[sq.type],
          difficulty: sq.difficulty,
          year: seedExam.year,
          session,
          questionNumber: sq.questionNumber,
          explanation: sq.explanation,
          knowledgePointId: sq.knowledgePointId,
        },
      });

      if (sq.type === 'CHOICE') {
        // Reset options to match source exactly.
        await prisma.questionOption.deleteMany({
          where: { questionId: question.id },
        });
        if (sq.options && sq.options.length > 0) {
          await prisma.questionOption.createMany({
            data: sq.options.map((opt) => ({
              questionId: question.id,
              label: opt.label,
              content: opt.content,
              isCorrect: opt.isCorrect,
            })),
          });
        }
      } else if (sq.type === 'CASE_ANALYSIS' && sq.caseScenario) {
        // Reset case scenario + sub-questions.
        await prisma.caseScenario.deleteMany({
          where: { questionId: question.id },
        });
        await prisma.caseScenario.create({
          data: {
            questionId: question.id,
            background: sq.caseScenario.background,
            figures: (sq.caseScenario.figures ?? undefined) as never,
            subQuestions: {
              create: sq.caseScenario.subQuestions.map((sub) => ({
                subNumber: sub.subNumber,
                content: sub.content,
                answerType: CaseAnswerType[sub.answerType],
                referenceAnswer: sub.referenceAnswer,
                score: sub.score,
                explanation: sub.explanation,
              })),
            },
          },
        });
      }

      await prisma.examQuestion.create({
        data: {
          examId: exam.id,
          questionId: question.id,
          orderNumber: qIdx + 1,
        },
      });
    }
  }

  console.log('\nDone.');
  console.log(
    `  ${payload.exams.length} exams, ` +
      `${payload.metadata.scopedQuestionRecordCount} questions, ` +
      `${payload.metadata.scenarioCount} case scenarios.`
  );
}

main()
  .catch((err) => {
    console.error(err);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
