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
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mmushtaq04.buysell.R
import com.mmushtaq04.buysell.ui.theme.BuySellTheme
import com.mmushtaq04.buysell.util.AppPreferencesManager

data class SimpleStockItem(val id: String, val title: String, val imei: String, val cost: Long)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SellWizardScreen(
    stockList: List<SimpleStockItem> = emptyList(),
    currentUserName: String = "Malik / Staff",
    showBuyCostInSellWizard: Boolean = true,
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
    val context = LocalContext.current
    var step by remember { mutableIntStateOf(1) }

    var selectedItem by remember { mutableStateOf<SimpleStockItem?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isCostVisible by remember { mutableStateOf(false) }

    var buyerName by remember { mutableStateOf("") }
    var buyerPhone by remember { mutableStateOf("") }
    var salePriceText by remember { mutableStateOf("") }
    var receivedAmountText by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("Cash") }
    var paymentDetails by remember { mutableStateOf("") }
    var promisedDateText by remember { mutableStateOf("") }

    val methods = listOf("Cash", "Easypaisa", "JazzCash", "Bank Transfer", "Other")

    // Automatically prefill receivedAmountText with salePriceText
    LaunchedEffect(step, salePriceText) {
        if (salePriceText.isNotBlank() && (receivedAmountText.isBlank() || (step == 3 && receivedAmountText.isBlank()))) {
            receivedAmountText = salePriceText
        }
    }

    val isStepValid = when (step) {
        1 -> selectedItem != null
        2 -> buyerName.isNotBlank() && buyerPhone.isNotBlank() && (salePriceText.toLongOrNull() ?: 0L) > 0
        3 -> true
        else -> false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = stringResource(R.string.sell_title), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(text = stringResource(R.string.buy_step, step, 3), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        Text(stringResource(R.string.sell_select_stock), fontSize = 16.sp, fontWeight = FontWeight.Bold)

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text(stringResource(R.string.label_search_stock)) },
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
                                    text = "Pehle stock mein phone hona chahiye",
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
                        Text(stringResource(R.string.sell_buyer_info_header), fontSize = 16.sp, fontWeight = FontWeight.Bold)

                        selectedItem?.let { item ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Selected Item: ${item.title}", fontWeight = FontWeight.Bold)
                                    Text("IMEI: ${item.imei}", fontSize = 12.sp)
                                }
                            }
                        }

                        // Hidden Buy Price Card with Eye Toggle
                        val isPrefEnabled = AppPreferencesManager.isShowBuyCostInSellEnabled(context)
                        if (showBuyCostInSellWizard && isPrefEnabled && selectedItem != null) {
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
                                        Text(
                                            text = stringResource(R.string.label_buy_price),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (isCostVisible) "Rs ${selectedItem?.cost ?: 0}" else "Rs ••••••",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCostVisible) MaterialTheme.colorScheme.primary else Color.Gray
                                        )
                                    }

                                    IconButton(onClick = { isCostVisible = !isCostVisible }) {
                                        Icon(
                                            imageVector = if (isCostVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = if (isCostVisible) "Hide buy price" else "Show buy price",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = buyerName,
                            onValueChange = { buyerName = it },
                            label = { Text(stringResource(R.string.label_buyer_name)) },
                            isError = buyerName.isBlank(),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = buyerPhone,
                            onValueChange = { buyerPhone = it },
                            label = { Text(stringResource(R.string.label_mobile_number)) },
                            isError = buyerPhone.isBlank(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = salePriceText,
                            onValueChange = { newPrice ->
                                val oldPrice = salePriceText
                                salePriceText = newPrice
                                if (receivedAmountText.isBlank() || receivedAmountText == oldPrice) {
                                    receivedAmountText = newPrice
                                }
                            },
                            label = { Text(stringResource(R.string.label_sale_price)) },
                            isError = (salePriceText.toLongOrNull() ?: 0L) <= 0,
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

                    3 -> {
                        val salePrice = salePriceText.toLongOrNull() ?: 0L
                        val received = receivedAmountText.toLongOrNull() ?: salePrice
                        val remaining = (salePrice - received).coerceAtLeast(0L)

                        Text(stringResource(R.string.label_payment_confirmation), fontSize = 16.sp, fontWeight = FontWeight.Bold)

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(stringResource(R.string.label_sale_price_summary, salePrice), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text(stringResource(R.string.label_recorded_by_summary, currentUserName), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (remaining > 0) {
                                    Text(stringResource(R.string.label_customer_udhaar_summary, remaining), color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
                                } else {
                                    Text(stringResource(R.string.label_full_payment_received), color = Color(0xFF2E7D32))
                                }
                            }
                        }

                        OutlinedTextField(
                            value = receivedAmountText,
                            onValueChange = { receivedAmountText = it },
                            label = { Text(stringResource(R.string.label_amount_given_by_customer)) },
                            placeholder = { Text(salePrice.toString()) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(stringResource(R.string.label_payment_mode), fontSize = 14.sp, fontWeight = FontWeight.Bold)

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
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (remaining > 0) {
                            OutlinedTextField(
                                value = promisedDateText,
                                onValueChange = { promisedDateText = it },
                                label = { Text(stringResource(R.string.label_promised_due_date)) },
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
                    enabled = isStepValid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (step < 3) stringResource(R.string.action_next) else stringResource(R.string.btn_save_sale),
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
        SimpleStockItem("1", "Apple iPhone 15 Pro", "358912345678901", 180000),
        SimpleStockItem("2", "Samsung Galaxy S24 Ultra", "351234567890123", 210000),
        SimpleStockItem("3", "iPad Air 5th Gen", "SER987654321", 95000)
    )

    BuySellTheme {
        SellWizardScreen(stockList = dummyStock)
    }
}
