package com.matchly.app.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.matchly.app.MatchlyApp
import com.matchly.app.R
import com.matchly.app.core.Formatters
import com.matchly.app.core.LocaleHelper
import com.matchly.app.data.model.Match
import com.matchly.app.data.model.MatchStatus
import com.matchly.app.data.model.matchStatus
import java.time.LocalDate

/**
 * Checks the day's fixtures for favourite teams/competitions and posts a notification when something
 * changed since the previous check (kickoff, goal, red card, half-time, full-time).
 *
 * HONEST LIMITS: Android runs periodic work at most every ~15 minutes, so these are NOT instant push
 * notifications. Real-time push requires Firebase Cloud Messaging plus a backend job - see docs/ROADMAP.md.
 * Demo data never triggers notifications.
 */
class MatchWatchWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? MatchlyApp ?: return Result.success()
        val container = app.container
        val settings = container.settings.value
        if (!settings.notifyEnabled) return Result.success()
        if (container.repository.isDemo) return Result.success()

        val ctx = LocaleHelper.wrap(applicationContext)
        if (!Notifications.canPost(ctx)) return Result.success()

        val favTeams = container.favorites.ids("team")
        val favComps = container.favorites.ids("competition")
        if (favTeams.isEmpty() && favComps.isEmpty()) return Result.success()

        val zone = settings.zone
        val today = LocalDate.now(zone)
        var matches: List<Match>? = null
        try {
            container.repository.fixtures(today, zone, force = true).collect { matches = it.data }
        } catch (e: Exception) {
            if (matches == null) return Result.retry()
        }
        val list = matches ?: return Result.retry()

        val prefs = applicationContext.getSharedPreferences("matchly_watch", Context.MODE_PRIVATE)
        if (prefs.getString("day", "") != today.toString()) {
            prefs.edit().clear().putString("day", today.toString()).apply()
        }
        val f = Formatters.from(settings)
        val relevant = list.filter {
            favTeams.contains(it.home.id) || favTeams.contains(it.away.id) || favComps.contains(it.competition.id)
        }

        for (m in relevant) {
            val key = "m${m.id}"
            val prev = prefs.getString(key, null)
            val status = m.matchStatus
            val hg = m.goals.home ?: 0
            val ag = m.goals.away ?: 0
            var reds = prefs.getInt("reds${m.id}", 0)

            if (status.isLive) {
                reds = checkRedCards(ctx, container, m, reds, settings.notifyRedCards && prev != null)
            }

            if (prev != null) {
                val parts = prev.split("|")
                val prevStatus = MatchStatus.valueOf(parts.getOrElse(0) { "UNKNOWN" })
                val prevTotal = (parts.getOrNull(1)?.toIntOrNull() ?: 0) + (parts.getOrNull(2)?.toIntOrNull() ?: 0)
                val names = "${m.home.name} - ${m.away.name}"
                val score = f.digits("$hg - $ag")
                val base = (m.id % 100_000L).toInt() * 10

                if (settings.notifyKickoff && prevStatus.isUpcoming && status.isLive) {
                    Notifications.post(ctx, base + 1, ctx.getString(R.string.notif_kickoff_title), names, m.id)
                }
                if (settings.notifyGoals && status.isLive && hg + ag > prevTotal) {
                    Notifications.post(ctx, base + 2, ctx.getString(R.string.notif_goal_title), "$names  $score", m.id)
                }
                if (settings.notifyHalfTime && prevStatus != MatchStatus.HALF_TIME && status == MatchStatus.HALF_TIME) {
                    Notifications.post(ctx, base + 3, ctx.getString(R.string.notif_halftime_title), "$names  $score", m.id)
                }
                if (settings.notifyFullTime && !prevStatus.isFinished && status.isFinished) {
                    Notifications.post(ctx, base + 4, ctx.getString(R.string.notif_fulltime_title), "$names  $score", m.id)
                }
            }
            prefs.edit().putString(key, "${status.name}|$hg|$ag").putInt("reds${m.id}", reds).apply()
        }
        return Result.success()
    }

    /** Costs one detail request per live favourite match per run; the backend cache absorbs repeats. */
    private suspend fun checkRedCards(
        ctx: Context,
        container: com.matchly.app.AppContainer,
        m: Match,
        previousReds: Int,
        notify: Boolean,
    ): Int {
        var detail: com.matchly.app.data.model.MatchDetail? = null
        try {
            container.repository.matchDetail(m.id, live = true, force = true).collect { detail = it.data }
        } catch (e: Exception) {
            return previousReds
        }
        val d = detail ?: return previousReds
        val redEvents = d.events.filter { it.type.equals("Card", true) && (it.detail ?: "").contains("Red", true) }
        if (notify && redEvents.size > previousReds && previousReds >= 0) {
            val last = redEvents.last()
            val text = "${m.home.name} - ${m.away.name}  ${last.player ?: ""}"
            Notifications.post(ctx, (m.id % 100_000L).toInt() * 10 + 5, ctx.getString(R.string.notif_red_title), text, m.id)
        }
        return redEvents.size
    }
}
