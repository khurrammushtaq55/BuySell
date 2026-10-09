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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mmushtaq04.buysell.R
import com.mmushtaq04.buysell.ui.theme.BuySellTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerDashboardScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToExpenses: () -> Unit = {},
    uiState: DashboardUiState = DashboardUiState(),
    onTimeRangeSelect: (TimeRange) -> Unit = {}
) {
    var showHelpDialogTitle by remember { mutableStateOf<String?>(null) }
    var showHelpDialogMsg by remember { mutableStateOf<String?>(null) }

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
                    Text(stringResource(R.string.action_confirm))
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
            // Time Range Filter Tab Row
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val ranges = listOf(
                    TimeRange.TODAY to stringResource(R.string.dashboard_filter_today),
                    TimeRange.THIS_WEEK to stringResource(R.string.dashboard_filter_week),
                    TimeRange.THIS_MONTH to stringResource(R.string.dashboard_filter_month),
                    TimeRange.ALL_TIME to stringResource(R.string.dashboard_filter_all)
                )

                ranges.forEachIndexed { index, (rangeKey, rangeLabel) ->
                    SegmentedButton(
                        selected = uiState.timeRange == rangeKey,
                        onClick = { onTimeRangeSelect(rangeKey) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = ranges.size)
                    ) {
                        Text(rangeLabel, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Hero Net Profit Card
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
                        text = "Rs ${uiState.netProfitRs}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = profitColor
                    )
                }
            }

            // Financial Summary Grid
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Net Revenue Card
                    DashboardSmallCard(
                        title = stringResource(R.string.dashboard_net_revenue),
                        value = "Rs ${uiState.netRevenueRs}",
                        icon = Icons.Default.AttachMoney,
                        iconColor = Color(0xFF2E7D32),
                        modifier = Modifier.weight(1f)
                    )

                    // Cost of Goods Card
                    DashboardSmallCard(
                        title = stringResource(R.string.dashboard_cost_goods),
                        value = "Rs ${uiState.cogsRs}",
                        icon = Icons.Default.Inventory2,
                        iconColor = Color(0xFF1565C0),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Operational Expenses Card (Clickable)
                    DashboardSmallCard(
                        title = stringResource(R.string.dashboard_total_expenses),
                        value = "Rs ${uiState.expensesRs}",
                        icon = Icons.AutoMirrored.Filled.ReceiptLong,
                        iconColor = MaterialTheme.colorScheme.error,
                        subtitle = "Tap to view/add expenses →",
                        onClick = onNavigateToExpenses,
                        modifier = Modifier.weight(1f)
                    )

                    // Returns & Refunds Card
                    DashboardSmallCard(
                        title = stringResource(R.string.dashboard_returns_refunds),
                        value = "Rs ${uiState.returnsRefundsRs}",
                        icon = Icons.AutoMirrored.Filled.AssignmentReturn,
                        iconColor = Color(0xFFE65100),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Top Selling Models Card
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
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFC107)
                            )
                            Text(
                                text = stringResource(R.string.dashboard_top_models),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
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
                                    text = "Rs ${model.revenueRs}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            // Capital Locked in Stock
            DashboardMetricCard(
                title = stringResource(R.string.dashboard_capital_stock),
                value = "Rs ${uiState.capitalInStockRs}",
                valueColor = Color(0xFF7B1FA2),
                helpTitle = stringResource(R.string.dashboard_help_capital_title),
                helpMsg = stringResource(R.string.dashboard_help_capital_msg),
                onHelpClick = { title, msg ->
                    showHelpDialogTitle = title
                    showHelpDialogMsg = msg
                }
            )

            // Slow Stock Warning
            DashboardMetricCard(
                title = stringResource(R.string.dashboard_slow_stock),
                value = if (uiState.slowStockCount > 0) "${uiState.slowStockCount} items • Rs ${uiState.slowStockValueRs}" else "0 items",
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
                        imageVector = Icons.Default.HelpOutline,
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
    BuySellTheme {
        OwnerDashboardScreen()
    }
}
