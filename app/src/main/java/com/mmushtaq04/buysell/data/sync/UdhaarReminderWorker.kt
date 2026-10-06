package com.mmushtaq04.buysell.data.sync

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mmushtaq04.buysell.R
import com.mmushtaq04.buysell.data.local.AppDatabase
import com.mmushtaq04.buysell.util.AppNotificationManager
import kotlinx.coroutines.flow.firstOrNull

class UdhaarReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val db = AppDatabase.getInstance(applicationContext)
        val now = System.currentTimeMillis()
        val meta = db.appMetaDao().getAppMeta()
        val shopId = meta?.activeShopId ?: return Result.success()

        val overduePromises = db.paymentPromiseDao().observeOverduePromises(shopId, now).firstOrNull() ?: emptyList()

        if (overduePromises.isNotEmpty()) {
            val notification = NotificationCompat.Builder(applicationContext, AppNotificationManager.CHANNEL_REMINDERS)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Late Udhaar Payments Warning")
                .setContentText("${overduePromises.size} payments date nikal gayi hain. Khata check karein.")
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
