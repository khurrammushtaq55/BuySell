package com.mmushtaq04.buysell.presentation.screens.settings

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.mmushtaq04.buysell.data.auth.FirebaseAuthManager
import com.mmushtaq04.buysell.data.local.AppDatabase
import com.mmushtaq04.buysell.data.local.CategoryPresets
import com.mmushtaq04.buysell.data.local.entity.CategoryEntity
import com.mmushtaq04.buysell.data.local.entity.ShopEntity
import com.mmushtaq04.buysell.data.local.entity.SyncOutboxEntity
import com.mmushtaq04.buysell.data.local.enums.Role
import com.mmushtaq04.buysell.data.local.enums.SyncOp
import com.mmushtaq04.buysell.data.sync.FirestoreSyncManager
import com.mmushtaq04.buysell.data.sync.SyncWorker
import com.mmushtaq04.buysell.domain.InviteManager
import com.mmushtaq04.buysell.util.AppPinManager
import com.mmushtaq04.buysell.util.AppPreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)

    private val _categories = MutableStateFlow<List<CategoryEntity>>(emptyList())
    val categories: StateFlow<List<CategoryEntity>> = _categories.asStateFlow()

    private val _primaryShop = MutableStateFlow<ShopEntity?>(null)
    val primaryShop: StateFlow<ShopEntity?> = _primaryShop.asStateFlow()

    private val _currencySymbol = MutableStateFlow(AppPreferencesManager.getCurrencySymbol(application))
    val currencySymbol: StateFlow<String> = _currencySymbol.asStateFlow()

    private val _currencyCode = MutableStateFlow(AppPreferencesManager.getCurrencyCode(application))
    val currencyCode: StateFlow<String> = _currencyCode.asStateFlow()

    fun updateCurrency(symbol: String, code: String) {
        AppPreferencesManager.setCurrency(getApplication(), symbol, code)
        _currencySymbol.value = symbol
        _currencyCode.value = code
    }

    companion object {
        private const val TAG = "SettingsViewModel"
    }

    init {
        loadCategories()
        observeShop()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            val user = db.userDao().getPrimaryUser()
            val meta = db.appMetaDao().getAppMeta()
            val shopId = meta?.activeShopId?.ifBlank { null } ?: user?.shopId ?: ""

            if (shopId.isNotBlank()) {
                val existing = db.categoryDao().getCategories(shopId)
                if (existing.isEmpty()) {
                    val presets = CategoryPresets.getPresetCategories(shopId)
                    db.categoryDao().insertCategories(presets)
                }

                db.categoryDao().observeAllCategories(shopId).collect { list ->
                    _categories.value = list
                }
            }
        }
    }

    private fun observeShop() {
        viewModelScope.launch {
            val user = db.userDao().getPrimaryUser()
            val meta = db.appMetaDao().getAppMeta()
            val shopId = meta?.activeShopId?.ifBlank { null } ?: user?.shopId ?: ""
            if (shopId.isNotBlank()) {
                db.shopDao().observeShopById(shopId).collect { shop ->
                    _primaryShop.value = shop
                }
            }
        }
    }

    fun generateInviteCode(role: String, onCodeGenerated: (String) -> Unit) {
        viewModelScope.launch {
            runCatching {
                val user = db.userDao().getPrimaryUser()
                val meta = db.appMetaDao().getAppMeta()
                val activeShopId = meta?.activeShopId?.ifBlank { null } ?: user?.shopId ?: ""
                val currentUserId = user?.id ?: ""

                if (activeShopId.isBlank()) {
                    Log.e(TAG, "Cannot generate invite: activeShopId is blank")
                    return@launch
                }

                val newCode = InviteManager.generateInviteCode()
                val now = System.currentTimeMillis()
                val expiresAt = now + 7 * 24 * 60 * 60 * 1000L // 7 days

                val inviteData = mapOf(
                    "code" to newCode,
                    "shop_id" to activeShopId,
                    "role" to role,
                    "created_by" to currentUserId,
                    "created_at" to now,
                    "expires_at" to expiresAt,
                    "is_used" to false
                )

                val firestore = FirebaseFirestore.getInstance()
                firestore.collection("invites").document(newCode).set(inviteData).await()

                Log.i(TAG, "✓ Successfully generated invite code '$newCode' for shop '$activeShopId' with role '$role'")
                onCodeGenerated(newCode)
            }.onFailure { e ->
                Log.e(TAG, "Failed to generate invite code: ${e.localizedMessage}", e)
            }
        }
    }

    fun updateShopProfile(ownerName: String, shopName: String, phone: String, address: String) {
        viewModelScope.launch {
            val user = db.userDao().getPrimaryUser()
            if (user?.role != Role.OWNER) {
                Log.w(TAG, "Non-owner user (${user?.role}) attempted to update shop profile. Operation blocked.")
                return@launch
            }

            val meta = db.appMetaDao().getAppMeta()
            val shopId = meta?.activeShopId?.ifBlank { null } ?: user.shopId

            if (shopId.isBlank()) return@launch

            val now = System.currentTimeMillis()

            if (ownerName.isNotBlank() && ownerName != user.displayName) {
                val updatedUser = user.copy(
                    displayName = ownerName,
                    updatedAt = now,
                    rev = user.rev + 1L
                )
                db.userDao().insertUser(updatedUser)
                db.syncDao().enqueueOutbox(
                    SyncOutboxEntity(
                        id = UUID.randomUUID().toString(),
                        entityType = "users",
                        entityId = updatedUser.id,
                        op = SyncOp.UPSERT,
                        payloadJson = Gson().toJson(updatedUser),
                        createdAt = now
                    )
                )
            }

            val existingShop = db.shopDao().getShopById(shopId)
            val updatedShop = existingShop?.copy(
                name = shopName,
                phone = phone.ifBlank { null },
                address = address.ifBlank { null },
                updatedAt = now,
                rev = existingShop.rev + 1L
            )
                ?: ShopEntity(
                    id = shopId,
                    name = shopName,
                    code = "SHOP",
                    ownerUserId = user.id,
                    phone = phone.ifBlank { null },
                    address = address.ifBlank { null },
                    createdAt = now,
                    updatedAt = now,
                    createdBy = ownerName.ifBlank { user?.displayName ?: "Owner" },
                    updatedBy = ownerName.ifBlank { user?.displayName ?: "Owner" }
                )

            db.shopDao().insertShop(updatedShop)
            db.syncDao().enqueueOutbox(
                SyncOutboxEntity(
                    id = UUID.randomUUID().toString(),
                    entityType = "shops",
                    entityId = updatedShop.id,
                    op = SyncOp.UPSERT,
                    payloadJson = Gson().toJson(updatedShop),
                    createdAt = now
                )
            )

            SyncWorker.enqueueOneTimeSync(getApplication())
            Log.i(TAG, "✓ Updated Shop Profile & Owner Name in Room DB & Enqueued Background Sync: ${updatedShop.name}")
        }
    }

    fun toggleCategory(category: CategoryEntity) {
        viewModelScope.launch {
            val updated = category.copy(
                enabled = !category.enabled,
                updatedAt = System.currentTimeMillis(),
                rev = category.rev + 1L
            )
            db.categoryDao().updateCategory(updated)
            db.syncDao().enqueueOutbox(
                SyncOutboxEntity(
                    id = UUID.randomUUID().toString(),
                    entityType = "categories",
                    entityId = updated.id,
                    op = SyncOp.UPSERT,
                    payloadJson = Gson().toJson(updated),
                    createdAt = System.currentTimeMillis()
                )
            )
            SyncWorker.enqueueOneTimeSync(getApplication())
            Log.d(TAG, "Toggled Category '${category.name}' enabled status to ${updated.enabled}")
        }
    }

    fun handleSignOut(
        authManager: FirebaseAuthManager,
        onRequireUnsyncedWarning: (pendingCount: Int) -> Unit,
        onReadyToSignOut: () -> Unit
    ) {
        viewModelScope.launch {
            val user = db.userDao().getPrimaryUser()
            val meta = db.appMetaDao().getAppMeta()
            val shopId = meta?.activeShopId?.ifBlank { null } ?: user?.shopId ?: ""
            val role = user?.role ?: Role.STAFF
            val activeSessionId = user?.activeSessionId ?: ""

            // 1. Attempt pushing pending outbox items if shopId is present (bounded by 5-second timeout)
            if (shopId.isNotBlank()) {
                withTimeoutOrNull(5_000L) {
                    runCatching {
                        FirestoreSyncManager(db).pushOutbox(shopId, role, activeSessionId)
                    }
                }
            }

            // 2. Check remaining pending outbox items
            val pendingItems = db.syncDao().getPendingOutbox()
            if (pendingItems.isNotEmpty()) {
                Log.w(TAG, "Unsynced pending items remaining (${pendingItems.size}). Prompting user with warning.")
                onRequireUnsyncedWarning(pendingItems.size)
            } else {
                Log.i(TAG, "All outbox items synced. Proceeding with clean sign-out.")
                executeForceSignOut(authManager, onReadyToSignOut)
            }
        }
    }

    fun executeForceSignOut(
        authManager: FirebaseAuthManager,
        onReadyToSignOut: () -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            authManager.signOut()
            db.clearAllTables()
            AppPinManager.clearPin(getApplication())
            withContext(Dispatchers.Main) {
                onReadyToSignOut()
            }
        }
    }
}
