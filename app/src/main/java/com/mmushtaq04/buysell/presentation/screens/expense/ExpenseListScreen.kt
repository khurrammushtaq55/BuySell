package com.mmushtaq04.buysell.presentation.screens.expense

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mmushtaq04.buysell.R
import com.mmushtaq04.buysell.data.local.entity.ExpenseEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseListScreen(
    onNavigateBack: () -> Unit = {},
    expenses: List<ExpenseEntity> = emptyList(),
    totalExpenseRs: Long = 0L,
    selectedCategory: String = "ALL",
    onCategoryFilterSelect: (String) -> Unit = {},
    onAddExpense: (amountRs: Long, category: String, note: String) -> Unit = { _, _, _ -> },
    onDeleteExpense: (String) -> Unit = {}
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var deleteCandidateId by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    if (showAddDialog) {
        AddExpenseDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { amountRs, category, note ->
                showAddDialog = false
                onAddExpense(amountRs, category, note)
            }
        )
    }

    if (deleteCandidateId != null) {
        AlertDialog(
            onDismissRequest = { deleteCandidateId = null },
            title = { Text(stringResource(R.string.action_confirm)) },
            text = { Text("Are you sure you want to delete this expense record?") },
            confirmButton = {
                Button(
                    onClick = {
                        val idToDelete = deleteCandidateId
                        deleteCandidateId = null
                        if (idToDelete != null) {
                            onDeleteExpense(idToDelete)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.action_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidateId = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.expense_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.expense_btn_add), fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Total Expenses Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = stringResource(R.string.dashboard_total_expenses),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = com.mmushtaq04.buysell.util.CurrencyFormatter.formatAmount(context, totalExpenseRs),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Category Filter Chips
            val categories = listOf(
                "ALL" to "All",
                "RENT" to stringResource(R.string.expense_category_rent),
                "ELECTRICITY" to stringResource(R.string.expense_category_electricity),
                "SALARY" to stringResource(R.string.expense_category_salary),
                "REPAIR" to stringResource(R.string.expense_category_repair),
                "TEA_REFRESHMENT" to stringResource(R.string.expense_category_refreshment),
                "OTHER" to stringResource(R.string.expense_category_other)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { (catKey, catLabel) ->
                    FilterChip(
                        selected = selectedCategory == catKey,
                        onClick = { onCategoryFilterSelect(catKey) },
                        label = { Text(catLabel, fontSize = 12.sp) },
                        leadingIcon = if (selectedCategory == catKey) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        } else null
                    )
                }
            }

            if (expenses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.expense_empty_msg),
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 88.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(expenses, key = { it.id }) { expense ->
                        ExpenseItemCard(
                            expense = expense,
                            onDelete = { deleteCandidateId = expense.id }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpenseItemCard(
    expense: ExpenseEntity,
    onDelete: () -> Unit
) {
    val (icon, categoryLabel) = getCategoryIconAndName(expense.categoryId ?: "OTHER")
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    val dateStr = remember(expense.expenseDate) { dateFormat.format(Date(expense.expenseDate)) }
    val amountRs = expense.amount / 100

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
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text = categoryLabel,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    if (!expense.note.isNullOrBlank()) {
                        Text(
                            text = expense.note,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = dateStr,
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = com.mmushtaq04.buysell.util.CurrencyFormatter.formatAmount(LocalContext.current, amountRs),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.error
                )

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun getCategoryIconAndName(categoryKey: String): Pair<ImageVector, String> {
    return when (categoryKey) {
        "RENT" -> Icons.Default.Business to stringResource(R.string.expense_category_rent)
        "ELECTRICITY" -> Icons.Default.Bolt to stringResource(R.string.expense_category_electricity)
        "SALARY" -> Icons.Default.Badge to stringResource(R.string.expense_category_salary)
        "REPAIR" -> Icons.Default.Build to stringResource(R.string.expense_category_repair)
        "TEA_REFRESHMENT" -> Icons.Default.LocalCafe to stringResource(R.string.expense_category_refreshment)
        else -> Icons.Default.Receipt to stringResource(R.string.expense_category_other)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddExpenseDialog(
    onDismiss: () -> Unit,
    onConfirm: (amountRs: Long, category: String, note: String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("RENT") }
    var noteText by remember { mutableStateOf("") }

    val categories = listOf(
        "RENT" to stringResource(R.string.expense_category_rent),
        "ELECTRICITY" to stringResource(R.string.expense_category_electricity),
        "SALARY" to stringResource(R.string.expense_category_salary),
        "REPAIR" to stringResource(R.string.expense_category_repair),
        "TEA_REFRESHMENT" to stringResource(R.string.expense_category_refreshment),
        "OTHER" to stringResource(R.string.expense_category_other)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.expense_dialog_title), fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { if (it.all { char -> char.isDigit() }) amountText = it },
                    label = { Text(stringResource(R.string.expense_label_amount)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(stringResource(R.string.expense_label_category), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    categories.forEach { (key, label) ->
                        FilterChip(
                            selected = selectedCategory == key,
                            onClick = { selectedCategory = key },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text(stringResource(R.string.expense_label_note)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toLongOrNull() ?: 0L
                    if (amount > 0) {
                        onConfirm(amount, selectedCategory, noteText.trim())
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
