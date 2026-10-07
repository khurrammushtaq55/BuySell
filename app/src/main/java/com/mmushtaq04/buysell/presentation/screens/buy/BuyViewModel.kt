package com.mmushtaq04.buysell.presentation.screens.buy

import android.app.Application
import android.util.Log
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
import com.mmushtaq04.buysell.domain.model.StockItem
import kotlinx.coroutines.launch
import java.util.UUID

class BuyViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val stockRepository = StockRepositoryImpl(db)

    companion object {
        private const val TAG = "BuyViewModel"
    }

    fun savePurchase(
        categoryName: String,
        brand: String,
        model: String,
        imei: String,
        color: String,
        issue: String,
        priceRs: Long,
        sellerName: String,
        sellerPhone: String,
        sellerCnic: String,
        recordedBy: String,
        paidAmountRs: Long,
        paymentMethodStr: String,
        paymentDetails: String,
        promisedDateStr: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            Log.d(TAG, "Starting savePurchase: Category=$categoryName, Brand=$brand, Model=$model, Price=Rs $priceRs, Seller=$sellerName, RecordedBy=$recordedBy")
            val meta = db.appMetaDao().getAppMeta()
            val activeShopId = meta?.activeShopId ?: "default_shop"
            val now = System.currentTimeMillis()

            // 1. Create or Find Seller Party
            val partyId = UUID.randomUUID().toString()
            val sellerParty = PartyEntity(
                id = partyId,
                shopId = activeShopId,
                name = sellerName.ifBlank { "Seller" },
                phone = sellerPhone.ifBlank { null },
                cnic = sellerCnic.ifBlank { null },
                typeHint = PartyTypeHint.SUPPLIER,
                createdAt = now,
                updatedAt = now,
                createdBy = recordedBy,
                updatedBy = recordedBy
            )
            db.partyDao().insertParty(sellerParty)
            db.syncDao().enqueueOutbox(
                SyncOutboxEntity(
                    id = UUID.randomUUID().toString(),
                    entityType = "parties",
                    entityId = sellerParty.id,
                    op = SyncOp.UPSERT,
                    payloadJson = Gson().toJson(sellerParty),
                    createdAt = now
                )
            )

            // 2. Find Category ID
            val categories = db.categoryDao().getCategories(activeShopId)
            val matchedCat = categories.firstOrNull { it.name.equals(categoryName, ignoreCase = true) }
            val categoryId = matchedCat?.id ?: UUID.randomUUID().toString()

            // Construct attributes JSON for color and issue
            val attrMap = mutableMapOf<String, String>()
            if (color.isNotBlank()) attrMap["color"] = color.trim()
            if (issue.isNotBlank()) attrMap["issue"] = issue.trim()
            val attributesJson = if (attrMap.isNotEmpty()) Gson().toJson(attrMap) else null

            // 3. Create Stock Item
            val stockItem = StockItem(
                id = UUID.randomUUID().toString(),
                shopId = activeShopId,
                categoryId = categoryId,
                brand = brand.ifBlank { "Generic" },
                model = model.ifBlank { categoryName },
                identifier = imei.ifBlank { null },
                attributes = attributesJson,
                condition = issue.ifBlank { "GOOD" },
                quantity = 1,
                remainingQty = 1,
                stockedAt = now
            )

            // 4. Record Purchase Txn
            val pricePaisa = priceRs * 100
            val txn = stockRepository.recordPurchase(
                stockItem = stockItem,
                purchasePrice = pricePaisa,
                partyId = partyId,
                createdByUserId = recordedBy
            )
            Log.i(TAG, "✓ Recorded Purchase Txn ID: ${txn.id}, StockItem ID: ${stockItem.id}")

            // 5. Record Payment
            val paidPaisa = (paidAmountRs * 100).coerceAtMost(pricePaisa)
            if (paidPaisa > 0) {
                val methodEnum = PaymentMethod.fromStr(paymentMethodStr)

                val payment = Payment(
                    id = UUID.randomUUID().toString(),
                    shopId = activeShopId,
                    txnId = txn.id,
                    partyId = partyId,
                    direction = PaymentDirection.OUT,
                    amount = paidPaisa,
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

            // 6. Record Promise if Partial Payment
            val remainingPaisa = pricePaisa - paidPaisa
            if (remainingPaisa > 0) {
                val promise = PaymentPromise(
                    id = UUID.randomUUID().toString(),
                    shopId = activeShopId,
                    partyId = partyId,
                    txnId = txn.id,
                    direction = PromiseDirection.PAY,
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

            // 7. Trigger Firestore Push
            runCatching {
                FirestoreSyncManager(db).pushOutbox(activeShopId, Role.OWNER, "session_active")
            }

            onSuccess()
        }
    }
}
