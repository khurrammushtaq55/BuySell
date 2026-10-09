package com.mmushtaq04.buysell.data.sync

import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import com.mmushtaq04.buysell.R
import com.mmushtaq04.buysell.data.local.AppDatabase
import com.mmushtaq04.buysell.util.AppNotificationManager
import kotlinx.coroutines.flow.firstOrNull
import java.util.concurrent.TimeUnit

class UdhaarReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "UdhaarReminderWorker"
        private const val WORK_NAME = "DailyUdhaarReminderWork"

        fun scheduleDailyReminder(context: Context) {
            runCatching {
                val constraints = Constraints.Builder().build()

                val dailyRequest = PeriodicWorkRequestBuilder<UdhaarReminderWorker>(24, TimeUnit.HOURS)
                    .setConstraints(constraints)
                    .build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    dailyRequest
                )
            }.onFailure { e ->
                Log.e(TAG, "Failed to schedule daily UdhaarReminderWorker: ${e.localizedMessage}", e)
            }
        }
    }

    override suspend fun doWork(): Result {
        val db = AppDatabase.getInstance(applicationContext)
        val now = System.currentTimeMillis()
        val meta = db.appMetaDao().getAppMeta()
        val shopId = meta?.activeShopId ?: return Result.success()

        val overduePromises = db.paymentPromiseDao().observeOverduePromises(shopId, now).firstOrNull() ?: emptyList()

        if (overduePromises.isNotEmpty()) {
            val title = applicationContext.getString(R.string.notif_overdue_title)
            val msg = applicationContext.getString(R.string.notif_overdue_msg, overduePromises.size)

            val notification = NotificationCompat.Builder(applicationContext, AppNotificationManager.CHANNEL_REMINDERS)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(msg)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()

            try {
                NotificationManagerCompat.from(applicationContext).notify(1001, notification)
            } catch (e: SecurityException) {
                // Notification permission not granted yet
            }
        }

        return Result.success()
    }
}
