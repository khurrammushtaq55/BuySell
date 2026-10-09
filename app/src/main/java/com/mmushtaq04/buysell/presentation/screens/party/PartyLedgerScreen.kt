package com.mmushtaq04.buysell.presentation.screens.party

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mmushtaq04.buysell.R
import com.mmushtaq04.buysell.data.local.enums.PaymentDirection
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartyLedgerScreen(
    uiState: PartyLedgerUiState = PartyLedgerUiState(),
    onNavigateBack: () -> Unit = {},
    onRecordPayment: (amountRs: Long, methodStr: String, note: String, direction: PaymentDirection) -> Unit = { _, _, _, _ -> }
) {
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
                        Text(uiState.partyName.ifBlank { "Customer Khata" }, fontWeight = FontWeight.Bold)
                        if (uiState.partyPhone.isNotBlank()) {
                            Text(uiState.partyPhone, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
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
                        text = "Net Udhaar Balance / Khata Status",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val balanceText = when {
                        isOwedToShop -> "Rs ${uiState.netBalanceRs} (Lene Hain)"
                        isShopOwes -> "Rs ${-uiState.netBalanceRs} (Dene Hain)"
                        else -> "Settled / Nill (0 Rs)"
                    }

                    Text(
                        text = balanceText,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = balanceColor
                    )
                }
            }

            // Action Buttons Row: Wasooli / Receive Cash (+) vs Pay Cash (-)
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        paymentDirectionToRecord = PaymentDirection.IN
                        showWasooliDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Wasooli (+)", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        paymentDirectionToRecord = PaymentDirection.OUT
                        showWasooliDialog = true
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.MoneyOff, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Payment Di (-)", fontWeight = FontWeight.Bold)
                }
            }

            Text("Khata History / Transactions", fontWeight = FontWeight.Bold, fontSize = 16.sp)

            if (uiState.ledgerHistory.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No transactions or payments recorded for this customer yet.", color = Color.Gray)
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

    val dialogTitle = if (direction == PaymentDirection.IN) "Record Wasooli / Cash Received (+)" else "Record Payment Given (-)"

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
                    label = { Text("Payment Amount (Rs) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Payment Method", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)

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
                    label = { Text("Description / Note (Optional)") },
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
