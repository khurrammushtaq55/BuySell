package com.mmushtaq04.buysell.presentation.screens.expense

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.mmushtaq04.buysell.data.local.AppDatabase
import com.mmushtaq04.buysell.data.local.entity.ExpenseEntity
import com.mmushtaq04.buysell.data.local.entity.SyncOutboxEntity
import com.mmushtaq04.buysell.data.local.enums.SyncOp
import com.mmushtaq04.buysell.data.local.enums.SyncState
import com.mmushtaq04.buysell.data.sync.SyncWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class ExpenseUiState(
    val expenses: List<ExpenseEntity> = emptyList(),
    val totalExpenseRs: Long = 0L,
    val selectedCategoryFilter: String = "ALL",
    val isLoading: Boolean = false
)

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "ExpenseViewModel"
    private val db = AppDatabase.getInstance(application)

    private val _uiState = MutableStateFlow(ExpenseUiState())
    val uiState: StateFlow<ExpenseUiState> = _uiState.asStateFlow()

    init {
        loadExpenses()
    }

    fun loadExpenses() {
        viewModelScope.launch {
            val user = db.userDao().getPrimaryUser()
            val meta = db.appMetaDao().getAppMeta()
            val activeShopId = meta?.activeShopId?.ifBlank { null } ?: user?.shopId ?: ""

            if (activeShopId.isBlank()) return@launch

            db.expenseDao().observeExpenses(activeShopId).collect { expenseList ->
                val filtered = if (_uiState.value.selectedCategoryFilter == "ALL") {
                    expenseList
                } else {
                    expenseList.filter { it.categoryId == _uiState.value.selectedCategoryFilter }
                }

                val totalPaisa = filtered.sumOf { it.amount }

                _uiState.value = _uiState.value.copy(
                    expenses = filtered,
                    totalExpenseRs = totalPaisa / 100,
                    isLoading = false
                )
            }
        }
    }

    fun setCategoryFilter(category: String) {
        _uiState.value = _uiState.value.copy(selectedCategoryFilter = category)
        loadExpenses()
    }

    fun addExpense(
        amountRs: Long,
        category: String,
        note: String,
        expenseDateMs: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            val user = db.userDao().getPrimaryUser()
            val meta = db.appMetaDao().getAppMeta()
            val activeShopId = meta?.activeShopId?.ifBlank { null } ?: user?.shopId ?: ""

            if (activeShopId.isBlank()) return@launch

            val now = System.currentTimeMillis()
            val recordedBy = user?.displayName ?: "Owner"

            val expenseEntity = ExpenseEntity(
                id = UUID.randomUUID().toString(),
                shopId = activeShopId,
                categoryId = category,
                amount = amountRs * 100,
                expenseDate = expenseDateMs,
                note = note.ifBlank { null },
                createdAt = now,
                updatedAt = now,
                createdBy = recordedBy,
                updatedBy = recordedBy,
                syncState = SyncState.PENDING
            )

            db.expenseDao().insertExpense(expenseEntity)

            db.syncDao().enqueueOutbox(
                SyncOutboxEntity(
                    id = UUID.randomUUID().toString(),
                    entityType = "expenses",
                    entityId = expenseEntity.id,
                    op = SyncOp.UPSERT,
                    payloadJson = Gson().toJson(expenseEntity),
                    createdAt = now
                )
            )

            SyncWorker.enqueueOneTimeSync(getApplication())
            Log.i(TAG, "✓ Added new Expense: Rs $amountRs for category '$category'")
        }
    }

    fun deleteExpense(expenseId: String) {
        viewModelScope.launch {
            val expense = db.expenseDao().getExpenseById(expenseId) ?: return@launch
            db.expenseDao().deleteExpense(expenseId)

            val now = System.currentTimeMillis()
            db.syncDao().enqueueOutbox(
                SyncOutboxEntity(
                    id = UUID.randomUUID().toString(),
                    entityType = "expenses",
                    entityId = expenseId,
                    op = SyncOp.DELETE,
                    payloadJson = Gson().toJson(expense),
                    createdAt = now
                )
            )

            SyncWorker.enqueueOneTimeSync(getApplication())
            Log.i(TAG, "✓ Deleted Expense ID: $expenseId")
        }
    }
}
