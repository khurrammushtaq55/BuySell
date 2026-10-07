package com.mmushtaq04.buysell.presentation.screens.onboarding

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
            val shopId = "default_shop"
            val now = System.currentTimeMillis()
            val currentUserId = authManager.currentUser?.uid ?: UUID.randomUUID().toString()
            val registeredUserName = name.ifBlank { authManager.currentUser?.displayName ?: "Malik / Staff" }

            // 0. Create & Insert User Entity
            val userEntity = UserEntity(
                id = currentUserId,
                displayName = registeredUserName,
                email = authManager.currentUser?.email,
                phone = shopPhone,
                shopId = shopId,
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
                phone = shopPhone.ifBlank { "" },
                address = shopAddress.ifBlank { "" },
                ownerUserId = currentUserId,
                shopId = shopId,
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

            // 3. Create & Insert Member Entity
            val roleEnum = Role.fromStr(role)
            val memberEntity = ShopMemberEntity(
                id = UUID.randomUUID().toString(),
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

            // 4. Save AppMetaEntity
            val meta = db.appMetaDao().getAppMeta() ?: AppMetaEntity(
                id = 1,
                deviceCode = "HC01",
                deviceId = UUID.randomUUID().toString()
            )
            db.appMetaDao().insertOrUpdate(meta.copy(activeShopId = shopId))

            // 5. Trigger Firestore Sync
            runCatching {
                FirestoreSyncManager(db).pushOutbox(shopId, roleEnum, "session_active")
            }

            Log.i(TAG, "✓ Shop '$shopName' successfully created and initialized!")
            onSuccess()
        }
    }
}
