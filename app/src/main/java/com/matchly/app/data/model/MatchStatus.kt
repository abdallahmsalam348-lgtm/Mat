package com.matchly.app.data.model

import java.time.Instant
import java.time.OffsetDateTime

/** Normalised fixture status. Provider short codes (API-Football style) are mapped here. */
enum class MatchStatus {
    SCHEDULED,
    TO_BE_DEFINED,
    FIRST_HALF,
    HALF_TIME,
    SECOND_HALF,
    EXTRA_TIME,
    BREAK_BEFORE_EXTRA,
    PENALTIES,
    LIVE,
    SUSPENDED,
    INTERRUPTED,
    FINISHED,
    FINISHED_EXTRA_TIME,
    FINISHED_PENALTIES,
    AWARDED,
    WALKOVER,
    POSTPONED,
    CANCELLED,
    ABANDONED,
    UNKNOWN;

    val isLive: Boolean
        get() = when (this) {
            FIRST_HALF, HALF_TIME, SECOND_HALF, EXTRA_TIME, BREAK_BEFORE_EXTRA, PENALTIES, LIVE -> true
            else -> false
        }

    val isFinished: Boolean
        get() = when (this) {
            FINISHED, FINISHED_EXTRA_TIME, FINISHED_PENALTIES, AWARDED, WALKOVER -> true
            else -> false
        }

    val isUpcoming: Boolean
        get() = this == SCHEDULED || this == TO_BE_DEFINED

    /** Postponed, cancelled, abandoned, suspended or interrupted. */
    val isDisrupted: Boolean
        get() = when (this) {
            POSTPONED, CANCELLED, ABANDONED, SUSPENDED, INTERRUPTED -> true
            else -> false
        }

    /** Statuses whose score block can be meaningful. */
    val canHaveScore: Boolean
        get() = isLive || isFinished || this == ABANDONED || this == SUSPENDED || this == INTERRUPTED

    companion object {
        fun fromCode(code: String?): MatchStatus = when (code?.trim()?.uppercase()) {
            "NS" -> SCHEDULED
            "TBD" -> TO_BE_DEFINED
            "1H" -> FIRST_HALF
            "HT" -> HALF_TIME
            "2H" -> SECOND_HALF
            "ET" -> EXTRA_TIME
            "BT" -> BREAK_BEFORE_EXTRA
            "P" -> PENALTIES
            "LIVE" -> LIVE
            "SUSP" -> SUSPENDED
            "INT" -> INTERRUPTED
            "FT" -> FINISHED
            "AET" -> FINISHED_EXTRA_TIME
            "PEN" -> FINISHED_PENALTIES
            "AWD" -> AWARDED
            "WO" -> WALKOVER
            "PST" -> POSTPONED
            "CANC" -> CANCELLED
            "ABD" -> ABANDONED
            else -> UNKNOWN
        }
    }
}

/** What the UI should print as the score of a match. */
data class DisplayScore(
    val home: Int?,
    val away: Int?,
    val penaltyHome: Int?,
    val penaltyAway: Int?,
) {
    val hasScore: Boolean get() = home != null && away != null
    val hasPenalties: Boolean get() = penaltyHome != null && penaltyAway != null
}

val Match.matchStatus: MatchStatus get() = MatchStatus.fromCode(status.code)

val Match.kickoffInstant: Instant?
    get() {
        if (kickoff.isBlank()) return null
        return try {
            Instant.parse(kickoff)
        } catch (e: Exception) {
            try {
                OffsetDateTime.parse(kickoff).toInstant()
            } catch (e2: Exception) {
                null
            }
        }
    }

/**
 * Score rules:
 *  - not started / postponed / cancelled / unknown without data  -> no score
 *  - live and the provider has not filled the goals yet           -> 0-0
 *  - penalty shoot-out scores are reported separately (goals exclude the shoot-out)
 */
fun Match.displayScore(): DisplayScore {
    val st = matchStatus
    if (st.isUpcoming || st == MatchStatus.POSTPONED || st == MatchStatus.CANCELLED) {
        return DisplayScore(null, null, null, null)
    }
    var h = goals.home
    var a = goals.away
    if (st.isLive) {
        if (h == null) h = 0
        if (a == null) a = 0
    }
    if (!st.canHaveScore && (h == null || a == null)) {
        return DisplayScore(null, null, null, null)
    }
    val ph = score.penalty.home
    val pa = score.penalty.away
    val showPens = ph != null && pa != null &&
        (st == MatchStatus.FINISHED_PENALTIES || st == MatchStatus.PENALTIES || ph + pa > 0)
    return DisplayScore(h, a, if (showPens) ph else null, if (showPens) pa else null)
}

/** +1 home won the shoot-out, -1 away won, 0 undecided/none. */
fun DisplayScore.penaltyWinner(): Int {
    val ph = penaltyHome ?: return 0
    val pa = penaltyAway ?: return 0
    return when {
        ph > pa -> 1
        pa > ph -> -1
        else -> 0
    }
}
