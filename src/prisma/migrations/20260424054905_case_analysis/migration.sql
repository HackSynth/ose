-- CreateTable
CREATE TABLE "CaseScenario" (
    "id" TEXT NOT NULL PRIMARY KEY,
    "questionId" TEXT NOT NULL,
    "background" TEXT NOT NULL,
    "figures" JSONB,
    CONSTRAINT "CaseScenario_questionId_fkey" FOREIGN KEY ("questionId") REFERENCES "Question" ("id") ON DELETE CASCADE ON UPDATE CASCADE
);

-- CreateTable
CREATE TABLE "CaseSubQuestion" (
    "id" TEXT NOT NULL PRIMARY KEY,
    "caseScenarioId" TEXT NOT NULL,
    "subNumber" INTEGER NOT NULL,
    "content" TEXT NOT NULL,
    "answerType" TEXT NOT NULL,
    "referenceAnswer" TEXT NOT NULL,
    "score" INTEGER NOT NULL,
    "explanation" TEXT NOT NULL,
    CONSTRAINT "CaseSubQuestion_caseScenarioId_fkey" FOREIGN KEY ("caseScenarioId") REFERENCES "CaseScenario" ("id") ON DELETE CASCADE ON UPDATE CASCADE
);

-- CreateTable
CREATE TABLE "UserCaseAnswer" (
    "id" TEXT NOT NULL PRIMARY KEY,
    "userId" TEXT NOT NULL,
    "caseSubQuestionId" TEXT NOT NULL,
    "answer" TEXT NOT NULL,
    "score" INTEGER,
    "feedback" TEXT,
    "createdAt" DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT "UserCaseAnswer_userId_fkey" FOREIGN KEY ("userId") REFERENCES "User" ("id") ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT "UserCaseAnswer_caseSubQuestionId_fkey" FOREIGN KEY ("caseSubQuestionId") REFERENCES "CaseSubQuestion" ("id") ON DELETE CASCADE ON UPDATE CASCADE
);

-- CreateIndex
CREATE UNIQUE INDEX "CaseScenario_questionId_key" ON "CaseScenario"("questionId");

-- CreateIndex
CREATE INDEX "CaseScenario_questionId_idx" ON "CaseScenario"("questionId");

-- CreateIndex
CREATE INDEX "CaseSubQuestion_caseScenarioId_idx" ON "CaseSubQuestion"("caseScenarioId");

-- CreateIndex
CREATE UNIQUE INDEX "CaseSubQuestion_caseScenarioId_subNumber_key" ON "CaseSubQuestion"("caseScenarioId", "subNumber");

-- CreateIndex
CREATE INDEX "UserCaseAnswer_userId_createdAt_idx" ON "UserCaseAnswer"("userId", "createdAt");

-- CreateIndex
CREATE INDEX "UserCaseAnswer_caseSubQuestionId_idx" ON "UserCaseAnswer"("caseSubQuestionId");
