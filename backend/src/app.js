import { createServer } from 'node:http';
import { gzipSync } from 'node:zlib';
import { timingSafeEqual } from 'node:crypto';
import { ApiError } from './errors.js';
import { createService } from './service.js';

const DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

function sameToken(a, b) {
  const x = Buffer.from(a);
  const y = Buffer.from(b);
  return x.length === y.length && timingSafeEqual(x, y);
}

function intParam(value, name, { min = 1, fallback } = {}) {
  if ((value === undefined || value === null || value === '') && fallback !== undefined) return fallback;
  const n = Number(value);
  if (!Number.isInteger(n) || n < min) throw new ApiError(400, 'bad_request', `Invalid ${name}.`);
  return n;
}

function currentSeason(now) {
  const d = new Date(now);
  return d.getUTCMonth() >= 6 ? d.getUTCFullYear() : d.getUTCFullYear() - 1;
}

export function createApp(config, { service = createService(config), now = () => Date.now(), log = () => {} } = {}) {
  const hits = new Map(); // ip -> timestamps (sliding one-minute window)

  function clientAllowed(ip) {
    const t = now();
    const list = (hits.get(ip) ?? []).filter((x) => t - x < 60000);
    if (list.length >= config.clientPerMinute) {
      hits.set(ip, list);
      return false;
    }
    list.push(t);
    hits.set(ip, list);
    return true;
  }
  const sweep = setInterval(() => {
    const t = now();
    for (const [ip, list] of hits) if (!list.some((x) => t - x < 60000)) hits.delete(ip);
  }, 5 * 60000);
  sweep.unref();

  async function route(method, url) {
    const path = url.pathname.replace(/\/+$/, '') || '/';
    const q = url.searchParams;
    let m;
    if (path === '/') {
      return { body: { name: 'MATCHLY backend', api: 'v1', docs: 'See backend/README.md' } };
    }
    if (path === '/v1/health') {
      const s = service.stats();
      return {
        body: {
          ok: true,
          configured: Boolean(config.apiKey),
          provider: service.providerName,
          plan: config.plan,
          requestsToday: s.usedToday,
          requestsLimit: s.budget,
        },
      };
    }
    if (path === '/v1/fixtures') {
      const date = q.get('date') ?? '';
      if (!DATE_RE.test(date)) throw new ApiError(400, 'bad_request', 'date must be YYYY-MM-DD.');
      return service.fixtures(date, q.get('tz') || 'UTC');
    }
    if (path === '/v1/live') return service.live();
    if (path === '/v1/competitions') return service.competitions();
    if (path === '/v1/h2h') return service.headToHead(intParam(q.get('home'), 'home'), intParam(q.get('away'), 'away'));
    if (path === '/v1/search') {
      const text = (q.get('q') ?? '').trim().slice(0, 60);
      if (text.length < 2) throw new ApiError(400, 'bad_request', 'q must have at least 2 characters.');
      return service.search(text);
    }
    if ((m = path.match(/^\/v1\/matches\/(\d+)$/))) return service.matchDetail(Number(m[1]));
    if ((m = path.match(/^\/v1\/matches\/(\d+)\/injuries$/))) return service.injuries(Number(m[1]));
    if ((m = path.match(/^\/v1\/competitions\/(\d+)\/(fixtures|standings|scorers)$/))) {
      const id = Number(m[1]);
      const season = intParam(q.get('season'), 'season', { min: 1900, fallback: currentSeason(now()) });
      if (m[2] === 'fixtures') return service.competitionFixtures(id, season);
      if (m[2] === 'standings') return service.standings(id, season);
      return service.topScorers(id, season);
    }
    if ((m = path.match(/^\/v1\/teams\/(\d+)$/))) return service.team(Number(m[1]));
    if ((m = path.match(/^\/v1\/teams\/(\d+)\/fixtures$/))) return service.teamFixtures(Number(m[1]));
    if ((m = path.match(/^\/v1\/players\/(\d+)$/))) {
      return service.player(Number(m[1]), intParam(q.get('season'), 'season', { min: 1900, fallback: currentSeason(now()) }));
    }
    throw new ApiError(404, 'not_found', 'Unknown endpoint.');
  }

  function send(req, res, status, payload, headers = {}) {
    let body = Buffer.from(JSON.stringify(payload));
    const h = { 'content-type': 'application/json; charset=utf-8', 'x-content-type-options': 'nosniff', ...headers };
    if (body.length > 1024 && /\bgzip\b/.test(req.headers['accept-encoding'] ?? '')) {
      body = gzipSync(body);
      h['content-encoding'] = 'gzip';
    }
    h['content-length'] = body.length;
    res.writeHead(status, h);
    res.end(req.method === 'HEAD' ? undefined : body);
  }

  const server = createServer(async (req, res) => {
    const started = now();
    let status = 200;
    let note = '';
    try {
      if (req.method !== 'GET' && req.method !== 'HEAD') throw new ApiError(405, 'method_not_allowed', 'Only GET is supported.');
      const url = new URL(req.url ?? '/', 'http://localhost');
      const ip = (req.headers['x-forwarded-for'] ?? '').split(',')[0].trim() || req.socket.remoteAddress || 'unknown';
      if (!clientAllowed(ip)) {
        throw new ApiError(429, 'rate_limited', 'Too many requests from this client.', { retryAfter: 30 });
      }
      const isOpen = url.pathname === '/' || url.pathname === '/v1/health';
      if (config.appToken && !isOpen) {
        const given = String(req.headers['x-matchly-token'] ?? '');
        if (!sameToken(given, config.appToken)) throw new ApiError(401, 'unauthorized', 'Missing or invalid app token.');
      }
      const result = await route(req.method, url);
      const body = result.data !== undefined && result.meta ? result : result.body ?? result;
      const headers = {};
      if (result.meta) {
        headers['cache-control'] = 'no-store';
        note = result.meta.stale ? 'stale' : result.meta.cached ? 'hit' : 'miss';
      }
      send(req, res, 200, body, headers);
    } catch (e) {
      const err = e instanceof ApiError ? e : new ApiError(500, 'internal_error', 'Unexpected server error.');
      status = err.status;
      const headers = {};
      if (err.extra?.retryAfter) headers['retry-after'] = String(err.extra.retryAfter);
      send(req, res, err.status, { error: err.code, message: err.message }, headers);
      if (!(e instanceof ApiError)) console.error('Unhandled error:', e?.message);
    } finally {
      log(`${req.method} ${req.url?.split('?')[0]} ${status} ${now() - started}ms ${note}`.trim());
    }
  });
  server.on('close', () => clearInterval(sweep));
  return server;
}
