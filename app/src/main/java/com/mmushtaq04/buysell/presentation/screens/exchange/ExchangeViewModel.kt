package com.mmushtaq04.buysell.presentation.screens.exchange

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
import com.mmushtaq04.buysell.data.local.enums.Scope
import com.mmushtaq04.buysell.data.local.enums.SyncOp
import com.mmushtaq04.buysell.data.repository.StockRepositoryImpl
import com.mmushtaq04.buysell.data.repository.toEntity
import com.mmushtaq04.buysell.domain.model.Payment
import com.mmushtaq04.buysell.domain.model.StockItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class ExchangeViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val stockRepository = StockRepositoryImpl(db)

    private val _stockItems = MutableStateFlow<List<StockItem>>(emptyList())
    val stockItems: StateFlow<List<StockItem>> = _stockItems.asStateFlow()

    companion object {
        private const val TAG = "ExchangeViewModel"
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

    fun processExchange(
        soldStockItemId: String,
        oldPhoneBrand: String,
        oldPhoneModel: String,
        oldPhoneImei: String,
        oldPhoneColor: String = "",
        oldPhoneIssue: String = "",
        oldPhoneRam: String = "",
        oldPhoneStorage: String = "",
        oldPhoneSpecs: String = "",
        oldPhoneValueRs: Long,
        newPhonePriceRs: Long,
        cashPaidRs: Long,
        customerName: String,
        customerPhone: String,
        customerCnic: String,
        recordedBy: String,
        paymentMethodStr: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            Log.d(TAG, "Starting processExchange: SoldStockItemID=$soldStockItemId, OldPhone=$oldPhoneBrand $oldPhoneModel, Value=Rs $oldPhoneValueRs, NewPrice=Rs $newPhonePriceRs, Customer=$customerName")
            val user = db.userDao().getPrimaryUser()
            val meta = db.appMetaDao().getAppMeta()
            val activeShopId = meta?.activeShopId?.ifBlank { null } ?: user?.shopId ?: ""
            val now = System.currentTimeMillis()
            val exchangeGroupId = "EXG-${UUID.randomUUID().toString().take(8)}"

            // 1. Create or Find Customer/Party
            val partyId = UUID.randomUUID().toString()
            val partyEntity = PartyEntity(
                id = partyId,
                shopId = activeShopId,
                name = customerName.ifBlank { "Exchange Customer" },
                phone = customerPhone.ifBlank { null },
                cnic = customerCnic.ifBlank { null },
                typeHint = PartyTypeHint.BOTH,
                createdAt = now,
                updatedAt = now,
                createdBy = recordedBy,
                updatedBy = recordedBy
            )
            db.partyDao().insertParty(partyEntity)
            db.syncDao().enqueueOutbox(
                SyncOutboxEntity(
                    id = UUID.randomUUID().toString(),
                    entityType = "parties",
                    entityId = partyEntity.id,
                    op = SyncOp.UPSERT,
                    payloadJson = Gson().toJson(partyEntity),
                    createdAt = now
                )
            )

            // Construct attributes JSON for trade-in device
            val attrMap = mutableMapOf<String, String>()
            if (oldPhoneColor.isNotBlank()) attrMap["color"] = oldPhoneColor.trim()
            if (oldPhoneIssue.isNotBlank()) attrMap["issue"] = oldPhoneIssue.trim()
            if (oldPhoneRam.isNotBlank()) attrMap["ram"] = oldPhoneRam.trim()
            if (oldPhoneStorage.isNotBlank()) attrMap["storage"] = oldPhoneStorage.trim()
            if (oldPhoneSpecs.isNotBlank()) attrMap["specs"] = oldPhoneSpecs.trim()
            val attributesJson = if (attrMap.isNotEmpty()) Gson().toJson(attrMap) else null

            // 2. PURCHASE of Customer's Old Trade-In Device
            val oldStockItem = StockItem(
                id = UUID.randomUUID().toString(),
                shopId = activeShopId,
                categoryId = "exchange_cat",
                brand = oldPhoneBrand.ifBlank { "Generic" },
                model = oldPhoneModel.ifBlank { "Trade-in Device" },
                identifier = oldPhoneImei.ifBlank { null },
                attributes = attributesJson,
                condition = oldPhoneIssue.ifBlank { "USED_EXCHANGE" },
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
                val scope = if (direction == PaymentDirection.OUT) Scope.VAULT else Scope.PUBLIC

                val payment = Payment(
                    id = UUID.randomUUID().toString(),
                    shopId = activeShopId,
                    txnId = saleTxn.id,
                    partyId = partyId,
                    direction = direction,
                    amount = cashPaisa,
                    method = methodEnum,
                    note = "Exchange Cash Difference",
                    payDate = now,
                    scope = scope
                )
                db.paymentDao().insertPayment(payment.toEntity(recordedBy))
            }

            onSuccess()
        }
    }
}
