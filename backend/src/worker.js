// Cloudflare Workers entry point. Secrets come from the Worker's variables (env), never from the code.
import { loadConfig } from './config.js';
import { createCore } from './core.js';

let cached = null; // survives between requests while the Worker isolate stays warm

function getCore(env) {
  const sig = `${env.API_FOOTBALL_KEY ?? ''}|${env.APP_TOKEN ?? ''}|${env.PLAN_PRESET ?? ''}`;
  if (!cached || cached.sig !== sig) {
    cached = { sig, core: createCore(loadConfig(env)) };
  }
  return cached.core;
}

export default {
  async fetch(request, env) {
    const u = new URL(request.url);
    const r = await getCore(env).handle({
      method: request.method,
      url: u.pathname + u.search,
      ip: request.headers.get('cf-connecting-ip') ?? 'unknown',
      header: (name) => request.headers.get(name),
    });
    const headers = { 'content-type': 'application/json; charset=utf-8', 'x-content-type-options': 'nosniff', ...r.headers };
    return new Response(request.method === 'HEAD' ? null : JSON.stringify(r.payload), { status: r.status, headers });
  },
};
