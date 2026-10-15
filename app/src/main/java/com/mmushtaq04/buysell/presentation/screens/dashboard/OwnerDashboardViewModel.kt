package com.mmushtaq04.buysell.presentation.screens.dashboard

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mmushtaq04.buysell.data.local.AppDatabase
import com.mmushtaq04.buysell.data.local.entity.StockItemEntity
import com.mmushtaq04.buysell.data.local.entity.TxnEntity
import com.mmushtaq04.buysell.data.local.entity.TxnLineEntity
import com.mmushtaq04.buysell.data.local.enums.ItemStatus
import com.mmushtaq04.buysell.data.local.enums.TxnType
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

enum class TimeRange {
    TODAY,
    THIS_MONTH,
    THIS_YEAR,
    ALL_TIME
}

data class ChartBarData(
    val label: String,
    val revenueRs: Long,
    val profitRs: Long
)

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
    val dailyExpenseRs: Long = 0L,
    val monthlyExpenseRs: Long = 0L,
    val yearlyExpenseRs: Long = 0L,
    val trendChartData: List<ChartBarData> = emptyList(),
    val capitalInStockRs: Long = 0L,
    val slowStockCount: Int = 0,
    val slowStockValueRs: Long = 0L,
    val topSellingModels: List<TopSellingModel> = emptyList(),
    val missingCostCount: Int = 0,
    val hasMissingCosts: Boolean = false,
    val isLoading: Boolean = false
)

class OwnerDashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "OwnerDashboardVM"
    private val db = AppDatabase.getInstance(application)

    private val _selectedTimeRange = MutableStateFlow(TimeRange.THIS_MONTH)

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        observeMetricsStream()
    }

    private fun observeMetricsStream() {
        viewModelScope.launch {
            val user = db.userDao().getPrimaryUser()
            val meta = db.appMetaDao().getAppMeta()
            val activeShopId = meta?.activeShopId?.ifBlank { null } ?: user?.shopId ?: ""

            if (activeShopId.isBlank()) return@launch

            combine(
                db.txnDao().observeTxns(activeShopId),
                db.expenseDao().observeExpenses(activeShopId),
                db.stockItemDao().observeInStockItems(activeShopId),
                _selectedTimeRange
            ) { _, _, _, range ->
                computeMetricsForRange(activeShopId, range)
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun setTimeRange(range: TimeRange) {
        _selectedTimeRange.value = range
    }

    fun loadMetrics(range: TimeRange = _selectedTimeRange.value) {
        _selectedTimeRange.value = range
    }

    private suspend fun computeMetricsForRange(activeShopId: String, range: TimeRange): DashboardUiState {
        val now = System.currentTimeMillis()
        val (startTimeMs, endTimeMs) = getTimeRangeBounds(range, now)

        // Today's range
        val (todayStart, todayEnd) = getTimeRangeBounds(TimeRange.TODAY, now)
        val todayCount = db.txnDao().getTodaySalesCount(activeShopId, todayStart, todayEnd)
        val todayPaisa = db.txnDao().getTodaySalesAmountPaisa(activeShopId, todayStart, todayEnd)

        // Expense Breakdown
        val (monthStart, monthEnd) = getTimeRangeBounds(TimeRange.THIS_MONTH, now)
        val (yearStart, yearEnd) = getTimeRangeBounds(TimeRange.THIS_YEAR, now)

        val dailyExpensePaisa = db.expenseDao().getTotalExpensesPaisa(activeShopId, todayStart, todayEnd)
        val monthlyExpensePaisa = db.expenseDao().getTotalExpensesPaisa(activeShopId, monthStart, monthEnd)
        val yearlyExpensePaisa = db.expenseDao().getTotalExpensesPaisa(activeShopId, yearStart, yearEnd)

        // Pre-fetch all lines and stock items into in-memory maps
        val allTxns = db.txnDao().getTxnsInTimeRange(activeShopId, startTimeMs, endTimeMs)
        val allLines = db.txnDao().getAllTxnLines(activeShopId)
        val lineMapByTxn = allLines.groupBy { it.txnId }
        val linePriceMap = allLines.associate { it.id to it.unitPrice }

        val allStock = db.stockItemDao().getAllStockItems(activeShopId)
        val stockMap = allStock.associateBy { it.id }

        var grossRevenuePaisa = 0L
        var returnsRefundsPaisa = 0L
        var netCogsPaisa = 0L
        var missingCostCount = 0

        val modelMap = mutableMapOf<String, ModelStat>()

        for (txn in allTxns) {
            when (txn.type) {
                TxnType.SALE -> {
                    grossRevenuePaisa += txn.totalAmount
                    val lines = lineMapByTxn[txn.id] ?: emptyList()
                    for (line in lines) {
                        val stockItem = stockMap[line.stockItemId]
                        val purchaseUnitPrice = stockItem?.purchaseLineId?.let { linePriceMap[it] }

                        val unitCost = if (purchaseUnitPrice != null) {
                            purchaseUnitPrice
                        } else {
                            missingCostCount++
                            0L
                        }

                        val lineCogs = unitCost * line.quantity
                        netCogsPaisa += lineCogs

                        val modelName = if (stockItem != null) {
                            "${stockItem.brand} ${stockItem.model}".trim()
                        } else {
                            "Device Item"
                        }

                        val lineRevenue = line.lineTotal
                        val lineProfit = lineRevenue - lineCogs

                        val stat = modelMap.getOrPut(modelName) { ModelStat() }
                        stat.unitsSold += line.quantity
                        stat.revenuePaisa += lineRevenue
                        stat.profitPaisa += lineProfit
                    }
                }
                TxnType.SALE_RETURN -> {
                    returnsRefundsPaisa += txn.totalAmount
                    val lines = lineMapByTxn[txn.id] ?: emptyList()
                    for (line in lines) {
                        val stockItem = stockMap[line.stockItemId]
                        val purchaseUnitPrice = stockItem?.purchaseLineId?.let { linePriceMap[it] }
                        if (purchaseUnitPrice != null) {
                            netCogsPaisa -= (purchaseUnitPrice * line.quantity)
                        }
                    }
                }
                else -> {}
            }
        }

        val netRevenuePaisa = (grossRevenuePaisa - returnsRefundsPaisa).coerceAtLeast(0L)
        val finalCogsPaisa = netCogsPaisa.coerceAtLeast(0L)
        val totalExpensesPaisa = db.expenseDao().getTotalExpensesPaisa(activeShopId, startTimeMs, endTimeMs)

        val netRevenueRs = netRevenuePaisa / 100
        val cogsRs = finalCogsPaisa / 100
        val expensesRs = totalExpensesPaisa / 100
        val returnsRefundsRs = returnsRefundsPaisa / 100

        val grossProfitRs = (netRevenuePaisa - finalCogsPaisa) / 100
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
        val inStockItems = allStock.filter { it.status == ItemStatus.IN_STOCK || it.remainingQty > 0 }

        var totalCapitalLockedPaisa = 0L
        for (item in inStockItems) {
            val unitCostPaisa = item.purchaseLineId?.let { linePriceMap[it] }
            if (unitCostPaisa != null) {
                totalCapitalLockedPaisa += unitCostPaisa * item.remainingQty
            } else {
                missingCostCount++
            }
        }
        val capitalLockedRs = totalCapitalLockedPaisa / 100

        // Slow Moving Stock (> 30 days)
        val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000L
        val slowItems = inStockItems.filter { (now - it.stockedAt) >= thirtyDaysMs }
        val slowCount = slowItems.sumOf { it.remainingQty }

        var totalSlowValuePaisa = 0L
        for (item in slowItems) {
            val unitCostPaisa = item.purchaseLineId?.let { linePriceMap[it] } ?: 0L
            totalSlowValuePaisa += unitCostPaisa * item.remainingQty
        }
        val slowValueRs = totalSlowValuePaisa / 100

        // Compute Trend Chart Data
        val trendData = computeTrendChartData(range, allTxns, lineMapByTxn, stockMap, linePriceMap)

        Log.i(TAG, "✓ Computed Dashboard Metrics for '$range': Revenue=$netRevenueRs, COGS=$cogsRs, Expenses=$expensesRs, NetProfit=$netProfitRs, MissingCostsCount=$missingCostCount")

        return DashboardUiState(
            timeRange = range,
            netRevenueRs = netRevenueRs,
            cogsRs = cogsRs,
            expensesRs = expensesRs,
            netProfitRs = netProfitRs,
            returnsRefundsRs = returnsRefundsRs,
            todaySalesCount = todayCount,
            todaySalesTotalRs = todayPaisa / 100,
            dailyExpenseRs = dailyExpensePaisa / 100,
            monthlyExpenseRs = monthlyExpensePaisa / 100,
            yearlyExpenseRs = yearlyExpensePaisa / 100,
            trendChartData = trendData,
            capitalInStockRs = capitalLockedRs,
            slowStockCount = slowCount,
            slowStockValueRs = slowValueRs,
            topSellingModels = topModels,
            missingCostCount = missingCostCount,
            hasMissingCosts = missingCostCount > 0,
            isLoading = false
        )
    }

    private fun computeTrendChartData(
        range: TimeRange,
        allTxns: List<TxnEntity>,
        lineMapByTxn: Map<String, List<TxnLineEntity>>,
        stockMap: Map<String, StockItemEntity>,
        linePriceMap: Map<String, Long>
    ): List<ChartBarData> {
        val labels = when (range) {
            TimeRange.TODAY -> listOf("Morning", "Noon", "Evening", "Night")
            TimeRange.THIS_MONTH -> listOf("Week 1", "Week 2", "Week 3", "Week 4")
            TimeRange.THIS_YEAR -> listOf("Q1", "Q2", "Q3", "Q4")
            TimeRange.ALL_TIME -> listOf("Phase 1", "Phase 2", "Phase 3", "Current")
        }

        if (allTxns.isEmpty()) {
            return labels.map { ChartBarData(it, 0L, 0L) }
        }

        val buckets = Array(labels.size) { Pair(0L, 0L) }

        val minTime = allTxns.minOfOrNull { it.txnDate } ?: 0L
        val maxTime = allTxns.maxOfOrNull { it.txnDate } ?: System.currentTimeMillis()
        val span = (maxTime - minTime).coerceAtLeast(1L)

        for (txn in allTxns) {
            if (txn.type == TxnType.SALE) {
                val idx = (((txn.txnDate - minTime).toDouble() / span) * (labels.size - 1))
                    .toInt()
                    .coerceIn(0, labels.size - 1)

                var txnCogs = 0L
                val lines = lineMapByTxn[txn.id] ?: emptyList()
                for (line in lines) {
                    val stockItem = stockMap[line.stockItemId]
                    val unitCost = stockItem?.purchaseLineId?.let { linePriceMap[it] } ?: 0L
                    txnCogs += unitCost * line.quantity
                }

                val current = buckets[idx]
                buckets[idx] = Pair(current.first + txn.totalAmount, current.second + txnCogs)
            }
        }

        return labels.mapIndexed { idx, label ->
            val (revPaisa, cogsPaisa) = buckets[idx]
            val revRs = revPaisa / 100
            val profitRs = (revPaisa - cogsPaisa) / 100
            ChartBarData(label = label, revenueRs = revRs, profitRs = profitRs)
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
            TimeRange.THIS_MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                start to nowMs
            }
            TimeRange.THIS_YEAR -> {
                cal.set(Calendar.DAY_OF_YEAR, 1)
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
