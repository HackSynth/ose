-- CreateTable
CREATE TABLE "AIQuestionGeneration" (
    "id" TEXT NOT NULL PRIMARY KEY,
    "userId" TEXT NOT NULL,
    "type" TEXT NOT NULL,
    "knowledgePointNames" TEXT NOT NULL,
    "difficulty" INTEGER NOT NULL,
    "count" INTEGER NOT NULL,
    "caseType" TEXT,
    "questionIds" JSONB NOT NULL,
    "createdAt" DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT "AIQuestionGeneration_userId_fkey" FOREIGN KEY ("userId") REFERENCES "User" ("id") ON DELETE CASCADE ON UPDATE CASCADE
);

-- RedefineTables
PRAGMA defer_foreign_keys=ON;
PRAGMA foreign_keys=OFF;
CREATE TABLE "new_Question" (
    "id" TEXT NOT NULL PRIMARY KEY,
    "content" TEXT NOT NULL,
    "type" TEXT NOT NULL DEFAULT 'CHOICE',
    "difficulty" INTEGER NOT NULL,
    "year" INTEGER NOT NULL,
    "session" TEXT NOT NULL,
    "questionNumber" INTEGER NOT NULL,
    "explanation" TEXT NOT NULL,
    "isAIGenerated" BOOLEAN NOT NULL DEFAULT false,
    "aiGeneratedBy" TEXT,
    "knowledgePointId" TEXT NOT NULL,
    CONSTRAINT "Question_knowledgePointId_fkey" FOREIGN KEY ("knowledgePointId") REFERENCES "KnowledgePoint" ("id") ON DELETE RESTRICT ON UPDATE CASCADE
);
INSERT INTO "new_Question" ("content", "difficulty", "explanation", "id", "knowledgePointId", "questionNumber", "session", "type", "year") SELECT "content", "difficulty", "explanation", "id", "knowledgePointId", "questionNumber", "session", "type", "year" FROM "Question";
DROP TABLE "Question";
ALTER TABLE "new_Question" RENAME TO "Question";
CREATE INDEX "Question_isAIGenerated_idx" ON "Question"("isAIGenerated");
CREATE INDEX "Question_knowledgePointId_idx" ON "Question"("knowledgePointId");
CREATE INDEX "Question_difficulty_idx" ON "Question"("difficulty");
CREATE INDEX "Question_year_session_idx" ON "Question"("year", "session");
CREATE UNIQUE INDEX "Question_year_session_questionNumber_key" ON "Question"("year", "session", "questionNumber");
PRAGMA foreign_keys=ON;
PRAGMA defer_foreign_keys=OFF;

-- CreateIndex
CREATE INDEX "AIQuestionGeneration_userId_createdAt_idx" ON "AIQuestionGeneration"("userId", "createdAt");

-- CreateIndex
CREATE INDEX "AIQuestionGeneration_type_idx" ON "AIQuestionGeneration"("type");
