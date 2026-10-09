package com.mmushtaq04.buysell.presentation.screens.dashboard

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mmushtaq04.buysell.data.local.AppDatabase
import com.mmushtaq04.buysell.data.local.enums.ItemStatus
import com.mmushtaq04.buysell.data.local.enums.TxnType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*

enum class TimeRange {
    TODAY,
    THIS_WEEK,
    THIS_MONTH,
    ALL_TIME
}

data class TopSellingModel(
    val modelName: String,
    val unitsSold: Int,
    val revenueRs: Long,
    val profitRs: Long
)

data class DashboardUiState(
    val timeRange: TimeRange = TimeRange.THIS_MONTH,
    val netRevenueRs: Long = 0L,
    val cogsRs: Long = 0L,
    val expensesRs: Long = 0L,
    val netProfitRs: Long = 0L,
    val returnsRefundsRs: Long = 0L,
    val todaySalesCount: Int = 0,
    val todaySalesTotalRs: Long = 0L,
    val capitalInStockRs: Long = 0L,
    val slowStockCount: Int = 0,
    val slowStockValueRs: Long = 0L,
    val topSellingModels: List<TopSellingModel> = emptyList(),
    val isLoading: Boolean = false
)

class OwnerDashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "OwnerDashboardVM"
    private val db = AppDatabase.getInstance(application)

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadMetrics(TimeRange.THIS_MONTH)
    }

    fun setTimeRange(range: TimeRange) {
        _uiState.value = _uiState.value.copy(timeRange = range)
        loadMetrics(range)
    }

    fun loadMetrics(range: TimeRange = _uiState.value.timeRange) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val user = db.userDao().getPrimaryUser()
            val meta = db.appMetaDao().getAppMeta()
            val activeShopId = meta?.activeShopId?.ifBlank { null } ?: user?.shopId ?: ""

            if (activeShopId.isBlank()) {
                _uiState.value = _uiState.value.copy(isLoading = false)
                return@launch
            }

            val now = System.currentTimeMillis()
            val (startTimeMs, endTimeMs) = getTimeRangeBounds(range, now)

            // Today's range for Today's Sales Card
            val (todayStart, todayEnd) = getTimeRangeBounds(TimeRange.TODAY, now)
            val todayCount = db.txnDao().getTodaySalesCount(activeShopId, todayStart, todayEnd)
            val todayPaisa = db.txnDao().getTodaySalesAmountPaisa(activeShopId, todayStart, todayEnd)

            // Transactions in selected time range
            val txns = db.txnDao().getTxnsInTimeRange(activeShopId, startTimeMs, endTimeMs)

            var grossRevenuePaisa = 0L
            var returnsRefundsPaisa = 0L
            var grossCogsPaisa = 0L

            val modelMap = mutableMapOf<String, ModelStat>()

            for (txn in txns) {
                when (txn.type) {
                    TxnType.SALE -> {
                        grossRevenuePaisa += txn.totalAmount
                        val lines = db.txnDao().getTxnLinesForTxn(txn.id)
                        for (line in lines) {
                            val stockItem = db.stockItemDao().getStockItemById(line.stockItemId)
                            val unitCost = if (stockItem?.purchaseLineId != null) {
                                db.txnDao().getUnitPriceByLineId(stockItem.purchaseLineId) ?: line.unitPrice
                            } else {
                                line.unitPrice
                            }

                            val lineCogs = unitCost * line.quantity
                            grossCogsPaisa += lineCogs

                            val modelName = if (stockItem != null) {
                                "${stockItem.brand} ${stockItem.model}".trim()
                            } else {
                                "Device Item"
                            }

                            val lineRevenue = line.unitPrice * line.quantity
                            val lineProfit = lineRevenue - lineCogs

                            val stat = modelMap.getOrPut(modelName) { ModelStat() }
                            stat.unitsSold += line.quantity
                            stat.revenuePaisa += lineRevenue
                            stat.profitPaisa += lineProfit
                        }
                    }
                    TxnType.SALE_RETURN -> {
                        returnsRefundsPaisa += txn.totalAmount
                    }
                    TxnType.PURCHASE_RETURN -> {
                        // Supplier refund reduces COGS
                        grossCogsPaisa = (grossCogsPaisa - txn.totalAmount).coerceAtLeast(0L)
                    }
                    else -> {}
                }
            }

            val netRevenuePaisa = (grossRevenuePaisa - returnsRefundsPaisa).coerceAtLeast(0L)
            val totalExpensesPaisa = db.expenseDao().getTotalExpensesPaisa(activeShopId, startTimeMs, endTimeMs)

            val netRevenueRs = netRevenuePaisa / 100
            val cogsRs = grossCogsPaisa / 100
            val expensesRs = totalExpensesPaisa / 100
            val returnsRefundsRs = returnsRefundsPaisa / 100

            val grossProfitRs = (netRevenuePaisa - grossCogsPaisa) / 100
            val netProfitRs = grossProfitRs - expensesRs

            // Top Selling Models
            val topModels = modelMap.entries
                .sortedByDescending { it.value.unitsSold }
                .take(5)
                .map { (name, stat) ->
                    TopSellingModel(
                        modelName = name,
                        unitsSold = stat.unitsSold,
                        revenueRs = stat.revenuePaisa / 100,
                        profitRs = stat.profitPaisa / 100
                    )
                }

            // Capital Locked in Stock
            val allStock = db.stockItemDao().getAllStockItems(activeShopId)
            val inStockItems = allStock.filter { it.status == ItemStatus.IN_STOCK || it.remainingQty > 0 }

            var totalCapitalLockedPaisa = 0L
            for (item in inStockItems) {
                val unitCostPaisa = if (item.purchaseLineId != null) {
                    db.txnDao().getUnitPriceByLineId(item.purchaseLineId) ?: 0L
                } else 0L
                totalCapitalLockedPaisa += unitCostPaisa * item.remainingQty
            }
            val capitalLockedRs = totalCapitalLockedPaisa / 100

            // Slow Moving Stock (> 30 days)
            val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000L
            val slowItems = inStockItems.filter { (now - it.stockedAt) >= thirtyDaysMs }
            val slowCount = slowItems.sumOf { it.remainingQty }

            var totalSlowValuePaisa = 0L
            for (item in slowItems) {
                val unitCostPaisa = if (item.purchaseLineId != null) {
                    db.txnDao().getUnitPriceByLineId(item.purchaseLineId) ?: 0L
                } else 0L
                totalSlowValuePaisa += unitCostPaisa * item.remainingQty
            }
            val slowValueRs = totalSlowValuePaisa / 100

            _uiState.value = DashboardUiState(
                timeRange = range,
                netRevenueRs = netRevenueRs,
                cogsRs = cogsRs,
                expensesRs = expensesRs,
                netProfitRs = netProfitRs,
                returnsRefundsRs = returnsRefundsRs,
                todaySalesCount = todayCount,
                todaySalesTotalRs = todayPaisa / 100,
                capitalInStockRs = capitalLockedRs,
                slowStockCount = slowCount,
                slowStockValueRs = slowValueRs,
                topSellingModels = topModels,
                isLoading = false
            )

            Log.i(TAG, "✓ Computed Dashboard Metrics for range '$range': Revenue=Rs $netRevenueRs, COGS=Rs $cogsRs, Expenses=Rs $expensesRs, NetProfit=Rs $netProfitRs")
        }
    }

    private fun getTimeRangeBounds(range: TimeRange, nowMs: Long): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMs }

        return when (range) {
            TimeRange.TODAY -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.add(Calendar.DAY_OF_MONTH, 1)
                val end = cal.timeInMillis - 1
                start to end
            }
            TimeRange.THIS_WEEK -> {
                cal.add(Calendar.DAY_OF_YEAR, -7)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                start to nowMs
            }
            TimeRange.THIS_MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                start to nowMs
            }
            TimeRange.ALL_TIME -> {
                0L to Long.MAX_VALUE
            }
        }
    }

    private class ModelStat {
        var unitsSold: Int = 0
        var revenuePaisa: Long = 0L
        var profitPaisa: Long = 0L
    }
}
