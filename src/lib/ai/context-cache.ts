const ANALYSIS_TTL_MS = 60_000;
const STABLE_TTL_MS = 60_000;
const CLEANUP_INTERVAL_MS = 5 * 60_000;

type CacheEntry<T> = { value: T; expiresAt: number };

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const analysisCache = new Map<string, CacheEntry<any>>();
// eslint-disable-next-line @typescript-eslint/no-explicit-any
const stableCache = new Map<string, CacheEntry<any>>();
let lastCleanup = 0;

function cleanup(now: number) {
  if (now - lastCleanup < CLEANUP_INTERVAL_MS) return;
  lastCleanup = now;
  for (const [key, entry] of analysisCache) {
    if (entry.expiresAt <= now) analysisCache.delete(key);
  }
  for (const [key, entry] of stableCache) {
    if (entry.expiresAt <= now) stableCache.delete(key);
  }
}

export async function getOrSetAnalysis<T>(userId: string, compute: () => Promise<T>): Promise<T> {
  const now = Date.now();
  cleanup(now);
  const key = `analysis:${userId}`;
  const cached = analysisCache.get(key) as CacheEntry<T> | undefined;
  if (cached && cached.expiresAt > now) return cached.value;
  const value = await compute();
  analysisCache.set(key, { value, expiresAt: now + ANALYSIS_TTL_MS });
  return value;
}

export async function getOrSetStable<T>(userId: string, compute: () => Promise<T>): Promise<T> {
  const now = Date.now();
  cleanup(now);
  const key = `stable:${userId}`;
  const cached = stableCache.get(key) as CacheEntry<T> | undefined;
  if (cached && cached.expiresAt > now) return cached.value;
  const value = await compute();
  stableCache.set(key, { value, expiresAt: now + STABLE_TTL_MS });
  return value;
}

export function invalidateLearningAnalysis(userId: string) {
  analysisCache.delete(`analysis:${userId}`);
}

export function invalidateLearningStable(userId: string) {
  stableCache.delete(`stable:${userId}`);
}

export function invalidateLearning(userId: string) {
  invalidateLearningAnalysis(userId);
  invalidateLearningStable(userId);
}
