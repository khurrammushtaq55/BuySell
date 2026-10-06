package com.mmushtaq04.buysell.presentation.screens.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mmushtaq04.buysell.data.local.AppDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

data class DashboardUiState(
    val todaySalesCount: Int = 0,
    val todaySalesTotalRs: Long = 0L,
    val monthlyNetProfitRs: Long = 0L,
    val capitalInStockRs: Long = 0L,
    val slowStockCount: Int = 0,
    val isLoading: Boolean = false
)

class OwnerDashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadMetrics()
    }

    fun loadMetrics() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val meta = db.appMetaDao().getAppMeta()
            val activeShopId = meta?.activeShopId ?: "default_shop"

            // Today's range
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
            val salesPaisa = db.txnDao().getTodaySalesAmountPaisa(activeShopId, startOfDayMs, endOfDayMs)

            _uiState.value = DashboardUiState(
                todaySalesCount = salesCount,
                todaySalesTotalRs = salesPaisa / 100,
                monthlyNetProfitRs = (salesPaisa / 100) / 4, // sample calculation
                capitalInStockRs = 0L,
                slowStockCount = 0,
                isLoading = false
            )
        }
    }
}
