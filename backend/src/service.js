import { TtlCache } from './cache.js';
import { ApiError } from './errors.js';
import { createUpstream } from './upstream.js';
import { createProvider } from './providers/index.js';

const MIN = 60 * 1000;
const HOUR = 60 * MIN;

const isLive = (code) => ['1H', 'HT', '2H', 'ET', 'BT', 'P', 'LIVE'].includes(code);
const isFinished = (code) => ['FT', 'AET', 'PEN', 'AWD', 'WO'].includes(code);

export function validTimeZone(tz) {
  try {
    new Intl.DateTimeFormat('en-US', { timeZone: tz });
    return true;
  } catch {
    return false;
  }
}

function todayIn(tz, now) {
  return new Date(now).toLocaleDateString('en-CA', { timeZone: tz }); // YYYY-MM-DD
}

/** Cache + de-duplication + stale-if-error around a provider. */
export function createService(config, { provider, upstream, now = () => Date.now() } = {}) {
  const up = upstream ?? createUpstream(config, { now });
  const prov = provider ?? createProvider(config.provider, up);
  const cache = new TtlCache({ maxStaleMs: config.maxStaleMs, now });
  const inflight = new Map();

  async function cached(key, ttl, loader) {
    const hit = cache.get(key);
    const meta = (extra) => ({ provider: prov.name, fetchedAt: null, quotaRemaining: up.stats().providerRemaining, ...extra });
    if (hit && hit.fresh) {
      return { data: hit.value, meta: meta({ cached: true, stale: false, fetchedAt: new Date(hit.storedAt).toISOString() }) };
    }
    if (inflight.has(key)) return inflight.get(key);
    const p = (async () => {
      try {
        const data = await loader();
        const ttlMs = typeof ttl === 'function' ? ttl(data) : ttl;
        cache.set(key, data, ttlMs);
        return { data, meta: meta({ cached: false, stale: false, fetchedAt: new Date(now()).toISOString() }) };
      } catch (e) {
        const serveStale = hit && e instanceof ApiError && !['not_found', 'bad_request', 'plan_restricted', 'provider_not_configured'].includes(e.code);
        if (serveStale) {
          return { data: hit.value, meta: meta({ cached: true, stale: true, fetchedAt: new Date(hit.storedAt).toISOString() }) };
        }
        throw e;
      } finally {
        inflight.delete(key);
      }
    })();
    inflight.set(key, p);
    return p;
  }

  const ttlFor = (data) => (Array.isArray(data) && data.some((m) => isLive(m.status?.code)) ? config.ttlLiveMs : config.ttlTodayMs);

  return {
    cache,
    stats: () => up.stats(),
    providerName: prov.name,

    fixtures(date, tz) {
      const zone = validTimeZone(tz) ? tz : 'UTC';
      const today = todayIn(zone, now());
      const ttl = (data) => {
        if (date === today) return ttlFor(data);
        if (date > today) return 30 * MIN;
        return HOUR;
      };
      return cached(`fixtures:${zone}:${date}`, ttl, () => prov.fixturesByDate(date, zone));
    },
    live() {
      return cached('live', config.ttlLiveMs, () => prov.live());
    },
    matchDetail(id) {
      const ttl = (d) => {
        const code = d.match?.status?.code;
        if (isLive(code)) return config.ttlLiveMs;
        if (isFinished(code)) return 6 * HOUR;
        return 10 * MIN;
      };
      return cached(`match:${id}`, ttl, () => prov.matchDetail(id));
    },
    injuries: (id) => cached(`injuries:${id}`, HOUR, () => prov.injuries(id)),
    headToHead: (h, a) => cached(`h2h:${h}:${a}`, 6 * HOUR, () => prov.headToHead(h, a)),
    competitions: () => cached('competitions', 12 * HOUR, () => prov.competitions()),
    competitionFixtures: (id, s) => cached(`compfix:${id}:${s}`, 15 * MIN, () => prov.competitionFixtures(id, s)),
    standings: (id, s) => cached(`standings:${id}:${s}`, 30 * MIN, () => prov.standings(id, s)),
    topScorers: (id, s) => cached(`scorers:${id}:${s}`, HOUR, () => prov.topScorers(id, s)),
    search: (q) => cached(`search:${q.toLowerCase()}`, 6 * HOUR, () => prov.search(q)),
    team: (id) => cached(`team:${id}`, 12 * HOUR, () => prov.team(id)),
    teamFixtures: (id) => cached(`teamfix:${id}`, 15 * MIN, () => prov.teamFixtures(id)),
    player: (id, s) => cached(`player:${id}:${s}`, 12 * HOUR, () => prov.player(id, s)),
  };
}
