package com.mmushtaq04.buysell.data.sync

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mmushtaq04.buysell.data.auth.FirebaseAuthManager
import com.mmushtaq04.buysell.data.local.AppDatabase
import com.mmushtaq04.buysell.data.local.entity.*
import com.mmushtaq04.buysell.data.local.enums.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.UUID

class FirestoreSyncManager(private val db: AppDatabase) {

    private val firestore = FirebaseFirestore.getInstance()
    private val syncDao = db.syncDao()
    private val gson = Gson()

    companion object {
        private const val TAG = "FirestoreSync"
        private const val NETWORK_TIMEOUT_MS = 5_000L
    }

    suspend fun pushOutbox(
        shopId: String,
        userRole: Role,
        activeSessionId: String
    ): Result<Int> = withContext(Dispatchers.IO) {
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
                        "users", "user" -> "users/${item.entityId}"
                        "shops", "shop" -> "shops/${item.entityId}"
                        "shop_members", "shopmember", "member" -> "shops/$shopId/members/${item.entityId}"
                        else -> "shops/$shopId/scopes/$scopeFolder/$collectionName/${item.entityId}"
                    }

                    Log.d(TAG, "Uploading [${index + 1}/${pendingList.size}] Entity: '${item.entityType}', ID: '${item.entityId}', Op: '${item.op}' -> Path: '$docPath'")

                    val docRef = firestore.document(docPath)

                    withTimeout(NETWORK_TIMEOUT_MS) {
                        if (item.op == SyncOp.UPSERT) {
                            docRef.set(payloadMap, SetOptions.merge()).await()
                        } else if (item.op == SyncOp.DELETE) {
                            val now = System.currentTimeMillis()
                            docRef.update(mapOf("deleted_at" to now, "updated_at" to now)).await()
                        }
                    }

                    // Remove from local outbox after ACK
                    syncDao.deleteOutbox(item.id)
                    pushedCount++
                    Log.i(TAG, "✓ [SYNC SUCCESS] Pushed '${item.entityType}' ID: '${item.entityId}' to Firestore ($docPath)")

                    // Staff Vault Purge Rule (schema §5): Staff uploads vault rows then purges them locally from Room DB
                    if (userRole == Role.STAFF && scope == Scope.VAULT) {
                        purgeLocalVaultRow(item.entityType, item.entityId)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "✗ [SYNC TIMEOUT/ERROR] Failed to push '${item.entityType}' ID: '${item.entityId}' to Firestore: ${e.message}")
                    // Timeout or network error -> Stop loop and return current pushed count so unsynced item remains in outbox
                    break
                }
            }

            Log.i(TAG, "<-- Finished pushOutbox. Successfully synced $pushedCount out of ${pendingList.size} items to Firestore.")
            pushedCount
        }
    }

    suspend fun pullChanges(
        shopId: String,
        userRole: Role
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            Log.d(TAG, "--> Starting pullChanges for shopId: '$shopId' (Role: $userRole)")
            var pulledCount = 0
            val scopesToPull = if (userRole == Role.STAFF) listOf("public") else listOf("public", "vault")
            val collections = listOf("parties", "categories", "stock_items", "txns", "txn_lines", "payments", "payment_promises", "expenses", "attachments", "payment_accounts")

            for (scopeFolder in scopesToPull) {
                for (colName in collections) {
                    val path6Segment = "shops/$shopId/scopes/$scopeFolder/$colName"
                    val cursor = syncDao.getCursor(path6Segment)
                    val lastPulled = cursor?.lastPulledAt ?: 0L

                    // Fetch ALL documents if lastPulled == 0L, or filter by updated_at > lastPulled
                    val query = if (lastPulled > 0L) {
                        firestore.collection(path6Segment).whereGreaterThan("updated_at", lastPulled)
                    } else {
                        firestore.collection(path6Segment)
                    }

                    val snapshot = runCatching {
                        withTimeout(NETWORK_TIMEOUT_MS) {
                            query.get().await()
                        }
                    }.getOrNull()

                    if (snapshot != null && !snapshot.isEmpty) {
                        val firstPath = snapshot.documents.firstOrNull()?.reference?.path ?: path6Segment
                        Log.i(TAG, "Fetched ${snapshot.documents.size} documents from Firestore path: '$firstPath'")

                        var maxDocUpdatedAt = lastPulled
                        for (doc in snapshot.documents) {
                            pulledCount++
                            val data = doc.data ?: continue
                            val docUpdatedAt = parseLongTimestamp(data["updated_at"])
                            if (docUpdatedAt > maxDocUpdatedAt) maxDocUpdatedAt = docUpdatedAt
                            applyFirestoreDocToRoom(colName, data)
                        }

                        if (maxDocUpdatedAt > lastPulled) {
                            syncDao.saveCursor(SyncCursorEntity(collectionPath = path6Segment, lastPulledAt = maxDocUpdatedAt))
                        }
                    }
                }
            }

            Log.i(TAG, "<-- Finished pullChanges. Pulled total $pulledCount changes from Firestore for shop '$shopId'.")
            pulledCount
        }
    }

    suspend fun restoreUserDataFromFirestore(userId: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            Log.i(TAG, "--> Restoring user data from Firestore for userId: '$userId'")

            var shopId: String? = null
            var userRole = Role.STAFF

            val userDoc = runCatching {
                withTimeout(NETWORK_TIMEOUT_MS) {
                    firestore.collection("users").document(userId).get().await()
                }
            }.getOrNull()

            if (userDoc != null && userDoc.exists()) {
                val userMap = userDoc.data ?: emptyMap()

                // Single-Device Session Enforcement: Check active_session_id
                val remoteSessionId = userDoc.getString("active_session_id") ?: userDoc.getString("activeSessionId")
                val localUser = db.userDao().getPrimaryUser()
                if (!remoteSessionId.isNullOrBlank() && localUser != null && !localUser.activeSessionId.isNullOrBlank() && remoteSessionId != localUser.activeSessionId) {
                    Log.w(TAG, "Session mismatch detected: Remote ($remoteSessionId) != Local (${localUser.activeSessionId}). Logged in on another device.")
                    FirebaseAuthManager().signOut()
                    db.clearAllTables()
                    return@runCatching ""
                }

                applyFirestoreDocToRoom("users", userMap)
                shopId = userDoc.getString("shop_id") ?: userDoc.getString("shopId")
            }

            if (shopId.isNullOrBlank()) {
                val shopQuery1 = runCatching {
                    withTimeout(NETWORK_TIMEOUT_MS) {
                        firestore.collection("shops")
                            .whereEqualTo("owner_user_id", userId)
                            .get()
                            .await()
                    }
                }.getOrNull()

                if (shopQuery1 != null && !shopQuery1.isEmpty) {
                    val shopDoc = shopQuery1.documents.first()
                    shopId = shopDoc.id
                    userRole = Role.OWNER
                    applyFirestoreDocToRoom("shops", shopDoc.data ?: emptyMap())
                } else {
                    val shopQuery2 = runCatching {
                        withTimeout(NETWORK_TIMEOUT_MS) {
                            firestore.collection("shops")
                                .whereEqualTo("ownerUserId", userId)
                                .get()
                                .await()
                        }
                    }.getOrNull()

                    if (shopQuery2 != null && !shopQuery2.isEmpty) {
                        val shopDoc = shopQuery2.documents.first()
                        shopId = shopDoc.id
                        userRole = Role.OWNER
                        applyFirestoreDocToRoom("shops", shopDoc.data ?: emptyMap())
                    }
                }
            }

            if (shopId.isNullOrBlank()) {
                runCatching {
                    val memberQuery = withTimeout(NETWORK_TIMEOUT_MS) {
                        firestore.collectionGroup("members")
                            .whereEqualTo("user_id", userId)
                            .get()
                            .await()
                    }

                    if (!memberQuery.isEmpty) {
                        val memberDoc = memberQuery.documents.first()
                        val pathSegments = memberDoc.reference.path.split("/")
                        if (pathSegments.size >= 2 && pathSegments[0] == "shops") {
                            shopId = pathSegments[1]
                        }
                    }
                }
            }

            if (shopId.isNullOrBlank()) {
                Log.w(TAG, "No shop ID found for restore, skipping restore step.")
                return@runCatching ""
            }

            // Query member document directly for verified role
            val memberDoc = runCatching {
                withTimeout(NETWORK_TIMEOUT_MS) {
                    firestore.collection("shops").document(shopId).collection("members").document(userId).get().await()
                }
            }.getOrNull()

            if (memberDoc != null && memberDoc.exists()) {
                val roleStr = memberDoc.getString("role")
                if (!roleStr.isNullOrBlank()) {
                    userRole = Role.fromStr(roleStr)
                }
            }

            Log.i(TAG, "Restoring data for shopId '$shopId' from Firestore...")

            val shopDoc = runCatching {
                withTimeout(NETWORK_TIMEOUT_MS) {
                    firestore.collection("shops").document(shopId).get().await()
                }
            }.getOrNull()

            if (shopDoc != null && shopDoc.exists()) {
                applyFirestoreDocToRoom("shops", shopDoc.data ?: emptyMap())
            }

            val existingMeta = db.appMetaDao().getAppMeta()
            val deviceCode = if (existingMeta?.deviceCode.isNullOrBlank() || existingMeta?.deviceCode == "HC01") {
                "DEV" + (10..99).random().toString()
            } else {
                existingMeta.deviceCode
            }

            val meta = existingMeta ?: AppMetaEntity(
                id = 1,
                deviceCode = deviceCode,
                deviceId = UUID.randomUUID().toString()
            )
            db.appMetaDao().insertOrUpdate(meta.copy(activeShopId = shopId, deviceCode = deviceCode))

            pullChanges(shopId, userRole)

            Log.i(TAG, "<-- Restored user data & shop '$shopId' successfully from Firestore!")
            shopId
        }
    }

    private suspend fun purgeLocalVaultRow(entityType: String, entityId: String) {
        runCatching {
            when (entityType.lowercase().trim()) {
                "parties", "party" -> db.partyDao().getPartyById(entityId)?.let { db.partyDao().insertParty(it.copy(deletedAt = System.currentTimeMillis())) }
                "payments", "payment" -> db.paymentDao().deletePayment(entityId)
                "expenses", "expense" -> db.expenseDao().deleteExpense(entityId)
                "txns", "txn" -> {
                    db.txnDao().getTxnById(entityId)?.let { db.txnDao().insertTxn(it.copy(deletedAt = System.currentTimeMillis())) }
                    db.txnDao().deleteTxnLinesByTxnId(entityId)
                }
            }
            Log.d(TAG, "Purged local staff vault row for entity '$entityType', ID '$entityId'")
        }
    }

    private suspend fun applyFirestoreDocToRoom(colName: String, data: Map<String, Any?>) {
        val json = gson.toJson(data)
        when (colName.lowercase().trim()) {
            "users", "user" -> {
                val obj = gson.fromJson(json, UserEntity::class.java)
                db.userDao().insertUser(obj)
            }
            "shops", "shop" -> {
                val obj = gson.fromJson(json, ShopEntity::class.java)
                db.shopDao().insertShop(obj)
            }
            "categories", "category" -> {
                val obj = gson.fromJson(json, CategoryEntity::class.java)
                db.categoryDao().insertCategories(listOf(obj))
            }
            "parties", "party" -> {
                val obj = gson.fromJson(json, PartyEntity::class.java)
                db.partyDao().insertParty(obj)
            }
            "stock_items", "stockitem", "stock" -> {
                val obj = gson.fromJson(json, StockItemEntity::class.java)
                db.stockItemDao().insertStockItem(obj)
            }
            "txns", "txn" -> {
                val obj = gson.fromJson(json, TxnEntity::class.java)
                db.txnDao().insertTxn(obj)
            }
            "txn_lines", "txnline", "line" -> {
                val obj = gson.fromJson(json, TxnLineEntity::class.java)
                db.txnDao().insertTxnLines(listOf(obj))
            }
            "payments", "payment" -> {
                val obj = gson.fromJson(json, PaymentEntity::class.java)
                db.paymentDao().insertPayment(obj)
            }
            "payment_promises", "paymentpromise", "promise" -> {
                val obj = gson.fromJson(json, PaymentPromiseEntity::class.java)
                db.paymentPromiseDao().insertPromise(obj)
            }
            "expenses", "expense" -> {
                val obj = gson.fromJson(json, ExpenseEntity::class.java)
                db.expenseDao().insertExpense(obj)
            }
        }
    }

    private fun getCollectionName(entityType: String): String {
        return when (entityType.lowercase().trim()) {
            "stock_items", "stockitem", "stock" -> "stock_items"
            "txn_lines", "txnline", "line" -> "txn_lines"
            "payment_promises", "paymentpromise", "promise" -> "payment_promises"
            "payment_accounts", "paymentaccount" -> "payment_accounts"
            "shop_members", "shopmember", "member" -> "members"
            "parties", "party" -> "parties"
            "categories", "category" -> "categories"
            "txns", "txn" -> "txns"
            "payments", "payment" -> "payments"
            "expenses", "expense" -> "expenses"
            "attachments", "attachment" -> "attachments"
            else -> entityType
        }
    }

    private fun parseLongTimestamp(valObj: Any?): Long {
        return when (valObj) {
            is Number -> valObj.toLong()
            is Timestamp -> valObj.toDate().time
            is String -> valObj.toLongOrNull() ?: 0L
            else -> 0L
        }
    }
}
