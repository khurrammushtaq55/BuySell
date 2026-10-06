package com.mmushtaq04.buysell.domain.model

import com.mmushtaq04.buysell.data.local.enums.*

data class Shop(
    val id: String,
    val name: String,
    val phone: String,
    val address: String,
    val currency: String = "PKR",
    val timezone: String = "Asia/Karachi",
    val ownerUserId: String,
    val receiptFooter: String? = null,
    val logoUri: String? = null,
    val slowStockDays: Int = 30,
    val preferredLanguage: String? = null,
    val cloudPhotoUploadEnabled: Boolean = false
)

data class Category(
    val id: String,
    val shopId: String,
    val name: String,
    val presetKey: String,
    val identifierType: IdentifierType,
    val trackingMode: TrackingMode,
    val fieldSchema: String? = null,
    val enabled: Boolean = true,
    val sortOrder: Int = 0
)

data class Party(
    val id: String,
    val shopId: String,
    val name: String,
    val phone: String? = null,
    val cnic: String? = null,
    val address: String? = null,
    val notes: String? = null,
    val typeHint: PartyTypeHint = PartyTypeHint.BOTH,
    val idPhotoUri: String? = null
)

data class PartyBalance(
    val partyId: String,
    val name: String,
    val phone: String?,
    val cnic: String?,
    val balance: Long // positive = party owes shop, negative = shop owes party
)

data class StockItem(
    val id: String,
    val shopId: String,
    val categoryId: String,
    val brand: String,
    val model: String,
    val identifier: String? = null,
    val identifier2: String? = null,
    val attributes: String? = null,
    val condition: String? = null,
    val quantity: Int = 1,
    val remainingQty: Int = 1,
    val status: ItemStatus = ItemStatus.IN_STOCK,
    val purchaseLineId: String? = null,
    val stockedAt: Long = System.currentTimeMillis(),
    val hasConflict: Boolean = false,
    val notes: String? = null
)

data class TxnLine(
    val id: String,
    val shopId: String,
    val txnId: String,
    val stockItemId: String,
    val quantity: Int = 1,
    val unitPrice: Long,
    val lineTotal: Long,
    val scope: Scope = Scope.PUBLIC
)

data class Txn(
    val id: String,
    val shopId: String,
    val type: TxnType,
    val partyId: String,
    val txnDate: Long = System.currentTimeMillis(),
    val totalAmount: Long,
    val receiptNo: String? = null,
    val exchangeGroupId: String? = null,
    val originalTxnId: String? = null,
    val scope: Scope = Scope.PUBLIC,
    val note: String? = null,
    val lines: List<TxnLine> = emptyList()
)

data class Payment(
    val id: String,
    val shopId: String,
    val txnId: String? = null,
    val partyId: String,
    val direction: PaymentDirection,
    val amount: Long,
    val method: PaymentMethod = PaymentMethod.CASH,
    val accountId: String? = null,
    val referenceNo: String? = null,
    val payDate: Long = System.currentTimeMillis(),
    val reversesPaymentId: String? = null,
    val scope: Scope = Scope.PUBLIC,
    val note: String? = null
)

data class PaymentPromise(
    val id: String,
    val shopId: String,
    val partyId: String,
    val txnId: String? = null,
    val direction: PromiseDirection,
    val amount: Long,
    val promisedDate: Long,
    val status: PromiseStatus = PromiseStatus.OPEN,
    val previousPromiseId: String? = null,
    val scope: Scope = Scope.PUBLIC,
    val note: String? = null
)
