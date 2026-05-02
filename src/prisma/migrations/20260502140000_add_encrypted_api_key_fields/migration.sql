-- AlterTable: add encrypted API key columns to UserAISettings
ALTER TABLE "UserAISettings" ADD COLUMN "apiKeyEncrypted" TEXT;
ALTER TABLE "UserAISettings" ADD COLUMN "imageApiKeyEncrypted" TEXT;
