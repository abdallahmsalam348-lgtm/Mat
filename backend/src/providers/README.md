# Adding a provider

A provider is a factory `(upstream) => object` that implements these async methods and returns the
MATCHLY API v1 shapes (see `app/src/main/java/com/matchly/app/data/model/Models.kt`):

`fixturesByDate(date, tz)`, `live()`, `matchDetail(id)`, `injuries(matchId)`, `headToHead(home, away)`,
`competitions()`, `competitionFixtures(id, season)`, `standings(id, season)`, `topScorers(id, season)`,
`search(q)`, `team(id)`, `teamFixtures(id)`, `player(id, season)`.

Status codes must use the API-Football short codes (NS, 1H, HT, 2H, ET, P, FT, AET, PEN, PST, CANC, ABD ...);
map your provider's statuses to them. Register the factory in `providers/index.js`. The Android app does not change.
