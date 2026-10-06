package com.mmushtaq04.buysell.presentation.screens.sell

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.mmushtaq04.buysell.data.local.AppDatabase
import com.mmushtaq04.buysell.data.local.entity.PartyEntity
import com.mmushtaq04.buysell.data.local.entity.SyncOutboxEntity
import com.mmushtaq04.buysell.data.local.enums.PartyTypeHint
import com.mmushtaq04.buysell.data.local.enums.PaymentDirection
import com.mmushtaq04.buysell.data.local.enums.PaymentMethod
import com.mmushtaq04.buysell.data.local.enums.PromiseDirection
import com.mmushtaq04.buysell.data.local.enums.Role
import com.mmushtaq04.buysell.data.local.enums.SyncOp
import com.mmushtaq04.buysell.data.repository.StockRepositoryImpl
import com.mmushtaq04.buysell.data.repository.toEntity
import com.mmushtaq04.buysell.data.sync.FirestoreSyncManager
import com.mmushtaq04.buysell.domain.model.Payment
import com.mmushtaq04.buysell.domain.model.PaymentPromise
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class SellViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val stockRepository = StockRepositoryImpl(db)

    private val _stockList = MutableStateFlow<List<SimpleStockItem>>(emptyList())
    val stockList: StateFlow<List<SimpleStockItem>> = _stockList.asStateFlow()

    init {
        loadInStockItems()
    }

    private fun loadInStockItems() {
        viewModelScope.launch {
            val meta = db.appMetaDao().getAppMeta()
            val activeShopId = meta?.activeShopId ?: "default_shop"

            stockRepository.observeInStockItems(activeShopId).collect { list ->
                _stockList.value = list.map { item ->
                    SimpleStockItem(
                        id = item.id,
                        title = "${item.brand} ${item.model}",
                        imei = item.identifier ?: "N/A",
                        cost = 0L
                    )
                }
            }
        }
    }

    fun saveSale(
        stockItemId: String,
        salePriceRs: Long,
        buyerName: String,
        buyerPhone: String,
        recordedBy: String,
        receivedAmountRs: Long,
        paymentMethodStr: String,
        paymentDetails: String,
        promisedDateStr: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val meta = db.appMetaDao().getAppMeta()
            val activeShopId = meta?.activeShopId ?: "default_shop"
            val now = System.currentTimeMillis()

            // 1. Create or Find Buyer Party
            val partyId = UUID.randomUUID().toString()
            val buyerParty = PartyEntity(
                id = partyId,
                shopId = activeShopId,
                name = buyerName.ifBlank { "Buyer" },
                phone = buyerPhone.ifBlank { null },
                typeHint = PartyTypeHint.CUSTOMER,
                createdAt = now,
                updatedAt = now,
                createdBy = recordedBy,
                updatedBy = recordedBy
            )
            db.partyDao().insertParty(buyerParty)
            db.syncDao().enqueueOutbox(
                SyncOutboxEntity(
                    id = UUID.randomUUID().toString(),
                    entityType = "parties",
                    entityId = buyerParty.id,
                    op = SyncOp.UPSERT,
                    payloadJson = Gson().toJson(buyerParty),
                    createdAt = now
                )
            )

            // 2. Record Sale Txn in Room DB
            val salePricePaisa = salePriceRs * 100
            val txn = stockRepository.recordSale(
                stockItemId = stockItemId,
                salePrice = salePricePaisa,
                partyId = partyId,
                createdByUserId = recordedBy,
                qtyToSell = 1
            )

            // 3. Record Payment IN
            val receivedPaisa = (receivedAmountRs * 100).coerceAtMost(salePricePaisa)
            if (receivedPaisa > 0) {
                val methodEnum = when (paymentMethodStr.uppercase()) {
                    "CASH" -> PaymentMethod.CASH
                    "EASYPAISA", "JAZZCASH" -> PaymentMethod.WALLET
                    "BANK TRANSFER" -> PaymentMethod.BANK
                    else -> PaymentMethod.OTHER
                }

                val payment = Payment(
                    id = UUID.randomUUID().toString(),
                    shopId = activeShopId,
                    txnId = txn.id,
                    partyId = partyId,
                    direction = PaymentDirection.IN,
                    amount = receivedPaisa,
                    method = methodEnum,
                    referenceNo = paymentDetails.ifBlank { null },
                    payDate = now
                )
                val paymentEntity = payment.toEntity(recordedBy)
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
            }

            // 4. Record Promise if Partial Payment
            val remainingPaisa = salePricePaisa - receivedPaisa
            if (remainingPaisa > 0) {
                val promise = PaymentPromise(
                    id = UUID.randomUUID().toString(),
                    shopId = activeShopId,
                    partyId = partyId,
                    txnId = txn.id,
                    direction = PromiseDirection.RECEIVE,
                    amount = remainingPaisa,
                    promisedDate = now + 7 * 24 * 60 * 60 * 1000L, // default 7 days
                    note = promisedDateStr.ifBlank { null }
                )
                val promiseEntity = promise.toEntity(recordedBy)
                db.paymentPromiseDao().insertPromise(promiseEntity)
                db.syncDao().enqueueOutbox(
                    SyncOutboxEntity(
                        id = UUID.randomUUID().toString(),
                        entityType = "payment_promises",
                        entityId = promiseEntity.id,
                        op = SyncOp.UPSERT,
                        payloadJson = Gson().toJson(promiseEntity),
                        createdAt = now
                    )
                )
            }

            // 5. Trigger Firestore Push
            runCatching {
                FirestoreSyncManager(db).pushOutbox(activeShopId, Role.OWNER, "session_active")
            }

            onSuccess()
        }
    }
}
