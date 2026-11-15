package com.matchly.app.data.model

import kotlinx.serialization.Serializable

/**
 * "MATCHLY API v1" – the provider-neutral JSON contract spoken by the backend proxy.
 * Every field has a default so missing data from a provider never crashes parsing.
 */

@Serializable
data class Meta(
    val provider: String? = null,
    val cached: Boolean = false,
    val stale: Boolean = false,
    val fetchedAt: String? = null,
    val quotaRemaining: Int? = null,
)

@Serializable
data class Envelope<T>(
    val data: T,
    val meta: Meta = Meta(),
)

@Serializable
data class Health(
    val ok: Boolean = false,
    val configured: Boolean = false,
    val provider: String? = null,
    val plan: String? = null,
    val requestsToday: Int? = null,
    val requestsLimit: Int? = null,
)

@Serializable
data class StatusInfo(
    val code: String = "NS",
    val elapsed: Int? = null,
    val extra: Int? = null,
)

@Serializable
data class TeamRef(
    val id: Long = 0,
    val name: String = "",
    val logo: String? = null,
)

@Serializable
data class CompetitionRef(
    val id: Long = 0,
    val name: String = "",
    val country: String? = null,
    val logo: String? = null,
    val flag: String? = null,
    val round: String? = null,
    val season: Int? = null,
)

@Serializable
data class GoalPair(
    val home: Int? = null,
    val away: Int? = null,
)

@Serializable
data class ScoreBreakdown(
    val halftime: GoalPair = GoalPair(),
    val fulltime: GoalPair = GoalPair(),
    val extratime: GoalPair = GoalPair(),
    val penalty: GoalPair = GoalPair(),
)

@Serializable
data class Match(
    val id: Long = 0,
    /** ISO-8601 instant in UTC, e.g. 2026-11-13T19:00:00Z */
    val kickoff: String = "",
    val status: StatusInfo = StatusInfo(),
    val competition: CompetitionRef = CompetitionRef(),
    val home: TeamRef = TeamRef(),
    val away: TeamRef = TeamRef(),
    val goals: GoalPair = GoalPair(),
    val score: ScoreBreakdown = ScoreBreakdown(),
    val venue: String? = null,
)

@Serializable
data class MatchEvent(
    val minute: Int? = null,
    val extra: Int? = null,
    val teamId: Long? = null,
    val player: String? = null,
    val assist: String? = null,
    /** Goal | Card | subst | Var */
    val type: String = "",
    val detail: String? = null,
    val comments: String? = null,
)

@Serializable
data class LineupPlayer(
    val id: Long? = null,
    val name: String = "",
    val number: Int? = null,
    val pos: String? = null,
    /** "row:col" grid position from the provider, when available */
    val grid: String? = null,
)

@Serializable
data class TeamLineup(
    val team: TeamRef = TeamRef(),
    val formation: String? = null,
    val coach: String? = null,
    val startXI: List<LineupPlayer> = emptyList(),
    val substitutes: List<LineupPlayer> = emptyList(),
)

@Serializable
data class StatItem(
    val type: String = "",
    val value: String? = null,
)

@Serializable
data class TeamStats(
    val team: TeamRef = TeamRef(),
    val stats: List<StatItem> = emptyList(),
)

@Serializable
data class MatchDetail(
    val match: Match = Match(),
    val events: List<MatchEvent> = emptyList(),
    val lineups: List<TeamLineup> = emptyList(),
    val statistics: List<TeamStats> = emptyList(),
)

@Serializable
data class Injury(
    val playerId: Long? = null,
    val player: String = "",
    val photo: String? = null,
    val team: TeamRef = TeamRef(),
    val type: String? = null,
    val reason: String? = null,
)

@Serializable
data class StandingRow(
    val rank: Int = 0,
    val team: TeamRef = TeamRef(),
    val points: Int = 0,
    val played: Int = 0,
    val win: Int = 0,
    val draw: Int = 0,
    val lose: Int = 0,
    val goalsFor: Int = 0,
    val goalsAgainst: Int = 0,
    val goalsDiff: Int = 0,
    val form: String? = null,
    val description: String? = null,
)

@Serializable
data class StandingGroup(
    val name: String? = null,
    val rows: List<StandingRow> = emptyList(),
)

@Serializable
data class PlayerRef(
    val id: Long = 0,
    val name: String = "",
    val photo: String? = null,
)

@Serializable
data class TopScorer(
    val player: PlayerRef = PlayerRef(),
    val team: TeamRef = TeamRef(),
    val goals: Int? = null,
    val assists: Int? = null,
    val appearances: Int? = null,
)

@Serializable
data class Competition(
    val id: Long = 0,
    val name: String = "",
    val type: String? = null,
    val logo: String? = null,
    val country: String? = null,
    val countryCode: String? = null,
    val flag: String? = null,
    val season: Int? = null,
)

@Serializable
data class TeamSummary(
    val id: Long = 0,
    val name: String = "",
    val logo: String? = null,
    val country: String? = null,
    val national: Boolean = false,
    val founded: Int? = null,
    val venue: String? = null,
    val city: String? = null,
)

@Serializable
data class SearchResult(
    val teams: List<TeamSummary> = emptyList(),
    val competitions: List<Competition> = emptyList(),
)

@Serializable
data class SquadPlayer(
    val id: Long = 0,
    val name: String = "",
    val age: Int? = null,
    val number: Int? = null,
    val position: String? = null,
    val photo: String? = null,
)

@Serializable
data class TeamProfile(
    val team: TeamSummary = TeamSummary(),
    val squad: List<SquadPlayer> = emptyList(),
)

@Serializable
data class PlayerSeasonStat(
    val competition: CompetitionRef = CompetitionRef(),
    val team: TeamRef = TeamRef(),
    val position: String? = null,
    val appearances: Int? = null,
    val minutes: Int? = null,
    val goals: Int? = null,
    val assists: Int? = null,
    val yellow: Int? = null,
    val red: Int? = null,
    val rating: String? = null,
)

@Serializable
data class PlayerProfile(
    val id: Long = 0,
    val name: String = "",
    val age: Int? = null,
    val nationality: String? = null,
    val height: String? = null,
    val weight: String? = null,
    val photo: String? = null,
    val injured: Boolean? = null,
    val team: TeamRef? = null,
    val stats: List<PlayerSeasonStat> = emptyList(),
)
