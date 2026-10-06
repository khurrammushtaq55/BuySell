package com.mmushtaq04.buysell.presentation.screens.stock

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class DisplayStock(
    val id: String,
    val brand: String,
    val model: String,
    val imei: String,
    val category: String,
    val remainingQty: Int,
    val status: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockListScreen(
    onNavigateBack: () -> Unit,
    onSelectItem: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val sampleItems = listOf(
        DisplayStock("1", "Apple", "iPhone 15 Pro Max 256GB", "358912345678901", "Mobile", 1, "IN_STOCK"),
        DisplayStock("2", "Samsung", "Galaxy S24 Ultra", "351234567890123", "Mobile", 1, "IN_STOCK"),
        DisplayStock("3", "Apple", "iPad Air 5 64GB WiFi", "SER987654321", "Tablet", 1, "IN_STOCK"),
        DisplayStock("4", "Anker", "20W Fast Charger", "N/A", "Accessories", 8, "IN_STOCK")
    )

    val filtered = sampleItems.filter {
        it.model.contains(searchQuery, ignoreCase = true) ||
                it.brand.contains(searchQuery, ignoreCase = true) ||
                it.imei.contains(searchQuery)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mera Stock (${filtered.size})", fontWeight = FontWeight.Bold) },
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
                label = { Text("Search by IMEI, Brand, or Model") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth()
            )

            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Koi stock item nahi mila", color = Color.Gray, fontSize = 16.sp)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(filtered) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectItem(item.id) },
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
                                    Text("${item.brand} ${item.model}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    if (item.imei != "N/A") {
                                        Text("IMEI: ${item.imei}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text("Category: ${item.category}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (item.remainingQty > 0) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                                ) {
                                    Text(
                                        text = if (item.remainingQty > 1) "Qty: ${item.remainingQty}" else "In Stock",
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
