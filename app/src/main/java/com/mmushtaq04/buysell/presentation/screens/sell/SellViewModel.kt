package com.mmushtaq04.buysell.presentation.screens.sell

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.google.gson.Gson
import com.mmushtaq04.buysell.data.local.AppDatabase
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

    private val _simpleStockList = MutableStateFlow<List<SimpleStockItem>>(emptyList())
    val simpleStockList: StateFlow<List<SimpleStockItem>> = _simpleStockList.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _saveError = MutableStateFlow<String?>(null)
    val saveError: StateFlow<String?> = _saveError.asStateFlow()

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

                    val simpleList = items.map { item ->
                        val lineId = item.purchaseLineId
                        val costPaisa = if (lineId != null) {
                            db.txnDao().getUnitPriceByLineId(lineId) ?: 0L
                        } else 0L

                        SimpleStockItem(
                            id = item.id,
                            title = "${item.brand} ${item.model}".trim(),
                            imei = item.identifier ?: "N/A",
                            cost = costPaisa / 100
                        )
                    }
                    _simpleStockList.value = simpleList
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
            if (_isSaving.value) return@launch
            _isSaving.value = true
            _saveError.value = null
            Log.d(TAG, "Starting saveSale: StockItemID=$stockItemId, SalePrice= $priceRs, Buyer=$buyerName, Paid= $paidAmountRs")

            val user = db.userDao().getPrimaryUser()
            val meta = db.appMetaDao().getAppMeta()
            val activeShopId = meta?.activeShopId?.ifBlank { null } ?: user?.shopId ?: ""
            val now = System.currentTimeMillis()

            try {
                db.withTransaction {
                    // 1. Create or Find Buyer Party via PartyResolver
                    val buyerParty = com.mmushtaq04.buysell.domain.PartyResolver.findOrCreate(
                        partyDao = db.partyDao(),
                        shopId = activeShopId,
                        rawName = buyerName,
                        rawPhone = buyerPhone,
                        rawCnic = buyerCnic,
                        typeHint = PartyTypeHint.CUSTOMER,
                        recordedBy = recordedBy,
                        nowMs = now,
                        generateId = { UUID.randomUUID().toString() }
                    )
                    val partyId = buyerParty.id
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
                        val parsedPromiseDate = parsePromisedDateMs(promisedDateStr, now)
                        val promise = PaymentPromise(
                            id = UUID.randomUUID().toString(),
                            shopId = activeShopId,
                            partyId = partyId,
                            txnId = txn.id,
                            direction = PromiseDirection.RECEIVE,
                            amount = remainingPaisa,
                            promisedDate = parsedPromiseDate,
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
            } catch (e: Exception) {
                Log.e(TAG, "Error saving sale: ${e.message}", e)
                _saveError.value = e.message ?: "Failed to save sale transaction"
            } finally {
                _isSaving.value = false
            }
        }
    }

    private fun parsePromisedDateMs(promisedDateStr: String?, defaultNowMs: Long): Long {
        if (promisedDateStr.isNullOrBlank()) return defaultNowMs + 7 * 24 * 60 * 60 * 1000L
        val parsedLong = promisedDateStr.toLongOrNull()
        if (parsedLong != null && parsedLong > 0) return parsedLong
        return runCatching {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            sdf.parse(promisedDateStr.trim())?.time
        }.getOrNull() ?: (defaultNowMs + 7 * 24 * 60 * 60 * 1000L)
    }
}
