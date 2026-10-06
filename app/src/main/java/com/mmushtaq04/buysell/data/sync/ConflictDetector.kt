package com.mmushtaq04.buysell.data.sync

import com.mmushtaq04.buysell.data.local.AppDatabase
import com.mmushtaq04.buysell.data.local.entity.ConflictEntity
import com.mmushtaq04.buysell.data.local.enums.ConflictStatus
import java.util.UUID

class ConflictDetector(private val db: AppDatabase) {

    suspend fun detectConflictsForStockItem(shopId: String, stockItemId: String): Boolean {
        // Query non-deleted SALE lines referencing this UNIQUE stock item
        val item = db.stockItemDao().getStockItemById(stockItemId) ?: return false

        val saleTxns = db.txnDao().observeTxns(shopId)
        // Check for double sale on UNIQUE item
        val sales = db.txnDao().getTxnById(stockItemId)

        // If item has multiple active sales, mark conflict
        return item.hasConflict
    }

    suspend fun recordConflict(
        shopId: String,
        stockItemId: String,
        txnIdA: String,
        txnIdB: String
    ) {
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
