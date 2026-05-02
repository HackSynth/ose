import { testApiHandler } from 'next-test-api-route-handler';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { decryptSecret } from '@/lib/crypto/secrets';
import { prisma } from '@/lib/prisma';
import { createTestUser, resetUserData, sessionFor } from '@/test/helpers';

const VALID_KEY = Buffer.alloc(32, 0xcd).toString('base64');

async function loadAISettingsRoute(session: unknown) {
  vi.resetModules();
  vi.doMock('@/lib/auth', () => ({ auth: async () => session }));
  return import('@/app/api/profile/ai-settings/route');
}

describe('/api/profile/ai-settings secret storage', () => {
  beforeEach(async () => {
    delete process.env.OSE_ENCRYPTION_KEY;
    await resetUserData();
  });

  afterEach(() => {
    delete process.env.OSE_ENCRYPTION_KEY;
    vi.doUnmock('@/lib/auth');
  });

  it('refuses to persist a new API key when OSE_ENCRYPTION_KEY is missing', async () => {
    const user = await createTestUser({
      id: 'ai-settings-no-key',
      email: 'ai-settings-no-key@example.com',
    });
    const appHandler = await loadAISettingsRoute(sessionFor(user));

    await testApiHandler({
      appHandler,
      async test({ fetch }) {
        const response = await fetch({
          method: 'PUT',
          headers: { 'content-type': 'application/json' },
          body: JSON.stringify({
            provider: 'openai',
            model: 'gpt-test',
            apiKey: 'sk-plaintext-should-not-save',
          }),
        });

        expect(response.status).toBe(503);
      },
    });

    const saved = await prisma.userAISettings.findUnique({ where: { userId: user.id } });
    expect(saved).toBeNull();
  });

  it('encrypts text and image API keys on write and decrypts them for settings reads', async () => {
    process.env.OSE_ENCRYPTION_KEY = VALID_KEY;
    const user = await createTestUser({
      id: 'ai-settings-encrypted',
      email: 'ai-settings-encrypted@example.com',
    });
    const appHandler = await loadAISettingsRoute(sessionFor(user));

    await testApiHandler({
      appHandler,
      async test({ fetch }) {
        const putResponse = await fetch({
          method: 'PUT',
          headers: { 'content-type': 'application/json' },
          body: JSON.stringify({
            provider: 'openai',
            model: 'gpt-test',
            apiKey: 'sk-text-secret',
            imageProvider: 'openai',
            imageModel: 'gpt-image-test',
            imageApiKey: 'sk-image-secret',
          }),
        });

        expect(putResponse.status).toBe(200);

        const getResponse = await fetch();
        const body = await getResponse.json();

        expect(getResponse.status).toBe(200);
        expect(body.hasApiKey).toBe(true);
        expect(body.hasImageApiKey).toBe(true);
        expect(body.apiKeyMasked).not.toContain('sk-text-secret');
        expect(body.imageApiKeyMasked).not.toContain('sk-image-secret');
      },
    });

    const saved = await prisma.userAISettings.findUnique({ where: { userId: user.id } });
    expect(saved?.apiKey).toBeNull();
    expect(saved?.imageApiKey).toBeNull();
    expect(saved?.apiKeyEncrypted).toMatch(/^v1:/);
    expect(saved?.imageApiKeyEncrypted).toMatch(/^v1:/);
    expect(decryptSecret(saved!.apiKeyEncrypted!)).toBe('sk-text-secret');
    expect(decryptSecret(saved!.imageApiKeyEncrypted!)).toBe('sk-image-secret');
  });

  it('lazy-migrates legacy plaintext API keys when encryption is configured', async () => {
    process.env.OSE_ENCRYPTION_KEY = VALID_KEY;
    const user = await createTestUser({
      id: 'ai-settings-legacy',
      email: 'ai-settings-legacy@example.com',
    });
    await prisma.userAISettings.create({
      data: {
        userId: user.id,
        provider: 'openai',
        model: 'gpt-test',
        apiKey: 'sk-legacy-secret',
      },
    });

    const { resolveAIConfig } = await import('@/lib/ai');
    const config = await resolveAIConfig(user.id);

    expect(config).toMatchObject({
      provider: 'openai',
      apiKey: 'sk-legacy-secret',
      model: 'gpt-test',
    });

    await vi.waitFor(async () => {
      const migrated = await prisma.userAISettings.findUnique({ where: { userId: user.id } });
      expect(migrated?.apiKey).toBeNull();
      expect(migrated?.apiKeyEncrypted).toMatch(/^v1:/);
      expect(decryptSecret(migrated!.apiKeyEncrypted!)).toBe('sk-legacy-secret');
    });
  });
});
