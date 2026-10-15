package com.mmushtaq04.buysell.presentation.screens.dashboard

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.AssignmentReturn
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mmushtaq04.buysell.R
import com.mmushtaq04.buysell.presentation.components.PremiumFeatureGate
import com.mmushtaq04.buysell.presentation.components.PremiumPaywallDialog

@Composable
fun OwnerDashboardScreen(
    viewModel: OwnerDashboardViewModel = viewModel(),
    isAdvancedDashboardLocked: Boolean = false,
    isInventoryInsightsLocked: Boolean = false,
    onNavigateBack: () -> Unit = {},
    onNavigateToExpenses: () -> Unit = {},
    onNavigateToPartyList: () -> Unit = {},
    onUnlockClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    OwnerDashboardContent(
        uiState = uiState,
        isAdvancedDashboardLocked = isAdvancedDashboardLocked,
        isInventoryInsightsLocked = isInventoryInsightsLocked,
        onTimeRangeSelected = { viewModel.setTimeRange(it) },
        onCustomTimeRangeSelected = { start, end -> viewModel.setCustomTimeRange(start, end) },
        onSortOrderChanged = { viewModel.setTopModelsSortOrder(it) },
        onNavigateBack = onNavigateBack,
        onNavigateToExpenses = onNavigateToExpenses,
        onNavigateToPartyList = onNavigateToPartyList,
        onUnlockClick = onUnlockClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerDashboardContent(
    uiState: DashboardUiState,
    isAdvancedDashboardLocked: Boolean = false,
    isInventoryInsightsLocked: Boolean = false,
    onTimeRangeSelected: (TimeRange) -> Unit = {},
    onCustomTimeRangeSelected: (Long, Long) -> Unit = { _, _ -> },
    onSortOrderChanged: (TopModelsSortOrder) -> Unit = {},
    onNavigateBack: () -> Unit = {},
    onNavigateToExpenses: () -> Unit = {},
    onNavigateToPartyList: () -> Unit = {},
    onUnlockClick: () -> Unit = {}
) {
    val context = LocalContext.current

    var showHelpDialogTitle by remember { mutableStateOf<String?>(null) }
    var showHelpDialogMsg by remember { mutableStateOf<String?>(null) }
    var showPaywallDialog by remember { mutableStateOf(false) }
    var showDateRangePicker by remember { mutableStateOf(false) }

    if (showPaywallDialog) {
        PremiumPaywallDialog(
            onDismiss = { showPaywallDialog = false },
            onUpgradeClick = { showPaywallDialog = false },
            onRestoreClick = { showPaywallDialog = false },
            onRedeemPromoCode = { _, onSuccess, _ ->
                showPaywallDialog = false
                onSuccess()
            }
        )
    }

    if (showDateRangePicker) {
        CustomDateRangePickerDialog(
            onDismiss = { showDateRangePicker = false },
            onDateRangeSelected = { start, end ->
                showDateRangePicker = false
                onCustomTimeRangeSelected(start, end)
            }
        )
    }

    if (showHelpDialogTitle != null && showHelpDialogMsg != null) {
        AlertDialog(
            onDismissRequest = {
                showHelpDialogTitle = null
                showHelpDialogMsg = null
            },
            title = { Text(showHelpDialogTitle!!) },
            text = { Text(showHelpDialogMsg!!) },
            confirmButton = {
                TextButton(onClick = {
                    showHelpDialogTitle = null
                    showHelpDialogMsg = null
                }) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.dashboard_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // P8 TASK T6: Share Summary Button (Free)
                    IconButton(onClick = {
                        shareDashboardSummary(context, "Hafeez Center Mobile", uiState)
                    }) {
                        Icon(Icons.Default.Share, contentDescription = stringResource(R.string.dashboard_btn_share))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Time Range Switcher Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TimeRange.entries.forEach { range ->
                    val isSelected = uiState.timeRangeSelection.preset != TimeRangePreset.CUSTOM && uiState.timeRange == range
                    FilterChip(
                        selected = isSelected,
                        onClick = { onTimeRangeSelected(range) },
                        label = {
                            Text(
                                when (range) {
                                    TimeRange.TODAY -> "Today"
                                    TimeRange.THIS_MONTH -> "This Month"
                                    TimeRange.THIS_YEAR -> "This Year"
                                    TimeRange.ALL_TIME -> "All Time"
                                },
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }

                // P8 TASK T3: Custom Range Chip (Gated under DASHBOARD_ADVANCED)
                val isCustomSelected = uiState.timeRangeSelection.preset == TimeRangePreset.CUSTOM
                FilterChip(
                    selected = isCustomSelected,
                    onClick = {
                        if (isAdvancedDashboardLocked) {
                            onUnlockClick()
                        } else {
                            showDateRangePicker = true
                        }
                    },
                    leadingIcon = {
                        Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(14.dp))
                    },
                    label = {
                        Text(
                            stringResource(R.string.dashboard_custom_range),
                            fontWeight = if (isCustomSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }

            // Unrecorded Purchase Costs Warning Banner
            if (uiState.hasMissingCosts) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = stringResource(
                                R.string.dashboard_warning_missing_costs,
                                uiState.missingCostSoldLines,
                                uiState.profitCoveredLines,
                                uiState.totalSoldLines
                            ),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // Hero Net Profit Card (Free / Open)
            val isProfitPositive = uiState.netProfitRs >= 0
            val profitColor = if (isProfitPositive) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = null,
                                tint = profitColor
                            )
                            Text(
                                text = stringResource(R.string.dashboard_net_profit),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(onClick = {
                            showHelpDialogTitle = "Net Profit Calculation"
                            showHelpDialogMsg = "Net Profit = Net Revenue (Aamdani) - COGS (Saman ki Lagat) - Expenses (Dukan ke Akhrajat).\n\nReturns & Refunds are already adjusted!"
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                contentDescription = "Help",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Text(
                        text = com.mmushtaq04.buysell.util.CurrencyFormatter.formatAmount(context, uiState.netProfitRs),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = profitColor
                    )

                    // P8 TASK T1: Insights Row (Margins, Avg Sale, Avg Unit Profit)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(
                                R.string.dashboard_net_margin,
                                String.format(java.util.Locale.US, "%.1f", uiState.netMarginPercent)
                            ),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(
                                R.string.dashboard_avg_sale_value,
                                com.mmushtaq04.buysell.util.CurrencyFormatter.formatAmount(context, uiState.avgSaleValueRs)
                            ),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(
                                R.string.dashboard_avg_unit_profit,
                                com.mmushtaq04.buysell.util.CurrencyFormatter.formatAmount(context, uiState.avgProfitPerUnitRs)
                            ),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // P8 TASK T2: Period Comparison Card (Gated under DASHBOARD_ADVANCED)
            if (uiState.timeRangeSelection.preset != TimeRangePreset.ALL_TIME) {
                PremiumFeatureGate(
                    isLocked = isAdvancedDashboardLocked,
                    featureName = stringResource(R.string.dashboard_comparison_title),
                    onUnlockClick = onUnlockClick
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.dashboard_comparison_title),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            HorizontalDivider()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val revChange = uiState.periodComparison.revenueChangePercent
                                Column {
                                    Text(stringResource(R.string.dashboard_comparison_vs_prev), fontSize = 11.sp, color = Color.Gray)
                                    if (revChange != null) {
                                        val isPositive = revChange >= 0f
                                        val changeColor = if (isPositive) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (isPositive) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                                contentDescription = null,
                                                tint = changeColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                text = String.format(java.util.Locale.US, "%+.1f%% Revenue", revChange),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = changeColor
                                            )
                                        }
                                    }
                                }

                                val profChange = uiState.periodComparison.profitChangePercent
                                Column {
                                    Text(stringResource(R.string.dashboard_comparison_vs_prev), fontSize = 11.sp, color = Color.Gray)
                                    if (profChange != null) {
                                        val isPositive = profChange >= 0f
                                        val changeColor = if (isPositive) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (isPositive) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                                contentDescription = null,
                                                tint = changeColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                text = String.format(java.util.Locale.US, "%+.1f%% Profit", profChange),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = changeColor
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Financial Summary Grid (Free / Open)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Net Revenue Card
                    DashboardSmallCard(
                        title = stringResource(R.string.dashboard_net_revenue),
                        value = com.mmushtaq04.buysell.util.CurrencyFormatter.formatAmount(context, uiState.netRevenueRs),
                        icon = Icons.Default.AttachMoney,
                        iconColor = Color(0xFF2E7D32),
                        modifier = Modifier.weight(1f)
                    )

                    // Cost of Goods Card
                    DashboardSmallCard(
                        title = stringResource(R.string.dashboard_cost_goods),
                        value = com.mmushtaq04.buysell.util.CurrencyFormatter.formatAmount(context, uiState.cogsRs),
                        icon = Icons.Default.Inventory2,
                        iconColor = Color(0xFF1565C0),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Operational Expenses Card
                    DashboardSmallCard(
                        title = stringResource(R.string.dashboard_total_expenses),
                        value = com.mmushtaq04.buysell.util.CurrencyFormatter.formatAmount(context, uiState.expensesRs),
                        icon = Icons.AutoMirrored.Filled.ReceiptLong,
                        iconColor = MaterialTheme.colorScheme.error,
                        subtitle = "Tap to view/add expenses →",
                        onClick = onNavigateToExpenses,
                        modifier = Modifier.weight(1f)
                    )

                    // Returns & Refunds Card
                    DashboardSmallCard(
                        title = stringResource(R.string.dashboard_returns_refunds),
                        value = com.mmushtaq04.buysell.util.CurrencyFormatter.formatAmount(context, uiState.returnsRefundsRs),
                        icon = Icons.AutoMirrored.Filled.AssignmentReturn,
                        iconColor = Color(0xFFE65100),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // P7: Receivables & Payables Pair (Free)
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                DashboardSmallCard(
                    title = stringResource(R.string.dashboard_receivables_title),
                    value = com.mmushtaq04.buysell.util.CurrencyFormatter.formatAmount(context, uiState.receivablesRs),
                    icon = Icons.Default.AccountBalanceWallet,
                    iconColor = Color(0xFF2E7D32),
                    subtitle = stringResource(R.string.dashboard_receivables_subtitle, uiState.receivablesPartyCount),
                    onClick = onNavigateToPartyList,
                    modifier = Modifier.weight(1f)
                )

                DashboardSmallCard(
                    title = stringResource(R.string.dashboard_payables_title),
                    value = com.mmushtaq04.buysell.util.CurrencyFormatter.formatAmount(context, uiState.payablesRs),
                    icon = Icons.Default.MoneyOff,
                    iconColor = MaterialTheme.colorScheme.error,
                    subtitle = stringResource(R.string.dashboard_payables_subtitle, uiState.payablesPartyCount),
                    onClick = onNavigateToPartyList,
                    modifier = Modifier.weight(1f)
                )
            }

            // P8 TASK T5: Below-Cost Sales Alert Card (Gated under DASHBOARD_ADVANCED)
            if (uiState.belowCostSalesCount > 0) {
                PremiumFeatureGate(
                    isLocked = isAdvancedDashboardLocked,
                    featureName = stringResource(R.string.dashboard_below_cost_title),
                    onUnlockClick = onUnlockClick
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Text(
                                    text = stringResource(R.string.dashboard_below_cost_title),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            Text(
                                text = stringResource(
                                    R.string.dashboard_below_cost_sub,
                                    uiState.belowCostSalesCount,
                                    com.mmushtaq04.buysell.util.CurrencyFormatter.formatAmount(context, uiState.belowCostTotalLossRs)
                                ),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            // P8 TASK T4: Aged Stock Breakdown Card (Gated under DASHBOARD_ADVANCED)
            PremiumFeatureGate(
                isLocked = isAdvancedDashboardLocked,
                featureName = stringResource(R.string.dashboard_aged_stock_title),
                onUnlockClick = onUnlockClick
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.dashboard_aged_stock_title),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        HorizontalDivider()
                        uiState.agedStockBuckets.forEach { bucket ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(bucket.rangeLabel, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text(
                                    text = "${bucket.itemCount} items • ${com.mmushtaq04.buysell.util.CurrencyFormatter.formatAmount(context, bucket.costValueRs)}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // P7: Money by Payment Method Card (Gated under DASHBOARD_ADVANCED)
            PremiumFeatureGate(
                isLocked = isAdvancedDashboardLocked,
                featureName = stringResource(R.string.dashboard_money_by_method),
                onUnlockClick = onUnlockClick
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.dashboard_money_by_method),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        HorizontalDivider()
                        if (uiState.paymentMethodTotals.isEmpty()) {
                            Text("No transactions recorded in this period", fontSize = 12.sp, color = Color.Gray)
                        } else {
                            uiState.paymentMethodTotals.forEach { (method, amountRs) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = method.name.replace("_", " "),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                    val isPositive = amountRs >= 0
                                    val amountColor = if (isPositive) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                                    Text(
                                        text = com.mmushtaq04.buysell.util.CurrencyFormatter.formatAmount(context, amountRs),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = amountColor
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Top Selling Models Card (Gated under INVENTORY_INSIGHTS)
            PremiumFeatureGate(
                isLocked = isInventoryInsightsLocked,
                featureName = stringResource(R.string.dashboard_top_models),
                onUnlockClick = onUnlockClick
            ) {
                if (uiState.topSellingModels.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(stringResource(R.string.dashboard_top_models), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    FilterChip(
                                        selected = uiState.topModelsSortOrder == TopModelsSortOrder.UNITS_SOLD,
                                        onClick = { onSortOrderChanged(TopModelsSortOrder.UNITS_SOLD) },
                                        label = { Text("Units", fontSize = 10.sp) }
                                    )
                                    FilterChip(
                                        selected = uiState.topModelsSortOrder == TopModelsSortOrder.PROFIT,
                                        onClick = { onSortOrderChanged(TopModelsSortOrder.PROFIT) },
                                        label = { Text("Profit", fontSize = 10.sp) }
                                    )
                                }
                            }
                            HorizontalDivider()
                            uiState.topSellingModels.forEachIndexed { idx, model ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "#${idx + 1}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Column {
                                            Text(model.modelName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text(
                                                text = stringResource(R.string.dashboard_units_sold, model.unitsSold, model.profitRs.toString()),
                                                fontSize = 11.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                    Text(
                                        text = com.mmushtaq04.buysell.util.CurrencyFormatter.formatAmount(context, model.revenueRs),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Inventory Insights Section (Capital Locked & Slow Moving Stock)
            PremiumFeatureGate(
                isLocked = isInventoryInsightsLocked,
                featureName = "Inventory Insights",
                onUnlockClick = onUnlockClick
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Capital Locked Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Capital Locked in Stock", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Purchase cost value of in-stock items", fontSize = 11.sp, color = Color.Gray)
                            }
                            Text(
                                text = com.mmushtaq04.buysell.util.CurrencyFormatter.formatAmount(context, uiState.capitalInStockRs),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Slow Moving Stock Card (> 30 days)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Slow Moving Stock", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("${uiState.slowStockCount} items in stock > 30 days", fontSize = 11.sp, color = Color.Gray)
                            }
                            Text(
                                text = com.mmushtaq04.buysell.util.CurrencyFormatter.formatAmount(context, uiState.slowStockValueRs),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomDateRangePickerDialog(
    onDismiss: () -> Unit,
    onDateRangeSelected: (Long, Long) -> Unit
) {
    val dateRangePickerState = rememberDateRangePickerState()

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val start = dateRangePickerState.selectedStartDateMillis
                    val end = dateRangePickerState.selectedEndDateMillis
                    if (start != null && end != null && start <= end) {
                        onDateRangeSelected(start, end)
                    }
                }
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DateRangePicker(
            state = dateRangePickerState,
            title = { Text(stringResource(R.string.dashboard_select_date_range), modifier = Modifier.padding(16.dp)) },
            modifier = Modifier.weight(1f)
        )
    }
}

private fun shareDashboardSummary(context: Context, shopName: String, uiState: DashboardUiState) {
    val rangeText = when (uiState.timeRangeSelection.preset) {
        TimeRangePreset.TODAY -> "Today"
        TimeRangePreset.THIS_MONTH -> "This Month"
        TimeRangePreset.THIS_YEAR -> "This Year"
        TimeRangePreset.ALL_TIME -> "All Time"
        TimeRangePreset.CUSTOM -> "Custom Range"
    }

    val summaryText = """
        📊 $shopName - Dashboard Summary ($rangeText)
        -------------------------------------------
        💵 Net Revenue: ${com.mmushtaq04.buysell.util.CurrencyFormatter.formatAmount(context, uiState.netRevenueRs)}
        📦 Cost of Goods: ${com.mmushtaq04.buysell.util.CurrencyFormatter.formatAmount(context, uiState.cogsRs)}
        🧾 Expenses: ${com.mmushtaq04.buysell.util.CurrencyFormatter.formatAmount(context, uiState.expensesRs)}
        📈 Net Profit: ${com.mmushtaq04.buysell.util.CurrencyFormatter.formatAmount(context, uiState.netProfitRs)} (${String.format(java.util.Locale.US, "%.1f%%", uiState.netMarginPercent)})
        🛒 Sales Count: ${uiState.saleTxnCount}
        💡 Avg Sale Value: ${com.mmushtaq04.buysell.util.CurrencyFormatter.formatAmount(context, uiState.avgSaleValueRs)}
        💰 Receivables: ${com.mmushtaq04.buysell.util.CurrencyFormatter.formatAmount(context, uiState.receivablesRs)}
        💳 Payables: ${com.mmushtaq04.buysell.util.CurrencyFormatter.formatAmount(context, uiState.payablesRs)}
        🏢 Capital in Stock: ${com.mmushtaq04.buysell.util.CurrencyFormatter.formatAmount(context, uiState.capitalInStockRs)}
        -------------------------------------------
        Generated via BuySell App
    """.trimIndent()

    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, summaryText)
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, context.getString(R.string.dashboard_btn_share))
    context.startActivity(shareIntent)
}

@Composable
private fun DashboardSmallCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Gray
                )
            }
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
