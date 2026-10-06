package com.mmushtaq04.buysell.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mmushtaq04.buysell.data.local.AppDatabase
import com.mmushtaq04.buysell.data.local.enums.Role

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val db = AppDatabase.getInstance(applicationContext)
        val syncManager = FirestoreSyncManager(db)

        val meta = db.appMetaDao().getAppMeta()
        val shopId = meta?.activeShopId ?: return Result.success()

        val pushed = syncManager.pushOutbox(shopId, Role.OWNER, "session-1").getOrDefault(0)
        val pulled = syncManager.pullChanges(shopId, Role.OWNER).getOrDefault(0)

        return Result.success()
    }
}
