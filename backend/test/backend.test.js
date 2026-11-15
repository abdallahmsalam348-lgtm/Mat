import test from 'node:test';
import assert from 'node:assert/strict';
import { createServer } from 'node:http';
import { loadConfig } from '../src/config.js';
import { createApp } from '../src/app.js';
import { normalizeFixture, normalizeEvent, normalizeStandings } from '../src/providers/apiFootball.js';

// ---- TEST FIXTURES (shaped like API-Football responses). Used only by tests, never served by the backend.
const fixtureItem = (id, short, extra = {}) => ({
  fixture: { id, date: '2026-11-13T21:00:00+02:00', timestamp: Date.parse('2026-11-13T21:00:00+02:00') / 1000, venue: { name: 'Test Ground' }, status: { short, elapsed: short === '2H' ? 67 : null, extra: null } },
  league: { id: 39, name: 'Test League', country: 'Testland', logo: 'l.png', flag: null, season: 2026, round: 'Round 1' },
  teams: { home: { id: 1, name: 'Alpha', logo: 'a.png' }, away: { id: 2, name: 'Beta', logo: 'b.png' } },
  goals: { home: 1, away: 1 },
  score: { halftime: { home: 0, away: 0 }, fulltime: { home: 1, away: 1 }, extratime: { home: null, away: null }, penalty: { home: 4, away: 3 } },
  ...extra,
});

async function startMock(handler) {
  const calls = [];
  const server = createServer((req, res) => {
    calls.push({ url: req.url, key: req.headers['x-apisports-key'] });
    const { status = 200, body, headers = {} } = handler(req, calls.length);
    res.writeHead(status, { 'content-type': 'application/json', ...headers });
    res.end(JSON.stringify(body));
  });
  await new Promise((r) => server.listen(0, '127.0.0.1', r));
  return { calls, base: `http://127.0.0.1:${server.address().port}`, close: () => new Promise((r) => server.close(r)) };
}

async function startApp(envOverrides, mock) {
  const config = loadConfig({ API_FOOTBALL_KEY: 'test-key', API_FOOTBALL_BASE: mock?.base ?? 'http://127.0.0.1:1', PLAN_PRESET: 'paid', ...envOverrides });
  const app = createApp(config);
  await new Promise((r) => app.listen(0, '127.0.0.1', r));
  const base = `http://127.0.0.1:${app.address().port}`;
  return { base, close: () => new Promise((r) => app.close(r)) };
}

const today = () => new Date().toLocaleDateString('en-CA', { timeZone: 'UTC' });

test('normalizes a fixture including penalty shoot-out and UTC kickoff', () => {
  const m = normalizeFixture(fixtureItem(10, 'PEN'));
  assert.equal(m.id, 10);
  assert.equal(m.kickoff, '2026-11-13T19:00:00.000Z');
  assert.equal(m.status.code, 'PEN');
  assert.deepEqual(m.goals, { home: 1, away: 1 });
  assert.deepEqual(m.score.penalty, { home: 4, away: 3 });
  assert.equal(m.home.name, 'Alpha');
  assert.equal(m.venue, 'Test Ground');
});

test('missing fields never throw', () => {
  const m = normalizeFixture({});
  assert.equal(m.status.code, 'NS');
  assert.equal(m.goals.home, null);
  assert.equal(m.kickoff, '');
});

test('substitution events: player = coming on, assist = going off', () => {
  const e = normalizeEvent({ type: 'subst', time: { elapsed: 60 }, team: { id: 1 }, player: { name: 'Out Guy' }, assist: { name: 'In Guy' }, detail: 'Substitution 1' });
  assert.equal(e.player, 'In Guy');
  assert.equal(e.assist, 'Out Guy');
  assert.equal(normalizeEvent({ type: 'Goal', player: { name: 'S' }, assist: { name: 'A' } }).assist, 'A');
});

test('standings are flattened into groups', () => {
  const g = normalizeStandings([{ league: { standings: [[{ rank: 1, team: { id: 1, name: 'A' }, points: 9, goalsDiff: 4, group: 'Group A', all: { played: 3, win: 3, draw: 0, lose: 0, goals: { for: 6, against: 2 } } }]] } }]);
  assert.equal(g[0].name, 'Group A');
  assert.equal(g[0].rows[0].goalsFor, 6);
  assert.equal(g[0].rows[0].points, 9);
});

test('not configured -> 503 provider_not_configured, health still works', async () => {
  const app = await startApp({ API_FOOTBALL_KEY: '' });
  try {
    const health = await (await fetch(`${app.base}/v1/health`)).json();
    assert.equal(health.ok, true);
    assert.equal(health.configured, false);
    const r = await fetch(`${app.base}/v1/fixtures?date=${today()}&tz=UTC`);
    assert.equal(r.status, 503);
    assert.equal((await r.json()).error, 'provider_not_configured');
  } finally { await app.close(); }
});

test('fetches, normalizes, sends the key upstream, and caches the second call', async () => {
  const mock = await startMock(() => ({ body: { errors: [], response: [fixtureItem(1, 'NS'), fixtureItem(2, '2H')] }, headers: { 'x-ratelimit-requests-remaining': '99', 'x-ratelimit-requests-limit': '100' } }));
  const app = await startApp({}, mock);
  try {
    const url = `${app.base}/v1/fixtures?date=${today()}&tz=UTC`;
    const a = await (await fetch(url)).json();
    assert.equal(a.data.length, 2);
    assert.equal(a.meta.cached, false);
    assert.equal(a.meta.quotaRemaining, 99);
    const b = await (await fetch(url)).json();
    assert.equal(b.meta.cached, true);
    assert.equal(mock.calls.length, 1, 'second request must be served from cache');
    assert.equal(mock.calls[0].key, 'test-key');
    assert.match(mock.calls[0].url, /fixtures\?date=\d{4}-\d{2}-\d{2}&timezone=UTC/);
  } finally { await app.close(); await mock.close(); }
});

test('concurrent identical requests share one upstream call', async () => {
  const mock = await startMock(() => ({ body: { errors: [], response: [fixtureItem(1, 'NS')] } }));
  const app = await startApp({}, mock);
  try {
    const url = `${app.base}/v1/fixtures?date=${today()}&tz=UTC`;
    await Promise.all([fetch(url), fetch(url), fetch(url)].map(async (p) => (await p).json()));
    assert.equal(mock.calls.length, 1);
  } finally { await app.close(); await mock.close(); }
});

test('provider plan error -> 403 plan_restricted', async () => {
  const mock = await startMock(() => ({ body: { errors: { plan: 'Free plans do not have access to this season, try from 2022 to 2024.' }, response: [] } }));
  const app = await startApp({}, mock);
  try {
    const r = await fetch(`${app.base}/v1/competitions/39/standings?season=2026`);
    assert.equal(r.status, 403);
    assert.equal((await r.json()).error, 'plan_restricted');
  } finally { await app.close(); await mock.close(); }
});

test('provider rate limit -> 429 with retry-after', async () => {
  const mock = await startMock(() => ({ body: { errors: { rateLimit: 'Too many requests. Your rate limit is 10 requests per minute.' }, response: [] } }));
  const app = await startApp({}, mock);
  try {
    const r = await fetch(`${app.base}/v1/live`);
    assert.equal(r.status, 429);
    assert.ok(r.headers.get('retry-after'));
    assert.equal((await r.json()).error, 'rate_limited');
  } finally { await app.close(); await mock.close(); }
});

test('daily budget stops upstream calls', async () => {
  const mock = await startMock(() => ({ body: { errors: [], response: [] } }));
  const app = await startApp({ DAILY_REQUEST_BUDGET: '2' }, mock);
  try {
    assert.equal((await fetch(`${app.base}/v1/competitions/1/standings?season=2025`)).status, 200);
    assert.equal((await fetch(`${app.base}/v1/competitions/2/standings?season=2025`)).status, 200);
    const third = await fetch(`${app.base}/v1/competitions/3/standings?season=2025`);
    assert.equal(third.status, 429);
    assert.equal(mock.calls.length, 2);
  } finally { await app.close(); await mock.close(); }
});

test('serves stale data when the provider fails after the entry expired', async () => {
  let fail = false;
  const mock = await startMock(() => (fail ? { status: 500, body: {} } : { body: { errors: [], response: [fixtureItem(1, 'NS')] } }));
  const app = await startApp({ TTL_TODAY_SECONDS: '0' }, mock);
  try {
    const url = `${app.base}/v1/fixtures?date=${today()}&tz=UTC`;
    assert.equal((await (await fetch(url)).json()).meta.stale, false);
    fail = true;
    await new Promise((r) => setTimeout(r, 5));
    const r = await fetch(url);
    assert.equal(r.status, 200);
    const body = await r.json();
    assert.equal(body.meta.stale, true);
    assert.equal(body.data.length, 1);
  } finally { await app.close(); await mock.close(); }
});

test('app token is enforced when configured', async () => {
  const mock = await startMock(() => ({ body: { errors: [], response: [] } }));
  const app = await startApp({ APP_TOKEN: 'secret-token' }, mock);
  try {
    assert.equal((await fetch(`${app.base}/v1/live`)).status, 401);
    assert.equal((await fetch(`${app.base}/v1/live`, { headers: { 'x-matchly-token': 'wrong' } })).status, 401);
    assert.equal((await fetch(`${app.base}/v1/live`, { headers: { 'x-matchly-token': 'secret-token' } })).status, 200);
    assert.equal((await fetch(`${app.base}/v1/health`)).status, 200);
  } finally { await app.close(); await mock.close(); }
});

test('input validation and unknown routes', async () => {
  const mock = await startMock(() => ({ body: { errors: [], response: [] } }));
  const app = await startApp({}, mock);
  try {
    assert.equal((await fetch(`${app.base}/v1/fixtures?date=tomorrow`)).status, 400);
    assert.equal((await fetch(`${app.base}/v1/search?q=a`)).status, 400);
    assert.equal((await fetch(`${app.base}/v1/nope`)).status, 404);
    assert.equal((await fetch(`${app.base}/v1/matches/abc`)).status, 404);
    assert.equal((await fetch(`${app.base}/v1/live`, { method: 'POST' })).status, 405);
    const s = await (await fetch(`${app.base}/v1/search?q=ab`)).json();
    assert.deepEqual(s.data, { teams: [], competitions: [] }); // < 3 chars: no upstream call
    assert.equal(mock.calls.length, 0);
  } finally { await app.close(); await mock.close(); }
});
