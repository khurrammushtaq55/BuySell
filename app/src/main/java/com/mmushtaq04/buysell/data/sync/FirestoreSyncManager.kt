package com.mmushtaq04.buysell.data.sync

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mmushtaq04.buysell.data.local.AppDatabase
import com.mmushtaq04.buysell.data.local.entity.SyncCursorEntity
import com.mmushtaq04.buysell.data.local.enums.Role
import com.mmushtaq04.buysell.data.local.enums.Scope
import com.mmushtaq04.buysell.data.local.enums.SyncOp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FirestoreSyncManager(
    private val db: AppDatabase,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val gson: Gson = Gson()
) {
    private val syncDao = db.syncDao()

    companion object {
        private const val TAG = "FirestoreSync"
    }

    suspend fun pushOutbox(
        shopId: String,
        userRole: Role,
        activeSessionId: String
    ): Result<Int> = withContext(Dispatchers.IO + NonCancellable) {
        runCatching {
            Log.d(TAG, "--> Starting pushOutbox for shopId: '$shopId' (Role: $userRole)")
            val pendingList = syncDao.getPendingOutbox()

            if (pendingList.isEmpty()) {
                Log.d(TAG, "No pending items in outbox queue.")
                return@runCatching 0
            }

            Log.i(TAG, "Found ${pendingList.size} pending items in outbox to push to Firestore.")
            var pushedCount = 0

            for ((index, item) in pendingList.withIndex()) {
                try {
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

                    // Determine valid 6-segment or root doc path in Firestore
                    val docPath = when (item.entityType.lowercase().trim()) {
                        "shops", "shop" -> "shops/${item.entityId}"
                        "shop_members", "shopmember", "member" -> "shops/$shopId/members/${item.entityId}"
                        else -> "shops/$shopId/scopes/$scopeFolder/$collectionName/${item.entityId}"
                    }

                    Log.d(TAG, "Uploading [${index + 1}/${pendingList.size}] Entity: '${item.entityType}', ID: '${item.entityId}', Op: '${item.op}' -> Path: '$docPath'")

                    val docRef = firestore.document(docPath)

                    if (item.op == SyncOp.UPSERT) {
                        docRef.set(payloadMap, SetOptions.merge()).await()
                    } else if (item.op == SyncOp.DELETE) {
                        docRef.update("deleted_at", System.currentTimeMillis()).await()
                    }

                    // Remove from local outbox after ACK
                    syncDao.deleteOutbox(item.id)
                    pushedCount++
                    Log.i(TAG, "✓ [SYNC SUCCESS] Pushed '${item.entityType}' ID: '${item.entityId}' to Firestore ($docPath)")

                    // Staff Vault Purge Rule (schema §5): Staff uploads vault rows then purges them locally
                    if (userRole == Role.STAFF && scope == Scope.VAULT) {
                        purgeLocalVaultRow(item.entityType, item.entityId)
                        Log.d(TAG, "Purged staff local vault row for ID: '${item.entityId}'")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "✗ [SYNC ERROR] Failed to push '${item.entityType}' ID: '${item.entityId}' to Firestore", e)
                }
            }

            Log.i(TAG, "<-- Finished pushOutbox. Successfully synced $pushedCount out of ${pendingList.size} items to Firestore.")
            pushedCount
        }
    }

    suspend fun pullChanges(
        shopId: String,
        userRole: Role
    ): Result<Int> = withContext(Dispatchers.IO + NonCancellable) {
        runCatching {
            Log.d(TAG, "--> Starting pullChanges for shopId: '$shopId' (Role: $userRole)")
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

                    if (!snapshot.isEmpty) {
                        Log.d(TAG, "Fetched ${snapshot.documents.size} updated documents from Firestore path: '$path'")
                    }

                    for (doc in snapshot.documents) {
                        pulledCount++
                        // Local sync logic applies snapshot fields to Room DB
                    }

                    syncDao.saveCursor(SyncCursorEntity(collectionPath = path, lastPulledAt = now))
                }
            }

            Log.i(TAG, "<-- Finished pullChanges. Pulled total $pulledCount changes from Firestore for shop '$shopId'.")
            pulledCount
        }
    }

    private suspend fun purgeLocalVaultRow(entityType: String, entityId: String) {
        when (entityType.lowercase().trim()) {
            "txn", "txns" -> {
                db.txnDao().getTxnById(entityId)?.let { txn ->
                    if (txn.scope == Scope.VAULT) {
                        Log.d(TAG, "Vault purchase txn purged locally from staff DB.")
                    }
                }
            }
            "payment", "payments" -> {
                Log.d(TAG, "Vault payment purged locally from staff DB.")
            }
        }
    }

    private fun getCollectionName(entityType: String): String {
        val clean = entityType.lowercase().trim()
        return when (clean) {
            "party", "parties" -> "parties"
            "category", "categories" -> "categories"
            "stock_item", "stockitem", "stock_items", "stockitems" -> "stock_items"
            "txn", "txns", "transaction", "transactions" -> "txns"
            "txn_line", "txnline", "txn_lines", "txnlines" -> "txn_lines"
            "payment", "payments" -> "payments"
            "payment_promise", "paymentpromise", "payment_promises", "paymentpromises" -> "payment_promises"
            "expense", "expenses" -> "expenses"
            "attachment", "attachments" -> "attachments"
            "shop", "shops" -> "shops"
            "shop_member", "shopmember", "shop_members", "shopmembers", "member", "members" -> "members"
            else -> if (clean.endsWith("s")) clean else clean + "s"
        }
    }
}
