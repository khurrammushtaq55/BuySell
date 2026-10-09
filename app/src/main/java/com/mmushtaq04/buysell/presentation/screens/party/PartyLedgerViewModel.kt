package com.mmushtaq04.buysell.presentation.screens.party

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.mmushtaq04.buysell.data.local.AppDatabase
import com.mmushtaq04.buysell.data.local.entity.PaymentEntity
import com.mmushtaq04.buysell.data.local.entity.SyncOutboxEntity
import com.mmushtaq04.buysell.data.local.enums.*
import com.mmushtaq04.buysell.data.sync.SyncWorker
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class LedgerItem(
    val id: String,
    val dateMs: Long,
    val title: String,
    val subtitle: String?,
    val amountRs: Long,
    val isPayment: Boolean,
    val direction: PaymentDirection?
)

data class PartyLedgerUiState(
    val partyId: String = "",
    val partyName: String = "",
    val partyPhone: String = "",
    val partyCnic: String = "",
    val shopName: String = "Mera Store",
    val netBalanceRs: Long = 0L,
    val ledgerHistory: List<LedgerItem> = emptyList(),
    val isLoading: Boolean = false
)

class PartyLedgerViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "PartyLedgerViewModel"
    private val db = AppDatabase.getInstance(application)

    private val _uiState = MutableStateFlow(PartyLedgerUiState())
    val uiState: StateFlow<PartyLedgerUiState> = _uiState.asStateFlow()

    fun loadLedger(partyId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(partyId = partyId, isLoading = true)

            val user = db.userDao().getPrimaryUser()
            val meta = db.appMetaDao().getAppMeta()
            val activeShopId = meta?.activeShopId?.ifBlank { null } ?: user?.shopId ?: ""

            if (activeShopId.isBlank()) return@launch

            val partyEntity = db.partyDao().getPartyById(partyId)
            val shopEntity = db.shopDao().getShopById(activeShopId)
            val activeShopName = shopEntity?.name?.ifBlank { "Mera Store" } ?: "Mera Store"

            combine(
                db.partyDao().observePartyBalance(activeShopId, partyId),
                db.txnDao().observeTxnsByParty(activeShopId, partyId),
                db.paymentDao().observePaymentsByParty(activeShopId, partyId)
            ) { balanceDto, txns, payments ->
                val ledgerList = mutableListOf<LedgerItem>()

                for (txn in txns) {
                    val title = when (txn.type) {
                        TxnType.SALE -> "Device Sale / Saman Becha"
                        TxnType.PURCHASE -> "Device Purchase / Saman Khareeda"
                        TxnType.SALE_RETURN -> "Sale Return / Saman Wapas Aaya"
                        TxnType.PURCHASE_RETURN -> "Purchase Return / Saman Wapas Kiya"
                    }
                    val subtitle = txn.receiptNo ?: txn.note

                    ledgerList.add(
                        LedgerItem(
                            id = txn.id,
                            dateMs = txn.txnDate,
                            title = title,
                            subtitle = subtitle,
                            amountRs = txn.totalAmount / 100,
                            isPayment = false,
                            direction = if (txn.type == TxnType.SALE) PaymentDirection.IN else PaymentDirection.OUT
                        )
                    )
                }

                for (pay in payments) {
                    val isWasooli = pay.direction == PaymentDirection.IN
                    val title = if (isWasooli) "Udhaar Wasooli / Cash Received (+)" else "Payment Paid / Cash Given (-)"
                    val methodStr = pay.method.toDisplayName()
                    val subtitle = listOfNotNull(methodStr, pay.referenceNo).joinToString(" • ")

                    ledgerList.add(
                        LedgerItem(
                            id = pay.id,
                            dateMs = pay.payDate,
                            title = title,
                            subtitle = subtitle,
                            amountRs = pay.amount / 100,
                            isPayment = true,
                            direction = pay.direction
                        )
                    )
                }

                // Sort chronological newest first
                ledgerList.sortByDescending { it.dateMs }

                _uiState.value = PartyLedgerUiState(
                    partyId = partyId,
                    partyName = balanceDto?.name ?: partyEntity?.name ?: "Customer",
                    partyPhone = balanceDto?.phone ?: partyEntity?.phone ?: "N/A",
                    partyCnic = balanceDto?.cnic ?: partyEntity?.cnic ?: "",
                    shopName = activeShopName,
                    netBalanceRs = (balanceDto?.balance ?: 0L) / 100,
                    ledgerHistory = ledgerList,
                    isLoading = false
                )
            }.collect()
        }
    }

    fun recordWasooliPayment(
        partyId: String,
        amountRs: Long,
        paymentMethodStr: String,
        note: String,
        direction: PaymentDirection = PaymentDirection.IN
    ) {
        viewModelScope.launch {
            val user = db.userDao().getPrimaryUser()
            val meta = db.appMetaDao().getAppMeta()
            val activeShopId = meta?.activeShopId?.ifBlank { null } ?: user?.shopId ?: ""

            if (activeShopId.isBlank()) return@launch

            val now = System.currentTimeMillis()
            val recordedBy = user?.displayName ?: "Owner"

            val paymentEntity = PaymentEntity(
                id = UUID.randomUUID().toString(),
                shopId = activeShopId,
                txnId = null,
                partyId = partyId,
                direction = direction,
                amount = amountRs * 100,
                method = PaymentMethod.fromStr(paymentMethodStr),
                referenceNo = note.ifBlank { null },
                payDate = now,
                createdAt = now,
                updatedAt = now,
                createdBy = recordedBy,
                updatedBy = recordedBy,
                scope = Scope.PUBLIC
            )

            db.paymentDao().insertPayment(paymentEntity)

            db.syncDao().enqueueOutbox(
                SyncOutboxEntity(
                    id = UUID.randomUUID().toString(),
                    entityType = "payments",
                    entityId = paymentEntity.id,
                    op = SyncOp.UPSERT,
                    payloadJson = Gson().toJson(paymentEntity),
                    createdAt = now
                )
            )

            SyncWorker.enqueueOneTimeSync(getApplication())
            Log.i(TAG, "✓ Recorded Wasooli Payment: $amountRs for PartyID: $partyId (Direction: $direction)")
        }
    }
}
