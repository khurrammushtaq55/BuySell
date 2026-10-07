package com.mmushtaq04.buysell.data.sync

import android.util.Log
import com.mmushtaq04.buysell.data.local.AppDatabase
import com.mmushtaq04.buysell.data.local.entity.ConflictEntity
import com.mmushtaq04.buysell.data.local.enums.ConflictStatus
import java.util.UUID

class ConflictDetector(private val db: AppDatabase) {

    companion object {
        private const val TAG = "ConflictDetector"
    }

    suspend fun checkConflictForStockItem(shopId: String, stockItemId: String): Boolean {
        val item = db.stockItemDao().getStockItemById(stockItemId) ?: return false

        // Check if there are multiple txn lines associated with this UNIQUE stock item in SALE txns
        val history = db.stockItemDao().findByIdentifierHistory(shopId, item.identifier ?: "")
        if (history.size > 1 && history.any { it.hasConflict }) {
            Log.w(TAG, "Conflict already flagged for stock item '$stockItemId'")
            return true
        }
        return item.hasConflict
    }

    suspend fun recordConflict(
        shopId: String,
        stockItemId: String,
        txnIdA: String,
        txnIdB: String
    ) {
        Log.w(TAG, "Recording double-sale conflict for stockItem '$stockItemId' between Txn A '$txnIdA' and Txn B '$txnIdB'")
        val conflict = ConflictEntity(
            id = UUID.randomUUID().toString(),
            shopId = shopId,
            stockItemId = stockItemId,
            txnIdA = txnIdA,
            txnIdB = txnIdB,
            status = ConflictStatus.OPEN
        )

        val item = db.stockItemDao().getStockItemById(stockItemId)
        if (item != null) {
            db.stockItemDao().updateStockItem(item.copy(hasConflict = true))
        }
    }
}
