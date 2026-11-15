package com.matchly.app.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.matchly.app.MainActivity
import com.matchly.app.R

object Notifications {
    const val CHANNEL_ID = "matchly_match_events"

    /** Call with a localised context so the channel name follows the app language. */
    fun ensureChannel(localizedContext: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            localizedContext.getString(R.string.channel_match_events),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = localizedContext.getString(R.string.channel_match_events_desc)
        }
        val manager = localizedContext.getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }

    fun canPost(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    @SuppressLint("MissingPermission")
    fun post(localizedContext: Context, notificationId: Int, title: String, text: String, matchId: Long) {
        if (!canPost(localizedContext)) return
        val intent = Intent(localizedContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_MATCH_ID, matchId)
        }
        val pending = PendingIntent.getActivity(
            localizedContext,
            (matchId % Int.MAX_VALUE).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(localizedContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_matchly)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        try {
            NotificationManagerCompat.from(localizedContext).notify(notificationId, notification)
        } catch (e: SecurityException) {
            // Permission revoked between the check and the call - ignore.
        }
    }
}
