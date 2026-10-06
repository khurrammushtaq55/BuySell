package com.mmushtaq04.buysell.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object AppNotificationManager {
    const val CHANNEL_HIGH_PRIORITY = "channel_high_priority"
    const val CHANNEL_REMINDERS = "channel_reminders"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val highChannel = NotificationChannel(
                CHANNEL_HIGH_PRIORITY,
                "Sync & Conflict Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical session and conflict notifications"
            }

            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDERS,
                "Udhaar & Due Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Business due dates and payment reminders"
            }

            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannels(listOf(highChannel, reminderChannel))
        }
    }
}
