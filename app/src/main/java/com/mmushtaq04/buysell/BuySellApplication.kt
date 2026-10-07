package com.mmushtaq04.buysell

import android.app.Application
import com.mmushtaq04.buysell.data.sync.SyncWorker
import com.mmushtaq04.buysell.data.sync.UdhaarReminderWorker
import com.mmushtaq04.buysell.util.AppNotificationManager

class BuySellApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // 1. Create Notification Channels
        AppNotificationManager.createNotificationChannels(this)

        // 2. Schedule Periodic Background SyncWorker (every 15 mins when online)
        SyncWorker.schedulePeriodicSync(this)

        // 3. Schedule Daily Udhaar Payment Due Reminders
        UdhaarReminderWorker.scheduleDailyReminder(this)
    }
}
