package com.matchly.app.core

import android.content.Context
import com.matchly.app.R
import com.matchly.app.data.model.Match
import com.matchly.app.data.model.MatchStatus
import com.matchly.app.data.model.kickoffInstant
import com.matchly.app.data.model.matchStatus

/** Human-readable status text shared by the app UI, notifications and widgets. */
object MatchLabels {
    fun statusText(ctx: Context, match: Match, f: Formatters): String {
        val kickoff = match.kickoffInstant
        return when (match.matchStatus) {
            MatchStatus.SCHEDULED -> if (kickoff != null) f.time(kickoff) else ctx.getString(R.string.status_tbd)
            MatchStatus.TO_BE_DEFINED -> ctx.getString(R.string.status_tbd)
            MatchStatus.FIRST_HALF, MatchStatus.SECOND_HALF, MatchStatus.EXTRA_TIME, MatchStatus.LIVE ->
                f.minute(match.status) ?: ctx.getString(R.string.status_live)
            MatchStatus.HALF_TIME -> ctx.getString(R.string.status_ht)
            MatchStatus.BREAK_BEFORE_EXTRA -> ctx.getString(R.string.status_break)
            MatchStatus.PENALTIES -> ctx.getString(R.string.status_penalties)
            MatchStatus.SUSPENDED -> ctx.getString(R.string.status_suspended)
            MatchStatus.INTERRUPTED -> ctx.getString(R.string.status_interrupted)
            MatchStatus.FINISHED -> ctx.getString(R.string.status_ft)
            MatchStatus.FINISHED_EXTRA_TIME -> ctx.getString(R.string.status_aet)
            MatchStatus.FINISHED_PENALTIES -> ctx.getString(R.string.status_pen)
            MatchStatus.AWARDED -> ctx.getString(R.string.status_awarded)
            MatchStatus.WALKOVER -> ctx.getString(R.string.status_walkover)
            MatchStatus.POSTPONED -> ctx.getString(R.string.status_postponed)
            MatchStatus.CANCELLED -> ctx.getString(R.string.status_cancelled)
            MatchStatus.ABANDONED -> ctx.getString(R.string.status_abandoned)
            MatchStatus.UNKNOWN -> if (kickoff != null) f.time(kickoff) else ""
        }
    }
}
