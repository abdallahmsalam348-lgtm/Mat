package com.matchly.app.data.demo

import com.matchly.app.data.FootballDataSource
import com.matchly.app.data.model.Competition
import com.matchly.app.data.model.CompetitionRef
import com.matchly.app.data.model.GoalPair
import com.matchly.app.data.model.Injury
import com.matchly.app.data.model.LineupPlayer
import com.matchly.app.data.model.Match
import com.matchly.app.data.model.MatchDetail
import com.matchly.app.data.model.MatchEvent
import com.matchly.app.data.model.PlayerProfile
import com.matchly.app.data.model.PlayerRef
import com.matchly.app.data.model.PlayerSeasonStat
import com.matchly.app.data.model.ScoreBreakdown
import com.matchly.app.data.model.SearchResult
import com.matchly.app.data.model.SquadPlayer
import com.matchly.app.data.model.StandingGroup
import com.matchly.app.data.model.StandingRow
import com.matchly.app.data.model.StatItem
import com.matchly.app.data.model.StatusInfo
import com.matchly.app.data.model.TeamLineup
import com.matchly.app.data.model.TeamProfile
import com.matchly.app.data.model.TeamRef
import com.matchly.app.data.model.TeamStats
import com.matchly.app.data.model.TeamSummary
import com.matchly.app.data.model.TopScorer
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * DEMO DATA ONLY. All clubs, players and results below are fictional and generated on the fly so the
 * UI can be explored without a sports-API subscription. The UI labels every demo screen as DEMO.
 * This is never real football data.
 */
class DemoDataSource : FootballDataSource {
    override val isDemo: Boolean = true

    private val season = 2026
    private val teams: List<TeamRef> = listOf(
        "Nile City FC", "Atlas United", "Cedar Rovers", "Oasis Athletic",
        "Harbor Town", "Summit FC", "Delta Sporting", "Coastal Wanderers",
    ).mapIndexed { i, n -> TeamRef(9101L + i, n, null) }

    private val leagueRef = CompetitionRef(9001, "Demo Premier Division", "Demoland", null, null, null, season)
    private val cupRef = CompetitionRef(9002, "Demo Cup", "Demoland", null, null, null, season)

    private val baseId = 9_000_000_000L

    private fun teamById(id: Long): TeamRef = teams.firstOrNull { it.id == id } ?: teams[0]

    // ---------------------------------------------------------------- match generation

    private fun matchesOn(date: LocalDate, now: Instant): List<Match> {
        val d = date.toEpochDay()
        val rotation = (d % 7).toInt()
        val hours = listOf(15 to 0, 17 to 30, 19 to 45, 21 to 0)
        return (0..3).map { k ->
            val home = teams[(2 * k + rotation) % 8]
            val away = teams[(2 * k + 1 + rotation) % 8]
            val kickoff = date.atTime(hours[k].first, hours[k].second).toInstant(ZoneOffset.UTC)
            val id = baseId + d * 10 + k
            val comp = if (k == 3) cupRef else leagueRef
            buildMatch(id, kickoff, comp.copy(round = "Round ${(d % 30).toInt() + 1}"), home, away, now)
        }
    }

    private fun seedOf(id: Long): Int = (id % 1_000_003L).toInt()

    private fun playedMinutes(kickoff: Instant, now: Instant): Int {
        val m = Duration.between(kickoff, now).toMinutes()
        return when {
            m < 0 -> 0
            m < 47 -> minOf(m.toInt(), 47)
            m < 62 -> 45
            m < 110 -> minOf(45 + (m - 62).toInt(), 93)
            else -> 90
        }
    }

    private fun goalsFor(seed: Int, played: Int): Pair<Int, Int> {
        val kHome = seed % 3 + 1
        val kAway = (seed / 3) % 3 + 1
        val capped = minOf(played, 90)
        return minOf(capped * kHome / 40, 5) to minOf(capped * kAway / 55, 4)
    }

    private fun buildMatch(id: Long, kickoff: Instant, comp: CompetitionRef, home: TeamRef, away: TeamRef, now: Instant): Match {
        val minutes = Duration.between(kickoff, now).toMinutes()
        val played = playedMinutes(kickoff, now)
        val status: StatusInfo = when {
            minutes < 0 -> StatusInfo("NS")
            minutes < 47 -> if (minutes > 45) StatusInfo("1H", 45, (minutes - 45).toInt()) else StatusInfo("1H", minutes.toInt(), null)
            minutes < 62 -> StatusInfo("HT", 45)
            minutes < 110 -> {
                val e = 45 + (minutes - 62).toInt()
                if (e > 90) StatusInfo("2H", 90, minOf(e - 90, 3)) else StatusInfo("2H", e, null)
            }
            else -> StatusInfo("FT", 90)
        }
        val (h, a) = goalsFor(seedOf(id), played)
        val hasGoals = minutes >= 0
        return Match(
            id = id,
            kickoff = kickoff.toString(),
            status = status,
            competition = comp,
            home = home,
            away = away,
            goals = if (hasGoals) GoalPair(h, a) else GoalPair(),
            score = ScoreBreakdown(),
            venue = "Demo Stadium",
        )
    }

    private fun findMatch(id: Long, now: Instant): Match? {
        val rel = id - baseId
        if (rel < 0) return null
        val day = rel / 10
        val date = LocalDate.ofEpochDay(day)
        return matchesOn(date, now).firstOrNull { it.id == id }
    }

    private fun past(days: Long, home: TeamRef, away: TeamRef, hg: Int, ag: Int, code: String, pens: GoalPair = GoalPair()): Match {
        val kickoff = LocalDate.now().minusDays(days).atTime(19, 0).toInstant(ZoneOffset.UTC)
        return Match(
            id = baseId + 500_000L + days,
            kickoff = kickoff.toString(),
            status = StatusInfo(code, 90),
            competition = leagueRef.copy(round = "Past meeting"),
            home = home,
            away = away,
            goals = GoalPair(hg, ag),
            score = ScoreBreakdown(penalty = pens),
            venue = "Demo Stadium",
        )
    }

    // ---------------------------------------------------------------- FootballDataSource

    override suspend fun fixturesByDate(date: String, timeZone: String): List<Match> =
        matchesOn(LocalDate.parse(date), Instant.now())

    override suspend fun live(): List<Match> {
        val now = Instant.now()
        val today = LocalDate.now(ZoneOffset.UTC)
        return matchesOn(today, now).filter {
            it.status.code == "1H" || it.status.code == "HT" || it.status.code == "2H"
        }
    }

    override suspend fun matchDetail(id: Long): MatchDetail {
        val now = Instant.now()
        val match = findMatch(id, now) ?: return MatchDetail(match = past(1, teams[0], teams[1], 1, 1, "FT"))
        val played = playedMinutes(Instant.parse(match.kickoff), now)
        val seed = seedOf(id)
        val kHome = seed % 3 + 1
        val kAway = (seed / 3) % 3 + 1
        val events = mutableListOf<MatchEvent>()
        val hg = match.goals.home ?: 0
        val ag = match.goals.away ?: 0
        for (i in 1..hg) {
            val minute = ((i * 40) + kHome - 1) / kHome
            events += MatchEvent(minute, null, match.home.id, "Demo Player ${(i * 3) % 11 + 1}", "Demo Player ${(i * 5) % 11 + 1}", "Goal", "Normal Goal")
        }
        for (i in 1..ag) {
            val minute = ((i * 55) + kAway - 1) / kAway
            events += MatchEvent(minute, null, match.away.id, "Demo Player ${(i * 4) % 11 + 1}", null, "Goal", "Normal Goal")
        }
        if (played >= 23) events += MatchEvent(23, null, match.home.id, "Demo Player 4", null, "Card", "Yellow Card")
        if (played >= 34 && seed % 5 == 0) events += MatchEvent(34, null, match.away.id, "Demo Player 9", null, "Var", "Goal cancelled")
        if (played >= 60) events += MatchEvent(60, null, match.away.id, "Demo Player 14", "Demo Player 8", "subst", "Substitution 1")
        if (played >= 71 && seed % 4 == 0) events += MatchEvent(71, null, match.away.id, "Demo Player 5", null, "Card", "Red Card")
        val sorted = events.sortedBy { it.minute ?: 0 }

        fun lineup(team: TeamRef): TeamLineup {
            val grids = listOf("1:1", "2:1", "2:2", "2:3", "2:4", "3:1", "3:2", "3:3", "4:1", "4:2", "4:3")
            val pos = listOf("G", "D", "D", "D", "D", "M", "M", "M", "F", "F", "F")
            val start = grids.mapIndexed { i, g -> LineupPlayer(null, "Demo Player ${i + 1}", i + 1, pos[i], g) }
            val subs = (12..18).map { LineupPlayer(null, "Demo Player $it", it, "M", null) }
            return TeamLineup(team, "4-3-3", "Demo Coach", start, subs)
        }

        fun stats(team: TeamRef, home: Boolean): TeamStats {
            val poss = if (home) 45 + seed % 11 else 55 - seed % 11
            val shots = 6 + (seed + if (home) 0 else 3) % 9
            return TeamStats(
                team,
                listOf(
                    StatItem("Ball Possession", "$poss%"),
                    StatItem("Total Shots", shots.toString()),
                    StatItem("Shots on Goal", (shots / 3 + 1).toString()),
                    StatItem("Corner Kicks", (2 + (seed + shots) % 7).toString()),
                    StatItem("Fouls", (8 + (seed + shots) % 6).toString()),
                    StatItem("Offsides", (seed % 4).toString()),
                    StatItem("Yellow Cards", (seed % 3).toString()),
                ),
            )
        }

        return MatchDetail(
            match = match,
            events = sorted,
            lineups = if (played > 0 || match.status.code == "NS") listOf(lineup(match.home), lineup(match.away)) else emptyList(),
            statistics = if (played > 0) listOf(stats(match.home, true), stats(match.away, false)) else emptyList(),
        )
    }

    override suspend fun headToHead(homeId: Long, awayId: Long): List<Match> {
        val h = teamById(homeId)
        val a = teamById(awayId)
        return listOf(
            past(30, h, a, 2, 1, "FT"),
            past(120, a, h, 0, 0, "FT"),
            past(400, h, a, 1, 1, "PEN", GoalPair(4, 3)),
        )
    }

    override suspend fun injuries(matchId: Long): List<Injury> {
        val m = findMatch(matchId, Instant.now()) ?: return emptyList()
        return listOf(
            Injury(null, "Demo Player 10", null, m.home, "Missing Fixture", "Knee Injury"),
            Injury(null, "Demo Player 6", null, m.away, "Questionable", "Muscle Injury"),
        )
    }

    override suspend fun competitions(): List<Competition> = listOf(
        Competition(9001, "Demo Premier Division", "League", null, "Demoland", "DL", null, season),
        Competition(9002, "Demo Cup", "Cup", null, "Demoland", "DL", null, season),
    )

    override suspend fun competitionFixtures(id: Long, season: Int): List<Match> {
        val now = Instant.now()
        val today = LocalDate.now(ZoneOffset.UTC)
        return (-14L..14L).flatMap { matchesOn(today.plusDays(it), now) }
            .filter { it.competition.id == id }
            .sortedBy { it.kickoff }
    }

    override suspend fun standings(id: Long, season: Int): List<StandingGroup> {
        val rows = teams.mapIndexed { i, t ->
            val win = 7 - i
            val draw = (i * 2) % 4
            val lose = 10 - win - draw
            val gf = win * 2 + draw + 3
            val ga = lose * 2 + draw + 2
            StandingRow(0, t, win * 3 + draw, 10, win, draw, lose, gf, ga, gf - ga, null, null)
        }.sortedWith(compareByDescending<StandingRow> { it.points }.thenByDescending { it.goalsDiff })
            .mapIndexed { index, r ->
                val desc = when {
                    index < 2 -> "Promotion"
                    index >= 6 -> "Relegation"
                    else -> null
                }
                val form = listOf("W", "D", "L", "W", "W").shuffled(java.util.Random(index.toLong())).joinToString("")
                r.copy(rank = index + 1, description = desc, form = form)
            }
        return listOf(StandingGroup("Demo Premier Division", rows))
    }

    override suspend fun topScorers(id: Long, season: Int): List<TopScorer> =
        (0..5).map { i ->
            TopScorer(
                PlayerRef(9201L + i, "Demo Striker ${i + 1}", null),
                teams[i % teams.size],
                9 - i,
                4 - i / 2,
                10,
            )
        }

    override suspend fun search(query: String): SearchResult {
        val q = query.trim().lowercase()
        val t = teams.filter { it.name.lowercase().contains(q) }
            .map { TeamSummary(it.id, it.name, null, "Demoland", false, 2000, "Demo Stadium", "Demo City") }
        val c = competitions().filter { it.name.lowercase().contains(q) }
        return SearchResult(t, c)
    }

    override suspend fun team(id: Long): TeamProfile {
        val t = teamById(id)
        val positions = listOf("Goalkeeper", "Defender", "Defender", "Defender", "Midfielder", "Midfielder", "Midfielder", "Attacker", "Attacker")
        val squad = positions.mapIndexed { i, p -> SquadPlayer(9300L + i, "Demo Player ${i + 1}", 20 + i, i + 1, p, null) }
        return TeamProfile(TeamSummary(t.id, t.name, null, "Demoland", false, 2000, "Demo Stadium", "Demo City"), squad)
    }

    override suspend fun teamFixtures(id: Long): List<Match> {
        val now = Instant.now()
        val today = LocalDate.now(ZoneOffset.UTC)
        return (-14L..14L).flatMap { matchesOn(today.plusDays(it), now) }
            .filter { it.home.id == id || it.away.id == id }
            .sortedBy { it.kickoff }
    }

    override suspend fun player(id: Long, season: Int): PlayerProfile = PlayerProfile(
        id = id,
        name = "Demo Player ${id % 100}",
        age = 24,
        nationality = "Demoland",
        height = "180 cm",
        weight = "75 kg",
        photo = null,
        injured = false,
        team = teams[(id % 8).toInt()],
        stats = listOf(
            PlayerSeasonStat(leagueRef, teams[(id % 8).toInt()], "Attacker", 10, 820, 6, 3, 1, 0, "7.1"),
        ),
    )
}
