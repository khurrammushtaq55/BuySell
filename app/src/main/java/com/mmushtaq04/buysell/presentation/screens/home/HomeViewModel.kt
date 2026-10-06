package com.mmushtaq04.buysell.presentation.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mmushtaq04.buysell.data.local.AppDatabase
import com.mmushtaq04.buysell.data.local.entity.ShopEntity
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
    val isLoading: Boolean = false
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeScreenData()
    }

    fun loadHomeScreenData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val meta = db.appMetaDao().getAppMeta()
            val activeShopId = meta?.activeShopId ?: "default_shop"

            // 1. Try reading Shop from Local Room DB first
            var localShop = db.shopDao().getShopById(activeShopId)

            // 2. If local shop not found, fetch from Cloud Firestore and cache in Room DB
            if (localShop == null) {
                runCatching {
                    val doc = firestore.collection("shops").document(activeShopId).get().await()
                    if (doc.exists()) {
                        val name = doc.getString("name") ?: "Mera Buy/Sell Store"
                        val phone = doc.getString("phone") ?: ""
                        val address = doc.getString("address") ?: ""
                        val ownerUserId = doc.getString("owner_user_id") ?: auth.currentUser?.uid ?: ""

                        val now = System.currentTimeMillis()
                        localShop = ShopEntity(
                            id = activeShopId,
                            name = name,
                            phone = phone,
                            address = address,
                            ownerUserId = ownerUserId,
                            shopId = activeShopId,
                            createdAt = now,
                            updatedAt = now,
                            createdBy = ownerUserId,
                            updatedBy = ownerUserId
                        )
                        db.shopDao().insertShop(localShop)
                    }
                }
            }

            val shopTitle = localShop?.name ?: "Mera Buy/Sell Store"

            // 3. Compute today's start and end timestamps (Karachi timezone / local midnight)
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfDayMs = cal.timeInMillis
            cal.add(Calendar.DAY_OF_MONTH, 1)
            val endOfDayMs = cal.timeInMillis - 1

            val salesCount = db.txnDao().getTodaySalesCount(activeShopId, startOfDayMs, endOfDayMs)
            val salesAmountPaisa = db.txnDao().getTodaySalesAmountPaisa(activeShopId, startOfDayMs, endOfDayMs)

            _uiState.value = HomeUiState(
                shopName = shopTitle,
                userRole = "Owner",
                todaySalesCount = salesCount,
                todaySalesAmountPaisa = salesAmountPaisa,
                isLoading = false
            )
        }
    }
}
