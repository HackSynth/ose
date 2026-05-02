import { createCipheriv, createDecipheriv, randomBytes } from 'node:crypto';

const ALGORITHM = 'aes-256-gcm';
const IV_LEN = 16;
const TAG_LEN = 16;
const KEY_LEN = 32;
const PREFIX = 'v1:';

function getKey(): Buffer | null {
  const raw = process.env.OSE_ENCRYPTION_KEY;
  if (!raw) return null;
  try {
    const key = Buffer.from(raw, 'base64');
    return key.length === KEY_LEN ? key : null;
  } catch {
    return null;
  }
}

export function isEncryptionEnabled(): boolean {
  return getKey() !== null;
}

export function isEncryptedSecret(value: string | null | undefined): boolean {
  return typeof value === 'string' && value.startsWith(PREFIX);
}

export function encryptSecret(plain: string): string {
  const key = getKey();
  if (!key) throw new Error('OSE_ENCRYPTION_KEY is not configured or is invalid');
  const iv = randomBytes(IV_LEN);
  const cipher = createCipheriv(ALGORITHM, key, iv);
  const ciphertext = Buffer.concat([cipher.update(plain, 'utf8'), cipher.final()]);
  const tag = cipher.getAuthTag();
  return `${PREFIX}${iv.toString('hex')}:${tag.toString('hex')}:${ciphertext.toString('hex')}`;
}

export function decryptSecret(encrypted: string): string | null {
  if (!isEncryptedSecret(encrypted)) return null;
  const key = getKey();
  if (!key) return null;
  try {
    const parts = encrypted.slice(PREFIX.length).split(':');
    if (parts.length !== 3) return null;
    const [ivHex, tagHex, ctHex] = parts;
    const iv = Buffer.from(ivHex, 'hex');
    const tag = Buffer.from(tagHex, 'hex');
    const ct = Buffer.from(ctHex, 'hex');
    if (iv.length !== IV_LEN || tag.length !== TAG_LEN) return null;
    const decipher = createDecipheriv(ALGORITHM, key, iv);
    decipher.setAuthTag(tag);
    return Buffer.concat([decipher.update(ct), decipher.final()]).toString('utf8');
  } catch {
    return null;
  }
}

/**
 * Resolve a stored key for use:
 * - If an encrypted value exists, decrypt it (null on failure).
 * - If only a legacy plaintext value exists AND encryption is enabled, return it
 *   so the caller can encrypt and migrate it. Without encryption enabled, the
 *   legacy plaintext is treated as unconfigured.
 */
export function resolveSecret(
  encrypted: string | null | undefined,
  plaintext: string | null | undefined,
): string | null {
  if (encrypted) {
    return decryptSecret(encrypted);
  }
  if (plaintext && isEncryptionEnabled()) {
    return plaintext;
  }
  return null;
}
