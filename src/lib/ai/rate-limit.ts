const WINDOW_MS = 60_000;
const MAX_CALLS = 10;
const CLEANUP_INTERVAL_MS = 5 * 60_000;

const calls = new Map<string, number[]>();
let lastCleanup = 0;

function cleanup(now: number) {
  if (now - lastCleanup < CLEANUP_INTERVAL_MS) return;
  lastCleanup = now;
  const cutoff = now - WINDOW_MS;
  for (const [userId, timestamps] of calls) {
    const fresh = timestamps.filter((t) => t > cutoff);
    if (fresh.length === 0) calls.delete(userId);
    else if (fresh.length !== timestamps.length) calls.set(userId, fresh);
  }
}

export function checkAIRateLimit(userId: string) {
  const now = Date.now();
  cleanup(now);
  const cutoff = now - WINDOW_MS;
  const recent = (calls.get(userId) ?? []).filter((time) => time > cutoff);
  if (recent.length >= MAX_CALLS) {
    calls.set(userId, recent);
    return false;
  }
  recent.push(now);
  calls.set(userId, recent);
  return true;
}
