import { beforeEach, describe, expect, it, vi } from 'vitest';

const postMock = vi.fn();
const getMock = vi.fn();
const putMock = vi.fn();
const patchMock = vi.fn();
const deleteMock = vi.fn();

vi.mock('./http', () => ({
  default: {
    post: postMock,
    get: getMock,
    put: putMock,
    patch: patchMock,
    delete: deleteMock,
  },
}));

describe('api ai request timeouts', () => {
  beforeEach(() => {
    vi.resetModules();
    postMock.mockReset();
    getMock.mockReset();
    putMock.mockReset();
    patchMock.mockReset();
    deleteMock.mockReset();
  });

  it('uses extended timeout for ai settings connectivity test', async () => {
    const { api } = await import('./index');
    await api.testAiSettings('OPENAI', { enabled: true });

    expect(postMock).toHaveBeenCalledWith(
      '/ai/settings/OPENAI/test',
      { enabled: true },
      expect.objectContaining({ timeout: 90000 }),
    );
  });

  it('uses extended timeout for ai question generation', async () => {
    const { api } = await import('./index');
    await api.aiGenerateQuestions({ provider: 'OPENAI' });

    expect(postMock).toHaveBeenCalledWith(
      '/ai/questions/generate',
      { provider: 'OPENAI' },
      expect.objectContaining({ timeout: 90000 }),
    );
  });
});
