package com.mmushtaq04.buysell.presentation.screens.settings

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.mmushtaq04.buysell.data.local.AppDatabase
import com.mmushtaq04.buysell.data.local.entity.ShopEntity
import com.mmushtaq04.buysell.data.local.entity.SyncOutboxEntity
import com.mmushtaq04.buysell.data.local.enums.Role
import com.mmushtaq04.buysell.data.local.enums.SyncOp
import com.mmushtaq04.buysell.data.sync.FirestoreSyncManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)

    val primaryShop: StateFlow<ShopEntity?> = db.shopDao().observePrimaryShop()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    companion object {
        private const val TAG = "SettingsViewModel"
    }

    fun updateShopProfile(
        newName: String,
        newPhone: String,
        newAddress: String,
        updatedByUserName: String
    ) {
        viewModelScope.launch {
            Log.d(TAG, "Updating shop profile: Name='$newName', Phone='$newPhone'")
            val meta = db.appMetaDao().getAppMeta()
            val shopId = meta?.activeShopId ?: "default_shop"
            val existingShop = db.shopDao().getShopById(shopId)

            if (existingShop != null) {
                val now = System.currentTimeMillis()
                val updatedShop = existingShop.copy(
                    name = newName,
                    phone = newPhone,
                    address = newAddress,
                    updatedAt = now,
                    updatedBy = updatedByUserName
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
                    FirestoreSyncManager(db).pushOutbox(shopId, Role.OWNER, "session_active")
                }
                Log.i(TAG, "✓ Updated Shop Profile in Room DB & Firestore")
            }
        }
    }
}
