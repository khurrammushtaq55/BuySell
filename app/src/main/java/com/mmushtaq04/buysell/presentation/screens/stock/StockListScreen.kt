package com.mmushtaq04.buysell.presentation.screens.stock

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
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

data class DisplayStock(
    val id: String,
    val brand: String,
    val model: String,
    val imei: String,
    val category: String,
    val color: String = "",
    val issue: String = "",
    val remainingQty: Int,
    val status: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockListScreen(
    stockItems: List<DisplayStock> = emptyList(),
    onNavigateBack: () -> Unit = {},
    onSelectItem: (String) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedStockDetail by remember { mutableStateOf<DisplayStock?>(null) }

    val filtered = stockItems.filter {
        it.model.contains(searchQuery, ignoreCase = true) ||
                it.brand.contains(searchQuery, ignoreCase = true) ||
                it.imei.contains(searchQuery) ||
                it.color.contains(searchQuery, ignoreCase = true)
    }

    if (selectedStockDetail != null) {
        val item = selectedStockDetail!!
        AlertDialog(
            onDismissRequest = { selectedStockDetail = null },
            icon = { Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = {
                Text(
                    text = "${item.brand} ${item.model}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailRow(label = "IMEI / Serial:", value = if (item.imei != "N/A") item.imei else "None")
                    DetailRow(label = "Color / Rung:", value = if (item.color.isNotBlank()) item.color else "N/A")
                    DetailRow(label = "Kharabi / Issue:", value = if (item.issue.isNotBlank()) item.issue else "Koi kharabi nahi (Clean)")
                    DetailRow(label = "Remaining Quantity:", value = "${item.remainingQty}")
                    DetailRow(label = "Status:", value = item.status)
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedStockDetail = null }) {
                    Text(stringResource(R.string.stock_close_dialog))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.stock_title, filtered.size), fontWeight = FontWeight.Bold) },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text(stringResource(R.string.stock_search_label)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth()
            )

            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.stock_empty_msg),
                        color = Color.Gray,
                        fontSize = 15.sp
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(filtered) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedStockDetail = item
                                    onSelectItem(item.id)
                                },
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
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("${item.brand} ${item.model}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    if (item.imei != "N/A") {
                                        Text("IMEI: ${item.imei}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (item.color.isNotBlank()) {
                                        Text("Color: ${item.color}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (item.remainingQty > 0) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                                ) {
                                    Text(
                                        text = if (item.remainingQty > 1) stringResource(R.string.stock_qty_label, item.remainingQty) else stringResource(R.string.stock_in_stock_label),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (item.remainingQty > 0) Color(0xFF2E7D32) else Color(0xFFC62828)
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

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Text(text = value, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Preview(showBackground = true)
@Composable
fun StockListScreenPreview() {
    val sampleItems = listOf(
        DisplayStock("1", "Apple", "iPhone 15 Pro Max 256GB", "358912345678901", "Mobile", "Natural Titanium", "None", 1, "IN_STOCK"),
        DisplayStock("2", "Samsung", "Galaxy S24 Ultra", "351234567890123", "Mobile", "Titanium Gray", "Glass crack minor", 1, "IN_STOCK")
    )

    BuySellTheme {
        StockListScreen(stockItems = sampleItems)
    }
}
