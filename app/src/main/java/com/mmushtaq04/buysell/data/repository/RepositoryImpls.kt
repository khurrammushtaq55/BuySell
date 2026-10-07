package com.mmushtaq04.buysell.data.repository

import com.google.gson.Gson
import com.mmushtaq04.buysell.data.local.AppDatabase
import com.mmushtaq04.buysell.data.local.entity.*
import com.mmushtaq04.buysell.data.local.enums.*
import com.mmushtaq04.buysell.data.sync.ConflictDetector
import com.mmushtaq04.buysell.data.sync.FirestoreSyncManager
import com.mmushtaq04.buysell.domain.model.*
import com.mmushtaq04.buysell.domain.repository.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

// --- Helper for Sync Outbox Enqueue ---
private suspend fun enqueueSyncOutbox(
    db: AppDatabase,
    entityType: String,
    entityId: String,
    op: SyncOp,
    payload: Any
) {
    runCatching {
        val json = Gson().toJson(payload)
        val outbox = SyncOutboxEntity(
            id = UUID.randomUUID().toString(),
            entityType = entityType,
            entityId = entityId,
            op = op,
            payloadJson = json,
            createdAt = System.currentTimeMillis()
        )
        db.syncDao().enqueueOutbox(outbox)

        val meta = db.appMetaDao().getAppMeta()
        val activeShopId = meta?.activeShopId ?: "default_shop"
        val user = db.userDao().getPrimaryUser()
        val role = user?.role ?: Role.STAFF
        val sessionId = user?.activeSessionId?.ifBlank { "session_active" } ?: "session_active"

        FirestoreSyncManager(db).pushOutbox(activeShopId, role, sessionId)
    }
}

// --- Mappers ---
fun StockItemEntity.toDomain() = StockItem(
    id = id,
    shopId = shopId,
    categoryId = categoryId,
    brand = brand,
    model = model,
    identifier = identifier,
    identifier2 = identifier2,
    attributes = attributes,
    condition = condition,
    quantity = quantity,
    remainingQty = remainingQty,
    status = status,
    purchaseLineId = purchaseLineId,
    stockedAt = stockedAt,
    hasConflict = hasConflict,
    notes = notes
)

fun StockItem.toEntity(userId: String = ""): StockItemEntity {
    val now = System.currentTimeMillis()
    return StockItemEntity(
        id = id.ifBlank { UUID.randomUUID().toString() },
        shopId = shopId,
        categoryId = categoryId,
        brand = brand,
        model = model,
        identifier = identifier,
        identifier2 = identifier2,
        attributes = attributes,
        condition = condition,
        quantity = quantity,
        remainingQty = remainingQty,
        status = status,
        purchaseLineId = purchaseLineId,
        stockedAt = stockedAt,
        hasConflict = hasConflict,
        notes = notes,
        createdAt = now,
        updatedAt = now,
        createdBy = userId,
        updatedBy = userId
    )
}

fun PartyEntity.toDomain() = Party(
    id = id,
    shopId = shopId,
    name = name,
    phone = phone,
    cnic = cnic,
    address = address,
    notes = notes,
    typeHint = typeHint,
    idPhotoUri = idPhotoUri
)

fun Party.toEntity(userId: String = ""): PartyEntity {
    val now = System.currentTimeMillis()
    return PartyEntity(
        id = id.ifBlank { UUID.randomUUID().toString() },
        shopId = shopId,
        name = name,
        phone = phone,
        cnic = cnic,
        address = address,
        notes = notes,
        typeHint = typeHint,
        idPhotoUri = idPhotoUri,
        createdAt = now,
        updatedAt = now,
        createdBy = userId,
        updatedBy = userId
    )
}

fun TxnLineEntity.toDomain() = TxnLine(
    id = id,
    shopId = shopId,
    txnId = txnId,
    stockItemId = stockItemId,
    quantity = quantity,
    unitPrice = unitPrice,
    lineTotal = lineTotal,
    scope = scope
)

fun TxnEntity.toDomain(lines: List<TxnLineEntity> = emptyList()) = Txn(
    id = id,
    shopId = shopId,
    type = type,
    partyId = partyId,
    txnDate = txnDate,
    totalAmount = totalAmount,
    receiptNo = receiptNo,
    exchangeGroupId = exchangeGroupId,
    originalTxnId = originalTxnId,
    scope = scope,
    note = note,
    lines = lines.map { it.toDomain() }
)

fun PaymentEntity.toDomain() = Payment(
    id = id,
    shopId = shopId,
    txnId = txnId,
    partyId = partyId,
    direction = direction,
    amount = amount,
    method = method,
    accountId = accountId,
    referenceNo = referenceNo,
    payDate = payDate,
    reversesPaymentId = reversesPaymentId,
    scope = scope,
    note = note
)

fun Payment.toEntity(userId: String = ""): PaymentEntity {
    val now = System.currentTimeMillis()
    return PaymentEntity(
        id = id.ifBlank { UUID.randomUUID().toString() },
        shopId = shopId,
        txnId = txnId,
        partyId = partyId,
        direction = direction,
        amount = amount,
        method = method,
        accountId = accountId,
        referenceNo = referenceNo,
        payDate = payDate,
        reversesPaymentId = reversesPaymentId,
        scope = scope,
        note = note,
        createdAt = now,
        updatedAt = now,
        createdBy = userId,
        updatedBy = userId
    )
}

fun PaymentPromiseEntity.toDomain() = PaymentPromise(
    id = id,
    shopId = shopId,
    partyId = partyId,
    txnId = txnId,
    direction = direction,
    amount = amount,
    promisedDate = promisedDate,
    status = status,
    previousPromiseId = previousPromiseId,
    scope = scope,
    note = note
)

fun PaymentPromise.toEntity(userId: String = ""): PaymentPromiseEntity {
    val now = System.currentTimeMillis()
    return PaymentPromiseEntity(
        id = id.ifBlank { UUID.randomUUID().toString() },
        shopId = shopId,
        partyId = partyId,
        txnId = txnId,
        direction = direction,
        amount = amount,
        promisedDate = promisedDate,
        status = status,
        previousPromiseId = previousPromiseId,
        scope = scope,
        note = note,
        createdAt = now,
        updatedAt = now,
        createdBy = userId,
        updatedBy = userId
    )
}

// --- Repositories Implementations ---

class StockRepositoryImpl(private val db: AppDatabase) : StockRepository {
    private val stockDao = db.stockItemDao()
    private val txnDao = db.txnDao()

    override fun observeInStockItems(shopId: String): Flow<List<StockItem>> {
        return stockDao.observeInStockItems(shopId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getStockItemById(id: String): StockItem? {
        return stockDao.getStockItemById(id)?.toDomain()
    }

    override suspend fun findInStockByIdentifier(shopId: String, identifier: String): StockItem? {
        return stockDao.findInStockByIdentifier(shopId, identifier)?.toDomain()
    }

    override suspend fun recordPurchase(
        stockItem: StockItem,
        purchasePrice: Long,
        partyId: String,
        createdByUserId: String
    ): Txn {
        val now = System.currentTimeMillis()
        val txnId = UUID.randomUUID().toString()
        val lineId = UUID.randomUUID().toString()
        val stockItemEntity = stockItem.copy(
            purchaseLineId = lineId,
            status = ItemStatus.IN_STOCK
        ).toEntity(createdByUserId)

        val txnEntity = TxnEntity(
            id = txnId,
            shopId = stockItem.shopId,
            type = TxnType.PURCHASE,
            partyId = partyId,
            txnDate = now,
            totalAmount = purchasePrice,
            scope = Scope.VAULT,
            createdAt = now,
            updatedAt = now,
            createdBy = createdByUserId,
            updatedBy = createdByUserId
        )

        val lineEntity = TxnLineEntity(
            id = lineId,
            shopId = stockItem.shopId,
            txnId = txnId,
            stockItemId = stockItemEntity.id,
            quantity = stockItem.quantity,
            unitPrice = purchasePrice / stockItem.quantity.coerceAtLeast(1),
            lineTotal = purchasePrice,
            scope = Scope.VAULT,
            createdAt = now,
            updatedAt = now,
            createdBy = createdByUserId,
            updatedBy = createdByUserId
        )

        stockDao.insertStockItem(stockItemEntity)
        txnDao.insertTxnWithLines(txnEntity, listOf(lineEntity))

        enqueueSyncOutbox(db, "stock_items", stockItemEntity.id, SyncOp.UPSERT, stockItemEntity)
        enqueueSyncOutbox(db, "txns", txnEntity.id, SyncOp.UPSERT, txnEntity)
        enqueueSyncOutbox(db, "txn_lines", lineEntity.id, SyncOp.UPSERT, lineEntity)

        return txnEntity.toDomain(listOf(lineEntity))
    }

    override suspend fun recordSale(
        stockItemId: String,
        salePrice: Long,
        partyId: String,
        createdByUserId: String,
        qtyToSell: Int
    ): Txn {
        val stockEntity = stockDao.getStockItemById(stockItemId)
            ?: throw IllegalArgumentException("Stock item $stockItemId not found")

        val now = System.currentTimeMillis()
        val txnId = UUID.randomUUID().toString()
        val lineId = UUID.randomUUID().toString()

        // Detect offline double-sale conflict if item was already marked as SOLD
        if (stockEntity.status == ItemStatus.SOLD) {
            ConflictDetector(db).recordConflict(
                shopId = stockEntity.shopId,
                stockItemId = stockItemId,
                txnIdA = stockEntity.purchaseLineId ?: "PREV_TXN",
                txnIdB = txnId
            )
        }

        val newRemaining = (stockEntity.remainingQty - qtyToSell).coerceAtLeast(0)
        val newStatus = if (newRemaining == 0) ItemStatus.SOLD else ItemStatus.IN_STOCK

        val updatedStockEntity = stockEntity.copy(
            remainingQty = newRemaining,
            status = newStatus,
            updatedAt = now,
            updatedBy = createdByUserId
        )

        val totalAmount = salePrice * qtyToSell
        val txnEntity = TxnEntity(
            id = txnId,
            shopId = stockEntity.shopId,
            type = TxnType.SALE,
            partyId = partyId,
            txnDate = now,
            totalAmount = totalAmount,
            scope = Scope.PUBLIC,
            createdAt = now,
            updatedAt = now,
            createdBy = createdByUserId,
            updatedBy = createdByUserId
        )

        val lineEntity = TxnLineEntity(
            id = lineId,
            shopId = stockEntity.shopId,
            txnId = txnId,
            stockItemId = stockItemId,
            quantity = qtyToSell,
            unitPrice = salePrice,
            lineTotal = totalAmount,
            scope = Scope.PUBLIC,
            createdAt = now,
            updatedAt = now,
            createdBy = createdByUserId,
            updatedBy = createdByUserId
        )

        stockDao.updateStockItem(updatedStockEntity)
        txnDao.insertTxnWithLines(txnEntity, listOf(lineEntity))

        enqueueSyncOutbox(db, "stock_items", updatedStockEntity.id, SyncOp.UPSERT, updatedStockEntity)
        enqueueSyncOutbox(db, "txns", txnEntity.id, SyncOp.UPSERT, txnEntity)
        enqueueSyncOutbox(db, "txn_lines", lineEntity.id, SyncOp.UPSERT, lineEntity)

        return txnEntity.toDomain(listOf(lineEntity))
    }

    override suspend fun findAvailableLotsFifo(
        shopId: String,
        categoryId: String,
        brand: String,
        model: String
    ): List<StockItem> {
        return stockDao.findAvailableLotsFifo(shopId, categoryId, brand, model).map { it.toDomain() }
    }
}

class PartyRepositoryImpl(private val db: AppDatabase) : PartyRepository {
    private val partyDao = db.partyDao()

    override fun observeParties(shopId: String): Flow<List<Party>> {
        return partyDao.observeParties(shopId).map { list -> list.map { it.toDomain() } }
    }

    override fun observePartiesWithBalances(shopId: String): Flow<List<PartyBalance>> {
        return partyDao.observePartiesWithBalances(shopId).map { list ->
            list.map {
                PartyBalance(
                    partyId = it.id,
                    name = it.name,
                    phone = it.phone,
                    cnic = it.cnic,
                    balance = it.balance
                )
            }
        }
    }

    override suspend fun getPartyById(partyId: String): Party? {
        return partyDao.getPartyById(partyId)?.toDomain()
    }

    override suspend fun createParty(party: Party) {
        val entity = party.toEntity()
        partyDao.insertParty(entity)
        enqueueSyncOutbox(db, "parties", entity.id, SyncOp.UPSERT, entity)
    }

    override suspend fun getPartyBalance(shopId: String, partyId: String): PartyBalance? {
        val dto = partyDao.getPartyBalance(shopId, partyId) ?: return null
        return PartyBalance(
            partyId = dto.id,
            name = dto.name,
            phone = dto.phone,
            cnic = dto.cnic,
            balance = dto.balance
        )
    }

    override suspend fun getPublicPartyBalance(shopId: String, partyId: String): PartyBalance? {
        val dto = partyDao.getPublicPartyBalance(shopId, partyId) ?: return null
        return PartyBalance(
            partyId = dto.id,
            name = dto.name,
            phone = dto.phone,
            cnic = dto.cnic,
            balance = dto.balance
        )
    }
}

class TxnRepositoryImpl(private val db: AppDatabase) : TxnRepository {
    private val txnDao = db.txnDao()

    override fun observeTxns(shopId: String): Flow<List<Txn>> {
        return txnDao.observeTxns(shopId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getTxnById(txnId: String): Txn? {
        val txnWithLines = txnDao.getTxnWithLinesById(txnId) ?: return null
        return txnWithLines.txn.toDomain(txnWithLines.lines)
    }
}

class PaymentRepositoryImpl(private val db: AppDatabase) : PaymentRepository {
    private val paymentDao = db.paymentDao()
    private val promiseDao = db.paymentPromiseDao()

    override fun observePaymentsByParty(shopId: String, partyId: String): Flow<List<Payment>> {
        return paymentDao.observePaymentsByParty(shopId, partyId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun recordPayment(payment: Payment) {
        val entity = payment.toEntity()
        paymentDao.insertPayment(entity)
        enqueueSyncOutbox(db, "payments", entity.id, SyncOp.UPSERT, entity)

        // Check if there is an OPEN promise on linked txn and if fully paid, auto-KEPT
        payment.txnId?.let { txnId ->
            val openPromise = promiseDao.getOpenPromiseForTxn(txnId)
            if (openPromise != null) {
                val sumPaid = paymentDao.getSumPaymentsForTxn(payment.shopId, txnId) ?: 0L
                val txnEntity = db.txnDao().getTxnById(txnId)
                if (txnEntity != null && sumPaid >= txnEntity.totalAmount) {
                    val updated = openPromise.copy(status = PromiseStatus.KEPT)
                    promiseDao.updatePromise(updated)
                    enqueueSyncOutbox(db, "payment_promises", updated.id, SyncOp.UPSERT, updated)
                }
            }
        }
    }

    override suspend fun reversePayment(paymentId: String, reason: String, userId: String) {
        // Reversal logic
    }

    override fun observeOverduePromises(shopId: String, nowMs: Long): Flow<List<PaymentPromise>> {
        return promiseDao.observeOverduePromises(shopId, nowMs).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun recordPromise(promise: PaymentPromise) {
        val entity = promise.toEntity()
        promiseDao.insertPromise(entity)
        enqueueSyncOutbox(db, "payment_promises", entity.id, SyncOp.UPSERT, entity)
    }
}
