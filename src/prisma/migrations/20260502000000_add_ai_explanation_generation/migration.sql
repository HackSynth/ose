-- CreateTable
CREATE TABLE "AIExplanationGeneration" (
    "id" TEXT NOT NULL PRIMARY KEY,
    "userId" TEXT NOT NULL,
    "questionId" TEXT NOT NULL,
    "wrongNoteId" TEXT,
    "wrongOptionId" TEXT,
    "status" TEXT NOT NULL DEFAULT 'PENDING',
    "provider" TEXT NOT NULL,
    "model" TEXT NOT NULL,
    "content" TEXT,
    "errorMessage" TEXT,
    "createdAt" DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" DATETIME NOT NULL,
    "completedAt" DATETIME,
    CONSTRAINT "AIExplanationGeneration_userId_fkey" FOREIGN KEY ("userId") REFERENCES "User" ("id") ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT "AIExplanationGeneration_questionId_fkey" FOREIGN KEY ("questionId") REFERENCES "Question" ("id") ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT "AIExplanationGeneration_wrongNoteId_fkey" FOREIGN KEY ("wrongNoteId") REFERENCES "WrongNote" ("id") ON DELETE SET NULL ON UPDATE CASCADE
);

-- CreateIndex
CREATE INDEX "AIExplanationGeneration_userId_createdAt_idx" ON "AIExplanationGeneration"("userId", "createdAt");

-- CreateIndex
CREATE INDEX "AIExplanationGeneration_questionId_idx" ON "AIExplanationGeneration"("questionId");

-- CreateIndex
CREATE INDEX "AIExplanationGeneration_wrongNoteId_status_createdAt_idx" ON "AIExplanationGeneration"("wrongNoteId", "status", "createdAt");
