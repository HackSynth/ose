import { afterEach, beforeEach, describe, expect, it } from 'vitest';

import { resolveDefaultModel } from '@/lib/ai/utils';

const DEFAULT_MODEL_ENV_KEYS = [
  'DEFAULT_CLAUDE_MODEL',
  'DEFAULT_OPENAI_MODEL',
  'DEFAULT_GEMINI_MODEL',
  'DEFAULT_CUSTOM_MODEL',
] as const;

function clearDefaultModelEnv() {
  for (const key of DEFAULT_MODEL_ENV_KEYS) delete process.env[key];
}

describe('resolveDefaultModel', () => {
  beforeEach(() => clearDefaultModelEnv());
  afterEach(() => clearDefaultModelEnv());

  it('returns built-in default for claude when no env override', () => {
    expect(resolveDefaultModel('claude')).toBe('claude-sonnet-4-6');
  });

  it('returns built-in default for openai when no env override', () => {
    expect(resolveDefaultModel('openai')).toBe('gpt-5-mini');
  });

  it('returns built-in default for gemini when no env override', () => {
    expect(resolveDefaultModel('gemini')).toBe('gemini-3-flash-preview');
  });

  it('returns built-in default for custom when no env override', () => {
    expect(resolveDefaultModel('custom')).toBe('default');
  });

  it('reads DEFAULT_CLAUDE_MODEL env override', () => {
    process.env.DEFAULT_CLAUDE_MODEL = 'claude-opus-4-7';
    expect(resolveDefaultModel('claude')).toBe('claude-opus-4-7');
  });

  it('reads DEFAULT_OPENAI_MODEL env override', () => {
    process.env.DEFAULT_OPENAI_MODEL = 'gpt-5';
    expect(resolveDefaultModel('openai')).toBe('gpt-5');
  });

  it('reads DEFAULT_GEMINI_MODEL env override', () => {
    process.env.DEFAULT_GEMINI_MODEL = 'gemini-3-pro';
    expect(resolveDefaultModel('gemini')).toBe('gemini-3-pro');
  });

  it('reads DEFAULT_CUSTOM_MODEL env override', () => {
    process.env.DEFAULT_CUSTOM_MODEL = 'my-local-llm';
    expect(resolveDefaultModel('custom')).toBe('my-local-llm');
  });

  it('falls back to built-in when env override is empty string', () => {
    process.env.DEFAULT_CLAUDE_MODEL = '';
    expect(resolveDefaultModel('claude')).toBe('claude-sonnet-4-6');
  });

  it('overrides are independent per provider', () => {
    process.env.DEFAULT_OPENAI_MODEL = 'gpt-5';
    expect(resolveDefaultModel('claude')).toBe('claude-sonnet-4-6');
    expect(resolveDefaultModel('openai')).toBe('gpt-5');
    expect(resolveDefaultModel('gemini')).toBe('gemini-3-flash-preview');
  });
});
