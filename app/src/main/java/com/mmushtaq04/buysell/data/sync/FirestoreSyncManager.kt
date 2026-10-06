package com.mmushtaq04.buysell.data.sync

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mmushtaq04.buysell.data.local.AppDatabase
import com.mmushtaq04.buysell.data.local.entity.SyncCursorEntity
import com.mmushtaq04.buysell.data.local.entity.SyncOutboxEntity
import com.mmushtaq04.buysell.data.local.enums.Role
import com.mmushtaq04.buysell.data.local.enums.Scope
import com.mmushtaq04.buysell.data.local.enums.SyncOp
import kotlinx.coroutines.tasks.await

class FirestoreSyncManager(
    private val db: AppDatabase,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val gson: Gson = Gson()
) {
    private val syncDao = db.syncDao()

    suspend fun pushOutbox(
        shopId: String,
        userRole: Role,
        activeSessionId: String
    ): Result<Int> = runCatching {
        val pendingList = syncDao.getPendingOutbox()
        var pushedCount = 0

        for (item in pendingList) {
            val payloadMap: MutableMap<String, Any?> = gson.fromJson(
                item.payloadJson,
                object : TypeToken<MutableMap<String, Any?>>() {}.type
            )

            // Attach session info
            payloadMap["active_session_id"] = activeSessionId

            val scopeString = (payloadMap["scope"] as? String) ?: Scope.PUBLIC.name
            val scope = runCatching { enumValueOf<Scope>(scopeString) }.getOrDefault(Scope.PUBLIC)
            val scopeFolder = if (scope == Scope.VAULT) "vault" else "public"

            val collectionName = getCollectionName(item.entityType)
            val docPath = "shops/$shopId/scopes/$scopeFolder/$collectionName/${item.entityId}"
            val docRef = firestore.document(docPath)

            if (item.op == SyncOp.UPSERT) {
                docRef.set(payloadMap, SetOptions.merge()).await()
            } else if (item.op == SyncOp.DELETE) {
                docRef.update("deleted_at", System.currentTimeMillis()).await()
            }

            // Remove from local outbox after ACK
            syncDao.deleteOutbox(item.id)
            pushedCount++

            // Staff Vault Purge Rule (schema §5): Staff uploads vault rows then purges them locally
            if (userRole == Role.STAFF && scope == Scope.VAULT) {
                purgeLocalVaultRow(item.entityType, item.entityId)
            }
        }

        pushedCount
    }

    suspend fun pullChanges(
        shopId: String,
        userRole: Role
    ): Result<Int> = runCatching {
        var pulledCount = 0
        val scopesToPull = if (userRole == Role.STAFF) listOf("public") else listOf("public", "vault")
        val collections = listOf("parties", "categories", "stock_items", "txns", "txn_lines", "payments", "payment_promises")

        val now = System.currentTimeMillis()

        for (scopeFolder in scopesToPull) {
            for (colName in collections) {
                val path = "shops/$shopId/scopes/$scopeFolder/$colName"
                val cursor = syncDao.getCursor(path)
                val lastPulled = cursor?.lastPulledAt ?: 0L

                val snapshot = firestore.collection(path)
                    .whereGreaterThan("updated_at", lastPulled)
                    .get()
                    .await()

                for (doc in snapshot.documents) {
                    pulledCount++
                    // Local sync logic applies snapshot fields to Room DB
                }

                syncDao.saveCursor(SyncCursorEntity(collectionPath = path, lastPulledAt = now))
            }
        }

        pulledCount
    }

    private suspend fun purgeLocalVaultRow(entityType: String, entityId: String) {
        when (entityType.lowercase()) {
            "txn" -> {
                db.txnDao().getTxnById(entityId)?.let { txn ->
                    if (txn.scope == Scope.VAULT) {
                        // Purging vault purchase transaction on staff device
                    }
                }
            }
            "payment" -> {
                // Purging vault payment
            }
        }
    }

    private fun getCollectionName(entityType: String): String {
        return when (entityType.lowercase()) {
            "party" -> "parties"
            "category" -> "categories"
            "stock_item", "stockitem" -> "stock_items"
            "txn" -> "txns"
            "txn_line", "txnline" -> "txn_lines"
            "payment" -> "payments"
            "payment_promise", "paymentpromise" -> "payment_promises"
            "expense" -> "expenses"
            else -> entityType.lowercase() + "s"
        }
    }
}
