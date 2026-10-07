package com.mmushtaq04.buysell.presentation.screens.settings

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.mmushtaq04.buysell.data.local.AppDatabase
import com.mmushtaq04.buysell.data.local.CategoryPresets
import com.mmushtaq04.buysell.data.local.entity.CategoryEntity
import com.mmushtaq04.buysell.data.local.entity.ShopEntity
import com.mmushtaq04.buysell.data.local.entity.SyncOutboxEntity
import com.mmushtaq04.buysell.data.local.enums.SyncOp
import com.mmushtaq04.buysell.data.sync.SyncWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)

    private val _categories = MutableStateFlow<List<CategoryEntity>>(emptyList())
    val categories: StateFlow<List<CategoryEntity>> = _categories.asStateFlow()

    private val _primaryShop = MutableStateFlow<ShopEntity?>(null)
    val primaryShop: StateFlow<ShopEntity?> = _primaryShop.asStateFlow()

    companion object {
        private const val TAG = "SettingsViewModel"
    }

    init {
        loadCategories()
        observeShop()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            val meta = db.appMetaDao().getAppMeta()
            val shopId = meta?.activeShopId ?: "default_shop"

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

    private fun observeShop() {
        viewModelScope.launch {
            val meta = db.appMetaDao().getAppMeta()
            val shopId = meta?.activeShopId ?: "default_shop"
            db.shopDao().observeShopById(shopId).collect { shop ->
                _primaryShop.value = shop
            }
        }
    }

    fun updateShopProfile(name: String, phone: String, address: String) {
        viewModelScope.launch {
            val meta = db.appMetaDao().getAppMeta()
            val shopId = meta?.activeShopId ?: "default_shop"
            val existingShop = db.shopDao().getShopById(shopId)
            val now = System.currentTimeMillis()

            val updatedShop = if (existingShop != null) {
                existingShop.copy(
                    name = name,
                    phone = phone.ifBlank { null },
                    address = address.ifBlank { null },
                    updatedAt = now,
                    rev = existingShop.rev + 1L
                )
            } else {
                val user = db.userDao().getPrimaryUser()
                ShopEntity(
                    id = shopId,
                    name = name,
                    code = "SHOP01",
                    ownerUserId = user?.id ?: "",
                    phone = phone.ifBlank { null },
                    address = address.ifBlank { null },
                    createdAt = now,
                    updatedAt = now,
                    createdBy = user?.displayName ?: "Owner",
                    updatedBy = user?.displayName ?: "Owner"
                )
            }

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
            Log.i(TAG, "✓ Updated Shop Profile in Room DB & Enqueued Background Sync: ${updatedShop.name}")
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
}
