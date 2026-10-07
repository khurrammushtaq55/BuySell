package com.mmushtaq04.buysell.presentation.screens.exchange

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
import androidx.compose.material.icons.filled.SwapHoriz
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
import com.mmushtaq04.buysell.presentation.screens.sell.SimpleStockItem
import com.mmushtaq04.buysell.ui.theme.BuySellTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExchangeWizardScreen(
    stockList: List<SimpleStockItem> = emptyList(),
    currentUserName: String = "Malik / Staff",
    onNavigateBack: () -> Unit = {},
    onSaveExchange: (
        soldStockItemId: String,
        newPhonePriceRs: Long,
        oldCategory: String,
        oldBrand: String,
        oldModel: String,
        oldImei: String,
        oldColor: String,
        oldIssue: String,
        oldPhoneValueRs: Long,
        customerName: String,
        customerPhone: String,
        recordedBy: String,
        cashPaidRs: Long,
        paymentMethodStr: String
    ) -> Unit = { _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> },
    onSaveSuccess: () -> Unit = {}
) {
    var step by remember { mutableIntStateOf(1) }

    // Step 1: Shop's New Phone
    var selectedItem by remember { mutableStateOf<SimpleStockItem?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var newPhonePriceText by remember { mutableStateOf("") }

    // Step 2: Customer's Old Phone
    var oldCategory by remember { mutableStateOf("Mobile") }
    var oldBrand by remember { mutableStateOf("") }
    var oldModel by remember { mutableStateOf("") }
    var oldImei by remember { mutableStateOf("") }
    var oldColorText by remember { mutableStateOf("") }
    var oldIssueText by remember { mutableStateOf("") }
    var oldPhoneValueText by remember { mutableStateOf("") }

    // Step 3: Customer & Payment
    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var cashPaidText by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("Cash") }
    var isSaving by remember { mutableStateOf(false) }

    val methods = listOf("Cash", "Easypaisa", "JazzCash", "Bank Transfer", "Other")
    val presetCategories = listOf("Mobile", "Tablet / iPad", "Laptop", "Console", "Smartwatch", "Earbuds", "Accessories")

    val showDefaultBrandChips = oldCategory.contains("Mobile", ignoreCase = true) ||
            oldCategory.contains("Phone", ignoreCase = true) ||
            oldCategory.contains("Tablet", ignoreCase = true) ||
            oldCategory.contains("iPad", ignoreCase = true)

    val topBrands = listOf("Apple", "Samsung", "Xiaomi", "Vivo", "Google", "OnePlus")
    val otherBrands = listOf("Oppo", "Realme", "Infinix", "Tecno", "Motorola", "Nokia", "Huawei")
    val allPresetBrands = topBrands + otherBrands

    var selectedChip by remember { mutableStateOf(if (oldBrand in allPresetBrands) oldBrand else if (oldBrand.isNotBlank()) "Other" else "") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = "Purana De Kar Naya (Exchange)", fontWeight = FontWeight.Bold, fontSize = 17.sp)
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
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = MaterialTheme.colorScheme.primary
                )

                // Explainer Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.SwapHoriz,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Exchange Flow (Dono 1 Flow Mein)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Purana phone khareed ke stock m jayega, naya bech diya jayega", fontSize = 12.sp)
                        }
                    }
                }

                when (step) {
                    1 -> {
                        Text("1. Shop ka naya phone choose karein:", fontSize = 16.sp, fontWeight = FontWeight.Bold)

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text("Mera Stock se IMEI ya Model search karein") },
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
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Stock mein koi phone nahi — pehle khareedari record karein",
                                    color = Color.Gray,
                                    fontSize = 14.sp
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.height(200.dp),
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
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(item.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                            Text("IMEI: ${item.imei}", fontSize = 12.sp, color = Color.Gray)
                                        }
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = newPhonePriceText,
                            onValueChange = { newPhonePriceText = it },
                            label = { Text("Naye Phone Ki Qeemat / Sale Price (Rs) *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    2 -> {
                        Text("2. Customer ke purane phone ki detail:", fontSize = 16.sp, fontWeight = FontWeight.Bold)

                        Text("Category:", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            presetCategories.forEach { cat ->
                                val isSelected = cat == oldCategory
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { oldCategory = cat },
                                    label = { Text(cat, fontSize = 13.sp) },
                                    leadingIcon = if (isSelected) {
                                        { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                            }
                        }

                        if (showDefaultBrandChips) {
                            Text("Brand select karein:", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                (allPresetBrands + "Other").forEach { preset ->
                                    val isSelected = (preset == selectedChip) || (preset == oldBrand && preset != "Other")
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            selectedChip = preset
                                            if (preset != "Other") {
                                                oldBrand = preset
                                            } else {
                                                if (oldBrand in allPresetBrands) {
                                                    oldBrand = ""
                                                }
                                            }
                                        },
                                        label = { Text(preset, fontSize = 13.sp) },
                                        leadingIcon = if (isSelected) {
                                            { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                        } else null
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = oldBrand,
                            onValueChange = {
                                oldBrand = it
                                if (it !in allPresetBrands) {
                                    selectedChip = "Other"
                                }
                            },
                            label = {
                                Text(
                                    if (showDefaultBrandChips && selectedChip == "Other") "Brand Name (Type manually) *"
                                    else "Brand Name *"
                                )
                            },
                            placeholder = { Text("e.g. Samsung, Apple, Sony, Dell, etc.") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = oldModel,
                            onValueChange = { oldModel = it },
                            label = { Text("Model Name (e.g. Galaxy A12) *") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = oldColorText,
                            onValueChange = { oldColorText = it },
                            label = { Text("Color / Rung (Optional)") },
                            placeholder = { Text("e.g. Black, Gold, Natural Titanium") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = oldIssueText,
                            onValueChange = { oldIssueText = it },
                            label = { Text("Kharabi / Fault / Issue (Optional)") },
                            placeholder = { Text("e.g. Battery health 80%, Glass crack, None") },
                            supportingText = { Text("Agar phone mein koi fault hai to yahan likhein") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = oldImei,
                            onValueChange = { oldImei = it },
                            label = { Text("IMEI / Serial Number") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = oldPhoneValueText,
                            onValueChange = { oldPhoneValueText = it },
                            label = { Text("Purane Phone Ki Qeemat / Khareed Price (Rs) *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    3 -> {
                        val newPrice = newPhonePriceText.toLongOrNull() ?: 0L
                        val oldPrice = oldPhoneValueText.toLongOrNull() ?: 0L
                        val netDiff = newPrice - oldPrice

                        Text("3. Cash Balance & Customer Detail:", fontSize = 16.sp, fontWeight = FontWeight.Bold)

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Naya Phone Sale Price: Rs $newPrice", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Purana Phone Buy Price: Rs $oldPrice", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                when {
                                    netDiff > 0 -> {
                                        Text("Customer Rs $netDiff cash dega", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                    }
                                    netDiff < 0 -> {
                                        Text("Shop customer ko Rs ${-netDiff} cash degi", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                    }
                                    else -> {
                                        Text("Hisaab Barabar (0 cash difference) ✓", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = customerName,
                            onValueChange = { customerName = it },
                            label = { Text("Customer Ka Naam *") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = customerPhone,
                            onValueChange = { customerPhone = it },
                            label = { Text("Mobile Number *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = cashPaidText,
                            onValueChange = { cashPaidText = it },
                            label = { Text("Kitne cash diye / liye? (Rs)") },
                            placeholder = { Text(kotlin.math.abs(netDiff).toString()) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )

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

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        OutlinedTextField(
                            value = currentUserName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Exchange Record Karne Wala (Logged-in User) *") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            supportingText = { Text("Aap ka logged-in account name — badla nahi ja sakta") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (step < 3) {
                            if (step == 1 && (selectedItem == null || newPhonePriceText.isBlank())) return@Button
                            if (step == 2 && (oldBrand.isBlank() || oldPhoneValueText.isBlank())) return@Button
                            step++
                        } else {
                            selectedItem?.let { item ->
                                isSaving = true
                                val newPrice = newPhonePriceText.toLongOrNull() ?: 0L
                                val oldPrice = oldPhoneValueText.toLongOrNull() ?: 0L
                                val cashPaid = cashPaidText.toLongOrNull() ?: kotlin.math.abs(newPrice - oldPrice)

                                onSaveExchange(
                                    item.id,
                                    newPrice,
                                    oldCategory,
                                    oldBrand,
                                    oldModel,
                                    oldImei,
                                    oldColorText,
                                    oldIssueText,
                                    oldPrice,
                                    customerName,
                                    customerPhone,
                                    currentUserName,
                                    cashPaid,
                                    paymentMethod
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
                    if (isSaving) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text(
                            text = if (step < 3) "Aage Chalein (Next)" else "Exchange Final Karein ✓",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ExchangeWizardScreenPreview() {
    val dummyStock = listOf(
        SimpleStockItem("1", "Apple iPhone 15 Pro", "358912345678901", 210000),
        SimpleStockItem("2", "Samsung Galaxy S24 Ultra", "351234567890123", 240000)
    )

    BuySellTheme {
        ExchangeWizardScreen(stockList = dummyStock)
    }
}
