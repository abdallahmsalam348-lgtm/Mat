import { existsSync, readFileSync } from 'node:fs';

/** Tiny .env reader for local development. Never overrides variables that are already set. */
export function loadDotEnv(path, env = process.env) {
  if (!existsSync(path)) return;
  for (const rawLine of readFileSync(path, 'utf8').split(/\r?\n/)) {
    const line = rawLine.trim();
    if (!line || line.startsWith('#')) continue;
    const eq = line.indexOf('=');
    if (eq < 1) continue;
    const key = line.slice(0, eq).trim();
    let value = line.slice(eq + 1).trim();
    if ((value.startsWith('"') && value.endsWith('"')) || (value.startsWith("'") && value.endsWith("'"))) {
      value = value.slice(1, -1);
    }
    if (env[key] === undefined) env[key] = value;
  }
}

const num = (v, fallback) => {
  if (v === undefined || v === null || String(v).trim() === '') return fallback;
  const n = Number(v);
  return Number.isFinite(n) && n >= 0 ? n : fallback;
};

const PRESETS = {
  // 100 requests/day: stretch them. ~15 minute refresh for today's list.
  free: { dailyBudget: 90, upstreamPerMinute: 10, ttlToday: 900, ttlLive: 600 },
  // Pro (7,500/day) and above.
  paid: { dailyBudget: 7000, upstreamPerMinute: 250, ttlToday: 60, ttlLive: 30 },
};

export function loadConfig(env = process.env) {
  const presetName = (env.PLAN_PRESET || 'free').toLowerCase() === 'paid' ? 'paid' : 'free';
  const p = PRESETS[presetName];
  return {
    port: num(env.PORT, 8787),
    provider: (env.PROVIDER || 'api-football').toLowerCase(),
    apiKey: (env.API_FOOTBALL_KEY || '').trim(),
    apiBase: (env.API_FOOTBALL_BASE || 'https://v3.football.api-sports.io').trim(),
    appToken: (env.APP_TOKEN || '').trim(),
    plan: presetName,
    dailyBudget: num(env.DAILY_REQUEST_BUDGET, p.dailyBudget),
    upstreamPerMinute: Math.max(1, num(env.UPSTREAM_PER_MINUTE, p.upstreamPerMinute)),
    ttlTodayMs: num(env.TTL_TODAY_SECONDS, p.ttlToday) * 1000,
    ttlLiveMs: num(env.TTL_LIVE_SECONDS, p.ttlLive) * 1000,
    clientPerMinute: Math.max(1, num(env.CLIENT_RATE_PER_MINUTE, 120)),
    upstreamTimeoutMs: num(env.UPSTREAM_TIMEOUT_MS, 15000),
    maxStaleMs: num(env.MAX_STALE_SECONDS, 24 * 3600) * 1000,
    maxQueueWaitMs: num(env.MAX_QUEUE_WAIT_MS, 8000),
  };
}
