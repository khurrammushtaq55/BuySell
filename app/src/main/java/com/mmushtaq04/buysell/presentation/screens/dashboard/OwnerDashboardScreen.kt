package com.mmushtaq04.buysell.presentation.screens.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mmushtaq04.buysell.ui.theme.BuySellTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerDashboardScreen(
    onNavigateBack: () -> Unit = {},
    todaySalesCount: Int = 0,
    todaySalesTotalRs: Long = 0L,
    monthlyNetProfitRs: Long = 0L,
    capitalInStockRs: Long = 0L,
    slowStockCount: Int = 0
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
                    Text("Samajh Aa Gaya")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Owner Dashboard & Reports", fontWeight = FontWeight.Bold) },
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
            // Card 1: Today Sales
            DashboardMetricCard(
                title = "Aaj ki sales",
                value = if (todaySalesCount > 0) "$todaySalesCount sales • Rs $todaySalesTotalRs" else "Abhi tak koi sale nahi hui (Rs 0)",
                valueColor = if (todaySalesCount > 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant,
                helpTitle = "Aaj ki sales ka matlab?",
                helpMsg = "Aaj subah se le kar ab tak jitni bhi mobile sales hui hain un ka kul jor.",
                onHelpClick = { title, msg ->
                    showHelpDialogTitle = title
                    showHelpDialogMsg = msg
                }
            )

            // Card 2: Monthly Profit
            DashboardMetricCard(
                title = "Is mahine ka munafa (Net Profit)",
                value = "Rs $monthlyNetProfitRs",
                valueColor = Color(0xFF1565C0),
                helpTitle = "Munafa (Profit) kaise banta hai?",
                helpMsg = "Is mahine ki Kul Sale − Khareedari ki qeemat − Shop ke kharch (Rent, Bijli, Repair).",
                onHelpClick = { title, msg ->
                    showHelpDialogTitle = title
                    showHelpDialogMsg = msg
                }
            )

            // Card 3: Capital in Stock
            DashboardMetricCard(
                title = "Stock mein band paisa (Capital)",
                value = "Rs $capitalInStockRs",
                valueColor = Color(0xFF7B1FA2),
                helpTitle = "Band paisa kya hai?",
                helpMsg = "Shop ke available stock mein jitni phones paday hain un ki kul khareedari ki qeemat.",
                onHelpClick = { title, msg ->
                    showHelpDialogTitle = title
                    showHelpDialogMsg = msg
                }
            )

            // Card 4: Slow Stock Warning
            DashboardMetricCard(
                title = "Slow Stock (30 din se zyada)",
                value = if (slowStockCount > 0) "$slowStockCount phones abhi tak nahi bechay" else "Koi slow stock nahi ✓",
                valueColor = if (slowStockCount > 0) Color(0xFFD32F2F) else Color(0xFF2E7D32),
                helpTitle = "Slow Stock kya hai?",
                helpMsg = "Jo mobile phones 30 din se zyada shop mein paday hain aur bechay nahi gaye.",
                onHelpClick = { title, msg ->
                    showHelpDialogTitle = title
                    showHelpDialogMsg = msg
                }
            )
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
        OwnerDashboardScreen(
            todaySalesCount = 4,
            todaySalesTotalRs = 185000,
            monthlyNetProfitRs = 62000,
            capitalInStockRs = 850000,
            slowStockCount = 2
        )
    }
}
