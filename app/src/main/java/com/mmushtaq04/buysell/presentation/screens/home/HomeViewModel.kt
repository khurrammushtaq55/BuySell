package com.mmushtaq04.buysell.presentation.screens.home

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.mmushtaq04.buysell.data.local.AppDatabase
import com.mmushtaq04.buysell.data.local.entity.AppMetaEntity
import com.mmushtaq04.buysell.data.local.entity.ShopEntity
import com.mmushtaq04.buysell.data.sync.FirestoreSyncManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.UUID

data class HomeUiState(
    val shopName: String = "Mera Store",
    val todaySalesCount: Int = 0,
    val todaySalesAmountPaisa: Long = 0,
    val isSyncing: Boolean = false
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val firestore = FirebaseFirestore.getInstance()

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    companion object {
        private const val TAG = "HomeViewModel"
    }

    init {
        loadHomeData()
        observeTodaySales()
    }

    fun loadHomeData() {
        viewModelScope.launch {
            Log.d(TAG, "Loading home data from AppDatabase & AppMeta...")
            val primaryUser = db.userDao().getPrimaryUser()
            val existingMeta = db.appMetaDao().getAppMeta()
            val activeShopId = existingMeta?.activeShopId?.ifBlank { null }
                ?: primaryUser?.shopId?.ifBlank { null }
                ?: ""

            val deviceCode = if (existingMeta?.deviceCode.isNullOrBlank() || existingMeta.deviceCode == "HC01") {
                "DEV" + (10..99).random().toString()
            } else {
                existingMeta.deviceCode
            }

            if (existingMeta == null || existingMeta.deviceCode != deviceCode || existingMeta.activeShopId != activeShopId) {
                val updatedMeta = (existingMeta ?: AppMetaEntity(id = 1, deviceCode = deviceCode, deviceId = UUID.randomUUID().toString()))
                    .copy(activeShopId = activeShopId, deviceCode = deviceCode)
                db.appMetaDao().insertOrUpdate(updatedMeta)
            }

            var localShop = if (activeShopId.isNotBlank()) db.shopDao().getShopById(activeShopId) else null

            if (localShop == null && activeShopId.isNotBlank()) {
                runCatching {
                    val shopDoc = firestore.collection("shops").document(activeShopId).get().await()
                    if (shopDoc.exists()) {
                        val name = shopDoc.getString("name") ?: "Mera Buy/Sell Store"
                        val phone = shopDoc.getString("phone")
                        val address = shopDoc.getString("address")
                        val ownerUserId = shopDoc.getString("owner_user_id") ?: ""

                        val now = System.currentTimeMillis()
                        localShop = ShopEntity(
                            id = activeShopId,
                            name = name,
                            code = "SHOP",
                            ownerUserId = ownerUserId,
                            phone = phone,
                            address = address,
                            createdAt = now,
                            updatedAt = now,
                            createdBy = ownerUserId,
                            updatedBy = ownerUserId
                        )
                        db.shopDao().insertShop(localShop)
                    }
                }
            }

            _uiState.value = _uiState.value.copy(
                shopName = localShop?.name ?: "Mera Store"
            )
        }
    }

    private fun observeTodaySales() {
        viewModelScope.launch {
            val primaryUser = db.userDao().getPrimaryUser()
            val existingMeta = db.appMetaDao().getAppMeta()
            val activeShopId = existingMeta?.activeShopId?.ifBlank { null }
                ?: primaryUser?.shopId?.ifBlank { null }
                ?: ""

            if (activeShopId.isBlank()) return@launch

            db.txnDao().observeTxns(activeShopId).collect {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val startOfDay = cal.timeInMillis
                val endOfDay = startOfDay + 24 * 60 * 60 * 1000L - 1L

                val salesCount = db.txnDao().getTodaySalesCount(activeShopId, startOfDay, endOfDay)
                val salesAmountPaisa = db.txnDao().getTodaySalesAmountPaisa(activeShopId, startOfDay, endOfDay)

                _uiState.value = _uiState.value.copy(
                    todaySalesCount = salesCount,
                    todaySalesAmountPaisa = salesAmountPaisa
                )
            }
        }
    }

    fun triggerSync() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true)
            val primaryUser = db.userDao().getPrimaryUser()
            val meta = db.appMetaDao().getAppMeta()
            val shopId = meta?.activeShopId?.ifBlank { null } ?: primaryUser?.shopId ?: ""
            val role = primaryUser?.role ?: com.mmushtaq04.buysell.data.local.enums.Role.STAFF
            val sessionId = primaryUser?.activeSessionId?.ifBlank { null } ?: ("sess_" + UUID.randomUUID().toString().take(12))

            if (shopId.isNotBlank()) {
                runCatching {
                    val syncManager = FirestoreSyncManager(db)
                    syncManager.pushOutbox(shopId, role, sessionId)
                    syncManager.pullChanges(shopId, role)
                }
            }

            _uiState.value = _uiState.value.copy(isSyncing = false)
        }
    }
}
