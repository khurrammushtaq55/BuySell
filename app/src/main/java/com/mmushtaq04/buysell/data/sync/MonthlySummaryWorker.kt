package com.mmushtaq04.buysell.data.sync

import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import com.mmushtaq04.buysell.R
import com.mmushtaq04.buysell.data.local.AppDatabase
import com.mmushtaq04.buysell.data.local.enums.TxnType
import com.mmushtaq04.buysell.util.AppNotificationManager
import com.mmushtaq04.buysell.util.AppPreferencesManager
import com.mmushtaq04.buysell.util.CurrencyFormatter
import java.util.Calendar
import java.util.concurrent.TimeUnit

class MonthlySummaryWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "MonthlySummaryWorker"
        private const val WORK_NAME = "MonthlyBusinessSummaryWork"

        fun scheduleMonthlySummary(context: Context, dayOfMonth: Int = -1, hour: Int = -1, minute: Int = -1) {
            runCatching {
                if (!AppPreferencesManager.isMonthlySummaryEnabled(context)) {
                    WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
                    Log.i(TAG, "Monthly summary disabled by user. Cancelled work.")
                    return
                }

                val (prefDay, prefHour, prefMin) = AppPreferencesManager.getMonthlySummarySchedule(context)
                val targetDay = if (dayOfMonth in 1..31) dayOfMonth else prefDay
                val targetHour = if (hour >= 0) hour else prefHour
                val targetMin = if (minute >= 0) minute else prefMin

                val now = Calendar.getInstance()
                val target = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, targetHour)
                    set(Calendar.MINUTE, targetMin)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
                    set(Calendar.DAY_OF_MONTH, targetDay.coerceIn(1, 31).coerceAtMost(maxDay))
                }

                if (target.before(now)) {
                    target.add(Calendar.MONTH, 1)
                    val maxDayNext = target.getActualMaximum(Calendar.DAY_OF_MONTH)
                    target.set(Calendar.DAY_OF_MONTH, targetDay.coerceIn(1, 31).coerceAtMost(maxDayNext))
                }

                val initialDelayMs = target.timeInMillis - now.timeInMillis

                val summaryRequest = OneTimeWorkRequestBuilder<MonthlySummaryWorker>()
                    .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
                    .build()

                WorkManager.getInstance(context).enqueueUniqueWork(
                    WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    summaryRequest
                )
                Log.i(TAG, "Scheduled MonthlySummaryWorker in ${initialDelayMs / 1000 / 60} minutes for Day $targetDay at $targetHour:$targetMin")
            }.onFailure { e ->
                Log.e(TAG, "Failed to schedule MonthlySummaryWorker: ${e.localizedMessage}", e)
            }
        }
    }

    override suspend fun doWork(): Result {
        val db = AppDatabase.getInstance(applicationContext)
        val meta = db.appMetaDao().getAppMeta()
        val user = db.userDao().getPrimaryUser()
        val shopId = meta?.activeShopId?.ifBlank { null } ?: user?.shopId ?: ""

        if (shopId.isNotBlank()) {
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val startOfMonth = cal.timeInMillis

            cal.add(Calendar.MONTH, 1)
            val endOfMonth = cal.timeInMillis - 1

            val shop = db.shopDao().getShopById(shopId)
            val shopName = shop?.name?.ifBlank { "Mera Store" } ?: "Mera Store"

            val txns = db.txnDao().getTxnsInTimeRange(shopId, startOfMonth, endOfMonth)
            val expenses = db.expenseDao().getExpensesInTimeRange(shopId, startOfMonth, endOfMonth)

            var salesCount = 0
            var salesRevenuePaisa = 0L
            var cogsPaisa = 0L

            for (txn in txns) {
                if (txn.type == TxnType.SALE) {
                    salesCount++
                    salesRevenuePaisa += txn.totalAmount
                    val lines = db.txnDao().getTxnLinesForTxn(txn.id)
                    for (line in lines) {
                        val stockItem = db.stockItemDao().getStockItemById(line.stockItemId)
                        if (stockItem?.purchaseLineId != null) {
                            val purchaseUnitPrice = db.txnDao().getUnitPriceByLineId(stockItem.purchaseLineId) ?: 0L
                            cogsPaisa += purchaseUnitPrice * line.quantity
                        }
                    }
                }
            }

            var expenseTotalPaisa = 0L
            for (exp in expenses) {
                expenseTotalPaisa += exp.amount
            }

            val grossProfitPaisa = salesRevenuePaisa - cogsPaisa
            val netProfitPaisa = grossProfitPaisa - expenseTotalPaisa

            val salesRevFormatted = CurrencyFormatter.formatPaisa(applicationContext, salesRevenuePaisa)
            val expFormatted = CurrencyFormatter.formatPaisa(applicationContext, expenseTotalPaisa)
            val profitFormatted = CurrencyFormatter.formatPaisa(applicationContext, netProfitPaisa)

            val title = applicationContext.getString(R.string.notif_monthly_summary_title, shopName)
            val msg = applicationContext.getString(
                R.string.notif_monthly_summary_msg,
                salesCount,
                salesRevFormatted,
                expFormatted,
                profitFormatted
            )

            val notification = NotificationCompat.Builder(applicationContext, AppNotificationManager.CHANNEL_DAILY_SUMMARY)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(msg)
                .setStyle(NotificationCompat.BigTextStyle().bigText(msg))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()

            try {
                NotificationManagerCompat.from(applicationContext).notify(3001, notification)
            } catch (_: SecurityException) {
                // Permission not granted
            }
        }

        // Re-schedule for next month
        scheduleMonthlySummary(applicationContext)

        return Result.success()
    }
}
