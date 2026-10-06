package com.mmushtaq04.buysell.domain.repository

import com.mmushtaq04.buysell.domain.model.*
import kotlinx.coroutines.flow.Flow

interface StockRepository {
    fun observeInStockItems(shopId: String): Flow<List<StockItem>>
    suspend fun getStockItemById(id: String): StockItem?
    suspend fun findInStockByIdentifier(shopId: String, identifier: String): StockItem?
    suspend fun recordPurchase(stockItem: StockItem, purchasePrice: Long, partyId: String, createdByUserId: String): Txn
    suspend fun recordSale(stockItemId: String, salePrice: Long, partyId: String, createdByUserId: String, qtyToSell: Int = 1): Txn
    suspend fun findAvailableLotsFifo(shopId: String, categoryId: String, brand: String, model: String): List<StockItem>
}

interface PartyRepository {
    fun observeParties(shopId: String): Flow<List<Party>>
    fun observePartiesWithBalances(shopId: String): Flow<List<PartyBalance>>
    suspend fun getPartyById(partyId: String): Party?
    suspend fun createParty(party: Party)
    suspend fun getPartyBalance(shopId: String, partyId: String): PartyBalance?
    suspend fun getPublicPartyBalance(shopId: String, partyId: String): PartyBalance?
}

interface TxnRepository {
    fun observeTxns(shopId: String): Flow<List<Txn>>
    suspend fun getTxnById(txnId: String): Txn?
}

interface PaymentRepository {
    fun observePaymentsByParty(shopId: String, partyId: String): Flow<List<Payment>>
    suspend fun recordPayment(payment: Payment)
    suspend fun reversePayment(paymentId: String, reason: String, userId: String)
    fun observeOverduePromises(shopId: String, nowMs: Long = System.currentTimeMillis()): Flow<List<PaymentPromise>>
    suspend fun recordPromise(promise: PaymentPromise)
}
