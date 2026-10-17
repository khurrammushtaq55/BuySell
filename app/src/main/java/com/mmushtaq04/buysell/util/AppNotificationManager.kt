package com.mmushtaq04.buysell.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.mmushtaq04.buysell.R

object AppNotificationManager {
    const val CHANNEL_HIGH_PRIORITY = "channel_high_priority"
    const val CHANNEL_REMINDERS = "channel_reminders"
    const val CHANNEL_DAILY_SUMMARY = "channel_daily_summary"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val highChannel = NotificationChannel(
                CHANNEL_HIGH_PRIORITY,
                context.getString(R.string.notif_channel_high_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notif_channel_high_desc)
            }

            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDERS,
                context.getString(R.string.notif_channel_reminders_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notif_channel_reminders_desc)
            }

            val summaryChannel = NotificationChannel(
                CHANNEL_DAILY_SUMMARY,
                context.getString(R.string.notif_channel_reports_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notif_channel_reports_desc)
            }

            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannels(listOf(highChannel, reminderChannel, summaryChannel))
        }
    }
}
