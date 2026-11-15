# MATCHLY backend

A small Node.js (>= 18.18, **no npm dependencies**) proxy between the app and the sports API.

* Keeps the API key on the server (environment variable only; never sent to the app, never logged).
* Caches responses (TTLs in `src/service.js`), shares one upstream call between simultaneous identical requests, and serves **stale** data when the provider fails.
* Protects your quota: daily budget (`DAILY_REQUEST_BUDGET`), per-minute window (`UPSTREAM_PER_MINUTE`), per-client limit, optional `x-matchly-token`.
* Translates provider errors into stable codes the app understands: `provider_not_configured` (503), `plan_restricted` (403), `rate_limited` (429 + `Retry-After`), `unauthorized` (401), `not_found` (404), `bad_request` (400).
* Never invents data: with no key it answers 503, and the app falls back to its clearly-labelled DEMO mode.

## Run locally
```
cd backend
cp .env.example .env     # then edit .env and add your key
npm start                # http://localhost:8787/v1/health
npm test                 # 13 tests against a mock provider
```

## Configuration (environment variables)
| Variable | Default | Meaning |
|---|---|---|
| `API_FOOTBALL_KEY` | (none) | **Secret.** Your API-Football key |
| `PLAN_PRESET` | `free` | `free`: budget 90/day, today's list cached 15 min, live data 10 min. `paid`: budget 7000/day, 60 s / 30 s |
| `APP_TOKEN` | (none) | If set, requests must send header `x-matchly-token` |
| `DAILY_REQUEST_BUDGET`, `UPSTREAM_PER_MINUTE`, `TTL_TODAY_SECONDS`, `TTL_LIVE_SECONDS` | preset | Override the preset |
| `CLIENT_RATE_PER_MINUTE` | 120 | Per-IP limit |
| `PORT` | 8787 | Render sets this itself |

## Endpoints (all GET, JSON)
`/v1/health`, `/v1/fixtures?date=YYYY-MM-DD&tz=Area/City`, `/v1/live`, `/v1/matches/:id`, `/v1/matches/:id/injuries`, `/v1/h2h?home=&away=`, `/v1/competitions`, `/v1/competitions/:id/{fixtures|standings|scorers}?season=`, `/v1/search?q=`, `/v1/teams/:id`, `/v1/teams/:id/fixtures`, `/v1/players/:id?season=`.

## Deploy
Render: see `docs/SETUP_EN.md` part B (or use `render.yaml` as a Blueprint). Any Node host works (Railway, Fly.io, a VPS).

## Known limits
* The cache and request counter live in memory: a restart/cold start resets them (Phase 1 in the roadmap: durable store).
* Written against API-Football's documented format; **not yet run against the live service**. Assumptions to verify with a real key: substitution `player`/`assist` order (the backend swaps them so `player` = player coming on), `goals` for penalty-decided matches, `injuries` and `next/last` availability on your plan.
