package com.mmushtaq04.buysell.presentation.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mmushtaq04.buysell.ui.theme.BuySellTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    shopName: String = "Mera Buy/Sell Store",
    userRole: String = "Owner",
    todaySalesCount: Int = 0,
    todaySalesAmountPaisa: Long = 0L,
    onNavigateToBuy: () -> Unit = {},
    onNavigateToSell: () -> Unit = {},
    onNavigateToExchange: () -> Unit = {},
    onNavigateToStock: () -> Unit = {},
    onNavigateToParties: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = shopName, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            text = "Role: $userRole • Cloud par save ✓",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Today Summary Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Aaj ki sales", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = if (todaySalesCount > 0) "$todaySalesCount sales • Rs ${todaySalesAmountPaisa / 100}" else "Abhi tak koi sale nahi hui (Rs 0)",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (todaySalesCount > 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = if (todaySalesCount > 0) Color(0xFF2E7D32) else Color.Gray,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Text(
                text = "Kaya karna chahte hain?",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            // Primary 2x2 Grid Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ActionCard(
                    title = "Phone Bechna",
                    subtitle = "Farokht",
                    icon = Icons.Default.Sell,
                    backgroundColor = Color(0xFFE8F5E9),
                    contentColor = Color(0xFF2E7D32),
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToSell
                )
                ActionCard(
                    title = "Phone Khareedna",
                    subtitle = "Khareedari",
                    icon = Icons.Default.ShoppingCart,
                    backgroundColor = Color(0xFFE3F2FD),
                    contentColor = Color(0xFF1565C0),
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToBuy
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ActionCard(
                    title = "Khata / Hisaab",
                    subtitle = "Baqi paisay",
                    icon = Icons.Default.MenuBook,
                    backgroundColor = Color(0xFFFFF8E1),
                    contentColor = Color(0xFFF57F17),
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToParties
                )
                ActionCard(
                    title = "Mera Stock",
                    subtitle = "Available phones",
                    icon = Icons.Default.Inventory2,
                    backgroundColor = Color(0xFFF3E5F5),
                    contentColor = Color(0xFF7B1FA2),
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToStock
                )
            }

            HorizontalDivider()

            // Exchange Option
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToExchange() },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Purana de kar naya (Exchange)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Customer purana phone de kar naya le raha hai",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    backgroundColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(130.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(36.dp)
            )
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = contentColor
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = contentColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    BuySellTheme {
        HomeScreen(
            todaySalesCount = 3,
            todaySalesAmountPaisa = 12500000L
        )
    }
}
