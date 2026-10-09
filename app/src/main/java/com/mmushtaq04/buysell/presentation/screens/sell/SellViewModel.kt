package com.mmushtaq04.buysell.presentation.screens.sell

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.google.gson.Gson
import com.mmushtaq04.buysell.data.local.AppDatabase
import com.mmushtaq04.buysell.data.local.entity.PartyEntity
import com.mmushtaq04.buysell.data.local.entity.SyncOutboxEntity
import com.mmushtaq04.buysell.data.local.enums.*
import com.mmushtaq04.buysell.data.repository.*
import com.mmushtaq04.buysell.data.sync.SyncWorker
import com.mmushtaq04.buysell.domain.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class SellViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val stockRepository = StockRepositoryImpl(db)

    private val _stockItems = MutableStateFlow<List<StockItem>>(emptyList())
    val stockItems: StateFlow<List<StockItem>> = _stockItems.asStateFlow()

    companion object {
        private const val TAG = "SellViewModel"
    }

    init {
        loadInStockItems()
    }

    private fun loadInStockItems() {
        viewModelScope.launch {
            val user = db.userDao().getPrimaryUser()
            val meta = db.appMetaDao().getAppMeta()
            val shopId = meta?.activeShopId?.ifBlank { null } ?: user?.shopId ?: ""
            if (shopId.isNotBlank()) {
                stockRepository.observeInStockItems(shopId).collect { items ->
                    _stockItems.value = items
                }
            }
        }
    }

    suspend fun getOriginalPurchaseCost(stockItemId: String): Long? {
        val stockEntity = db.stockItemDao().getStockItemById(stockItemId) ?: return null
        val purchaseLineId = stockEntity.purchaseLineId ?: return null
        return db.txnDao().getUnitPriceByLineId(purchaseLineId)
    }

    fun saveSale(
        stockItemId: String,
        priceRs: Long,
        buyerName: String,
        buyerPhone: String,
        buyerCnic: String,
        recordedBy: String,
        paidAmountRs: Long,
        paymentMethodStr: String,
        paymentDetails: String,
        promisedDateStr: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            Log.d(TAG, "Starting saveSale: StockItemID=$stockItemId, SalePrice=Rs $priceRs, Buyer=$buyerName, Paid=Rs $paidAmountRs")
            val user = db.userDao().getPrimaryUser()
            val meta = db.appMetaDao().getAppMeta()
            val activeShopId = meta?.activeShopId?.ifBlank { null } ?: user?.shopId ?: ""
            val now = System.currentTimeMillis()

            db.withTransaction {
                // 1. Create or Find Buyer Party
                val partyId = UUID.randomUUID().toString()
                val buyerParty = PartyEntity(
                    id = partyId,
                    shopId = activeShopId,
                    name = buyerName.ifBlank { "Customer" },
                    phone = buyerPhone.ifBlank { null },
                    cnic = buyerCnic.ifBlank { null },
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

                // 2. Record Sale Txn
                val pricePaisa = priceRs * 100
                val txn = stockRepository.recordSale(
                    stockItemId = stockItemId,
                    salePrice = pricePaisa,
                    partyId = partyId,
                    createdByUserId = recordedBy,
                    qtyToSell = 1
                )
                Log.i(TAG, "✓ Recorded Sale Txn ID: ${txn.id} for StockItemID: $stockItemId")

                // 3. Record Payment
                val paidPaisa = (paidAmountRs * 100).coerceAtMost(pricePaisa)
                if (paidPaisa > 0) {
                    val methodEnum = PaymentMethod.fromStr(paymentMethodStr)

                    val payment = Payment(
                        id = UUID.randomUUID().toString(),
                        shopId = activeShopId,
                        txnId = txn.id,
                        partyId = partyId,
                        direction = PaymentDirection.IN,
                        amount = paidPaisa,
                        method = methodEnum,
                        referenceNo = paymentDetails.ifBlank { null },
                        payDate = now,
                        scope = Scope.PUBLIC
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
                val remainingPaisa = pricePaisa - paidPaisa
                if (remainingPaisa > 0) {
                    val promise = PaymentPromise(
                        id = UUID.randomUUID().toString(),
                        shopId = activeShopId,
                        partyId = partyId,
                        txnId = txn.id,
                        direction = PromiseDirection.RECEIVE,
                        amount = remainingPaisa,
                        promisedDate = now + 7 * 24 * 60 * 60 * 1000L, // default 7 days
                        note = promisedDateStr.ifBlank { null },
                        scope = Scope.PUBLIC
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
            }

            // 5. Enqueue Non-Blocking Background SyncWorker
            SyncWorker.enqueueOneTimeSync(getApplication())
            com.mmushtaq04.buysell.util.RatingManager.recordInteraction(getApplication())
            Log.i(TAG, "✓ saveSale completed and background sync enqueued.")
            onSuccess()
        }
    }
}
