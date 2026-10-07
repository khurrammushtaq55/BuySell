package com.mmushtaq04.buysell.presentation.screens.buy

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mmushtaq04.buysell.R
import com.mmushtaq04.buysell.ui.theme.BuySellTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuyWizardScreen(
    currentUserName: String = "Malik / Staff",
    enabledCategories: List<String> = emptyList(),
    onNavigateBack: () -> Unit = {},
    onSavePurchase: (
        categoryName: String,
        brand: String,
        model: String,
        imei: String,
        color: String,
        issue: String,
        ram: String,
        storage: String,
        specs: String,
        priceRs: Long,
        sellerName: String,
        sellerPhone: String,
        sellerCnic: String,
        recordedBy: String,
        paidAmountRs: Long,
        paymentMethodStr: String,
        paymentDetails: String,
        promisedDateStr: String
    ) -> Unit = { _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> },
    onSaveSuccess: () -> Unit = {}
) {
    var step by remember { mutableIntStateOf(1) }

    val activeCategories = if (enabledCategories.isNotEmpty()) {
        enabledCategories
    } else {
        listOf(
            "Mobile", "Tablet / iPad", "Laptop", "Console", "Smartwatch", "Earbuds / Audio", "Accessories"
        )
    }

    // Form states
    var category by remember(activeCategories) { mutableStateOf(activeCategories.firstOrNull() ?: "Mobile") }
    var brand by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var imei by remember { mutableStateOf("") }
    var colorText by remember { mutableStateOf("") }
    var issueText by remember { mutableStateOf("") }
    var ramText by remember { mutableStateOf("") }
    var storageText by remember { mutableStateOf("") }
    var specsText by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }

    var sellerName by remember { mutableStateOf("") }
    var sellerPhone by remember { mutableStateOf("") }
    var sellerCnic by remember { mutableStateOf("") }

    var paidAmountText by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("Cash") }
    var paymentDetailsText by remember { mutableStateOf("") }
    var promisedDateText by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    val methods = listOf("Cash", "Easypaisa", "JazzCash", "Bank Transfer", "Other")

    // Automatically prefill paidAmountText with full priceText
    LaunchedEffect(step, priceText) {
        if (priceText.isNotBlank() && (paidAmountText.isBlank() || step == 4 && paidAmountText.isBlank())) {
            paidAmountText = priceText
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.buy_title) + " (Step $step/4)", fontWeight = FontWeight.Bold) },
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
            // Progress Bar
            LinearProgressIndicator(
                progress = { step / 4f },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = MaterialTheme.colorScheme.primary
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (step) {
                    1 -> StepCategorySelect(
                        categoriesList = activeCategories,
                        selectedCategory = category,
                        onSelectCategory = { category = it }
                    )
                    2 -> StepDeviceDetails(
                        category = category,
                        brand = brand, onBrandChange = { brand = it },
                        model = model, onModelChange = { model = it },
                        imei = imei, onImeiChange = { imei = it },
                        color = colorText, onColorChange = { colorText = it },
                        ram = ramText, onRamChange = { ramText = it },
                        storage = storageText, onStorageChange = { storageText = it },
                        specs = specsText, onSpecsChange = { specsText = it },
                        issue = issueText, onIssueChange = { issueText = it },
                        price = priceText, onPriceChange = { newPrice ->
                            val oldPrice = priceText
                            priceText = newPrice
                            if (paidAmountText.isBlank() || paidAmountText == oldPrice) {
                                paidAmountText = newPrice
                            }
                        }
                    )
                    3 -> StepSellerInfo(
                        name = sellerName, onNameChange = { sellerName = it },
                        phone = sellerPhone, onPhoneChange = { sellerPhone = it },
                        cnic = sellerCnic, onCnicChange = { sellerCnic = it },
                        currentUserName = currentUserName
                    )
                    4 -> StepPayment(
                        totalPrice = priceText.toLongOrNull() ?: 0L,
                        paidAmount = paidAmountText, onPaidChange = { paidAmountText = it },
                        selectedMethod = paymentMethod, onMethodSelect = { paymentMethod = it },
                        details = paymentDetailsText, onDetailsChange = { paymentDetailsText = it },
                        promisedDate = promisedDateText, onPromisedChange = { promisedDateText = it },
                        methodsList = methods
                    )
                }
            }

            // Navigation Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (step > 1) {
                    OutlinedButton(
                        onClick = { step-- },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.action_back))
                    }
                }

                Button(
                    onClick = {
                        if (step < 4) {
                            step++
                        } else {
                            isSaving = true
                            onSavePurchase(
                                category,
                                brand,
                                model,
                                imei,
                                colorText,
                                issueText,
                                ramText,
                                storageText,
                                specsText,
                                priceText.toLongOrNull() ?: 0L,
                                sellerName,
                                sellerPhone,
                                sellerCnic,
                                currentUserName,
                                paidAmountText.toLongOrNull() ?: 0L,
                                paymentMethod,
                                paymentDetailsText,
                                promisedDateText
                            )
                            isSaving = false
                            onSaveSuccess()
                        }
                    },
                    enabled = when (step) {
                        1 -> category.isNotBlank()
                        2 -> brand.isNotBlank() && priceText.isNotBlank()
                        3 -> sellerName.isNotBlank() && sellerPhone.isNotBlank()
                        4 -> paidAmountText.isNotBlank() && !isSaving
                        else -> true
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    if (step < 4) {
                        Text(stringResource(R.string.action_next))
                    } else {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.btn_save_purchase))
                    }
                }
            }
        }
    }
}

@Composable
private fun StepCategorySelect(
    categoriesList: List<String>,
    selectedCategory: String,
    onSelectCategory: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.buy_select_category), fontSize = 16.sp, fontWeight = FontWeight.Bold)

        categoriesList.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { cat ->
                    val isSelected = cat == selectedCategory
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectCategory(cat) },
                        label = { Text(cat, fontSize = 14.sp) },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.CheckCircle, contentDescription = null) }
                        } else null,
                        modifier = Modifier.weight(1f).height(48.dp)
                    )
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StepDeviceDetails(
    category: String,
    brand: String, onBrandChange: (String) -> Unit,
    model: String, onModelChange: (String) -> Unit,
    imei: String, onImeiChange: (String) -> Unit,
    color: String, onColorChange: (String) -> Unit,
    ram: String, onRamChange: (String) -> Unit,
    storage: String, onStorageChange: (String) -> Unit,
    specs: String, onSpecsChange: (String) -> Unit,
    issue: String, onIssueChange: (String) -> Unit,
    price: String, onPriceChange: (String) -> Unit
) {
    val allPresetBrands = listOf("Apple", "Samsung", "Xiaomi", "Vivo", "Oppo", "Realme", "Infinix", "Techno")
    val showDefaultBrandChips = category.equals("Mobile", ignoreCase = true) || category.equals("Tablet / iPad", ignoreCase = true)
    var selectedChip by remember { mutableStateOf<String?>(null) }

    val presetRamList = listOf("4GB", "6GB", "8GB", "12GB", "16GB", "32GB")
    val presetStorageList = listOf("64GB", "128GB", "256GB", "512GB", "1TB")

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.buy_fill_device_details), fontSize = 16.sp, fontWeight = FontWeight.Bold)

        if (showDefaultBrandChips) {
            Text(stringResource(R.string.buy_select_brand), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                (allPresetBrands + "Other").forEach { preset ->
                    val isSelected = (preset == selectedChip) || (preset == brand && preset != "Other")
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedChip = preset
                            if (preset != "Other") {
                                onBrandChange(preset)
                            } else {
                                if (brand in allPresetBrands) {
                                    onBrandChange("")
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
            value = brand,
            onValueChange = {
                onBrandChange(it)
                if (it !in allPresetBrands) {
                    selectedChip = "Other"
                }
            },
            label = { Text(stringResource(R.string.label_brand_name)) },
            isError = brand.isBlank(),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = model,
            onValueChange = onModelChange,
            label = { Text(stringResource(R.string.label_item_model_name)) },
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
                val isSelected = r == ram
                FilterChip(
                    selected = isSelected,
                    onClick = { onRamChange(if (isSelected) "" else r) },
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
                val isSelected = s == storage
                FilterChip(
                    selected = isSelected,
                    onClick = { onStorageChange(if (isSelected) "" else s) },
                    label = { Text(s, fontSize = 12.sp) },
                    leadingIcon = if (isSelected) {
                        { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    } else null
                )
            }
        }

        OutlinedTextField(
            value = imei,
            onValueChange = onImeiChange,
            label = { Text(stringResource(R.string.label_imei)) },
            supportingText = { Text(stringResource(R.string.sub_imei)) },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = color,
            onValueChange = onColorChange,
            label = { Text(stringResource(R.string.label_color)) },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = specs,
            onValueChange = onSpecsChange,
            label = { Text(stringResource(R.string.label_specs)) },
            supportingText = { Text(stringResource(R.string.sub_specs_hint)) },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = issue,
            onValueChange = onIssueChange,
            label = { Text(stringResource(R.string.label_fault_issue)) },
            supportingText = { Text(stringResource(R.string.sub_fault_issue)) },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = price,
            onValueChange = onPriceChange,
            label = { Text(stringResource(R.string.label_purchase_price)) },
            isError = price.isBlank(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun StepSellerInfo(
    name: String, onNameChange: (String) -> Unit,
    phone: String, onPhoneChange: (String) -> Unit,
    cnic: String, onCnicChange: (String) -> Unit,
    currentUserName: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.buy_seller_info_header), fontSize = 16.sp, fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text(stringResource(R.string.label_seller_name)) },
            isError = name.isBlank(),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = phone,
            onValueChange = onPhoneChange,
            label = { Text(stringResource(R.string.label_mobile_number)) },
            isError = phone.isBlank(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = cnic,
            onValueChange = onCnicChange,
            label = { Text(stringResource(R.string.label_cnic)) },
            isError = cnic.isBlank(),
            supportingText = { Text(stringResource(R.string.sub_cnic)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

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
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StepPayment(
    totalPrice: Long,
    paidAmount: String, onPaidChange: (String) -> Unit,
    selectedMethod: String, onMethodSelect: (String) -> Unit,
    details: String, onDetailsChange: (String) -> Unit,
    promisedDate: String, onPromisedChange: (String) -> Unit,
    methodsList: List<String>
) {
    val paidVal = paidAmount.toLongOrNull() ?: 0L
    val remaining = totalPrice - paidVal

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.buy_payment_header), fontSize = 16.sp, fontWeight = FontWeight.Bold)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Kul Qeemat: Rs $totalPrice", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                if (remaining > 0) {
                    Text("Baqi Udhaar: Rs $remaining", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                } else {
                    Text("Mukammal Ada Kar Diya ✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }
        }

        OutlinedTextField(
            value = paidAmount,
            onValueChange = onPaidChange,
            label = { Text(stringResource(R.string.label_paid_amount)) },
            isError = paidAmount.isBlank(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Text("Payment Tariqa Select Karein:", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            methodsList.forEach { method ->
                val isSelected = method == selectedMethod
                FilterChip(
                    selected = isSelected,
                    onClick = { onMethodSelect(method) },
                    label = { Text(method, fontSize = 12.sp) },
                    leadingIcon = if (isSelected) {
                        { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    } else null
                )
            }
        }

        OutlinedTextField(
            value = details,
            onValueChange = onDetailsChange,
            label = { Text("Payment Detail / Reference No") },
            modifier = Modifier.fillMaxWidth()
        )

        if (remaining > 0) {
            OutlinedTextField(
                value = promisedDate,
                onValueChange = onPromisedChange,
                label = { Text("Baqi Wapsi Date") },
                supportingText = { Text("Jaise: 7 din baad, ya tareeq likhein") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BuyWizardPreview() {
    BuySellTheme {
        BuyWizardScreen()
    }
}
