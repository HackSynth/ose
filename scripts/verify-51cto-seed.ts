import { PrismaClient } from '@prisma/client';

const p = new PrismaClient();

async function main() {
  const examCount = await p.exam.count();
  const qCount = await p.question.count();
  const choiceCount = await p.question.count({ where: { type: 'CHOICE' } });
  const caseCount = await p.question.count({ where: { type: 'CASE_ANALYSIS' } });
  const scenarioCount = await p.caseScenario.count();
  const subQCount = await p.caseSubQuestion.count();
  const optionCount = await p.questionOption.count();
  const examQCount = await p.examQuestion.count();
  const kpCount = await p.knowledgePoint.count();

  console.log('Exams:', examCount);
  console.log('Questions:', qCount, '(choice:', choiceCount, '/ case:', caseCount, ')');
  console.log('CaseScenarios:', scenarioCount, 'SubQuestions:', subQCount);
  console.log('QuestionOptions:', optionCount);
  console.log('ExamQuestion links:', examQCount);
  console.log('KnowledgePoints:', kpCount);

  const top = await p.knowledgePoint.findMany({
    where: { parentId: { not: null } },
    include: { _count: { select: { questions: true } } },
    orderBy: { questions: { _count: 'desc' } },
    take: 10,
  });
  console.log('\nTop 10 leaf KPs by question count:');
  top.forEach((k) => console.log(`  ${k.id} ${k.name}: ${k._count.questions}`));

  const recentExam = await p.exam.findFirst({
    where: { id: 'exam-51cto-19359' },
    include: {
      questions: {
        take: 3,
        include: {
          question: {
            include: { options: true, knowledgePoint: true },
          },
        },
      },
    },
  });
  if (recentExam) {
    console.log(`\nSample exam: ${recentExam.title}`);
    recentExam.questions.forEach((eq) => {
      const q = eq.question;
      console.log(
        `  q#${q.questionNumber} kp=${q.knowledgePointId} (${q.knowledgePoint.name}) opts=${q.options.length}`
      );
    });
  }
}

main()
  .catch((e) => {
    console.error(e);
    process.exit(1);
  })
  .finally(() => p.$disconnect());
