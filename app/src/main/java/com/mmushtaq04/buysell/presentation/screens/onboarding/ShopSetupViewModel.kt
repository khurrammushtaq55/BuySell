package com.mmushtaq04.buysell.presentation.screens.onboarding

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.mmushtaq04.buysell.data.auth.FirebaseAuthManager
import com.mmushtaq04.buysell.data.local.AppDatabase
import com.mmushtaq04.buysell.data.local.CategoryPresets
import com.mmushtaq04.buysell.data.local.entity.AppMetaEntity
import com.mmushtaq04.buysell.data.local.entity.ShopEntity
import com.mmushtaq04.buysell.data.local.entity.ShopMemberEntity
import com.mmushtaq04.buysell.data.local.entity.SyncOutboxEntity
import com.mmushtaq04.buysell.data.local.entity.UserEntity
import com.mmushtaq04.buysell.data.local.enums.MemberStatus
import com.mmushtaq04.buysell.data.local.enums.Role
import com.mmushtaq04.buysell.data.local.enums.SyncOp
import com.mmushtaq04.buysell.data.sync.FirestoreSyncManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ShopSetupViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val authManager = FirebaseAuthManager()

    companion object {
        private const val TAG = "ShopSetupViewModel"
    }

    fun createShop(
        name: String,
        role: String,
        shopName: String,
        shopPhone: String,
        shopAddress: String,
        selectedCategories: List<String>,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            Log.d(TAG, "Creating shop '$shopName' for user '$name' as role '$role'")
            val shopId = "shop_" + UUID.randomUUID().toString()
            val shopCode = "S" + (10000..99999).random().toString()
            val deviceCode = "DEV" + (10..99).random().toString()

            val now = System.currentTimeMillis()
            val currentUserId = authManager.currentUser?.uid ?: UUID.randomUUID().toString()
            val registeredUserName = name.ifBlank { authManager.currentUser?.displayName ?: "Malik / Staff" }
            val roleEnum = Role.fromStr(role)

            val activeSessionId = "sess_" + UUID.randomUUID().toString().take(12)

            // 0. Create & Insert User Entity
            val userEntity = UserEntity(
                id = currentUserId,
                displayName = registeredUserName,
                email = authManager.currentUser?.email,
                phone = shopPhone,
                role = roleEnum,
                shopId = shopId,
                activeSessionId = activeSessionId,
                createdAt = now,
                updatedAt = now,
                createdBy = registeredUserName,
                updatedBy = registeredUserName
            )
            db.userDao().insertUser(userEntity)
            db.syncDao().enqueueOutbox(
                SyncOutboxEntity(
                    id = UUID.randomUUID().toString(),
                    entityType = "users",
                    entityId = userEntity.id,
                    op = SyncOp.UPSERT,
                    payloadJson = Gson().toJson(userEntity),
                    createdAt = now
                )
            )

            // 1. Create & Insert Shop Entity
            val shopEntity = ShopEntity(
                id = shopId,
                name = shopName.ifBlank { "Hafeez Center Store" },
                code = shopCode,
                ownerUserId = currentUserId,
                phone = shopPhone.ifBlank { "" },
                address = shopAddress.ifBlank { "" },
                createdAt = now,
                updatedAt = now,
                createdBy = registeredUserName,
                updatedBy = registeredUserName
            )
            db.shopDao().insertShop(shopEntity)
            db.syncDao().enqueueOutbox(
                SyncOutboxEntity(
                    id = UUID.randomUUID().toString(),
                    entityType = "shops",
                    entityId = shopEntity.id,
                    op = SyncOp.UPSERT,
                    payloadJson = Gson().toJson(shopEntity),
                    createdAt = now
                )
            )

            // 2. Initialize Preset Categories
            val presetCategoriesList = CategoryPresets.getPresetCategories(
                shopId = shopId,
                selectedCategoryNames = selectedCategories.toSet(),
                userId = currentUserId
            )
            db.categoryDao().insertCategories(presetCategoriesList)
            presetCategoriesList.forEach { cat ->
                db.syncDao().enqueueOutbox(
                    SyncOutboxEntity(
                        id = UUID.randomUUID().toString(),
                        entityType = "categories",
                        entityId = cat.id,
                        op = SyncOp.UPSERT,
                        payloadJson = Gson().toJson(cat),
                        createdAt = now
                    )
                )
            }

            // 3. Create & Insert Member Entity (Keyed by currentUserId for Firestore match /members/{uid})
            val memberEntity = ShopMemberEntity(
                id = currentUserId,
                shopId = shopId,
                userId = currentUserId,
                role = roleEnum,
                status = MemberStatus.ACTIVE,
                joinedAt = now,
                createdAt = now,
                updatedAt = now,
                createdBy = registeredUserName,
                updatedBy = registeredUserName
            )
            db.syncDao().enqueueOutbox(
                SyncOutboxEntity(
                    id = UUID.randomUUID().toString(),
                    entityType = "shop_members",
                    entityId = memberEntity.id,
                    op = SyncOp.UPSERT,
                    payloadJson = Gson().toJson(memberEntity),
                    createdAt = now
                )
            )

            // 4. Save AppMetaEntity with unique device code
            val existingMeta = db.appMetaDao().getAppMeta()
            val finalDeviceCode = if (existingMeta?.deviceCode.isNullOrBlank() || existingMeta.deviceCode == "HC01") {
                deviceCode
            } else {
                existingMeta.deviceCode
            }

            val meta = existingMeta ?: AppMetaEntity(
                id = 1,
                deviceCode = finalDeviceCode,
                deviceId = UUID.randomUUID().toString()
            )
            db.appMetaDao().insertOrUpdate(meta.copy(activeShopId = shopId, deviceCode = finalDeviceCode))

            // 5. Trigger Firestore Sync
            runCatching {
                FirestoreSyncManager(db).pushOutbox(shopId, roleEnum, activeSessionId)
            }

            Log.i(TAG, "✓ Shop '$shopName' ($shopId) successfully created with device code '$finalDeviceCode'!")
            onSuccess()
        }
    }

    fun joinShopWithInvite(
        inviteCode: String,
        userName: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            runCatching {
                val codeClean = inviteCode.trim().uppercase()
                if (codeClean.length != 8) {
                    onError("Sahi 8-digit invite code darj karein")
                    return@launch
                }

                Log.i(TAG, "Attempting to join shop with invite code: '$codeClean'")
                val firestore = FirebaseFirestore.getInstance()

                // 1. Fetch /invites/{codeClean}
                val inviteDoc = firestore.collection("invites").document(codeClean).get().await()
                if (!inviteDoc.exists()) {
                    onError("Yeh invite code majood nahi hai. Dukan owner se naya code lein.")
                    return@launch
                }

                val inviteData = inviteDoc.data ?: emptyMap()
                val shopId = inviteData["shop_id"] as? String ?: inviteData["shopId"] as? String
                val roleStr = inviteData["role"] as? String ?: "STAFF"
                val isUsed = (inviteData["is_used"] as? Boolean) ?: false
                val expiresAt = parseLongTimestamp(inviteData["expires_at"])

                if (shopId.isNullOrBlank()) {
                    onError("Invite code ghalat hai (Invalid shop ID).")
                    return@launch
                }

                if (isUsed) {
                    onError("Yeh invite code pehle se istemal ho chuka hai. Owner se naya code lein.")
                    return@launch
                }

                if (expiresAt > 0L && System.currentTimeMillis() > expiresAt) {
                    onError("Invite code ki 7 din ki muddat khatam ho chuki hai. Owner se naya code lein.")
                    return@launch
                }

                val currentUserId = authManager.currentUser?.uid ?: UUID.randomUUID().toString()
                val registeredUserName = userName.ifBlank { authManager.currentUser?.displayName ?: "Staff Member" }
                val verifiedRole = Role.fromStr(roleStr)

                // 2. Fetch Shop Document from Firestore to verify shop existence
                val shopDoc = firestore.collection("shops").document(shopId).get().await()
                val shopName = if (shopDoc.exists()) shopDoc.getString("name") ?: "Hafeez Center Store" else "Hafeez Center Store"
                val shopCode = if (shopDoc.exists()) shopDoc.getString("code") ?: ("S" + (10000..99999).random().toString()) else ("S" + (10000..99999).random().toString())

                val now = System.currentTimeMillis()

                val activeSessionId = "sess_" + UUID.randomUUID().toString().take(12)

                // 3 & 4. ATOMIC WRITE BATCH: Create Member Document + Mark Invite as Used
                val batch = firestore.batch()

                val memberRef = firestore.collection("shops").document(shopId)
                    .collection("members").document(currentUserId)
                val memberPayload = mapOf(
                    "user_id" to currentUserId,
                    "shop_id" to shopId,
                    "role" to verifiedRole.name,
                    "invite_code" to codeClean,
                    "display_name" to registeredUserName,
                    "active_session_id" to activeSessionId,
                    "joined_at" to now
                )
                batch.set(memberRef, memberPayload)

                val inviteRef = firestore.collection("invites").document(codeClean)
                val inviteUpdate = mapOf(
                    "is_used" to true,
                    "used_by" to currentUserId,
                    "used_at" to now
                )
                batch.update(inviteRef, inviteUpdate)

                // Atomically commit both operations in a single Firestore transaction
                batch.commit().await()

                // 5. Store in Local Room DB
                val userEntity = UserEntity(
                    id = currentUserId,
                    displayName = registeredUserName,
                    email = authManager.currentUser?.email,
                    role = verifiedRole,
                    shopId = shopId,
                    activeSessionId = activeSessionId,
                    createdAt = now,
                    updatedAt = now,
                    createdBy = registeredUserName,
                    updatedBy = registeredUserName
                )
                db.userDao().insertUser(userEntity)

                val shopEntity = ShopEntity(
                    id = shopId,
                    name = shopName,
                    code = shopCode,
                    ownerUserId = shopDoc.getString("owner_user_id") ?: shopDoc.getString("ownerUserId") ?: "",
                    phone = shopDoc.getString("phone") ?: "",
                    address = shopDoc.getString("address") ?: "",
                    createdAt = now,
                    updatedAt = now,
                    createdBy = registeredUserName,
                    updatedBy = registeredUserName
                )
                db.shopDao().insertShop(shopEntity)

                val deviceCode = "DEV" + (10..99).random().toString()
                val existingMeta = db.appMetaDao().getAppMeta()
                val finalDeviceCode = if (existingMeta?.deviceCode.isNullOrBlank() || existingMeta?.deviceCode == "HC01") {
                    deviceCode
                } else {
                    existingMeta.deviceCode
                }

                val meta = existingMeta ?: AppMetaEntity(
                    id = 1,
                    deviceCode = finalDeviceCode,
                    deviceId = UUID.randomUUID().toString()
                )
                db.appMetaDao().insertOrUpdate(meta.copy(activeShopId = shopId, deviceCode = finalDeviceCode))

                // 6. Download Shop's Stock, Parties & Transactions
                FirestoreSyncManager(db).restoreUserDataFromFirestore(currentUserId)

                Log.i(TAG, "✓ Successfully joined shop '$shopId' as role '$verifiedRole' via invite code '$codeClean'!")
                onSuccess()
            }.onFailure { e ->
                Log.e(TAG, "Failed to join shop with invite: ${e.localizedMessage}", e)
                onError(e.localizedMessage ?: "Dukan join karne mein msla hua. Internet connection check karein.")
            }
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
