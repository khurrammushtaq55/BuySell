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

data class HomeUiState(
    val shopName: String = "Mera Buy/Sell Store",
    val userRole: String = "Owner",
    val todaySalesCount: Int = 0,
    val todaySalesAmountPaisa: Long = 0L,
    val isSyncing: Boolean = false,
    val isOnline: Boolean = true
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
    }

    fun loadHomeData() {
        viewModelScope.launch {
            Log.d(TAG, "Loading home data from AppDatabase & AppMeta...")
            val meta = db.appMetaDao().getAppMeta() ?: AppMetaEntity(
                id = 1,
                deviceCode = "HC01",
                deviceId = "dev-01"
            )

            val activeShopId = meta.activeShopId ?: "default_shop"
            var localShop = db.shopDao().getShopById(activeShopId)

            if (localShop == null) {
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

            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfDay = cal.timeInMillis
            val endOfDay = startOfDay + 24 * 60 * 60 * 1000L - 1L

            val todayCount = db.txnDao().getTodaySalesCount(activeShopId, startOfDay, endOfDay)
            val todayAmount = db.txnDao().getTodaySalesAmountPaisa(activeShopId, startOfDay, endOfDay)

            val user = db.userDao().getPrimaryUser()
            val userRole = user?.role?.name ?: "Owner"
            val shopTitle = localShop?.name ?: "Mera Buy/Sell Store"

            _uiState.value = _uiState.value.copy(
                shopName = shopTitle,
                userRole = userRole,
                todaySalesCount = todayCount,
                todaySalesAmountPaisa = todayAmount
            )
        }
    }

    fun triggerSync() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true)
            val meta = db.appMetaDao().getAppMeta()
            val shopId = meta?.activeShopId ?: "default_shop"
            val user = db.userDao().getPrimaryUser()
            val role = user?.role ?: com.mmushtaq04.buysell.data.local.enums.Role.STAFF
            val sessionId = user?.activeSessionId ?: "session_active"

            runCatching {
                val syncManager = FirestoreSyncManager(db)
                syncManager.pushOutbox(shopId, role, sessionId)
                syncManager.pullChanges(shopId, role)
            }

            _uiState.value = _uiState.value.copy(isSyncing = false)
        }
    }
}
