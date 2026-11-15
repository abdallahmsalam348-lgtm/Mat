package com.matchly.app.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.matchly.app.widget.WidgetUpdateWorker
import java.util.concurrent.TimeUnit

object WorkScheduler {
    private const val WATCH = "matchly-watch"
    private const val WIDGETS = "matchly-widgets"

    private val network = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    /** Favourite-match notifications: checked every 15 minutes (Android's minimum for periodic work). */
    fun syncNotifications(context: Context, enabled: Boolean) {
        val wm = WorkManager.getInstance(context)
        if (!enabled) {
            wm.cancelUniqueWork(WATCH)
            return
        }
        val request = PeriodicWorkRequestBuilder<MatchWatchWorker>(15, TimeUnit.MINUTES)
            .setConstraints(network)
            .build()
        wm.enqueueUniquePeriodicWork(WATCH, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    /** Widget refresh: every 30 minutes, only while at least one widget is on the home screen. */
    fun scheduleWidgets(context: Context) {
        val request = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(30, TimeUnit.MINUTES)
            .setConstraints(network)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WIDGETS, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun cancelWidgets(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WIDGETS)
    }
}
