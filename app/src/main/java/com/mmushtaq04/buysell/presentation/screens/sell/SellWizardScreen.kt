package com.mmushtaq04.buysell.presentation.screens.sell

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mmushtaq04.buysell.ui.theme.BuySellTheme

data class SimpleStockItem(val id: String, val title: String, val imei: String, val cost: Long)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SellWizardScreen(
    stockList: List<SimpleStockItem> = emptyList(),
    currentUserName: String = "Malik / Staff",
    onNavigateBack: () -> Unit = {},
    onSaveSale: (
        stockItemId: String,
        salePriceRs: Long,
        buyerName: String,
        buyerPhone: String,
        recordedBy: String,
        receivedAmountRs: Long,
        paymentMethodStr: String,
        paymentDetails: String,
        promisedDateStr: String
    ) -> Unit = { _, _, _, _, _, _, _, _, _ -> },
    onSaveSuccess: () -> Unit = {}
) {
    var step by remember { mutableIntStateOf(1) }

    var selectedItem by remember { mutableStateOf<SimpleStockItem?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    var buyerName by remember { mutableStateOf("") }
    var buyerPhone by remember { mutableStateOf("") }
    var salePriceText by remember { mutableStateOf("") }
    var receivedAmountText by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("Cash") }
    var paymentDetails by remember { mutableStateOf("") }
    var promisedDateText by remember { mutableStateOf("") }

    val methods = listOf("Cash", "Easypaisa", "JazzCash", "Bank Transfer", "Other")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = "Phone Bechna", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(text = "Step $step of 3", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (step > 1) step-- else onNavigateBack()
                    }) {
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
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                LinearProgressIndicator(
                    progress = { step / 3f },
                    modifier = Modifier.fillMaxWidth().height(8.dp)
                )

                when (step) {
                    1 -> {
                        Text("Stock se phone choose karein:", fontSize = 16.sp, fontWeight = FontWeight.Bold)

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text("IMEI ya Model search karein") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        val filtered = stockList.filter {
                            it.title.contains(searchQuery, ignoreCase = true) || it.imei.contains(searchQuery)
                        }

                        if (filtered.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Pehle stock mein phone hona chahiye — pehle khareedari record karein",
                                    color = Color.Gray,
                                    fontSize = 14.sp
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.height(280.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filtered) { item ->
                                    val isSelected = selectedItem?.id == item.id
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedItem = item },
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text(item.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                            Text("IMEI: ${item.imei}", fontSize = 12.sp, color = Color.Gray)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        Text("Gahak (Buyer) ki detail:", fontSize = 16.sp, fontWeight = FontWeight.Bold)

                        selectedItem?.let { item ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Selected Item: ${item.title}", fontWeight = FontWeight.Bold)
                                    Text("IMEI: ${item.imei}", fontSize = 12.sp)
                                }
                            }
                        }

                        OutlinedTextField(
                            value = buyerName,
                            onValueChange = { buyerName = it },
                            label = { Text("Buyer Ka Naam *") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = buyerPhone,
                            onValueChange = { buyerPhone = it },
                            label = { Text("Mobile Number *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = salePriceText,
                            onValueChange = { salePriceText = it },
                            label = { Text("Bechnay ki qeemat (Rs) *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Text("Sale Record Karne Wala (User):", fontSize = 15.sp, fontWeight = FontWeight.Bold)

                        OutlinedTextField(
                            value = currentUserName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Record Karne Wala (Logged-in User) *") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            supportingText = { Text("Aap ka logged-in account name — badla nahi ja sakta") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    3 -> {
                        val salePrice = salePriceText.toLongOrNull() ?: 0L
                        val received = receivedAmountText.toLongOrNull() ?: salePrice
                        val remaining = (salePrice - received).coerceAtLeast(0L)

                        Text("Payment confirmation:", fontSize = 16.sp, fontWeight = FontWeight.Bold)

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Sale Price: Rs $salePrice", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text("Record karne wala: $currentUserName", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (remaining > 0) {
                                    Text("Customer par baqi udhaar: Rs $remaining", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
                                } else {
                                    Text("Full payment received ✓", color = Color(0xFF2E7D32))
                                }
                            }
                        }

                        OutlinedTextField(
                            value = receivedAmountText,
                            onValueChange = { receivedAmountText = it },
                            label = { Text("Customer ne kitne diye? (Rs)") },
                            placeholder = { Text(salePrice.toString()) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Mode of Payment Selection
                        Text("Mode of Payment:", fontSize = 14.sp, fontWeight = FontWeight.Bold)

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            methods.forEach { method ->
                                val isSelected = method == paymentMethod
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { paymentMethod = method },
                                    label = { Text(method, fontSize = 13.sp) },
                                    leadingIcon = if (isSelected) {
                                        { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                            }
                        }

                        // Optional / Custom Details Text Box
                        OutlinedTextField(
                            value = paymentDetails,
                            onValueChange = { paymentDetails = it },
                            label = {
                                Text(
                                    if (paymentMethod == "Other") "Payment Method Detail / Name *"
                                    else "Bank / Account / Ref Info (Optional)"
                                )
                            },
                            placeholder = { Text("e.g. HBL / Easypaisa Txn #12345 / Account title") },
                            supportingText = { Text("Bank name, account details, or transaction ID") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (remaining > 0) {
                            OutlinedTextField(
                                value = promisedDateText,
                                onValueChange = { promisedDateText = it },
                                label = { Text("Baqi kab dega? (e.g. 20 March)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (step < 3) {
                            if (step == 1 && selectedItem == null) return@Button
                            step++
                        } else {
                            selectedItem?.let { item ->
                                val salePrice = salePriceText.toLongOrNull() ?: 0L
                                val received = receivedAmountText.toLongOrNull() ?: salePrice
                                onSaveSale(
                                    item.id,
                                    salePrice,
                                    buyerName,
                                    buyerPhone,
                                    currentUserName,
                                    received,
                                    paymentMethod,
                                    paymentDetails,
                                    promisedDateText
                                )
                            }
                            onSaveSuccess()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (step < 3) "Aage Chalein (Next)" else "Sale Record Karein ✓",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SellWizardScreenPreview() {
    val dummyStock = listOf(
        SimpleStockItem("1", "Apple iPhone 15 Pro", "358912345678901", 210000),
        SimpleStockItem("2", "Samsung Galaxy S24 Ultra", "351234567890123", 240000),
        SimpleStockItem("3", "iPad Air 5th Gen", "SER987654321", 115000)
    )

    BuySellTheme {
        SellWizardScreen(stockList = dummyStock)
    }
}
