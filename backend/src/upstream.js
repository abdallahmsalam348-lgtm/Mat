import { ApiError } from './errors.js';

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
const utcDay = (t) => new Date(t).toISOString().slice(0, 10);

function msUntilUtcMidnight(t) {
  const d = new Date(t);
  const next = Date.UTC(d.getUTCFullYear(), d.getUTCMonth(), d.getUTCDate() + 1);
  return next - t;
}

/**
 * Budgeted, rate-limited access to the sports API. Every upstream call in the backend goes through here:
 *  - the key is added here and never leaves this module
 *  - a daily budget (below the provider quota) and a per-minute window protect the plan
 *  - provider "errors" objects (HTTP 200 with errors) are translated into proper HTTP errors
 */
export function createUpstream(config, { fetchImpl = globalThis.fetch, now = () => Date.now() } = {}) {
  const state = { day: utcDay(now()), count: 0, remaining: null, limit: null, windowHits: [] };

  function rollover() {
    const d = utcDay(now());
    if (d !== state.day) {
      state.day = d;
      state.count = 0;
      state.remaining = null;
    }
  }

  async function acquireMinuteSlot() {
    const started = now();
    for (;;) {
      const t = now();
      state.windowHits = state.windowHits.filter((x) => t - x < 60000);
      if (state.windowHits.length < config.upstreamPerMinute) {
        state.windowHits.push(t);
        return;
      }
      const wait = state.windowHits[0] + 60000 - t;
      if (t - started + wait > config.maxQueueWaitMs) {
        throw new ApiError(429, 'rate_limited', 'Provider per-minute limit reached, retry shortly.', {
          retryAfter: Math.ceil(wait / 1000),
        });
      }
      await sleep(Math.min(wait + 5, 1000));
    }
  }

  function mapProviderErrors(errors) {
    const list = Array.isArray(errors) ? errors.map((e) => ['error', String(e)]) : Object.entries(errors || {});
    if (!list.length) return null;
    const text = list.map(([k, v]) => `${k}: ${v}`).join('; ');
    const keys = list.map(([k]) => k.toLowerCase());
    const lower = text.toLowerCase();
    if (keys.includes('plan') || lower.includes('free plans do not have access')) {
      return new ApiError(403, 'plan_restricted', text);
    }
    if (keys.includes('ratelimit') || keys.includes('requests') || lower.includes('too many requests') || lower.includes('request limit')) {
      return new ApiError(429, 'rate_limited', text, { retryAfter: 60 });
    }
    if (keys.includes('token') || keys.includes('key') || keys.includes('access') || lower.includes('application key') || lower.includes('suspended')) {
      return new ApiError(502, 'upstream_auth', 'The provider rejected the backend API key or account.');
    }
    return new ApiError(400, 'bad_request', text);
  }

  async function get(path, params = {}) {
    if (!config.apiKey) {
      throw new ApiError(503, 'provider_not_configured', 'No sports API key is configured on the backend.');
    }
    rollover();
    const t = now();
    if (state.count >= config.dailyBudget || (state.remaining !== null && state.remaining <= 0)) {
      throw new ApiError(429, 'rate_limited', 'Daily provider request budget reached.', {
        retryAfter: Math.ceil(msUntilUtcMidnight(t) / 1000),
      });
    }
    await acquireMinuteSlot();

    const url = new URL(path, config.apiBase.endsWith('/') ? config.apiBase : config.apiBase + '/');
    for (const [k, v] of Object.entries(params)) {
      if (v !== undefined && v !== null && String(v) !== '') url.searchParams.set(k, String(v));
    }
    state.count += 1;

    let res;
    try {
      res = await fetchImpl(url, {
        headers: { 'x-apisports-key': config.apiKey, accept: 'application/json' },
        signal: AbortSignal.timeout(config.upstreamTimeoutMs),
      });
    } catch (e) {
      const timeout = e && (e.name === 'TimeoutError' || e.name === 'AbortError');
      throw new ApiError(timeout ? 504 : 502, timeout ? 'upstream_timeout' : 'upstream_unreachable', 'Could not reach the sports data provider.');
    }

    const remaining = Number(res.headers.get('x-ratelimit-requests-remaining'));
    const limit = Number(res.headers.get('x-ratelimit-requests-limit'));
    if (Number.isFinite(remaining) && res.headers.get('x-ratelimit-requests-remaining') !== null) state.remaining = remaining;
    if (Number.isFinite(limit) && res.headers.get('x-ratelimit-requests-limit') !== null) state.limit = limit;

    if (res.status === 429) {
      throw new ApiError(429, 'rate_limited', 'The provider rate limit was reached.', { retryAfter: 60 });
    }
    if (res.status === 401 || res.status === 403) {
      throw new ApiError(502, 'upstream_auth', 'The provider rejected the backend API key.');
    }
    if (!res.ok) {
      throw new ApiError(502, 'upstream_error', `The provider answered HTTP ${res.status}.`);
    }
    let body;
    try {
      body = await res.json();
    } catch {
      throw new ApiError(502, 'upstream_error', 'The provider returned invalid JSON.');
    }
    const err = mapProviderErrors(body && body.errors);
    if (err) throw err;
    return Array.isArray(body.response) ? body.response : [];
  }

  return {
    get,
    stats() {
      rollover();
      return { day: state.day, usedToday: state.count, budget: config.dailyBudget, providerRemaining: state.remaining, providerLimit: state.limit };
    },
  };
}
