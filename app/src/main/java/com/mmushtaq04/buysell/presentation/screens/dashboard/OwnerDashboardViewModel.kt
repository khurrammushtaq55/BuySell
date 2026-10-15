package com.mmushtaq04.buysell.presentation.screens.dashboard

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mmushtaq04.buysell.R
import com.mmushtaq04.buysell.data.local.AppDatabase
import com.mmushtaq04.buysell.data.local.dao.PartyBalanceDto
import com.mmushtaq04.buysell.data.local.entity.StockItemEntity
import com.mmushtaq04.buysell.data.local.entity.TxnEntity
import com.mmushtaq04.buysell.data.local.entity.TxnLineEntity
import com.mmushtaq04.buysell.data.local.enums.PaymentMethod
import com.mmushtaq04.buysell.data.local.enums.TxnType
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

enum class TimeRange {
    TODAY,
    THIS_MONTH,
    THIS_YEAR,
    ALL_TIME
}

enum class TimeRangePreset {
    TODAY,
    THIS_MONTH,
    THIS_YEAR,
    ALL_TIME,
    CUSTOM
}

data class TimeRangeSelection(
    val preset: TimeRangePreset = TimeRangePreset.THIS_MONTH,
    val customStartMs: Long? = null,
    val customEndMs: Long? = null
)

enum class TopModelsSortOrder {
    UNITS_SOLD,
    PROFIT
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

data class AgedStockBucket(
    val rangeLabel: String,
    val itemCount: Int,
    val costValueRs: Long
)

data class BelowCostSaleItem(
    val modelName: String,
    val quantity: Int,
    val lossRs: Long
)

data class PeriodComparison(
    val previousRevenueRs: Long = 0L,
    val previousProfitRs: Long = 0L,
    val revenueChangePercent: Float? = null,
    val profitChangePercent: Float? = null
)

data class DashboardUiState(
    val timeRange: TimeRange = TimeRange.THIS_MONTH,
    val timeRangeSelection: TimeRangeSelection = TimeRangeSelection(),
    val topModelsSortOrder: TopModelsSortOrder = TopModelsSortOrder.UNITS_SOLD,
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
    val missingCostSoldLines: Int = 0,
    val missingCostStockItems: Int = 0,
    val profitCoveredLines: Int = 0,
    val profitExcludedLines: Int = 0,
    val totalSoldLines: Int = 0,
    val hasMissingCosts: Boolean = false,
    val receivablesRs: Long = 0L,
    val receivablesPartyCount: Int = 0,
    val payablesRs: Long = 0L,
    val payablesPartyCount: Int = 0,
    val overduePromisesRs: Long = 0L,
    val overduePromisesCount: Int = 0,
    val topDebtors: List<PartyBalanceDto> = emptyList(),
    val paymentMethodTotals: Map<PaymentMethod, Long> = emptyMap(),
    // P8 TASK T1: Insights
    val netMarginPercent: Float = 0f,
    val avgSaleValueRs: Long = 0L,
    val avgProfitPerUnitRs: Long = 0L,
    val saleTxnCount: Int = 0,
    // P8 TASK T2: Period Comparison
    val periodComparison: PeriodComparison = PeriodComparison(),
    // P8 TASK T4: Aged Stock Buckets
    val agedStockBuckets: List<AgedStockBucket> = emptyList(),
    // P8 TASK T5: Below-Cost Sales Alerts
    val belowCostSalesCount: Int = 0,
    val belowCostTotalLossRs: Long = 0L,
    val topBelowCostSales: List<BelowCostSaleItem> = emptyList(),
    val isLoading: Boolean = false
)

class OwnerDashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "OwnerDashboardVM"
    private val db = AppDatabase.getInstance(application)

    companion object {
        const val DEFAULT_SLOW_STOCK_DAYS = 30L
    }

    private val _selectedTimeRange = MutableStateFlow(TimeRangeSelection())
    private val _topModelsSortOrder = MutableStateFlow(TopModelsSortOrder.UNITS_SOLD)

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        observeMetricsStream()
    }

    @OptIn(FlowPreview::class)
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
                _selectedTimeRange,
                _topModelsSortOrder
            ) { _, _, _, rangeSelection, sortOrder ->
                rangeSelection to sortOrder
            }
                .debounce(300L)
                .collectLatest { (rangeSelection, sortOrder) ->
                    val newState = computeMetricsForSelection(activeShopId, rangeSelection, sortOrder)
                    _uiState.value = newState
                }
        }
    }

    fun setTimeRange(range: TimeRange) {
        val preset = when (range) {
            TimeRange.TODAY -> TimeRangePreset.TODAY
            TimeRange.THIS_MONTH -> TimeRangePreset.THIS_MONTH
            TimeRange.THIS_YEAR -> TimeRangePreset.THIS_YEAR
            TimeRange.ALL_TIME -> TimeRangePreset.ALL_TIME
        }
        _selectedTimeRange.value = TimeRangeSelection(preset = preset)
    }

    fun setCustomTimeRange(startMs: Long, endMs: Long) {
        if (startMs <= endMs) {
            _selectedTimeRange.value = TimeRangeSelection(
                preset = TimeRangePreset.CUSTOM,
                customStartMs = startMs,
                customEndMs = endMs
            )
        }
    }

    fun setTopModelsSortOrder(sortOrder: TopModelsSortOrder) {
        _topModelsSortOrder.value = sortOrder
    }

    private suspend fun computeMetricsForSelection(
        activeShopId: String,
        selection: TimeRangeSelection,
        sortOrder: TopModelsSortOrder
    ): DashboardUiState {
        val now = System.currentTimeMillis()
        val (startTimeMs, endTimeMs) = getTimeSelectionBounds(selection, now)

        // Today's range for daily metrics
        val (todayStart, todayEnd) = getTimeSelectionBounds(TimeRangeSelection(TimeRangePreset.TODAY), now)
        val todayCount = db.txnDao().getTodaySalesCount(activeShopId, todayStart, todayEnd)
        val todayPaisa = db.txnDao().getTodaySalesAmountPaisa(activeShopId, todayStart, todayEnd)

        // Expense Breakdown
        val (monthStart, monthEnd) = getTimeSelectionBounds(TimeRangeSelection(TimeRangePreset.THIS_MONTH), now)
        val (yearStart, yearEnd) = getTimeSelectionBounds(TimeRangeSelection(TimeRangePreset.THIS_YEAR), now)

        val dailyExpensePaisa = db.expenseDao().getTotalExpensesPaisa(activeShopId, todayStart, todayEnd)
        val monthlyExpensePaisa = db.expenseDao().getTotalExpensesPaisa(activeShopId, monthStart, monthEnd)
        val yearlyExpensePaisa = db.expenseDao().getTotalExpensesPaisa(activeShopId, yearStart, yearEnd)

        // Main range metrics calculation
        val currentMetrics = computeRangeMetricsInternal(activeShopId, startTimeMs, endTimeMs, sortOrder)

        // P8 TASK T2: Comparison metrics calculation using EXACT SAME computation function
        val prevBounds = getPreviousTimeRangeBounds(selection, startTimeMs, endTimeMs)
        val comparison = if (prevBounds != null) {
            val prevMetrics = computeRangeMetricsInternal(activeShopId, prevBounds.first, prevBounds.second, sortOrder)
            PeriodComparison(
                previousRevenueRs = prevMetrics.netRevenueRs,
                previousProfitRs = prevMetrics.netProfitRs,
                revenueChangePercent = calculateChangePercent(currentMetrics.netRevenuePaisa, prevMetrics.netRevenuePaisa),
                profitChangePercent = calculateChangePercent(currentMetrics.netProfitPaisa, prevMetrics.netProfitPaisa)
            )
        } else {
            PeriodComparison()
        }

        // P8 TASK T4: Aged Stock Buckets
        val inStockItems = db.stockItemDao().getInStockItems(activeShopId)
        val inStockPurchaseLineIds = inStockItems.mapNotNull { it.purchaseLineId }.distinct()
        val inStockPurchaseLines = fetchTxnLinesByIds(inStockPurchaseLineIds)
        val inStockPriceMap = inStockPurchaseLines.associate { it.id to it.unitPrice }

        val agedBuckets = computeAgedStockBuckets(inStockItems, inStockPriceMap, now)

        // Financial aggregates (Receivables & Payables)
        val receivablesDto = db.partyDao().getReceivablesAggregate(activeShopId)
        val payablesDto = db.partyDao().getPayablesAggregate(activeShopId)
        val topDebtorsList = db.partyDao().getTopDebtors(activeShopId)

        // Overdue promises aggregate
        val overdueDto = db.paymentPromiseDao().getOverdueReceivePromisesAggregate(activeShopId, todayStart)

        // Payment method totals for time range
        val methodTotalsList = db.paymentDao().getPaymentMethodTotalsInRange(activeShopId, startTimeMs, endTimeMs)
        val methodTotalsMap = methodTotalsList.associate { it.method to (it.totalAmountPaisa / 100) }

        val mappedTimeRange = when (selection.preset) {
            TimeRangePreset.TODAY -> TimeRange.TODAY
            TimeRangePreset.THIS_MONTH -> TimeRange.THIS_MONTH
            TimeRangePreset.THIS_YEAR -> TimeRange.THIS_YEAR
            TimeRangePreset.ALL_TIME -> TimeRange.ALL_TIME
            TimeRangePreset.CUSTOM -> TimeRange.THIS_MONTH
        }

        return DashboardUiState(
            timeRange = mappedTimeRange,
            timeRangeSelection = selection,
            topModelsSortOrder = sortOrder,
            netRevenueRs = currentMetrics.netRevenueRs,
            cogsRs = currentMetrics.cogsRs,
            expensesRs = currentMetrics.expensesRs,
            netProfitRs = currentMetrics.netProfitRs,
            returnsRefundsRs = currentMetrics.returnsRefundsRs,
            todaySalesCount = todayCount,
            todaySalesTotalRs = todayPaisa / 100,
            dailyExpenseRs = dailyExpensePaisa / 100,
            monthlyExpenseRs = monthlyExpensePaisa / 100,
            yearlyExpenseRs = yearlyExpensePaisa / 100,
            trendChartData = currentMetrics.trendChartData,
            capitalInStockRs = currentMetrics.capitalLockedRs,
            slowStockCount = currentMetrics.slowCount,
            slowStockValueRs = currentMetrics.slowValueRs,
            topSellingModels = currentMetrics.topSellingModels,
            missingCostSoldLines = currentMetrics.missingCostSoldLines,
            missingCostStockItems = currentMetrics.missingCostStockItems,
            profitCoveredLines = currentMetrics.profitCoveredLines,
            profitExcludedLines = currentMetrics.missingCostSoldLines,
            totalSoldLines = currentMetrics.totalSoldLines,
            hasMissingCosts = (currentMetrics.missingCostSoldLines + currentMetrics.missingCostStockItems) > 0,
            receivablesRs = receivablesDto.totalAmount / 100,
            receivablesPartyCount = receivablesDto.partyCount,
            payablesRs = payablesDto.totalAmount / 100,
            payablesPartyCount = payablesDto.partyCount,
            overduePromisesRs = overdueDto.totalAmount / 100,
            overduePromisesCount = overdueDto.itemCount,
            topDebtors = topDebtorsList,
            paymentMethodTotals = methodTotalsMap,
            // P8 TASK T1: Insights
            netMarginPercent = currentMetrics.netMarginPercent,
            avgSaleValueRs = currentMetrics.avgSaleValueRs,
            avgProfitPerUnitRs = currentMetrics.avgProfitPerUnitRs,
            saleTxnCount = currentMetrics.saleTxnCount,
            // P8 TASK T2: Comparison
            periodComparison = comparison,
            // P8 TASK T4: Aged Stock Buckets
            agedStockBuckets = agedBuckets,
            // P8 TASK T5: Below-cost Alerts
            belowCostSalesCount = currentMetrics.belowCostSalesCount,
            belowCostTotalLossRs = currentMetrics.belowCostTotalLossRs,
            topBelowCostSales = currentMetrics.topBelowCostSales,
            isLoading = false
        )
    }

    private class RawRangeMetrics(
        val netRevenuePaisa: Long,
        val netRevenueRs: Long,
        val cogsRs: Long,
        val expensesRs: Long,
        val netProfitPaisa: Long,
        val netProfitRs: Long,
        val returnsRefundsRs: Long,
        val saleTxnCount: Int,
        val totalUnitsSold: Int,
        val netMarginPercent: Float,
        val avgSaleValueRs: Long,
        val avgProfitPerUnitRs: Long,
        val missingCostSoldLines: Int,
        val missingCostStockItems: Int,
        val profitCoveredLines: Int,
        val totalSoldLines: Int,
        val capitalLockedRs: Long,
        val slowCount: Int,
        val slowValueRs: Long,
        val topSellingModels: List<TopSellingModel>,
        val trendChartData: List<ChartBarData>,
        val belowCostSalesCount: Int,
        val belowCostTotalLossRs: Long,
        val topBelowCostSales: List<BelowCostSaleItem>
    )

    private suspend fun computeRangeMetricsInternal(
        activeShopId: String,
        startTimeMs: Long,
        endTimeMs: Long,
        sortOrder: TopModelsSortOrder
    ): RawRangeMetrics {
        val now = System.currentTimeMillis()
        val rangeTxns = db.txnDao().getTxnsInTimeRange(activeShopId, startTimeMs, endTimeMs)
        val txnIds = rangeTxns.map { it.id }

        val rangeLines = fetchTxnLinesForTxns(txnIds)
        val lineMapByTxn = rangeLines.groupBy { it.txnId }

        val stockItemIds = rangeLines.map { it.stockItemId }.distinct()
        val referencedStockItems = fetchStockItemsByIds(stockItemIds)
        val stockMap = referencedStockItems.associateBy { it.id }

        val purchaseLineIds = referencedStockItems.mapNotNull { it.purchaseLineId }.distinct()
        val purchaseLines = fetchTxnLinesByIds(purchaseLineIds)
        val linePriceMap = purchaseLines.associate { it.id to it.unitPrice }

        var grossRevenuePaisa = 0L
        var returnsRefundsPaisa = 0L
        var netCogsPaisa = 0L

        var missingCostSoldLines = 0
        var profitCoveredLines = 0
        var totalSoldLines = 0
        var saleTxnCount = 0
        var totalUnitsSold = 0

        var belowCostSalesCount = 0
        var belowCostTotalLossPaisa = 0L
        val belowCostItemList = mutableListOf<BelowCostSaleItem>()

        val modelMap = mutableMapOf<String, ModelStat>()
        val defaultModelName = getApplication<Application>().getString(R.string.dashboard_unnamed_device)

        for (txn in rangeTxns) {
            when (txn.type) {
                TxnType.SALE -> {
                    saleTxnCount++
                    grossRevenuePaisa += txn.totalAmount
                    val lines = lineMapByTxn[txn.id] ?: emptyList()
                    for (line in lines) {
                        totalSoldLines++
                        totalUnitsSold += line.quantity
                        val stockItem = stockMap[line.stockItemId]
                        val purchaseUnitPrice = stockItem?.purchaseLineId?.let { linePriceMap[it] }

                        val modelName = if (stockItem != null && (stockItem.brand.isNotBlank() || stockItem.model.isNotBlank())) {
                            "${stockItem.brand} ${stockItem.model}".trim()
                        } else {
                            defaultModelName
                        }

                        val lineRevenue = line.lineTotal

                        if (purchaseUnitPrice != null) {
                            val lineCogs = purchaseUnitPrice * line.quantity
                            netCogsPaisa += lineCogs
                            profitCoveredLines++

                            // P8 TASK T5: Below cost sales alert check
                            if (lineRevenue < lineCogs) {
                                val lossPaisa = lineCogs - lineRevenue
                                belowCostSalesCount++
                                belowCostTotalLossPaisa += lossPaisa
                                belowCostItemList.add(
                                    BelowCostSaleItem(
                                        modelName = modelName,
                                        quantity = line.quantity,
                                        lossRs = lossPaisa / 100
                                    )
                                )
                            }

                            val lineProfit = lineRevenue - lineCogs
                            val stat = modelMap.getOrPut(modelName) { ModelStat() }
                            stat.unitsSold += line.quantity
                            stat.revenuePaisa += lineRevenue
                            stat.profitPaisa += lineProfit
                        } else {
                            missingCostSoldLines++
                            val stat = modelMap.getOrPut(modelName) { ModelStat() }
                            stat.unitsSold += line.quantity
                            stat.revenuePaisa += lineRevenue
                        }
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

        val netRevenuePaisa = grossRevenuePaisa - returnsRefundsPaisa
        val finalCogsPaisa = netCogsPaisa
        val totalExpensesPaisa = db.expenseDao().getTotalExpensesPaisa(activeShopId, startTimeMs, endTimeMs)

        val netRevenueRs = netRevenuePaisa / 100
        val cogsRs = finalCogsPaisa / 100
        val expensesRs = totalExpensesPaisa / 100
        val returnsRefundsRs = returnsRefundsPaisa / 100

        val grossProfitPaisa = netRevenuePaisa - finalCogsPaisa
        val netProfitPaisa = grossProfitPaisa - totalExpensesPaisa

        val netProfitRs = netProfitPaisa / 100

        // P8 TASK T1: Margins and averages with zero guards
        val netMarginPercent = if (netRevenuePaisa > 0) {
            (netProfitPaisa.toFloat() / netRevenuePaisa.toFloat()) * 100f
        } else {
            0f
        }

        val avgSaleValueRs = if (saleTxnCount > 0) {
            (netRevenuePaisa / saleTxnCount) / 100
        } else {
            0L
        }

        val avgProfitPerUnitRs = if (totalUnitsSold > 0) {
            netProfitRs / totalUnitsSold
        } else {
            0L
        }

        // Top Selling Models
        val topModels = modelMap.entries
            .sortedWith(
                if (sortOrder == TopModelsSortOrder.PROFIT) {
                    compareByDescending<Map.Entry<String, ModelStat>> { it.value.profitPaisa }
                        .thenByDescending { it.value.unitsSold }
                } else {
                    compareByDescending<Map.Entry<String, ModelStat>> { it.value.unitsSold }
                        .thenByDescending { it.value.profitPaisa }
                }
            )
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
        val inStockItems = db.stockItemDao().getInStockItems(activeShopId)
        val inStockPurchaseLineIds = inStockItems.mapNotNull { it.purchaseLineId }.distinct()
        val inStockPurchaseLines = fetchTxnLinesByIds(inStockPurchaseLineIds)
        val inStockPriceMap = inStockPurchaseLines.associate { it.id to it.unitPrice }

        var totalCapitalLockedPaisa = 0L
        var missingCostStockItems = 0

        for (item in inStockItems) {
            val unitCostPaisa = item.purchaseLineId?.let { inStockPriceMap[it] }
            if (unitCostPaisa != null) {
                totalCapitalLockedPaisa += unitCostPaisa * item.remainingQty
            } else {
                missingCostStockItems++
            }
        }
        val capitalLockedRs = totalCapitalLockedPaisa / 100

        // Slow Moving Stock (> 30 days)
        val slowStockMs = DEFAULT_SLOW_STOCK_DAYS * 24 * 60 * 60 * 1000L
        val slowItems = inStockItems.filter { (now - it.stockedAt) >= slowStockMs }
        val slowCount = slowItems.sumOf { it.remainingQty }

        var totalSlowValuePaisa = 0L
        for (item in slowItems) {
            val unitCostPaisa = item.purchaseLineId?.let { inStockPriceMap[it] } ?: 0L
            totalSlowValuePaisa += unitCostPaisa * item.remainingQty
        }
        val slowValueRs = totalSlowValuePaisa / 100

        val trendData = computeTrendChartData(TimeRange.THIS_MONTH, rangeTxns, lineMapByTxn, stockMap, linePriceMap)

        val topBelowCost = belowCostItemList.sortedByDescending { it.lossRs }.take(5)

        return RawRangeMetrics(
            netRevenuePaisa = netRevenuePaisa,
            netRevenueRs = netRevenueRs,
            cogsRs = cogsRs,
            expensesRs = expensesRs,
            netProfitPaisa = netProfitPaisa,
            netProfitRs = netProfitRs,
            returnsRefundsRs = returnsRefundsRs,
            saleTxnCount = saleTxnCount,
            totalUnitsSold = totalUnitsSold,
            netMarginPercent = netMarginPercent,
            avgSaleValueRs = avgSaleValueRs,
            avgProfitPerUnitRs = avgProfitPerUnitRs,
            missingCostSoldLines = missingCostSoldLines,
            missingCostStockItems = missingCostStockItems,
            profitCoveredLines = profitCoveredLines,
            totalSoldLines = totalSoldLines,
            capitalLockedRs = capitalLockedRs,
            slowCount = slowCount,
            slowValueRs = slowValueRs,
            topSellingModels = topModels,
            trendChartData = trendData,
            belowCostSalesCount = belowCostSalesCount,
            belowCostTotalLossRs = belowCostTotalLossPaisa / 100,
            topBelowCostSales = topBelowCost
        )
    }

    private fun computeAgedStockBuckets(
        inStockItems: List<StockItemEntity>,
        inStockPriceMap: Map<String, Long>,
        nowMs: Long
    ): List<AgedStockBucket> {
        var count0_30 = 0
        var value0_30Paisa = 0L
        var count31_60 = 0
        var value31_60Paisa = 0L
        var count61_90 = 0
        var value61_90Paisa = 0L
        var count90_plus = 0
        var value90_plusPaisa = 0L

        for (item in inStockItems) {
            val ageDays = ((nowMs - item.stockedAt) / (24 * 60 * 60 * 1000L)).toInt()
            val unitCost = item.purchaseLineId?.let { inStockPriceMap[it] } ?: 0L
            val totalCostPaisa = unitCost * item.remainingQty

            when {
                ageDays <= 30 -> {
                    count0_30 += item.remainingQty
                    value0_30Paisa += totalCostPaisa
                }
                ageDays in 31..60 -> {
                    count31_60 += item.remainingQty
                    value31_60Paisa += totalCostPaisa
                }
                ageDays in 61..90 -> {
                    count61_90 += item.remainingQty
                    value61_90Paisa += totalCostPaisa
                }
                else -> {
                    count90_plus += item.remainingQty
                    value90_plusPaisa += totalCostPaisa
                }
            }
        }

        val context = getApplication<Application>()
        return listOf(
            AgedStockBucket(context.getString(R.string.dashboard_aged_0_30), count0_30, value0_30Paisa / 100),
            AgedStockBucket(context.getString(R.string.dashboard_aged_31_60), count31_60, value31_60Paisa / 100),
            AgedStockBucket(context.getString(R.string.dashboard_aged_61_90), count61_90, value61_90Paisa / 100),
            AgedStockBucket(context.getString(R.string.dashboard_aged_90_plus), count90_plus, value90_plusPaisa / 100)
        )
    }

    private fun getPreviousTimeRangeBounds(
        selection: TimeRangeSelection,
        currentStartMs: Long,
        currentEndMs: Long
    ): Pair<Long, Long>? {
        return when (selection.preset) {
            TimeRangePreset.TODAY -> {
                val cal = Calendar.getInstance().apply { timeInMillis = currentStartMs }
                cal.add(Calendar.DAY_OF_MONTH, -1)
                val prevStart = cal.timeInMillis
                cal.add(Calendar.DAY_OF_MONTH, 1)
                val prevEnd = cal.timeInMillis - 1
                prevStart to prevEnd
            }
            TimeRangePreset.THIS_MONTH -> {
                val calStart = Calendar.getInstance().apply { timeInMillis = currentStartMs }
                calStart.add(Calendar.MONTH, -1)
                val prevStart = calStart.timeInMillis

                val calEnd = Calendar.getInstance().apply { timeInMillis = currentEndMs }
                calEnd.add(Calendar.MONTH, -1)
                val prevEnd = calEnd.timeInMillis
                prevStart to prevEnd
            }
            TimeRangePreset.THIS_YEAR -> {
                val calStart = Calendar.getInstance().apply { timeInMillis = currentStartMs }
                calStart.add(Calendar.YEAR, -1)
                val prevStart = calStart.timeInMillis

                val calEnd = Calendar.getInstance().apply { timeInMillis = currentEndMs }
                calEnd.add(Calendar.YEAR, -1)
                val prevEnd = calEnd.timeInMillis
                prevStart to prevEnd
            }
            TimeRangePreset.CUSTOM -> {
                val duration = (currentEndMs - currentStartMs).coerceAtLeast(1L)
                val prevStart = currentStartMs - duration
                val prevEnd = currentStartMs - 1L
                prevStart to prevEnd
            }
            TimeRangePreset.ALL_TIME -> null
        }
    }

    private fun calculateChangePercent(current: Long, previous: Long): Float? {
        if (previous == 0L) {
            return if (current > 0L) 100f else if (current < 0L) -100f else 0f
        }
        return ((current.toFloat() - previous.toFloat()) / kotlin.math.abs(previous.toFloat())) * 100f
    }

    private suspend fun fetchTxnLinesForTxns(txnIds: List<String>): List<TxnLineEntity> {
        if (txnIds.isEmpty()) return emptyList()
        val results = mutableListOf<TxnLineEntity>()
        txnIds.chunked(800).forEach { chunk ->
            results.addAll(db.txnDao().getTxnLinesForTxnIds(chunk))
        }
        return results
    }

    private suspend fun fetchStockItemsByIds(ids: List<String>): List<StockItemEntity> {
        if (ids.isEmpty()) return emptyList()
        val results = mutableListOf<StockItemEntity>()
        ids.chunked(800).forEach { chunk ->
            results.addAll(db.stockItemDao().getStockItemsByIds(chunk))
        }
        return results
    }

    private suspend fun fetchTxnLinesByIds(ids: List<String>): List<TxnLineEntity> {
        if (ids.isEmpty()) return emptyList()
        val results = mutableListOf<TxnLineEntity>()
        ids.chunked(800).forEach { chunk ->
            results.addAll(db.txnDao().getTxnLinesByIds(chunk))
        }
        return results
    }

    private fun computeTrendChartData(
        range: TimeRange,
        allTxns: List<TxnEntity>,
        lineMapByTxn: Map<String, List<TxnLineEntity>>,
        stockMap: Map<String, StockItemEntity>,
        linePriceMap: Map<String, Long>
    ): List<ChartBarData> {
        val labels = listOf("W1", "W2", "W3", "W4", "W5")

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

    private fun getTimeSelectionBounds(selection: TimeRangeSelection, nowMs: Long): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMs }

        return when (selection.preset) {
            TimeRangePreset.TODAY -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.add(Calendar.DAY_OF_MONTH, 1)
                val end = cal.timeInMillis - 1
                start to end
            }
            TimeRangePreset.THIS_MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                start to nowMs
            }
            TimeRangePreset.THIS_YEAR -> {
                cal.set(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                start to nowMs
            }
            TimeRangePreset.ALL_TIME -> {
                0L to Long.MAX_VALUE
            }
            TimeRangePreset.CUSTOM -> {
                val start = selection.customStartMs ?: 0L
                val end = selection.customEndMs ?: Long.MAX_VALUE
                start to end
            }
        }
    }

    private class ModelStat {
        var unitsSold: Int = 0
        var revenuePaisa: Long = 0L
        var profitPaisa: Long = 0L
    }
}
