package com.mmushtaq04.buysell.data.sync

import android.content.Context
import android.util.Log
import androidx.work.*
import com.mmushtaq04.buysell.data.local.AppDatabase
import com.mmushtaq04.buysell.data.local.enums.Role
import java.util.UUID
import java.util.concurrent.TimeUnit

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "SyncWorker"
        private const val ONE_TIME_WORK_NAME = "OneTimeSyncWork"
        private const val PERIODIC_WORK_NAME = "PeriodicSyncWork"

        fun enqueueOneTimeSync(context: Context) {
            runCatching {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val request = OneTimeWorkRequestBuilder<SyncWorker>()
                    .setConstraints(constraints)
                    .build()

                WorkManager.getInstance(context).enqueueUniqueWork(
                    ONE_TIME_WORK_NAME,
                    ExistingWorkPolicy.KEEP,
                    request
                )
            }.onFailure { e ->
                Log.e(TAG, "Failed to enqueue one-time SyncWorker: ${e.localizedMessage}", e)
            }
        }

        fun schedulePeriodicSync(context: Context) {
            runCatching {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val periodicRequest = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                    .setConstraints(constraints)
                    .build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    PERIODIC_WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    periodicRequest
                )
            }.onFailure { e ->
                Log.e(TAG, "Failed to schedule periodic SyncWorker: ${e.localizedMessage}", e)
            }
        }
    }

    override suspend fun doWork(): Result {
        val db = AppDatabase.getInstance(applicationContext)
        val syncManager = FirestoreSyncManager(db)

        val meta = db.appMetaDao().getAppMeta()
        val shopId = meta?.activeShopId ?: run {
            Log.d(TAG, "SyncWorker skipped: No active shopId found in app_meta.")
            return Result.success()
        }

        val user = db.userDao().getPrimaryUser()
        val role = user?.role ?: Role.STAFF
        val sessionId = user?.activeSessionId?.ifBlank { null } ?: ("sess_" + UUID.randomUUID().toString().take(12))

        Log.d(TAG, "Executing background SyncWorker for active shop '$shopId' with Role '$role'...")
        val pushed = syncManager.pushOutbox(shopId, role, sessionId).getOrDefault(0)
        val pulled = syncManager.pullChanges(shopId, role).getOrDefault(0)
        Log.i(TAG, "SyncWorker completed. Pushed $pushed outbox items, pulled $pulled cloud changes.")

        return Result.success()
    }
}
