package com.mmushtaq04.buysell.presentation.screens.dashboard

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.AssignmentReturn
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mmushtaq04.buysell.R
import com.mmushtaq04.buysell.presentation.components.PremiumFeatureGate
import com.mmushtaq04.buysell.ui.theme.BuySellTheme

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
    var showDateRangePicker by remember { mutableStateOf(false) }

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
                        shareDashboardSummary(context, uiState.shopName, uiState)
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Time Range Switcher Chips (Horizontally Scrollable for Small Screens)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TimeRange.entries.forEach { range ->
                    val isSelected = uiState.timeRangeSelection.preset != TimeRangePreset.CUSTOM && uiState.timeRange == range
                    FilterChip(
                        selected = isSelected,
                        onClick = { onTimeRangeSelected(range) },
                        label = {
                            Text(
                                when (range) {
                                    TimeRange.TODAY -> stringResource(R.string.dashboard_filter_today)
                                    TimeRange.THIS_MONTH -> stringResource(R.string.dashboard_filter_month)
                                    TimeRange.THIS_YEAR -> stringResource(R.string.dashboard_filter_year)
                                    TimeRange.ALL_TIME -> stringResource(R.string.dashboard_filter_all)
                                },
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }

                // Custom Range Chip (Gated under DASHBOARD_ADVANCED)
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

                        val helpTitle = stringResource(R.string.dashboard_help_net_profit_title)
                        val helpMsg = stringResource(R.string.dashboard_help_net_profit_msg)
                        IconButton(onClick = {
                            showHelpDialogTitle = helpTitle
                            showHelpDialogMsg = helpMsg
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

                    // Financial Insights (3 Separate Boxes for Maximum Clarity)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Box 1: Net Margin %
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = stringResource(R.string.dashboard_insight_margin),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = String.format(java.util.Locale.US, "%.1f%%", uiState.netMarginPercent),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Box 2: Avg Sale Value
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = stringResource(R.string.dashboard_insight_avg_sale),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = com.mmushtaq04.buysell.util.CurrencyFormatter.formatAmount(context, uiState.avgSaleValueRs),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Box 3: Avg Profit Per Unit
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = stringResource(R.string.dashboard_insight_unit_profit),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = com.mmushtaq04.buysell.util.CurrencyFormatter.formatAmount(context, uiState.avgProfitPerUnitRs),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = profitColor
                                )
                            }
                        }
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
                        subtitle = stringResource(R.string.dashboard_expenses_subtitle),
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
                            Text(stringResource(R.string.dashboard_no_method_txns), fontSize = 12.sp, color = Color.Gray)
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
                                        label = { Text(stringResource(R.string.dashboard_sort_units), fontSize = 10.sp) }
                                    )
                                    FilterChip(
                                        selected = uiState.topModelsSortOrder == TopModelsSortOrder.PROFIT,
                                        onClick = { onSortOrderChanged(TopModelsSortOrder.PROFIT) },
                                        label = { Text(stringResource(R.string.dashboard_sort_profit), fontSize = 10.sp) }
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
                featureName = stringResource(R.string.dashboard_feature_inventory_insights),
                onUnlockClick = onUnlockClick
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            ) {
                                Box(
                                    modifier = Modifier.padding(6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Insights,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Text(
                                text = stringResource(R.string.dashboard_feature_inventory_insights),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        HorizontalDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Box 1: Capital Locked in Stock
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Inventory2,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = stringResource(R.string.dashboard_capital_locked_title),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = com.mmushtaq04.buysell.util.CurrencyFormatter.formatAmount(context, uiState.capitalInStockRs),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = stringResource(R.string.dashboard_capital_locked_sub),
                                        fontSize = 10.sp,
                                        color = Color.Gray,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Box 2: Slow Moving Stock (> 30 days)
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFFFF3E0)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = null,
                                            tint = Color(0xFFE65100),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = stringResource(R.string.dashboard_slow_stock),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = com.mmushtaq04.buysell.util.CurrencyFormatter.formatAmount(context, uiState.slowStockValueRs),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFE65100),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = stringResource(R.string.dashboard_slow_stock_sub, uiState.slowStockCount),
                                        fontSize = 10.sp,
                                        color = Color.Gray,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
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
                Text(stringResource(R.string.action_cancel))
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
        TimeRangePreset.TODAY -> context.getString(R.string.dashboard_filter_today)
        TimeRangePreset.THIS_MONTH -> context.getString(R.string.dashboard_filter_month)
        TimeRangePreset.THIS_YEAR -> context.getString(R.string.dashboard_filter_year)
        TimeRangePreset.ALL_TIME -> context.getString(R.string.dashboard_filter_all)
        TimeRangePreset.CUSTOM -> context.getString(R.string.dashboard_custom_range)
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
    val isClickable = onClick != null
    Card(
        modifier = modifier.then(if (isClickable) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = iconColor.copy(alpha = 0.12f)
                ) {
                    Box(
                        modifier = Modifier.padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (subtitle != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = subtitle,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (isClickable) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Owner Dashboard - Pro Unlocked")
@Composable
fun OwnerDashboardContentUnlockedPreview() {
    BuySellTheme {
        OwnerDashboardContent(
            uiState = DashboardUiState(
                shopName = "Mushtaq Mobile Store",
                netRevenueRs = 850000L,
                cogsRs = 620000L,
                expensesRs = 35000L,
                netProfitRs = 195000L,
                returnsRefundsRs = 12000L,
                receivablesRs = 145000L,
                receivablesPartyCount = 8,
                payablesRs = 82000L,
                payablesPartyCount = 4,
                overduePromisesRs = 45000L,
                overduePromisesCount = 3,
                capitalInStockRs = 1250000L,
                slowStockCount = 5,
                slowStockValueRs = 320000L,
                netMarginPercent = 22.9f,
                avgSaleValueRs = 85000L,
                avgProfitPerUnitRs = 19500L,
                saleTxnCount = 10,
                periodComparison = PeriodComparison(
                    previousRevenueRs = 720000L,
                    previousProfitRs = 150000L,
                    revenueChangePercent = 18.1f,
                    profitChangePercent = 30.0f
                ),
                topSellingModels = listOf(
                    TopSellingModel("Apple iPhone 15 Pro", 4, 840000L, 120000L),
                    TopSellingModel("Samsung Galaxy S24 Ultra", 3, 630000L, 90000L),
                    TopSellingModel("Xiaomi 14 Ultra", 2, 350000L, 50000L)
                ),
                agedStockBuckets = listOf(
                    AgedStockBucket("0-30 Days (Fresh)", 12, 850000L),
                    AgedStockBucket("31-60 Days (Aging)", 4, 250000L),
                    AgedStockBucket("61-90 Days (Slow)", 2, 110000L),
                    AgedStockBucket("90+ Days (Stagnant)", 1, 40000L)
                ),
                paymentMethodTotals = mapOf(
                    com.mmushtaq04.buysell.data.local.enums.PaymentMethod.CASH to 450000L,
                    com.mmushtaq04.buysell.data.local.enums.PaymentMethod.BANK to 300000L,
                    com.mmushtaq04.buysell.data.local.enums.PaymentMethod.WALLET to 100000L
                )
            ),
            isAdvancedDashboardLocked = false,
            isInventoryInsightsLocked = false
        )
    }
}

@Preview(showBackground = true, name = "Owner Dashboard - Free Gated")
@Composable
fun OwnerDashboardContentLockedPreview() {
    BuySellTheme {
        OwnerDashboardContent(
            uiState = DashboardUiState(
                shopName = "Mushtaq Mobile Store",
                netRevenueRs = 250000L,
                cogsRs = 180000L,
                expensesRs = 15000L,
                netProfitRs = 55000L,
                receivablesRs = 45000L,
                receivablesPartyCount = 3,
                payablesRs = 20000L,
                payablesPartyCount = 2,
                netMarginPercent = 22.0f,
                avgSaleValueRs = 50000L,
                avgProfitPerUnitRs = 11000L,
                saleTxnCount = 5
            ),
            isAdvancedDashboardLocked = true,
            isInventoryInsightsLocked = true
        )
    }
}
