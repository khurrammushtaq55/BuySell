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

        val user = db.userDao().getPrimaryUser()
        val role = user?.role ?: Role.STAFF
        val sessionId = user?.activeSessionId?.ifBlank { "session_active" } ?: "session_active"

        Log.d(TAG, "Executing background SyncWorker for active shop '$shopId' with Role '$role'...")
        val pushed = syncManager.pushOutbox(shopId, role, sessionId).getOrDefault(0)
        val pulled = syncManager.pullChanges(shopId, role).getOrDefault(0)
        Log.i(TAG, "SyncWorker completed. Pushed $pushed outbox items, pulled $pulled cloud changes.")

        return Result.success()
    }
}
