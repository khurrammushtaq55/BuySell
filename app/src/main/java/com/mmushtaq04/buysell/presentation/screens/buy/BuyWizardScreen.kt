package com.mmushtaq04.buysell.presentation.screens.buy

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        priceRs: Long,
        sellerName: String,
        sellerPhone: String,
        sellerCnic: String,
        recordedBy: String,
        paidAmountRs: Long,
        paymentMethodStr: String,
        paymentDetails: String,
        promisedDateStr: String
    ) -> Unit = { _, _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> },
    onSaveSuccess: () -> Unit = {}
) {
    var step by remember { mutableIntStateOf(1) }

    val activeCategories = enabledCategories.ifEmpty {
        listOf(
        "Mobile", "Tablet / iPad", "Laptop", "Console", "Smartwatch", "Earbuds / Audio", "Accessories"
    )
    }

    // Form states
    var category by remember { mutableStateOf(activeCategories.firstOrNull() ?: "Mobile") }
    var brand by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var imei by remember { mutableStateOf("") }
    var colorText by remember { mutableStateOf("") }
    var issueText by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }

    var sellerName by remember { mutableStateOf("") }
    var sellerPhone by remember { mutableStateOf("") }
    var sellerCnic by remember { mutableStateOf("") }

    var paidAmountText by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("Cash") }
    var paymentDetails by remember { mutableStateOf("") }
    var promisedDateText by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    val isStepValid = when (step) {
        1 -> category.isNotBlank()
        2 -> brand.isNotBlank() && model.isNotBlank() && (priceText.toLongOrNull() ?: 0L) > 0
        3 -> sellerName.isNotBlank() && sellerPhone.isNotBlank() && sellerCnic.isNotBlank()
        4 -> !isSaving
        else -> false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = "Phone Khareedna", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(text = "Step $step of 4", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                // Progress Bar
                LinearProgressIndicator(
                    progress = { step / 4f },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = MaterialTheme.colorScheme.primary
                )

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
                        issue = issueText, onIssueChange = { issueText = it },
                        price = priceText, onPriceChange = { priceText = it }
                    )
                    3 -> StepSellerInfo(
                        name = sellerName, onNameChange = { sellerName = it },
                        phone = sellerPhone, onPhoneChange = { sellerPhone = it },
                        cnic = sellerCnic, onCnicChange = { sellerCnic = it },
                        currentUserName = currentUserName
                    )
                    4 -> StepPaymentInfo(
                        totalPrice = priceText.toLongOrNull() ?: 0L,
                        paidAmount = paidAmountText, onPaidAmountChange = { paidAmountText = it },
                        selectedMethod = paymentMethod, onMethodChange = { paymentMethod = it },
                        paymentDetails = paymentDetails, onDetailsChange = { paymentDetails = it },
                        promisedDate = promisedDateText, onPromisedDateChange = { promisedDateText = it }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Navigation Button
                Button(
                    onClick = {
                        if (step < 4) {
                            step++
                        } else {
                            isSaving = true
                            val price = priceText.toLongOrNull() ?: 0L
                            val paid = paidAmountText.toLongOrNull() ?: price
                            onSavePurchase(
                                category,
                                brand,
                                model,
                                imei,
                                colorText,
                                issueText,
                                price,
                                sellerName,
                                sellerPhone,
                                sellerCnic,
                                currentUserName,
                                paid,
                                paymentMethod,
                                paymentDetails,
                                promisedDateText
                            )
                            onSaveSuccess()
                        }
                    },
                    enabled = isStepValid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text(
                            text = if (step < 4) "Aage Chalein (Next)" else "Save Karein ✓",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
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
        Text("Category choose karein:", fontSize = 16.sp, fontWeight = FontWeight.Bold)

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
    issue: String, onIssueChange: (String) -> Unit,
    price: String, onPriceChange: (String) -> Unit
) {
    val showDefaultBrandChips = category.contains("Mobile", ignoreCase = true) ||
            category.contains("Phone", ignoreCase = true) ||
            category.contains("Tablet", ignoreCase = true) ||
            category.contains("iPad", ignoreCase = true)

    val topBrands = listOf("Apple", "Samsung", "Xiaomi", "Vivo", "Google", "OnePlus")
    val otherBrands = listOf("Oppo", "Realme", "Infinix", "Tecno", "Motorola", "Nokia", "Huawei")
    val allPresetBrands = topBrands + otherBrands

    var selectedChip by remember { mutableStateOf(if (brand in allPresetBrands) brand else if (brand.isNotBlank()) "Other" else "") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Device ki detail bharein (Mandatory fields *) :", fontSize = 16.sp, fontWeight = FontWeight.Bold)

        if (showDefaultBrandChips) {
            Text("Brand select karein:", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)

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
            label = {
                Text(
                    if (showDefaultBrandChips && selectedChip == "Other") "Brand Name (Type manually) *"
                    else "Brand Name *"
                )
            },
            isError = brand.isBlank(),
            placeholder = { Text("e.g. Apple, Samsung, Sony, Bose, Dell, etc.") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = model,
            onValueChange = onModelChange,
            label = { Text("Model Name / Item Name *") },
            isError = model.isBlank(),
            placeholder = { Text("e.g. Galaxy S23, Charger 20W, Airpods Pro") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = color,
            onValueChange = onColorChange,
            label = { Text("Color / Rung (Optional)") },
            placeholder = { Text("e.g. Black, Gold, Natural Titanium") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = issue,
            onValueChange = onIssueChange,
            label = { Text("Kharabi / Fault / Issue (Optional)") },
            placeholder = { Text("e.g. Battery health 80%, Glass crack, None") },
            supportingText = { Text("Agar phone mein koi fault hai to yahan likhein") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = imei,
            onValueChange = onImeiChange,
            label = { Text("IMEI / Serial Number (Optional for accessories)") },
            supportingText = { Text("15 digit IMEI or Serial number") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = price,
            onValueChange = onPriceChange,
            label = { Text("Khareedne ki qeemat (Rs) *") },
            isError = (price.toLongOrNull() ?: 0L) <= 0,
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
        Text("Bechne wale ki maloomat (Seller - Mandatory *):", fontSize = 16.sp, fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Seller Ka Naam *") },
            isError = name.isBlank(),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = phone,
            onValueChange = onPhoneChange,
            label = { Text("Mobile Number *") },
            isError = phone.isBlank(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = cnic,
            onValueChange = onCnicChange,
            label = { Text("CNIC Number *") },
            isError = cnic.isBlank(),
            supportingText = { Text("13 digits, dashto ke baghair bhi chalega") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        Text("Khareedari Record Karne Wala (User):", fontSize = 15.sp, fontWeight = FontWeight.Bold)

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
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StepPaymentInfo(
    totalPrice: Long,
    paidAmount: String, onPaidAmountChange: (String) -> Unit,
    selectedMethod: String, onMethodChange: (String) -> Unit,
    paymentDetails: String, onDetailsChange: (String) -> Unit,
    promisedDate: String, onPromisedDateChange: (String) -> Unit
) {
    val paid = paidAmount.toLongOrNull() ?: totalPrice
    val remaining = (totalPrice - paid).coerceAtLeast(0L)

    val methods = listOf("Cash", "Easypaisa", "JazzCash", "Bank Transfer", "Other")

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Payment aur Baqi date:", fontSize = 16.sp, fontWeight = FontWeight.Bold)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Total Qeemat: Rs $totalPrice", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                if (remaining > 0) {
                    Text("Baqi dene wale: Rs $remaining", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
                } else {
                    Text("Poori payment adaa hui ✓", color = Color(0xFF2E7D32))
                }
            }
        }

        OutlinedTextField(
            value = paidAmount,
            onValueChange = onPaidAmountChange,
            label = { Text("Kitne paisay diye? (Rs)") },
            placeholder = { Text(totalPrice.toString()) },
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
                val isSelected = method == selectedMethod
                FilterChip(
                    selected = isSelected,
                    onClick = { onMethodChange(method) },
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
            onValueChange = onDetailsChange,
            label = {
                Text(
                    if (selectedMethod == "Other") "Payment Method Detail / Name *"
                    else "Bank / Account / Ref Info (Optional)"
                )
            },
            placeholder = { Text("e.g. Meezan Bank / Txn ID #98765 / Slip info") },
            supportingText = { Text("Bank name, account details, or transaction ID") },
            modifier = Modifier.fillMaxWidth()
        )

        if (remaining > 0) {
            OutlinedTextField(
                value = promisedDate,
                onValueChange = onPromisedDateChange,
                label = { Text("Baqi kab denge? (e.g. 15 March)") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BuyWizardScreenPreview() {
    BuySellTheme {
        BuyWizardScreen()
    }
}
