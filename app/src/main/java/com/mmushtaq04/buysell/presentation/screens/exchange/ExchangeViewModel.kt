package com.mmushtaq04.buysell.presentation.screens.exchange

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.mmushtaq04.buysell.data.local.AppDatabase
import com.mmushtaq04.buysell.data.local.entity.PartyEntity
import com.mmushtaq04.buysell.data.local.enums.PartyTypeHint
import com.mmushtaq04.buysell.data.local.enums.PaymentDirection
import com.mmushtaq04.buysell.data.local.enums.PaymentMethod
import com.mmushtaq04.buysell.data.repository.StockRepositoryImpl
import com.mmushtaq04.buysell.data.repository.toEntity
import com.mmushtaq04.buysell.domain.model.Payment
import com.mmushtaq04.buysell.domain.model.StockItem
import com.mmushtaq04.buysell.presentation.screens.sell.SimpleStockItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class ExchangeViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val stockRepository = StockRepositoryImpl(db)

    private val _stockList = MutableStateFlow<List<SimpleStockItem>>(emptyList())
    val stockList: StateFlow<List<SimpleStockItem>> = _stockList.asStateFlow()

    companion object {
        private const val TAG = "ExchangeViewModel"
    }

    init {
        loadInStockItems()
    }

    private fun loadInStockItems() {
        viewModelScope.launch {
            val meta = db.appMetaDao().getAppMeta()
            val activeShopId = meta?.activeShopId ?: "default_shop"

            stockRepository.observeInStockItems(activeShopId).collect { list ->
                _stockList.value = list.map { item ->
                    val costPaisa = item.purchaseLineId?.let { db.txnDao().getUnitPriceByLineId(it) } ?: 0L
                    SimpleStockItem(
                        id = item.id,
                        title = "${item.brand} ${item.model}",
                        imei = item.identifier ?: "N/A",
                        cost = costPaisa / 100
                    )
                }
                Log.d(TAG, "Loaded ${list.size} in-stock items for Exchange")
            }
        }
    }

    fun saveExchange(
        soldStockItemId: String,
        newPhonePriceRs: Long,
        oldCategory: String,
        oldBrand: String,
        oldModel: String,
        oldImei: String,
        oldColor: String,
        oldIssue: String,
        oldPhoneValueRs: Long,
        customerName: String,
        customerPhone: String,
        recordedBy: String,
        cashPaidRs: Long,
        paymentMethodStr: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            Log.d(TAG, "Starting saveExchange: SoldItem ID=$soldStockItemId, NewPrice=Rs $newPhonePriceRs, OldBrand=$oldBrand, OldPrice=Rs $oldPhoneValueRs, Customer=$customerName")
            val meta = db.appMetaDao().getAppMeta()
            val activeShopId = meta?.activeShopId ?: "default_shop"
            val now = System.currentTimeMillis()
            val exchangeGroupId = UUID.randomUUID().toString()

            // 1. Create or Find Customer Party
            val partyId = UUID.randomUUID().toString()
            val customerParty = PartyEntity(
                id = partyId,
                shopId = activeShopId,
                name = customerName.ifBlank { "Exchange Customer" },
                phone = customerPhone.ifBlank { null },
                typeHint = PartyTypeHint.BOTH,
                createdAt = now,
                updatedAt = now,
                createdBy = recordedBy,
                updatedBy = recordedBy
            )
            db.partyDao().insertParty(customerParty)

            // 2. PURCHASE of Customer's Old Phone
            val categories = db.categoryDao().getCategories(activeShopId)
            val matchedCat = categories.firstOrNull { it.name.equals(oldCategory, ignoreCase = true) }
            val categoryId = matchedCat?.id ?: UUID.randomUUID().toString()

            val attrMap = mutableMapOf<String, String>()
            if (oldColor.isNotBlank()) attrMap["color"] = oldColor.trim()
            if (oldIssue.isNotBlank()) attrMap["issue"] = oldIssue.trim()
            val attributesJson = if (attrMap.isNotEmpty()) Gson().toJson(attrMap) else null

            val oldStockItem = StockItem(
                id = UUID.randomUUID().toString(),
                shopId = activeShopId,
                categoryId = categoryId,
                brand = oldBrand.ifBlank { "Generic" },
                model = oldModel.ifBlank { oldCategory },
                identifier = oldImei.ifBlank { null },
                attributes = attributesJson,
                condition = oldIssue.ifBlank { "GOOD" },
                quantity = 1,
                remainingQty = 1,
                stockedAt = now
            )

            val purchaseValuePaisa = oldPhoneValueRs * 100
            val purchaseTxn = stockRepository.recordPurchase(
                stockItem = oldStockItem,
                purchasePrice = purchaseValuePaisa,
                partyId = partyId,
                createdByUserId = recordedBy
            )

            // Link Exchange Group ID to Purchase Txn
            val purchaseEntity = db.txnDao().getTxnById(purchaseTxn.id)
            if (purchaseEntity != null) {
                db.txnDao().insertTxn(purchaseEntity.copy(exchangeGroupId = exchangeGroupId))
            }

            // 3. SALE of Shop's Selected Device
            val salePricePaisa = newPhonePriceRs * 100
            val saleTxn = stockRepository.recordSale(
                stockItemId = soldStockItemId,
                salePrice = salePricePaisa,
                partyId = partyId,
                createdByUserId = recordedBy,
                qtyToSell = 1
            )

            // Link Exchange Group ID to Sale Txn
            val saleEntity = db.txnDao().getTxnById(saleTxn.id)
            if (saleEntity != null) {
                db.txnDao().insertTxn(saleEntity.copy(exchangeGroupId = exchangeGroupId))
            }

            Log.i(TAG, "✓ Linked Exchange Group ID '$exchangeGroupId' between Purchase Txn ${purchaseTxn.id} & Sale Txn ${saleTxn.id}")

            // 4. Record Net Cash Difference Payment
            val netDifferenceRs = newPhonePriceRs - oldPhoneValueRs
            val cashPaisa = cashPaidRs * 100

            if (cashPaisa > 0) {
                val methodEnum = PaymentMethod.fromStr(paymentMethodStr)

                val direction = if (netDifferenceRs >= 0) PaymentDirection.IN else PaymentDirection.OUT

                val payment = Payment(
                    id = UUID.randomUUID().toString(),
                    shopId = activeShopId,
                    txnId = saleTxn.id,
                    partyId = partyId,
                    direction = direction,
                    amount = cashPaisa,
                    method = methodEnum,
                    note = "Exchange Cash Difference",
                    payDate = now
                )
                db.paymentDao().insertPayment(payment.toEntity(recordedBy))
            }

            onSuccess()
        }
    }
}
