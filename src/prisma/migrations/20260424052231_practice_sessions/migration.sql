-- CreateTable
CREATE TABLE "PracticeSession" (
    "id" TEXT NOT NULL PRIMARY KEY,
    "userId" TEXT NOT NULL,
    "mode" TEXT NOT NULL,
    "topicId" TEXT,
    "total" INTEGER NOT NULL,
    "startedAt" DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "completedAt" DATETIME,
    CONSTRAINT "PracticeSession_userId_fkey" FOREIGN KEY ("userId") REFERENCES "User" ("id") ON DELETE CASCADE ON UPDATE CASCADE
);

-- CreateTable
CREATE TABLE "PracticeSessionQuestion" (
    "id" TEXT NOT NULL PRIMARY KEY,
    "practiceSessionId" TEXT NOT NULL,
    "questionId" TEXT NOT NULL,
    "order" INTEGER NOT NULL,
    CONSTRAINT "PracticeSessionQuestion_practiceSessionId_fkey" FOREIGN KEY ("practiceSessionId") REFERENCES "PracticeSession" ("id") ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT "PracticeSessionQuestion_questionId_fkey" FOREIGN KEY ("questionId") REFERENCES "Question" ("id") ON DELETE CASCADE ON UPDATE CASCADE
);

-- RedefineTables
PRAGMA defer_foreign_keys=ON;
PRAGMA foreign_keys=OFF;
CREATE TABLE "new_UserAnswer" (
    "id" TEXT NOT NULL PRIMARY KEY,
    "userId" TEXT NOT NULL,
    "questionId" TEXT NOT NULL,
    "selectedOptionId" TEXT NOT NULL,
    "practiceSessionId" TEXT,
    "isCorrect" BOOLEAN NOT NULL,
    "timeSpent" INTEGER NOT NULL,
    "createdAt" DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT "UserAnswer_userId_fkey" FOREIGN KEY ("userId") REFERENCES "User" ("id") ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT "UserAnswer_questionId_fkey" FOREIGN KEY ("questionId") REFERENCES "Question" ("id") ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT "UserAnswer_selectedOptionId_fkey" FOREIGN KEY ("selectedOptionId") REFERENCES "QuestionOption" ("id") ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT "UserAnswer_practiceSessionId_fkey" FOREIGN KEY ("practiceSessionId") REFERENCES "PracticeSession" ("id") ON DELETE SET NULL ON UPDATE CASCADE
);
INSERT INTO "new_UserAnswer" ("createdAt", "id", "isCorrect", "questionId", "selectedOptionId", "timeSpent", "userId") SELECT "createdAt", "id", "isCorrect", "questionId", "selectedOptionId", "timeSpent", "userId" FROM "UserAnswer";
DROP TABLE "UserAnswer";
ALTER TABLE "new_UserAnswer" RENAME TO "UserAnswer";
CREATE INDEX "UserAnswer_userId_createdAt_idx" ON "UserAnswer"("userId", "createdAt");
CREATE INDEX "UserAnswer_practiceSessionId_idx" ON "UserAnswer"("practiceSessionId");
CREATE INDEX "UserAnswer_questionId_idx" ON "UserAnswer"("questionId");
CREATE INDEX "UserAnswer_isCorrect_idx" ON "UserAnswer"("isCorrect");
PRAGMA foreign_keys=ON;
PRAGMA defer_foreign_keys=OFF;

-- CreateIndex
CREATE INDEX "PracticeSession_userId_startedAt_idx" ON "PracticeSession"("userId", "startedAt");

-- CreateIndex
CREATE INDEX "PracticeSession_mode_idx" ON "PracticeSession"("mode");

-- CreateIndex
CREATE INDEX "PracticeSessionQuestion_practiceSessionId_order_idx" ON "PracticeSessionQuestion"("practiceSessionId", "order");

-- CreateIndex
CREATE INDEX "PracticeSessionQuestion_questionId_idx" ON "PracticeSessionQuestion"("questionId");

-- CreateIndex
CREATE UNIQUE INDEX "PracticeSessionQuestion_practiceSessionId_questionId_key" ON "PracticeSessionQuestion"("practiceSessionId", "questionId");
