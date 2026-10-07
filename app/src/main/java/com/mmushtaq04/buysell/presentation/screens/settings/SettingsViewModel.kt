package com.mmushtaq04.buysell.presentation.screens.settings

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.mmushtaq04.buysell.data.local.AppDatabase
import com.mmushtaq04.buysell.data.local.entity.CategoryEntity
import com.mmushtaq04.buysell.data.local.entity.SyncOutboxEntity
import com.mmushtaq04.buysell.data.local.enums.Role
import com.mmushtaq04.buysell.data.local.enums.SyncOp
import com.mmushtaq04.buysell.data.sync.FirestoreSyncManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)

    private val _categories = MutableStateFlow<List<CategoryEntity>>(emptyList())
    val categories: StateFlow<List<CategoryEntity>> = _categories.asStateFlow()

    companion object {
        private const val TAG = "SettingsViewModel"
    }

    init {
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            val meta = db.appMetaDao().getAppMeta()
            val shopId = meta?.activeShopId ?: "default_shop"
            db.categoryDao().observeAllCategories(shopId).collect { list ->
                _categories.value = list
            }
        }
    }

    fun updateShopProfile(name: String, phone: String, address: String) {
        viewModelScope.launch {
            val meta = db.appMetaDao().getAppMeta()
            val shopId = meta?.activeShopId ?: "default_shop"
            val existingShop = db.shopDao().getShopById(shopId)
            if (existingShop != null) {
                val now = System.currentTimeMillis()
                val updatedShop = existingShop.copy(
                    name = name,
                    phone = phone.ifBlank { null },
                    address = address.ifBlank { null },
                    updatedAt = now
                )
                db.shopDao().updateShop(updatedShop)
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
                runCatching {
                    val user = db.userDao().getPrimaryUser()
                    val role = user?.role ?: Role.OWNER
                    val sessionId = user?.activeSessionId?.ifBlank { "session_active" } ?: "session_active"
                    FirestoreSyncManager(db).pushOutbox(shopId, role, sessionId)
                }
                Log.i(TAG, "✓ Updated Shop Profile in Room DB & Firestore")
            }
        }
    }

    fun toggleCategory(category: CategoryEntity) {
        viewModelScope.launch {
            val updated = category.copy(
                enabled = !category.enabled,
                updatedAt = System.currentTimeMillis()
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
            Log.d(TAG, "Toggled Category '${category.name}' enabled status to ${updated.enabled}")
        }
    }
}
