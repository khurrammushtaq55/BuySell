package com.mmushtaq04.buysell.presentation.screens.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.AssignmentReturn
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mmushtaq04.buysell.R
import com.mmushtaq04.buysell.presentation.components.PremiumFeatureGate
import com.mmushtaq04.buysell.presentation.components.PremiumPaywallDialog
import com.mmushtaq04.buysell.ui.theme.BuySellTheme

@Composable
fun OwnerDashboardScreen(
    viewModel: OwnerDashboardViewModel = viewModel(),
    isAdvancedDashboardLocked: Boolean = false,
    isInventoryInsightsLocked: Boolean = false,
    onNavigateBack: () -> Unit = {},
    onNavigateToExpenses: () -> Unit = {},
    onUnlockClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    OwnerDashboardContent(
        uiState = uiState,
        isAdvancedDashboardLocked = isAdvancedDashboardLocked,
        isInventoryInsightsLocked = isInventoryInsightsLocked,
        onTimeRangeSelected = { viewModel.setTimeRange(it) },
        onNavigateBack = onNavigateBack,
        onNavigateToExpenses = onNavigateToExpenses,
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
    onNavigateBack: () -> Unit = {},
    onNavigateToExpenses: () -> Unit = {},
    onUnlockClick: () -> Unit = {}
) {
    val context = LocalContext.current

    var showHelpDialogTitle by remember { mutableStateOf<String?>(null) }
    var showHelpDialogMsg by remember { mutableStateOf<String?>(null) }
    var showPaywallDialog by remember { mutableStateOf(false) }

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

    if (showHelpDialogTitle != null && showHelpDialogMsg != null) {
        AlertDialog(
            onDismissRequest = {
                showHelpDialogTitle = null
                showHelpDialogMsg = null
            },
            icon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(text = showHelpDialogTitle!!, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(text = showHelpDialogMsg!!, fontSize = 14.sp)
            },
            confirmButton = {
                TextButton(onClick = {
                    showHelpDialogTitle = null
                    showHelpDialogMsg = null
                }) {
                    Text(stringResource(R.string.action_ok))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.dashboard_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Time Range Segmented Selector
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val ranges = listOf(
                        TimeRange.TODAY to stringResource(R.string.dashboard_filter_today),
                        TimeRange.THIS_MONTH to stringResource(R.string.dashboard_filter_month),
                        TimeRange.THIS_YEAR to stringResource(R.string.dashboard_filter_year),
                        TimeRange.ALL_TIME to stringResource(R.string.dashboard_filter_all)
                    )

                    ranges.forEach { (rangeOption, label) ->
                        val isSelected = uiState.timeRange == rangeOption
                        FilterChip(
                            selected = isSelected,
                            onClick = { onTimeRangeSelected(rangeOption) },
                            label = {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            if (uiState.hasMissingCosts) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
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
                            text = stringResource(R.string.dashboard_warning_missing_costs, uiState.missingCostCount),
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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
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

            // Expense Breakdown Section (Free / Open)
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = stringResource(R.string.dashboard_expense_breakdown),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        TextButton(onClick = onNavigateToExpenses) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.expense_btn_add), fontSize = 12.sp)
                        }
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ExpenseStatItem(
                            label = stringResource(R.string.dashboard_daily_expense),
                            value = com.mmushtaq04.buysell.util.CurrencyFormatter.formatAmount(context, uiState.dailyExpenseRs),
                            modifier = Modifier.weight(1f)
                        )
                        ExpenseStatItem(
                            label = stringResource(R.string.dashboard_monthly_expense),
                            value = com.mmushtaq04.buysell.util.CurrencyFormatter.formatAmount(context, uiState.monthlyExpenseRs),
                            modifier = Modifier.weight(1f)
                        )
                        ExpenseStatItem(
                            label = stringResource(R.string.dashboard_yearly_expense),
                            value = com.mmushtaq04.buysell.util.CurrencyFormatter.formatAmount(context, uiState.yearlyExpenseRs),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Sales & Profit Analytics Graph (DASHBOARD_ADVANCED)
            PremiumFeatureGate(
                isLocked = isAdvancedDashboardLocked,
                featureName = stringResource(R.string.dashboard_trend_graph_title),
                onUnlockClick = onUnlockClick,
                placeholder = {
                    val dummyData = listOf(
                        ChartBarData("P1", 100000L, 20000L),
                        ChartBarData("P2", 150000L, 30000L),
                        ChartBarData("P3", 120000L, 25000L),
                        ChartBarData("P4", 180000L, 40000L)
                    )
                    DashboardTrendBarChart(data = dummyData)
                }
            ) {
                DashboardTrendBarChart(data = uiState.trendChartData)
            }

            // Top Selling Models Card (DASHBOARD_ADVANCED)
            PremiumFeatureGate(
                isLocked = isAdvancedDashboardLocked,
                featureName = stringResource(R.string.dashboard_top_models),
                onUnlockClick = onUnlockClick,
                placeholder = {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Top Selling Models", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("••••••••••••••••", color = Color.Gray, fontSize = 13.sp)
                            Text("••••••••••••••••", color = Color.Gray, fontSize = 13.sp)
                        }
                    }
                }
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
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Capital Locked in Stock & Slow Stock (INVENTORY_INSIGHTS)
            PremiumFeatureGate(
                isLocked = isInventoryInsightsLocked,
                featureName = stringResource(R.string.dashboard_capital_stock),
                onUnlockClick = onUnlockClick,
                placeholder = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        DashboardMetricCard(
                            title = stringResource(R.string.dashboard_capital_stock),
                            value = "Rs ••••••",
                            valueColor = Color(0xFF7B1FA2),
                            helpTitle = stringResource(R.string.dashboard_help_capital_title),
                            helpMsg = stringResource(R.string.dashboard_help_capital_msg),
                            onHelpClick = { title, msg ->
                                showHelpDialogTitle = title
                                showHelpDialogMsg = msg
                            }
                        )
                    }
                }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DashboardMetricCard(
                        title = stringResource(R.string.dashboard_capital_stock),
                        value = com.mmushtaq04.buysell.util.CurrencyFormatter.formatAmount(context, uiState.capitalInStockRs),
                        valueColor = Color(0xFF7B1FA2),
                        helpTitle = stringResource(R.string.dashboard_help_capital_title),
                        helpMsg = stringResource(R.string.dashboard_help_capital_msg),
                        onHelpClick = { title, msg ->
                            showHelpDialogTitle = title
                            showHelpDialogMsg = msg
                        }
                    )

                    val formattedSlowVal = com.mmushtaq04.buysell.util.CurrencyFormatter.formatAmount(context, uiState.slowStockValueRs)
                    DashboardMetricCard(
                        title = stringResource(R.string.dashboard_slow_stock),
                        value = if (uiState.slowStockCount > 0) "${uiState.slowStockCount} items • $formattedSlowVal" else "0 items",
                        valueColor = if (uiState.slowStockCount > 0) MaterialTheme.colorScheme.error else Color(0xFF2E7D32),
                        helpTitle = stringResource(R.string.dashboard_help_slow_stock_title),
                        helpMsg = stringResource(R.string.dashboard_help_slow_stock_msg),
                        onHelpClick = { title, msg ->
                            showHelpDialogTitle = title
                            showHelpDialogMsg = msg
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpenseStatItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun DashboardTrendBarChart(
    data: List<ChartBarData>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.dashboard_trend_graph_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(modifier = Modifier.size(10.dp), shape = RoundedCornerShape(2.dp), color = Color(0xFF1565C0)) {}
                        Text("Sales", fontSize = 11.sp, color = Color.Gray)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(modifier = Modifier.size(10.dp), shape = RoundedCornerShape(2.dp), color = Color(0xFF2E7D32)) {}
                        Text("Profit", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }

            HorizontalDivider()

            val maxVal = data.maxOfOrNull { maxOf(it.revenueRs, it.profitRs) }?.coerceAtLeast(1L) ?: 1L

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                data.forEach { bar ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier.height(100.dp)
                        ) {
                            // Revenue Bar
                            val revFrac = (bar.revenueRs.toFloat() / maxVal).coerceIn(0.08f, 1f)
                            Surface(
                                modifier = Modifier
                                    .width(12.dp)
                                    .fillMaxHeight(revFrac),
                                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
                                color = Color(0xFF1565C0)
                            ) {}

                            // Profit Bar
                            val profitFrac = (bar.profitRs.coerceAtLeast(0L).toFloat() / maxVal).coerceIn(0.05f, 1f)
                            Surface(
                                modifier = Modifier
                                    .width(12.dp)
                                    .fillMaxHeight(profitFrac),
                                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
                                color = if (bar.profitRs >= 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                            ) {}
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = bar.label,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
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
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
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
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = value,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun DashboardMetricCard(
    title: String,
    value: String,
    valueColor: Color,
    helpTitle: String,
    helpMsg: String,
    onHelpClick: (String, String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                IconButton(onClick = { onHelpClick(helpTitle, helpMsg) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                        contentDescription = "Help",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun OwnerDashboardScreenPreview() {
    val sampleState = DashboardUiState(
        timeRange = TimeRange.THIS_MONTH,
        netRevenueRs = 450000L,
        cogsRs = 320000L,
        expensesRs = 25000L,
        netProfitRs = 105000L,
        returnsRefundsRs = 12000L,
        dailyExpenseRs = 1500L,
        monthlyExpenseRs = 25000L,
        yearlyExpenseRs = 180000L,
        trendChartData = listOf(
            ChartBarData("Week 1", 110000L, 25000L),
            ChartBarData("Week 2", 130000L, 30000L),
            ChartBarData("Week 3", 95000L, 20000L),
            ChartBarData("Week 4", 127000L, 30000L)
        ),
        topSellingModels = listOf(
            TopSellingModel("Apple iPhone 15 Pro", 8, 1200000L, 160000L),
            TopSellingModel("Samsung Galaxy S24", 5, 750000L, 90000L)
        ),
        capitalInStockRs = 850000L,
        slowStockCount = 2,
        slowStockValueRs = 120000L
    )

    BuySellTheme {
        OwnerDashboardContent(
            uiState = sampleState,
            isAdvancedDashboardLocked = false,
            isInventoryInsightsLocked = false
        )
    }
}
