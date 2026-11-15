import test from 'node:test';
import assert from 'node:assert/strict';
import { createServer } from 'node:http';
import worker from '../src/worker.js';

async function mockProvider(handler) {
  const server = createServer((req, res) => {
    const { status = 200, body } = handler(req);
    res.writeHead(status, { 'content-type': 'application/json' });
    res.end(JSON.stringify(body));
  });
  await new Promise((r) => server.listen(0, '127.0.0.1', r));
  return { base: `http://127.0.0.1:${server.address().port}`, close: () => new Promise((r) => server.close(r)) };
}

const req = (path, headers = {}) => new Request(`https://matchly-api.example.workers.dev${path}`, { headers });

test('worker: health is open and reports configuration', async () => {
  const res = await worker.fetch(req('/v1/health'), { API_FOOTBALL_KEY: 'k', APP_TOKEN: 'secret' });
  assert.equal(res.status, 200);
  const body = await res.json();
  assert.equal(body.ok, true);
  assert.equal(body.configured, true);
});

test('worker: unconfigured key gives provider_not_configured', async () => {
  const res = await worker.fetch(req('/v1/live'), {});
  assert.equal(res.status, 503);
  assert.equal((await res.json()).error, 'provider_not_configured');
});

test('worker: app token is enforced', async () => {
  const env = { API_FOOTBALL_KEY: 'k', APP_TOKEN: 'secret-token-1', API_FOOTBALL_BASE: 'http://127.0.0.1:1' };
  const noToken = await worker.fetch(req('/v1/live'), env);
  assert.equal(noToken.status, 401);
  const wrong = await worker.fetch(req('/v1/live', { 'x-matchly-token': 'nope' }), env);
  assert.equal(wrong.status, 401);
});

test('worker: proxies and caches provider data without exposing the key', async () => {
  let hits = 0;
  const mock = await mockProvider(() => {
    hits += 1;
    return { body: { errors: [], response: [] } };
  });
  try {
    const env = { API_FOOTBALL_KEY: 'worker-test-key', APP_TOKEN: 'tok-123456', API_FOOTBALL_BASE: mock.base, PLAN_PRESET: 'paid' };
    const h = { 'x-matchly-token': 'tok-123456' };
    const a = await worker.fetch(req('/v1/live', h), env);
    assert.equal(a.status, 200);
    const text = await a.text();
    assert.ok(!text.includes('worker-test-key'));
    const b = await worker.fetch(req('/v1/live', h), env);
    assert.equal((await b.json()).meta.cached, true);
    assert.equal(hits, 1);
  } finally { await mock.close(); }
});

test('worker: rejects non-GET and unknown routes', async () => {
  const env = { API_FOOTBALL_KEY: 'k' };
  const post = await worker.fetch(new Request('https://x.workers.dev/v1/live', { method: 'POST' }), env);
  assert.equal(post.status, 405);
  const nf = await worker.fetch(req('/v1/nothing'), env);
  assert.equal(nf.status, 404);
});
