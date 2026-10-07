package com.mmushtaq04.buysell.data.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mmushtaq04.buysell.data.local.AppDatabase
import com.mmushtaq04.buysell.data.local.enums.Role

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "SyncWorker"
    }

    override suspend fun doWork(): Result {
        val db = AppDatabase.getInstance(applicationContext)
        val syncManager = FirestoreSyncManager(db)

        val meta = db.appMetaDao().getAppMeta()
        val shopId = meta?.activeShopId ?: run {
            Log.d(TAG, "SyncWorker skipped: No active shopId found in app_meta.")
            return Result.success()
        }

        Log.d(TAG, "Executing background SyncWorker for active shop '$shopId'...")
        val pushed = syncManager.pushOutbox(shopId, Role.OWNER, "session-1").getOrDefault(0)
        val pulled = syncManager.pullChanges(shopId, Role.OWNER).getOrDefault(0)
        Log.i(TAG, "SyncWorker completed. Pushed $pushed outbox items, pulled $pulled cloud changes.")

        return Result.success()
    }
}
