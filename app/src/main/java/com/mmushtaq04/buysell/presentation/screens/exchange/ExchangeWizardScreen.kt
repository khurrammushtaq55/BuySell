package com.mmushtaq04.buysell.presentation.screens.exchange

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mmushtaq04.buysell.R
import com.mmushtaq04.buysell.presentation.components.ExpandableOptionalSection
import com.mmushtaq04.buysell.presentation.screens.sell.SimpleStockItem
import com.mmushtaq04.buysell.ui.theme.BuySellTheme
import com.mmushtaq04.buysell.util.AppPreferencesManager
import com.mmushtaq04.buysell.util.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExchangeWizardScreen(
    stockList: List<SimpleStockItem> = emptyList(),
    currentUserName: String = "Malik / Staff",
    enabledCategories: List<String> = emptyList(),
    initialStep: Int = 1,
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
        customerCnic: String,
        recordedBy: String,
        cashPaidRs: Long,
        paymentMethodStr: String
    ) -> Unit = { _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> },
    onSaveSuccess: () -> Unit = {}
) {
    val context = LocalContext.current
    var step by remember { mutableIntStateOf(initialStep) }

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
    var customerCnic by remember { mutableStateOf("") }
    var cashPaidText by remember { mutableStateOf("") }
    val methods = stringArrayResource(R.array.payment_methods).toList()
    val defaultPaymentMethod = methods.firstOrNull() ?: stringResource(R.string.payment_method_cash)
    var paymentMethod by remember(defaultPaymentMethod) { mutableStateOf(defaultPaymentMethod) }
    var isSaving by remember { mutableStateOf(false) }

    // Preference for showing original bought price (purchase cost)
    val isPrefEnabled = remember { AppPreferencesManager.isShowBuyCostInSellEnabled(context) }
    var showBoughtCostInExchange by remember { mutableStateOf(false) }

    val allPresetBrands = listOf("Apple", "Samsung", "Xiaomi", "Vivo", "Oppo", "Realme", "Infinix", "Techno")
    val showDefaultBrandChips = oldCategory.equals("Mobile", ignoreCase = true) || oldCategory.equals("Tablet / iPad", ignoreCase = true)
    var selectedChip by remember { mutableStateOf<String?>(null) }

    val presetRamList = listOf("2GB", "4GB", "6GB", "8GB", "12GB", "16GB", "24GB", "32GB", "64GB", "128GB", "256GB", "512GB", "1TB")
    val presetStorageList = listOf("32GB", "64GB", "128GB", "256GB", "512GB", "1TB", "2TB", "4TB", "8TB")

    val filteredStock = stockList.filter {
        it.title.contains(searchQuery, ignoreCase = true) || it.imei.contains(searchQuery, ignoreCase = true)
    }

    val newPriceVal = newPhonePriceText.toLongOrNull() ?: 0L
    val oldPriceVal = oldPhoneValueText.toLongOrNull() ?: 0L
    val absDiffVal = kotlin.math.abs(newPriceVal - oldPriceVal)

    LaunchedEffect(step, newPhonePriceText, oldPhoneValueText) {
        if (step == 3) {
            cashPaidText = absDiffVal.toString()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.exchange_wizard_title, step, 3), fontWeight = FontWeight.Bold, fontSize = 18.sp)
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Segmented Progress Bar (Option 1: Modern & Minimalist 3-Step Segments)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (i in 1..3) {
                    val isCompletedOrActive = i <= step
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                if (isCompletedOrActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            )
                    )
                }
            }

            when (step) {
                1 -> {
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
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text("IMEI: ${item.imei}", fontSize = 12.sp, color = Color.Gray)
                                        if (isPrefEnabled && item.cost > 0) {
                                            Text(
                                                "Bought Price: ${CurrencyFormatter.formatAmount(context, item.cost)}",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                    if (isSelected) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }

                    if (selectedItem != null) {
                        // Bought Price Card with Eye Toggle
                        if (isPrefEnabled && selectedItem!!.cost > 0) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Original Bought Price: ", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            if (showBoughtCostInExchange) CurrencyFormatter.formatAmount(context, selectedItem!!.cost) else "••••••",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(
                                        onClick = { showBoughtCostInExchange = !showBoughtCostInExchange },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            if (showBoughtCostInExchange) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = "Toggle Cost Visibility",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = newPhonePriceText,
                            onValueChange = { newValue -> newPhonePriceText = newValue.filter { it.isDigit() } },
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
                    // Step 2: Customer's old trade-in device details
                    val oldValNum = oldPhoneValueText.toLongOrNull() ?: 0L
                    val isOldValueValid = oldPhoneValueText.isNotBlank() && oldValNum > 0L

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

                        val identifierLabelRes = when {
                            oldCategory.contains("Mobile", ignoreCase = true) -> R.string.label_imei
                            oldCategory.contains("Tablet", ignoreCase = true) || oldCategory.contains("Laptop", ignoreCase = true) || oldCategory.contains("Console", ignoreCase = true) || oldCategory.contains("Smartwatch", ignoreCase = true) || oldCategory.contains("Camera", ignoreCase = true) -> R.string.exchange_old_imei_label
                            else -> R.string.label_serial_or_item_code
                        }

                        OutlinedTextField(
                            value = oldImei,
                            onValueChange = { oldImei = it },
                            label = { Text(stringResource(identifierLabelRes)) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = oldPhoneValueText,
                            onValueChange = { newValue -> oldPhoneValueText = newValue.filter { it.isDigit() } },
                            label = { Text(stringResource(R.string.exchange_old_value_label) + " *") },
                            isError = oldPhoneValueText.isNotBlank() && oldValNum <= 0L,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Collapsible Optional Details Section (RAM, Storage, Color, Specs, Defects)
                        val hasOptionalValues = oldRamText.isNotBlank() || oldStorageText.isNotBlank() || oldColorText.isNotBlank() || oldSpecsText.isNotBlank() || oldIssueText.isNotBlank()
                        val hasRamStoragePresets = oldCategory.contains("Mobile", ignoreCase = true) || oldCategory.contains("Tablet", ignoreCase = true) || oldCategory.contains("Laptop", ignoreCase = true)

                        ExpandableOptionalSection(
                            initialExpanded = hasOptionalValues
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                if (hasRamStoragePresets) {
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
                                }

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
                            }
                        }
                    }

                    Button(
                        onClick = { step = 3 },
                        enabled = oldCategory.isNotBlank() && isOldValueValid,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.exchange_btn_step3))
                    }
                }

                3 -> {
                    // Step 3: Customer Info & Cash Difference Summary
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

                        // Summary Bought Price Card
                        if (isPrefEnabled && selectedItem != null && selectedItem!!.cost > 0) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Original Bought Price: ", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            if (showBoughtCostInExchange) CurrencyFormatter.formatAmount(context, selectedItem!!.cost) else "••••••",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(
                                        onClick = { showBoughtCostInExchange = !showBoughtCostInExchange },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            if (showBoughtCostInExchange) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = "Toggle Cost Visibility",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = customerName,
                            onValueChange = { customerName = it },
                            label = { Text(stringResource(R.string.exchange_customer_name_label) + " *") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            isError = customerName.isBlank(),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = customerPhone,
                            onValueChange = { newValue -> customerPhone = newValue.filter { it.isDigit() || it == '+' } },
                            label = { Text(stringResource(R.string.exchange_customer_phone_label) + " *") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                            isError = customerPhone.isBlank(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = customerCnic,
                            onValueChange = { newValue -> customerCnic = newValue.filter { it.isDigit() } },
                            label = { Text(stringResource(R.string.label_cnic)) },
                            leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = customerCnic.isBlank(),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = cashPaidText,
                            onValueChange = { newValue -> cashPaidText = newValue.filter { it.isDigit() } },
                            label = { Text(stringResource(R.string.exchange_cash_paid_label) + " *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = cashPaidText.isBlank(),
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
                                customerCnic,
                                currentUserName,
                                cashPaidText.toLongOrNull() ?: 0L,
                                paymentMethod
                            )
                            isSaving = false
                            onSaveSuccess()
                        },
                        enabled = !isSaving && customerName.isNotBlank() && customerPhone.isNotBlank() && cashPaidText.isNotBlank(),
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

@Preview(showBackground = true, name = "Exchange Step 1 - Select Shop Stock")
@Composable
fun ExchangeWizardStep1Preview() {
    BuySellTheme {
        ExchangeWizardScreen(
            initialStep = 1,
            stockList = listOf(
                SimpleStockItem("1", "Apple iPhone 15 Pro Max 256GB", "358912345678901", 180000),
                SimpleStockItem("2", "Samsung Galaxy S24 Ultra", "351234567890123", 210000)
            ),
            currentUserName = "Malik Mushtaq (Owner)"
        )
    }
}

@Preview(showBackground = true, name = "Exchange Step 2 - Trade-In Device Details")
@Composable
fun ExchangeWizardStep2Preview() {
    BuySellTheme {
        ExchangeWizardScreen(
            initialStep = 2,
            stockList = listOf(
                SimpleStockItem("1", "Apple iPhone 15 Pro Max 256GB", "358912345678901", 180000)
            ),
            currentUserName = "Malik Mushtaq (Owner)"
        )
    }
}

@Preview(showBackground = true, name = "Exchange Step 3 - Settlement & Customer Info")
@Composable
fun ExchangeWizardStep3Preview() {
    BuySellTheme {
        ExchangeWizardScreen(
            initialStep = 3,
            stockList = listOf(
                SimpleStockItem("1", "Apple iPhone 15 Pro Max 256GB", "358912345678901", 180000)
            ),
            currentUserName = "Malik Mushtaq (Owner)"
        )
    }
}
