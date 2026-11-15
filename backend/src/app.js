import { createServer } from 'node:http';
import { gzipSync } from 'node:zlib';
import { createService } from './service.js';
import { createCore } from './core.js';

/** Node HTTP server (used for local runs, Render and the unit tests). */
export function createApp(config, { service = createService(config), now = () => Date.now(), log = () => {} } = {}) {
  const core = createCore(config, { service, now });

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

  return createServer(async (req, res) => {
    const started = now();
    const ip = (req.headers['x-forwarded-for'] ?? '').split(',')[0].trim() || req.socket.remoteAddress || 'unknown';
    const r = await core.handle({
      method: req.method,
      url: req.url,
      ip,
      header: (name) => req.headers[name.toLowerCase()],
    });
    send(req, res, r.status, r.payload, r.headers);
    log(`${req.method} ${req.url?.split('?')[0]} ${r.status} ${now() - started}ms ${r.note}`.trim());
  });
}
