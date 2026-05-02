import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import {
  decryptSecret,
  encryptSecret,
  isEncryptedSecret,
  isEncryptionEnabled,
  resolveSecret,
} from '@/lib/crypto/secrets';

const VALID_KEY = Buffer.alloc(32, 0xab).toString('base64'); // 32-byte key

function withKey(key: string | undefined, fn: () => void) {
  const prev = process.env.OSE_ENCRYPTION_KEY;
  if (key === undefined) {
    delete process.env.OSE_ENCRYPTION_KEY;
  } else {
    process.env.OSE_ENCRYPTION_KEY = key;
  }
  try {
    fn();
  } finally {
    if (prev === undefined) {
      delete process.env.OSE_ENCRYPTION_KEY;
    } else {
      process.env.OSE_ENCRYPTION_KEY = prev;
    }
  }
}

describe('isEncryptionEnabled', () => {
  it('returns false when OSE_ENCRYPTION_KEY is absent', () => {
    withKey(undefined, () => {
      expect(isEncryptionEnabled()).toBe(false);
    });
  });

  it('returns false when key is wrong length', () => {
    withKey(Buffer.alloc(16).toString('base64'), () => {
      expect(isEncryptionEnabled()).toBe(false);
    });
  });

  it('returns true when a valid 32-byte key is set', () => {
    withKey(VALID_KEY, () => {
      expect(isEncryptionEnabled()).toBe(true);
    });
  });
});

describe('isEncryptedSecret', () => {
  it('returns false for null/undefined', () => {
    expect(isEncryptedSecret(null)).toBe(false);
    expect(isEncryptedSecret(undefined)).toBe(false);
  });

  it('returns false for plaintext strings', () => {
    expect(isEncryptedSecret('sk-abc123')).toBe(false);
    expect(isEncryptedSecret('')).toBe(false);
  });

  it('returns true for v1: prefixed strings', () => {
    expect(isEncryptedSecret('v1:aabb:ccdd:eeff')).toBe(true);
  });
});

describe('encryptSecret / decryptSecret round-trip', () => {
  it('decrypts back to the original value', () => {
    withKey(VALID_KEY, () => {
      const plain = 'sk-test-secret';
      const encrypted = encryptSecret(plain);
      expect(isEncryptedSecret(encrypted)).toBe(true);
      expect(decryptSecret(encrypted)).toBe(plain);
    });
  });

  it('produces a different ciphertext each call (random IV)', () => {
    withKey(VALID_KEY, () => {
      const a = encryptSecret('same-value');
      const b = encryptSecret('same-value');
      expect(a).not.toBe(b);
    });
  });

  it('handles empty strings', () => {
    withKey(VALID_KEY, () => {
      const encrypted = encryptSecret('');
      expect(decryptSecret(encrypted)).toBe('');
    });
  });

  it('handles unicode / multi-byte characters', () => {
    withKey(VALID_KEY, () => {
      const plain = '密钥-🔑-€100';
      expect(decryptSecret(encryptSecret(plain))).toBe(plain);
    });
  });

  it('throws when OSE_ENCRYPTION_KEY is absent', () => {
    withKey(undefined, () => {
      expect(() => encryptSecret('x')).toThrow();
    });
  });
});

describe('decryptSecret', () => {
  it('returns null for non-v1 strings', () => {
    withKey(VALID_KEY, () => {
      expect(decryptSecret('plaintext')).toBeNull();
      expect(decryptSecret('')).toBeNull();
    });
  });

  it('returns null when key is absent', () => {
    let encrypted!: string;
    withKey(VALID_KEY, () => {
      encrypted = encryptSecret('secret');
    });
    withKey(undefined, () => {
      expect(decryptSecret(encrypted)).toBeNull();
    });
  });

  it('returns null when ciphertext is tampered', () => {
    withKey(VALID_KEY, () => {
      const encrypted = encryptSecret('secret');
      const tampered = encrypted.slice(0, -4) + 'xxxx';
      expect(decryptSecret(tampered)).toBeNull();
    });
  });

  it('returns null when format has wrong segment count', () => {
    withKey(VALID_KEY, () => {
      expect(decryptSecret('v1:onlytwoparts')).toBeNull();
    });
  });
});

describe('resolveSecret', () => {
  it('decrypts the encrypted value when present', () => {
    withKey(VALID_KEY, () => {
      const encrypted = encryptSecret('my-api-key');
      expect(resolveSecret(encrypted, null)).toBe('my-api-key');
    });
  });

  it('returns plaintext when encryption is enabled and no encrypted value', () => {
    withKey(VALID_KEY, () => {
      expect(resolveSecret(null, 'legacy-key')).toBe('legacy-key');
    });
  });

  it('returns null when only plaintext exists and encryption is NOT enabled', () => {
    withKey(undefined, () => {
      expect(resolveSecret(null, 'legacy-key')).toBeNull();
    });
  });

  it('returns null when both are null', () => {
    withKey(VALID_KEY, () => {
      expect(resolveSecret(null, null)).toBeNull();
    });
    withKey(undefined, () => {
      expect(resolveSecret(null, null)).toBeNull();
    });
  });

  it('prefers encrypted over plaintext', () => {
    withKey(VALID_KEY, () => {
      const encrypted = encryptSecret('correct-key');
      expect(resolveSecret(encrypted, 'old-key')).toBe('correct-key');
    });
  });

  it('returns null when decryption fails (tampered)', () => {
    withKey(VALID_KEY, () => {
      const tampered = 'v1:aabbccdd:eeff0011:22334455';
      expect(resolveSecret(tampered, 'legacy-key')).toBeNull();
    });
  });
});
