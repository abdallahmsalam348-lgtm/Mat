package com.matchly.app.widget

import android.content.Context
import com.matchly.app.MatchlyApp
import com.matchly.app.R
import com.matchly.app.core.Formatters
import com.matchly.app.core.LocaleHelper
import com.matchly.app.core.MatchLabels
import com.matchly.app.data.model.Match
import com.matchly.app.data.model.displayScore
import com.matchly.app.data.model.matchStatus
import java.time.Instant
import java.time.LocalDate

data class WidgetRow(
    val matchId: Long,
    val home: String,
    val away: String,
    val homeScore: String,
    val awayScore: String,
    val status: String,
    val live: Boolean,
    val competition: String,
)

data class WidgetModel(
    val rows: List<WidgetRow>,
    val demo: Boolean,
    val message: String?,
    val title: String,
    val updated: String,
)

object WidgetData {
    suspend fun load(context: Context, limit: Int): WidgetModel {
        val app = context.applicationContext as? MatchlyApp
        val ctx = LocaleHelper.wrap(context.applicationContext)
        val title = ctx.getString(R.string.app_name)
        if (app == null) return WidgetModel(emptyList(), false, ctx.getString(R.string.widget_no_data), title, "")

        val container = app.container
        val settings = container.settings.value
        val f = Formatters.from(settings)
        val zone = settings.zone
        val repo = container.repository

        var matches: List<Match>? = null
        try {
            repo.fixtures(LocalDate.now(zone), zone, false).collect { matches = it.data }
        } catch (e: Exception) {
            // keep whatever was emitted before the failure (cached data)
        }
        val list = matches
        val demo = repo.isDemo
        val updated = f.time(Instant.now())
        if (list == null) {
            return WidgetModel(emptyList(), demo, ctx.getString(R.string.widget_no_data), title, updated)
        }
        if (list.isEmpty()) {
            return WidgetModel(emptyList(), demo, ctx.getString(R.string.widget_no_matches), title, updated)
        }
        val picked = WidgetSelection.select(
            list,
            container.favorites.ids("team"),
            container.favorites.ids("competition"),
            limit,
        )
        val rows = picked.map { m ->
            val s = m.displayScore()
            WidgetRow(
                matchId = m.id,
                home = m.home.name,
                away = m.away.name,
                homeScore = if (s.hasScore) f.number(s.home) else "",
                awayScore = if (s.hasScore) f.number(s.away) else "",
                status = MatchLabels.statusText(ctx, m, f),
                live = m.matchStatus.isLive,
                competition = m.competition.name,
            )
        }
        return WidgetModel(rows, demo, null, title, updated)
    }
}
