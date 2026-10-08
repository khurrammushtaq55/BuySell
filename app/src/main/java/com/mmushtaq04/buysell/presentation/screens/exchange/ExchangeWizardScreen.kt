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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mmushtaq04.buysell.R
import com.mmushtaq04.buysell.presentation.screens.sell.SimpleStockItem
import com.mmushtaq04.buysell.ui.theme.BuySellTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExchangeWizardScreen(
    stockList: List<SimpleStockItem> = emptyList(),
    currentUserName: String = "Malik / Staff",
    enabledCategories: List<String> = emptyList(),
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
        oldRam: String,
        oldStorage: String,
        oldSpecs: String,
        oldPhoneValueRs: Long,
        customerName: String,
        customerPhone: String,
        recordedBy: String,
        cashPaidRs: Long,
        paymentMethodStr: String
    ) -> Unit = { _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> },
    onSaveSuccess: () -> Unit = {}
) {
    var step by remember { mutableIntStateOf(1) }

    val activeCategories = enabledCategories.ifEmpty {
        listOf("Mobile", "Tablet / iPad", "Laptop", "Console", "Smartwatch", "Earbuds", "Accessories")
    }

    // Step 1: Shop's New Device
    var selectedItem by remember { mutableStateOf<SimpleStockItem?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var newPhonePriceText by remember { mutableStateOf("") }

    // Step 2: Customer's Old Trade-In Device
    var oldCategory by remember { mutableStateOf(activeCategories.firstOrNull() ?: "Mobile") }
    var oldBrand by remember { mutableStateOf("") }
    var oldModel by remember { mutableStateOf("") }
    var oldImei by remember { mutableStateOf("") }
    var oldColorText by remember { mutableStateOf("") }
    var oldIssueText by remember { mutableStateOf("") }
    var oldRamText by remember { mutableStateOf("") }
    var oldStorageText by remember { mutableStateOf("") }
    var oldSpecsText by remember { mutableStateOf("") }
    var oldPhoneValueText by remember { mutableStateOf("") }

    // Step 3: Customer & Payment
    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var cashPaidText by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("Cash") }
    var isSaving by remember { mutableStateOf(false) }

    val methods = listOf("Cash", "Easypaisa", "JazzCash", "Bank Transfer", "Other")

    val allPresetBrands = listOf("Apple", "Samsung", "Xiaomi", "Vivo", "Oppo", "Realme", "Infinix", "Techno")
    val showDefaultBrandChips = oldCategory.equals("Mobile", ignoreCase = true) || oldCategory.equals("Tablet / iPad", ignoreCase = true)
    var selectedChip by remember { mutableStateOf<String?>(null) }

    val presetRamList = listOf("4GB", "6GB", "8GB", "12GB", "16GB", "32GB")
    val presetStorageList = listOf("64GB", "128GB", "256GB", "512GB", "1TB")

    val filteredStock = stockList.filter {
        it.title.contains(searchQuery, ignoreCase = true) || it.imei.contains(searchQuery, ignoreCase = true)
    }

    // Automatically prefill cashPaidText with net cash difference in Step 3
    val newPriceVal = newPhonePriceText.toLongOrNull() ?: 0L
    val oldPriceVal = oldPhoneValueText.toLongOrNull() ?: 0L
    val netDiffVal = (newPriceVal - oldPriceVal).coerceAtLeast(0L)

    LaunchedEffect(step, newPhonePriceText, oldPhoneValueText) {
        if (step == 3 && (cashPaidText.isBlank() || cashPaidText == "0")) {
            if (netDiffVal > 0) {
                cashPaidText = netDiffVal.toString()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.exchange_wizard_title, step, 3), fontWeight = FontWeight.Bold) },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (step) {
                1 -> {
                    // Step 1: Select new item from stock
                    Text(stringResource(R.string.exchange_step1_header), fontSize = 16.sp, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text(stringResource(R.string.exchange_search_label)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredStock) { item ->
                            val isSelected = selectedItem?.id == item.id
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedItem = item },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(item.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text("IMEI: ${item.imei}", fontSize = 12.sp, color = Color.Gray)
                                    }
                                    if (isSelected) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }

                    if (selectedItem != null) {
                        OutlinedTextField(
                            value = newPhonePriceText,
                            onValueChange = { newPhonePriceText = it },
                            label = { Text(stringResource(R.string.exchange_new_price_label)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Button(
                        onClick = { step = 2 },
                        enabled = selectedItem != null && newPhonePriceText.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.exchange_btn_step2))
                    }
                }

                2 -> {
                    // Step 2: Customer's old device details
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(stringResource(R.string.exchange_step2_header), fontSize = 16.sp, fontWeight = FontWeight.Bold)

                        Text(stringResource(R.string.exchange_select_category), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            activeCategories.forEach { cat ->
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
                            Text(stringResource(R.string.buy_select_brand), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
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
                            label = { Text(stringResource(R.string.label_brand_name)) },
                            isError = oldBrand.isBlank(),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = oldModel,
                            onValueChange = { oldModel = it },
                            label = { Text(stringResource(R.string.exchange_old_model_label)) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Memory / RAM Selection
                        Text(stringResource(R.string.label_ram), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            presetRamList.forEach { r ->
                                val isSelected = r == oldRamText
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { oldRamText = if (isSelected) "" else r },
                                    label = { Text(r, fontSize = 12.sp) },
                                    leadingIcon = if (isSelected) {
                                        { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                    } else null
                                )
                            }
                        }

                        // Storage / Capacity Selection
                        Text(stringResource(R.string.label_storage), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            presetStorageList.forEach { s ->
                                val isSelected = s == oldStorageText
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { oldStorageText = if (isSelected) "" else s },
                                    label = { Text(s, fontSize = 12.sp) },
                                    leadingIcon = if (isSelected) {
                                        { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                    } else null
                                )
                            }
                        }

                        OutlinedTextField(
                            value = oldImei,
                            onValueChange = { oldImei = it },
                            label = { Text(stringResource(R.string.exchange_old_imei_label)) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = oldColorText,
                            onValueChange = { oldColorText = it },
                            label = { Text(stringResource(R.string.label_color)) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = oldSpecsText,
                            onValueChange = { oldSpecsText = it },
                            label = { Text(stringResource(R.string.label_specs)) },
                            supportingText = { Text(stringResource(R.string.sub_specs_hint)) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = oldIssueText,
                            onValueChange = { oldIssueText = it },
                            label = { Text(stringResource(R.string.exchange_defects_label)) },
                            supportingText = { Text(stringResource(R.string.exchange_defects_sub)) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = oldPhoneValueText,
                            onValueChange = { oldPhoneValueText = it },
                            label = { Text(stringResource(R.string.exchange_old_value_label)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Button(
                        onClick = { step = 3 },
                        enabled = oldCategory.isNotBlank() && oldPhoneValueText.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.exchange_btn_step3))
                    }
                }

                3 -> {
                    // Step 3: Customer Info & Cash Difference
                    val newPrice = newPhonePriceText.toLongOrNull() ?: 0L
                    val oldVal = oldPhoneValueText.toLongOrNull() ?: 0L
                    val diff = newPrice - oldVal

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(stringResource(R.string.exchange_step3_header), fontSize = 16.sp, fontWeight = FontWeight.Bold)

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(stringResource(R.string.exchange_new_price_summary, newPrice), fontWeight = FontWeight.SemiBold)
                                Text(stringResource(R.string.exchange_old_value_summary, oldVal), fontWeight = FontWeight.SemiBold)
                                HorizontalDivider()
                                Text(
                                    text = if (diff >= 0) stringResource(R.string.exchange_customer_due, diff) else stringResource(R.string.exchange_shop_due, -diff),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = if (diff >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        OutlinedTextField(
                            value = customerName,
                            onValueChange = { customerName = it },
                            label = { Text(stringResource(R.string.exchange_customer_name_label)) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = customerPhone,
                            onValueChange = { customerPhone = it },
                            label = { Text(stringResource(R.string.exchange_customer_phone_label)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = cashPaidText,
                            onValueChange = { cashPaidText = it },
                            label = { Text(stringResource(R.string.exchange_cash_paid_label)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(stringResource(R.string.exchange_payment_method_label), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            methods.forEach { method ->
                                FilterChip(
                                    selected = paymentMethod == method,
                                    onClick = { paymentMethod = method },
                                    label = { Text(method, fontSize = 12.sp) },
                                    leadingIcon = if (paymentMethod == method) {
                                        { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                    } else null
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        OutlinedTextField(
                            value = currentUserName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.label_recorded_by)) },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            supportingText = { Text(stringResource(R.string.sub_readonly_user)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Button(
                        onClick = {
                            val selected = selectedItem ?: return@Button
                            isSaving = true
                            onSaveExchange(
                                selected.id,
                                newPrice,
                                oldCategory,
                                oldBrand,
                                oldModel,
                                oldImei,
                                oldColorText,
                                oldIssueText,
                                oldRamText,
                                oldStorageText,
                                oldSpecsText,
                                oldVal,
                                customerName,
                                customerPhone,
                                currentUserName,
                                cashPaidText.toLongOrNull() ?: 0L,
                                paymentMethod
                            )
                            isSaving = false
                            onSaveSuccess()
                        },
                        enabled = !isSaving,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.exchange_btn_finish))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ExchangeWizardPreview() {
    BuySellTheme {
        ExchangeWizardScreen()
    }
}
