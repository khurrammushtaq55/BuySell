package com.mmushtaq04.buysell.presentation.screens.buy

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mmushtaq04.buysell.data.local.AppDatabase
import com.mmushtaq04.buysell.data.local.entity.PartyEntity
import com.mmushtaq04.buysell.data.local.enums.PartyTypeHint
import com.mmushtaq04.buysell.data.local.enums.PaymentDirection
import com.mmushtaq04.buysell.data.local.enums.PaymentMethod
import com.mmushtaq04.buysell.data.local.enums.PromiseDirection
import com.mmushtaq04.buysell.data.repository.StockRepositoryImpl
import com.mmushtaq04.buysell.data.repository.toEntity
import com.mmushtaq04.buysell.domain.model.Payment
import com.mmushtaq04.buysell.domain.model.PaymentPromise
import com.mmushtaq04.buysell.domain.model.StockItem
import kotlinx.coroutines.launch
import java.util.UUID

class BuyViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val stockRepository = StockRepositoryImpl(db)

    fun savePurchase(
        categoryName: String,
        brand: String,
        model: String,
        imei: String,
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

            // 2. Find Category ID
            val categories = db.categoryDao().getCategories(activeShopId)
            val matchedCat = categories.firstOrNull { it.name.equals(categoryName, ignoreCase = true) }
            val categoryId = matchedCat?.id ?: UUID.randomUUID().toString()

            // 3. Create Stock Item
            val stockItem = StockItem(
                id = UUID.randomUUID().toString(),
                shopId = activeShopId,
                categoryId = categoryId,
                brand = brand.ifBlank { "Generic" },
                model = model.ifBlank { categoryName },
                identifier = imei.ifBlank { null },
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

            // 5. Record Payment
            val paidPaisa = (paidAmountRs * 100).coerceAtMost(pricePaisa)
            if (paidPaisa > 0) {
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
                    direction = PaymentDirection.OUT,
                    amount = paidPaisa,
                    method = methodEnum,
                    referenceNo = paymentDetails.ifBlank { null },
                    payDate = now
                )
                db.paymentDao().insertPayment(payment.toEntity(recordedBy))
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
                db.paymentPromiseDao().insertPromise(promise.toEntity(recordedBy))
            }

            onSuccess()
        }
    }
}
