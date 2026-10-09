package com.mmushtaq04.buysell.presentation.screens.party

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
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
import com.mmushtaq04.buysell.data.local.enums.PaymentDirection
import com.mmushtaq04.buysell.ui.theme.BuySellTheme
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartyLedgerScreen(
    uiState: PartyLedgerUiState = PartyLedgerUiState(),
    onNavigateBack: () -> Unit = {},
    onRecordPayment: (amountRs: Long, methodStr: String, note: String, direction: PaymentDirection) -> Unit = { _, _, _, _ -> }
) {
    val context = LocalContext.current
    var showWasooliDialog by remember { mutableStateOf(false) }
    var paymentDirectionToRecord by remember { mutableStateOf(PaymentDirection.IN) }

    if (showWasooliDialog) {
        RecordWasooliDialog(
            direction = paymentDirectionToRecord,
            onDismiss = { showWasooliDialog = false },
            onConfirm = { amountRs, methodStr, note ->
                showWasooliDialog = false
                onRecordPayment(amountRs, methodStr, note, paymentDirectionToRecord)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(uiState.partyName.ifBlank { stringResource(R.string.party_ledger_default_title) }, fontWeight = FontWeight.Bold)
                        if (uiState.partyPhone.isNotBlank()) {
                            Text(uiState.partyPhone, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.partyPhone.isNotBlank()) {
                        // Direct Phone Call Action
                        IconButton(onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${uiState.partyPhone}"))
                            context.startActivity(intent)
                        }) {
                            Icon(Icons.Default.Call, contentDescription = "Call Customer", tint = MaterialTheme.colorScheme.primary)
                        }

                        // Direct WhatsApp Chat & Statement Share Action
                        IconButton(onClick = {
                            val msg = when {
                                uiState.netBalanceRs > 0 -> {
                                    context.getString(
                                        R.string.party_ledger_whatsapp_remind_owed,
                                        uiState.partyName,
                                        uiState.shopName,
                                        uiState.netBalanceRs.toString()
                                    )
                                }
                                uiState.netBalanceRs < 0 -> {
                                    context.getString(
                                        R.string.party_ledger_whatsapp_remind_pay,
                                        uiState.partyName,
                                        uiState.shopName,
                                        (-uiState.netBalanceRs).toString()
                                    )
                                }
                                else -> {
                                    context.getString(
                                        R.string.party_ledger_whatsapp_remind_settled,
                                        uiState.partyName,
                                        uiState.shopName
                                    )
                                }
                            }

                            // Clean phone number (e.g. 03001234567 -> 923001234567)
                            val cleanPhone = uiState.partyPhone.replace(Regex("[^0-9]"), "").let { digits ->
                                if (digits.startsWith("0")) "92" + digits.substring(1) else digits
                            }

                            val encodedMsg = Uri.encode(msg)
                            val whatsappUri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMsg")
                            val intent = Intent(Intent.ACTION_VIEW, whatsappUri).apply {
                                setPackage("com.whatsapp")
                            }

                            runCatching {
                                context.startActivity(intent)
                            }.onFailure {
                                val fallbackIntent = Intent(Intent.ACTION_VIEW, whatsappUri)
                                runCatching {
                                    context.startActivity(fallbackIntent)
                                }.onFailure {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, msg)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Statement"))
                                }
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Share Udhaar Reminder", tint = Color(0xFF25D366))
                        }
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
            // Net Udhaar Balance Card
            val isOwedToShop = uiState.netBalanceRs > 0
            val isShopOwes = uiState.netBalanceRs < 0
            val balanceColor = when {
                isOwedToShop -> Color(0xFF2E7D32)
                isShopOwes -> MaterialTheme.colorScheme.error
                else -> Color.Gray
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.party_ledger_net_balance_status),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val balanceText = when {
                        isOwedToShop -> stringResource(R.string.party_ledger_lene_hain, uiState.netBalanceRs.toString())
                        isShopOwes -> stringResource(R.string.party_ledger_dene_hain, (-uiState.netBalanceRs).toString())
                        else -> stringResource(R.string.party_ledger_settled_nil)
                    }

                    Text(
                        text = balanceText,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = balanceColor
                    )
                }
            }

            // Contextual Action Button Logic:
            // 1. Customer Owes Shop (balance > 0) -> Show ONLY "Wasooli (+)"
            // 2. Shop Owes Customer/Supplier (balance < 0) -> Show ONLY "Payment Di (-)"
            // 3. Settled (balance == 0) -> Show Settled Badge (Hide Payment Buttons!)
            when {
                isOwedToShop -> {
                    Button(
                        onClick = {
                            paymentDirectionToRecord = PaymentDirection.IN
                            showWasooliDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.party_ledger_btn_record_wasooli), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
                isShopOwes -> {
                    Button(
                        onClick = {
                            paymentDirectionToRecord = PaymentDirection.OUT
                            showWasooliDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Icon(Icons.Default.MoneyOff, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.party_ledger_btn_record_payment_given), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
                else -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.party_ledger_settled_badge), fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF2E7D32))
                        }
                    }
                }
            }

            Text(stringResource(R.string.party_ledger_history_title), fontWeight = FontWeight.Bold, fontSize = 16.sp)

            if (uiState.ledgerHistory.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.party_ledger_history_empty), color = Color.Gray)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(uiState.ledgerHistory, key = { it.id }) { item ->
                        LedgerItemCard(item = item)
                    }
                }
            }
        }
    }
}

@Composable
private fun LedgerItemCard(item: LedgerItem) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    val dateStr = remember(item.dateMs) { dateFormat.format(Date(item.dateMs)) }

    val (icon, color) = if (item.isPayment) {
        if (item.direction == PaymentDirection.IN) {
            Icons.Default.ArrowDownward to Color(0xFF2E7D32)
        } else {
            Icons.Default.ArrowUpward to MaterialTheme.colorScheme.error
        }
    } else {
        if (item.direction == PaymentDirection.IN) {
            Icons.Default.ShoppingCart to MaterialTheme.colorScheme.primary
        } else {
            Icons.Default.Inventory to Color(0xFF1565C0)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = color.copy(alpha = 0.15f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
                    }
                }

                Column {
                    Text(item.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    if (!item.subtitle.isNullOrBlank()) {
                        Text(item.subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(dateStr, fontSize = 10.sp, color = Color.Gray)
                }
            }

            Text(
                text = "Rs ${item.amountRs}",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = color
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecordWasooliDialog(
    direction: PaymentDirection,
    onDismiss: () -> Unit,
    onConfirm: (amountRs: Long, methodStr: String, note: String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var selectedMethod by remember { mutableStateOf("CASH") }
    var noteText by remember { mutableStateOf("") }

    val methods = listOf(
        "CASH" to "Cash",
        "BANK" to "Bank Transfer",
        "WALLET" to "EasyPaisa / JazzCash",
        "CHEQUE" to "Cheque",
        "OTHER" to "Other"
    )

    val dialogTitle = if (direction == PaymentDirection.IN) {
        stringResource(R.string.party_ledger_dialog_wasooli_title)
    } else {
        stringResource(R.string.party_ledger_dialog_payment_given_title)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(dialogTitle, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { if (it.all { char -> char.isDigit() }) amountText = it },
                    label = { Text(stringResource(R.string.party_ledger_dialog_amount_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(stringResource(R.string.party_ledger_dialog_method_label), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    methods.forEach { (key, label) ->
                        FilterChip(
                            selected = selectedMethod == key,
                            onClick = { selectedMethod = key },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text(stringResource(R.string.party_ledger_dialog_note_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toLongOrNull() ?: 0L
                    if (amount > 0) {
                        onConfirm(amount, selectedMethod, noteText.trim())
                    }
                },
                enabled = (amountText.toLongOrNull() ?: 0L) > 0
            ) {
                Text(stringResource(R.string.action_save), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun PartyLedgerScreenOwedPreview() {
    BuySellTheme {
        PartyLedgerScreen(
            uiState = PartyLedgerUiState(
                partyName = "Ali Ahmed",
                partyPhone = "03001234567",
                shopName = "Hafeez Center Electronics",
                netBalanceRs = 15000L,
                ledgerHistory = listOf(
                    LedgerItem("1", System.currentTimeMillis() - 86400000L, "Device Sale / Saman Becha", "iPhone 13 128GB", 85000L, false, PaymentDirection.IN),
                    LedgerItem("2", System.currentTimeMillis(), "Udhaar Wasooli / Cash Received (+)", "Cash • Part Payment", 70000L, true, PaymentDirection.IN)
                )
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PartyLedgerScreenSettledPreview() {
    BuySellTheme {
        PartyLedgerScreen(
            uiState = PartyLedgerUiState(
                partyName = "Bilal Khan",
                partyPhone = "03335554433",
                shopName = "Hafeez Center Electronics",
                netBalanceRs = 0L,
                ledgerHistory = listOf(
                    LedgerItem("1", System.currentTimeMillis(), "Udhaar Wasooli / Cash Received (+)", "EasyPaisa", 15000L, true, PaymentDirection.IN)
                )
            )
        )
    }
}
