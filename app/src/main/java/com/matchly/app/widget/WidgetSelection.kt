package com.matchly.app.widget

import com.matchly.app.data.model.Match
import com.matchly.app.data.model.kickoffInstant
import com.matchly.app.data.model.matchStatus
import java.time.Instant

/** Chooses which matches a home-screen widget shows: live first, then upcoming, then finished; favourites win ties. */
object WidgetSelection {
    fun rank(m: Match, favTeams: Set<Long>, favComps: Set<Long>): Int {
        val st = m.matchStatus
        val group = when {
            st.isLive -> 0
            st.isUpcoming -> 1
            st.isFinished -> 2
            else -> 3
        }
        val fav = favTeams.contains(m.home.id) || favTeams.contains(m.away.id) || favComps.contains(m.competition.id)
        return group * 2 + if (fav) 0 else 1
    }

    fun select(matches: List<Match>, favTeams: Set<Long>, favComps: Set<Long>, limit: Int): List<Match> =
        matches
            .sortedWith(
                compareBy<Match>({ rank(it, favTeams, favComps) }, { it.kickoffInstant ?: Instant.MAX }),
            )
            .take(limit)
}
