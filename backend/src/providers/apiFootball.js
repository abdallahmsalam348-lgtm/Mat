import { ApiError } from '../errors.js';

/**
 * API-Football (api-sports.io) v3 provider. Converts provider JSON into the provider-neutral
 * "MATCHLY API v1" shapes the Android app understands. To add another provider (Sportmonks,
 * football-data.org ...), create a module with the same method names and register it in providers/index.js.
 */

const num = (v) => (v === null || v === undefined || v === '' || Number.isNaN(Number(v)) ? null : Number(v));
const str = (v) => (v === null || v === undefined ? null : String(v));

function toIso(date, timestamp) {
  const ts = num(timestamp);
  if (ts !== null) return new Date(ts * 1000).toISOString();
  const t = Date.parse(date || '');
  return Number.isNaN(t) ? '' : new Date(t).toISOString();
}

const team = (t) => ({ id: num(t?.id) ?? 0, name: t?.name ?? '', logo: t?.logo ?? null });
const pair = (p) => ({ home: num(p?.home), away: num(p?.away) });

export function normalizeFixture(item) {
  const f = item?.fixture ?? {};
  const l = item?.league ?? {};
  return {
    id: num(f.id) ?? 0,
    kickoff: toIso(f.date, f.timestamp),
    status: { code: f.status?.short ?? 'NS', elapsed: num(f.status?.elapsed), extra: num(f.status?.extra) },
    competition: {
      id: num(l.id) ?? 0,
      name: l.name ?? '',
      country: l.country ?? null,
      logo: l.logo ?? null,
      flag: l.flag ?? null,
      round: l.round ?? null,
      season: num(l.season),
    },
    home: team(item?.teams?.home),
    away: team(item?.teams?.away),
    goals: pair(item?.goals),
    score: {
      halftime: pair(item?.score?.halftime),
      fulltime: pair(item?.score?.fulltime),
      extratime: pair(item?.score?.extratime),
      penalty: pair(item?.score?.penalty),
    },
    venue: f.venue?.name ?? null,
  };
}

/** For substitutions MATCHLY uses: player = player coming ON, assist = player going OFF. */
export function normalizeEvent(e) {
  const type = e?.type ?? '';
  let player = e?.player?.name ?? null;
  let assist = e?.assist?.name ?? null;
  if (type.toLowerCase() === 'subst') {
    // API-Football: player = replaced player, assist = substitute entering.
    const out = player;
    player = assist;
    assist = out;
  }
  return {
    minute: num(e?.time?.elapsed),
    extra: num(e?.time?.extra),
    teamId: num(e?.team?.id),
    player,
    assist,
    type,
    detail: e?.detail ?? null,
    comments: e?.comments ?? null,
  };
}

const lineupPlayer = (p) => ({
  id: num(p?.player?.id),
  name: p?.player?.name ?? '',
  number: num(p?.player?.number),
  pos: p?.player?.pos ?? null,
  grid: p?.player?.grid ?? null,
});

export function normalizeLineup(l) {
  return {
    team: team(l?.team),
    formation: l?.formation ?? null,
    coach: l?.coach?.name ?? null,
    startXI: (l?.startXI ?? []).map(lineupPlayer),
    substitutes: (l?.substitutes ?? []).map(lineupPlayer),
  };
}

export function normalizeStatistics(s) {
  return {
    team: team(s?.team),
    stats: (s?.statistics ?? []).map((x) => ({ type: x?.type ?? '', value: str(x?.value) })),
  };
}

export function normalizeMatchDetail(item) {
  return {
    match: normalizeFixture(item),
    events: (item?.events ?? []).map(normalizeEvent),
    lineups: (item?.lineups ?? []).map(normalizeLineup),
    statistics: (item?.statistics ?? []).map(normalizeStatistics),
  };
}

export function normalizeStandings(response) {
  const league = response?.[0]?.league;
  const groups = league?.standings ?? [];
  return groups.map((rows) => ({
    name: rows?.[0]?.group ?? null,
    rows: (rows ?? []).map((r) => ({
      rank: num(r.rank) ?? 0,
      team: team(r.team),
      points: num(r.points) ?? 0,
      played: num(r.all?.played) ?? 0,
      win: num(r.all?.win) ?? 0,
      draw: num(r.all?.draw) ?? 0,
      lose: num(r.all?.lose) ?? 0,
      goalsFor: num(r.all?.goals?.for) ?? 0,
      goalsAgainst: num(r.all?.goals?.against) ?? 0,
      goalsDiff: num(r.goalsDiff) ?? 0,
      form: r.form ?? null,
      description: r.description ?? null,
    })),
  }));
}

export function normalizeCompetition(item) {
  const seasons = item?.seasons ?? [];
  const current = seasons.find((s) => s.current) ?? seasons[seasons.length - 1];
  return {
    id: num(item?.league?.id) ?? 0,
    name: item?.league?.name ?? '',
    type: item?.league?.type ?? null,
    logo: item?.league?.logo ?? null,
    country: item?.country?.name ?? null,
    countryCode: item?.country?.code ?? null,
    flag: item?.country?.flag ?? null,
    season: num(current?.year),
  };
}

const teamSummary = (item) => ({
  id: num(item?.team?.id) ?? 0,
  name: item?.team?.name ?? '',
  logo: item?.team?.logo ?? null,
  country: item?.team?.country ?? null,
  national: Boolean(item?.team?.national),
  founded: num(item?.team?.founded),
  venue: item?.venue?.name ?? null,
  city: item?.venue?.city ?? null,
});

export function normalizePlayer(item) {
  const p = item?.player ?? {};
  const stats = item?.statistics ?? [];
  return {
    id: num(p.id) ?? 0,
    name: p.name ?? '',
    age: num(p.age),
    nationality: p.nationality ?? null,
    height: p.height ?? null,
    weight: p.weight ?? null,
    photo: p.photo ?? null,
    injured: typeof p.injured === 'boolean' ? p.injured : null,
    team: stats[0]?.team ? team(stats[0].team) : null,
    stats: stats.map((s) => ({
      competition: { id: num(s.league?.id) ?? 0, name: s.league?.name ?? '', country: s.league?.country ?? null, logo: s.league?.logo ?? null, flag: s.league?.flag ?? null, season: num(s.league?.season) },
      team: team(s.team),
      position: s.games?.position ?? null,
      appearances: num(s.games?.appearences ?? s.games?.appearances),
      minutes: num(s.games?.minutes),
      goals: num(s.goals?.total),
      assists: num(s.goals?.assists),
      yellow: num(s.cards?.yellow),
      red: num(s.cards?.red),
      rating: str(s.games?.rating),
    })),
  };
}

export function createApiFootballProvider(upstream) {
  const get = upstream.get;
  return {
    name: 'api-football',

    async fixturesByDate(date, tz) {
      return (await get('fixtures', { date, timezone: tz })).map(normalizeFixture);
    },
    async live() {
      return (await get('fixtures', { live: 'all' })).map(normalizeFixture);
    },
    async matchDetail(id) {
      const r = await get('fixtures', { id });
      if (!r.length) throw new ApiError(404, 'not_found', 'Match not found.');
      return normalizeMatchDetail(r[0]);
    },
    async injuries(matchId) {
      return (await get('injuries', { fixture: matchId })).map((i) => ({
        playerId: num(i.player?.id),
        player: i.player?.name ?? '',
        photo: i.player?.photo ?? null,
        team: team(i.team),
        type: i.player?.type ?? null,
        reason: i.player?.reason ?? null,
      }));
    },
    async headToHead(home, away) {
      return (await get('fixtures/headtohead', { h2h: `${home}-${away}`, last: 10 })).map(normalizeFixture);
    },
    async competitions() {
      return (await get('leagues', { current: 'true' })).map(normalizeCompetition);
    },
    async competitionFixtures(id, season) {
      return (await get('fixtures', { league: id, season })).map(normalizeFixture);
    },
    async standings(id, season) {
      return normalizeStandings(await get('standings', { league: id, season }));
    },
    async topScorers(id, season) {
      return (await get('players/topscorers', { league: id, season })).map((i) => {
        const s = i.statistics?.[0] ?? {};
        return {
          player: { id: num(i.player?.id) ?? 0, name: i.player?.name ?? '', photo: i.player?.photo ?? null },
          team: team(s.team),
          goals: num(s.goals?.total),
          assists: num(s.goals?.assists),
          appearances: num(s.games?.appearences ?? s.games?.appearances),
        };
      });
    },
    async search(q) {
      if (q.trim().length < 3) return { teams: [], competitions: [] };
      const [teams, leagues] = await Promise.all([get('teams', { search: q }), get('leagues', { search: q })]);
      return { teams: teams.map(teamSummary), competitions: leagues.map(normalizeCompetition) };
    },
    async team(id) {
      const [info, squads] = await Promise.all([get('teams', { id }), get('players/squads', { team: id })]);
      if (!info.length) throw new ApiError(404, 'not_found', 'Team not found.');
      return {
        team: teamSummary(info[0]),
        squad: (squads[0]?.players ?? []).map((p) => ({
          id: num(p.id) ?? 0, name: p.name ?? '', age: num(p.age), number: num(p.number), position: p.position ?? null, photo: p.photo ?? null,
        })),
      };
    },
    async teamFixtures(id) {
      const [last, next] = await Promise.all([get('fixtures', { team: id, last: 5 }), get('fixtures', { team: id, next: 5 })]);
      const seen = new Set();
      return [...last, ...next].map(normalizeFixture).filter((m) => (seen.has(m.id) ? false : seen.add(m.id)));
    },
    async player(id, season) {
      const r = await get('players', { id, season });
      if (!r.length) throw new ApiError(404, 'not_found', 'Player not found.');
      return normalizePlayer(r[0]);
    },
  };
}
