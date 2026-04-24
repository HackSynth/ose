-- AlterTable: add owner column to Question
ALTER TABLE "Question" ADD COLUMN "createdByUserId" TEXT;

-- AlterTable: add owner column to Exam
ALTER TABLE "Exam" ADD COLUMN "createdByUserId" TEXT;

-- CreateIndex
CREATE INDEX "Question_createdByUserId_idx" ON "Question"("createdByUserId");
CREATE INDEX "Exam_createdByUserId_idx" ON "Exam"("createdByUserId");

-- Deduplicate UserCaseAnswer before adding unique constraint (keeps latest row per (userId, caseSubQuestionId))
DELETE FROM "UserCaseAnswer"
WHERE "id" NOT IN (
  SELECT "id" FROM "UserCaseAnswer" u1
  WHERE "createdAt" = (
    SELECT MAX("createdAt") FROM "UserCaseAnswer" u2
    WHERE u2."userId" = u1."userId" AND u2."caseSubQuestionId" = u1."caseSubQuestionId"
  )
  GROUP BY "userId", "caseSubQuestionId"
);

-- CreateIndex
CREATE UNIQUE INDEX "UserCaseAnswer_userId_caseSubQuestionId_key" ON "UserCaseAnswer"("userId", "caseSubQuestionId");
