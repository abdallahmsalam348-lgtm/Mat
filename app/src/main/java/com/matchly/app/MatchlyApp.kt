package com.matchly.app

import android.app.Application
import com.matchly.app.core.LocaleHelper
import com.matchly.app.notifications.Notifications
import com.matchly.app.notifications.WorkScheduler

class MatchlyApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.cache.prune(7L * 24 * 3600 * 1000)
        Notifications.ensureChannel(LocaleHelper.wrap(this))
        WorkScheduler.syncNotifications(this, container.settings.value.notifyEnabled)
    }
}
