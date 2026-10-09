package com.mmushtaq04.buysell.presentation.screens.party

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
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
import com.mmushtaq04.buysell.ui.theme.BuySellTheme

data class DisplayPartyBalance(
    val id: String,
    val name: String,
    val phone: String,
    val balance: Long // positive = owes shop, negative = shop owes party
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartyListScreen(
    parties: List<DisplayPartyBalance> = emptyList(),
    onNavigateBack: () -> Unit = {},
    onSelectParty: (String) -> Unit = {},
    onAddParty: (name: String, phone: String, cnic: String) -> Unit = { _, _, _ -> }
) {
    var searchQuery by remember { mutableStateOf("") }
    var showAddPartyDialog by remember { mutableStateOf(false) }

    val filtered = parties.filter {
        it.name.contains(searchQuery, ignoreCase = true) || it.phone.contains(searchQuery)
    }

    if (showAddPartyDialog) {
        AddPartyDialog(
            onDismiss = { showAddPartyDialog = false },
            onConfirm = { name, phone, cnic ->
                showAddPartyDialog = false
                onAddParty(name, phone, cnic)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.action_khata), fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.party_subtitle), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddPartyDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.party_btn_add), fontWeight = FontWeight.Bold) }
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
                label = { Text(stringResource(R.string.party_search_label)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth()
            )

            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.party_empty_msg),
                        fontSize = 15.sp,
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(filtered) { party ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectParty(party.id) },
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
                                    Text(party.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text(stringResource(R.string.party_phone_prefix, party.phone), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    val amountRs = party.balance / 100
                                    when {
                                        party.balance > 0 -> {
                                            Text(stringResource(R.string.party_owes_shop, amountRs), color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        }
                                        party.balance < 0 -> {
                                            Text(stringResource(R.string.party_shop_owes, -amountRs), color = Color(0xFFC62828), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        }
                                        else -> {
                                            Text(stringResource(R.string.party_settled), color = Color.Gray, fontSize = 14.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddPartyDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, phone: String, cnic: String) -> Unit
) {
    var nameText by remember { mutableStateOf("") }
    var phoneText by remember { mutableStateOf("") }
    var cnicText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Naya Customer / Party", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text("Customer Name *") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phoneText,
                    onValueChange = { phoneText = it },
                    label = { Text("Phone Number *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = cnicText,
                    onValueChange = { cnicText = it },
                    label = { Text("CNIC / Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nameText.isNotBlank() && phoneText.isNotBlank()) {
                        onConfirm(nameText.trim(), phoneText.trim(), cnicText.trim())
                    }
                },
                enabled = nameText.isNotBlank() && phoneText.isNotBlank()
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
fun PartyListScreenPreview() {
    val sampleParties = listOf(
        DisplayPartyBalance("1", "Ali Ahmed", "03001234567", 2000000L),
        DisplayPartyBalance("2", "Usman Traders", "03129876543", -500000L),
        DisplayPartyBalance("3", "Bilal Khan", "03335554433", 0L)
    )

    BuySellTheme {
        PartyListScreen(parties = sampleParties)
    }
}
