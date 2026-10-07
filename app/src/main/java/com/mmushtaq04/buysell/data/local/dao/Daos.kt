package com.mmushtaq04.buysell.data.local.dao

import androidx.room.*
import com.mmushtaq04.buysell.data.local.entity.*
import com.mmushtaq04.buysell.data.local.enums.ItemStatus
import kotlinx.coroutines.flow.Flow

data class PartyBalanceDto(
    val id: String,
    val name: String,
    val phone: String?,
    val cnic: String?,
    val balance: Long // PKR paisa
)

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: String): UserEntity?

    @Query("SELECT * FROM users LIMIT 1")
    suspend fun getPrimaryUser(): UserEntity?

    @Query("SELECT * FROM users LIMIT 1")
    fun observePrimaryUser(): Flow<UserEntity?>
}

@Dao
interface ShopDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShop(shop: ShopEntity)

    @Update
    suspend fun updateShop(shop: ShopEntity)

    @Query("SELECT * FROM shops WHERE id = :shopId AND deleted_at IS NULL LIMIT 1")
    suspend fun getShopById(shopId: String): ShopEntity?

    @Query("SELECT * FROM shops WHERE id = :shopId AND deleted_at IS NULL LIMIT 1")
    fun observeShopById(shopId: String): Flow<ShopEntity?>

    @Query("SELECT * FROM shops WHERE deleted_at IS NULL LIMIT 1")
    fun observePrimaryShop(): Flow<ShopEntity?>
}

@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Query("SELECT * FROM categories WHERE shop_id = :shopId AND enabled = 1 AND deleted_at IS NULL ORDER BY sort_order ASC")
    fun observeCategories(shopId: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE shop_id = :shopId AND deleted_at IS NULL ORDER BY sort_order ASC")
    fun observeAllCategories(shopId: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE shop_id = :shopId AND enabled = 1 AND deleted_at IS NULL ORDER BY sort_order ASC")
    suspend fun getEnabledCategories(shopId: String): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE shop_id = :shopId AND deleted_at IS NULL")
    suspend fun getCategories(shopId: String): List<CategoryEntity>
}

@Dao
interface PartyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParty(party: PartyEntity)

    @Update
    suspend fun updateParty(party: PartyEntity)

    @Query("SELECT * FROM parties WHERE id = :partyId AND deleted_at IS NULL LIMIT 1")
    suspend fun getPartyById(partyId: String): PartyEntity?

    @Query("SELECT * FROM parties WHERE shop_id = :shopId AND deleted_at IS NULL ORDER BY name ASC")
    fun observeParties(shopId: String): Flow<List<PartyEntity>>

    @Query("SELECT * FROM parties WHERE shop_id = :shopId AND deleted_at IS NULL")
    suspend fun getAllParties(shopId: String): List<PartyEntity>

    @Query("""
        SELECT p.id, p.name, p.phone, p.cnic,
          COALESCE((SELECT SUM(
            CASE t.type
              WHEN 'SALE' THEN t.total_amount
              WHEN 'SALE_RETURN' THEN -t.total_amount
              WHEN 'PURCHASE' THEN -t.total_amount
              WHEN 'PURCHASE_RETURN' THEN t.total_amount
              ELSE 0 END)
            FROM txns t
            WHERE t.party_id = p.id AND t.deleted_at IS NULL), 0)
          - COALESCE((SELECT SUM(CASE WHEN y.direction = 'IN' THEN y.amount ELSE -y.amount END)
            FROM payments y
            WHERE y.party_id = p.id AND y.deleted_at IS NULL), 0) AS balance
        FROM parties p
        WHERE p.shop_id = :shopId AND p.deleted_at IS NULL
        ORDER BY p.name ASC
    """)
    fun observePartiesWithBalances(shopId: String): Flow<List<PartyBalanceDto>>

    @Query("""
        SELECT p.id, p.name, p.phone, p.cnic,
          COALESCE((SELECT SUM(
            CASE t.type
              WHEN 'SALE' THEN t.total_amount
              WHEN 'SALE_RETURN' THEN -t.total_amount
              WHEN 'PURCHASE' THEN -t.total_amount
              WHEN 'PURCHASE_RETURN' THEN t.total_amount
              ELSE 0 END)
            FROM txns t
            WHERE t.party_id = p.id AND t.deleted_at IS NULL), 0)
          - COALESCE((SELECT SUM(CASE WHEN y.direction = 'IN' THEN y.amount ELSE -y.amount END)
            FROM payments y
            WHERE y.party_id = p.id AND y.deleted_at IS NULL), 0) AS balance
        FROM parties p
        WHERE p.shop_id = :shopId AND p.id = :partyId AND p.deleted_at IS NULL
        LIMIT 1
    """)
    suspend fun getPartyBalance(shopId: String, partyId: String): PartyBalanceDto?

    @Query("""
        SELECT p.id, p.name, p.phone, p.cnic,
          COALESCE((SELECT SUM(
            CASE t.type
              WHEN 'SALE' THEN t.total_amount
              WHEN 'SALE_RETURN' THEN -t.total_amount
              ELSE 0 END)
            FROM txns t
            WHERE t.party_id = p.id AND t.scope = 'PUBLIC' AND t.deleted_at IS NULL), 0)
          - COALESCE((SELECT SUM(CASE WHEN y.direction = 'IN' THEN y.amount ELSE -y.amount END)
            FROM payments y
            WHERE y.party_id = p.id AND y.scope = 'PUBLIC' AND y.deleted_at IS NULL), 0) AS balance
        FROM parties p
        WHERE p.shop_id = :shopId AND p.id = :partyId AND p.deleted_at IS NULL
        LIMIT 1
    """)
    suspend fun getPublicPartyBalance(shopId: String, partyId: String): PartyBalanceDto?
}

@Dao
interface StockItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockItem(item: StockItemEntity)

    @Update
    suspend fun updateStockItem(item: StockItemEntity)

    @Query("SELECT * FROM stock_items WHERE id = :id AND deleted_at IS NULL LIMIT 1")
    suspend fun getStockItemById(id: String): StockItemEntity?

    @Query("SELECT * FROM stock_items WHERE shop_id = :shopId AND status = 'IN_STOCK' AND deleted_at IS NULL ORDER BY stocked_at DESC")
    fun observeInStockItems(shopId: String): Flow<List<StockItemEntity>>

    @Query("SELECT * FROM stock_items WHERE shop_id = :shopId AND deleted_at IS NULL")
    suspend fun getAllStockItems(shopId: String): List<StockItemEntity>

    @Query("SELECT * FROM stock_items WHERE shop_id = :shopId AND identifier = :identifier AND status = 'IN_STOCK' AND deleted_at IS NULL LIMIT 1")
    suspend fun findInStockByIdentifier(shopId: String, identifier: String): StockItemEntity?

    @Query("SELECT * FROM stock_items WHERE shop_id = :shopId AND identifier = :identifier AND deleted_at IS NULL ORDER BY stocked_at DESC")
    suspend fun findByIdentifierHistory(shopId: String, identifier: String): List<StockItemEntity>

    // Quantity lot FIFO query
    @Query("""
        SELECT * FROM stock_items 
        WHERE shop_id = :shopId 
          AND category_id = :categoryId 
          AND brand = :brand 
          AND model = :model 
          AND status = 'IN_STOCK' 
          AND remaining_qty > 0 
          AND deleted_at IS NULL 
        ORDER BY stocked_at ASC
    """)
    suspend fun findAvailableLotsFifo(shopId: String, categoryId: String, brand: String, model: String): List<StockItemEntity>
}

data class TxnWithLines(
    @Embedded val txn: TxnEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "txn_id"
    )
    val lines: List<TxnLineEntity>
)

@Dao
interface TxnDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTxn(txn: TxnEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTxnLines(lines: List<TxnLineEntity>)

    @Transaction
    suspend fun insertTxnWithLines(txn: TxnEntity, lines: List<TxnLineEntity>) {
        insertTxn(txn)
        insertTxnLines(lines)
    }

    @Query("SELECT * FROM txns WHERE id = :txnId AND deleted_at IS NULL LIMIT 1")
    suspend fun getTxnById(txnId: String): TxnEntity?

    @Query("DELETE FROM txns WHERE id = :txnId")
    suspend fun deleteTxn(txnId: String)

    @Query("SELECT unit_price FROM txn_lines WHERE id = :lineId LIMIT 1")
    suspend fun getUnitPriceByLineId(lineId: String): Long?

    @Transaction
    @Query("SELECT * FROM txns WHERE id = :txnId AND deleted_at IS NULL LIMIT 1")
    suspend fun getTxnWithLinesById(txnId: String): TxnWithLines?

    @Query("SELECT * FROM txns WHERE shop_id = :shopId AND deleted_at IS NULL ORDER BY txn_date DESC")
    fun observeTxns(shopId: String): Flow<List<TxnEntity>>

    @Query("SELECT * FROM txns WHERE shop_id = :shopId AND deleted_at IS NULL")
    suspend fun getAllTxns(shopId: String): List<TxnEntity>

    @Query("SELECT * FROM txns WHERE shop_id = :shopId AND party_id = :partyId AND deleted_at IS NULL ORDER BY txn_date DESC")
    fun observeTxnsByParty(shopId: String, partyId: String): Flow<List<TxnEntity>>

    @Query("""
        SELECT COUNT(*) FROM txns 
        WHERE shop_id = :shopId 
          AND type = 'SALE' 
          AND txn_date >= :startOfDayMs 
          AND txn_date <= :endOfDayMs 
          AND deleted_at IS NULL
    """)
    suspend fun getTodaySalesCount(shopId: String, startOfDayMs: Long, endOfDayMs: Long): Int

    @Query("""
        SELECT COALESCE(SUM(total_amount), 0) FROM txns 
        WHERE shop_id = :shopId 
          AND type = 'SALE' 
          AND txn_date >= :startOfDayMs 
          AND txn_date <= :endOfDayMs 
          AND deleted_at IS NULL
    """)
    suspend fun getTodaySalesAmountPaisa(shopId: String, startOfDayMs: Long, endOfDayMs: Long): Long

    @Query("""
        SELECT COUNT(*) FROM txns 
        WHERE shop_id = :shopId 
          AND type = 'SALE' 
          AND txn_date >= :startOfDayMs 
          AND txn_date <= :endOfDayMs 
          AND deleted_at IS NULL
    """)
    fun observeTodaySalesCount(shopId: String, startOfDayMs: Long, endOfDayMs: Long): Flow<Int>

    @Query("""
        SELECT COALESCE(SUM(total_amount), 0) FROM txns 
        WHERE shop_id = :shopId 
          AND type = 'SALE' 
          AND txn_date >= :startOfDayMs 
          AND txn_date <= :endOfDayMs 
          AND deleted_at IS NULL
    """)
    fun observeTodaySalesAmountPaisa(shopId: String, startOfDayMs: Long, endOfDayMs: Long): Flow<Long>
}

@Dao
interface PaymentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity)

    @Query("DELETE FROM payments WHERE id = :paymentId")
    suspend fun deletePayment(paymentId: String)

    @Query("SELECT * FROM payments WHERE shop_id = :shopId AND party_id = :partyId AND deleted_at IS NULL ORDER BY pay_date DESC")
    fun observePaymentsByParty(shopId: String, partyId: String): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE shop_id = :shopId AND deleted_at IS NULL")
    suspend fun getAllPayments(shopId: String): List<PaymentEntity>

    @Query("SELECT * FROM payments WHERE shop_id = :shopId AND txn_id = :txnId AND deleted_at IS NULL ORDER BY pay_date DESC")
    suspend fun getPaymentsByTxn(shopId: String, txnId: String): List<PaymentEntity>

    @Query("SELECT SUM(amount) FROM payments WHERE shop_id = :shopId AND txn_id = :txnId AND deleted_at IS NULL")
    suspend fun getSumPaymentsForTxn(shopId: String, txnId: String): Long?
}

@Dao
interface PaymentPromiseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPromise(promise: PaymentPromiseEntity)

    @Update
    suspend fun updatePromise(promise: PaymentPromiseEntity)

    @Query("SELECT * FROM payment_promises WHERE shop_id = :shopId AND status = 'OPEN' AND promised_date < :nowMs AND deleted_at IS NULL ORDER BY promised_date ASC")
    fun observeOverduePromises(shopId: String, nowMs: Long): Flow<List<PaymentPromiseEntity>>

    @Query("SELECT * FROM payment_promises WHERE shop_id = :shopId AND party_id = :partyId AND deleted_at IS NULL ORDER BY promised_date DESC")
    fun observePromisesByParty(shopId: String, partyId: String): Flow<List<PaymentPromiseEntity>>

    @Query("SELECT * FROM payment_promises WHERE txn_id = :txnId AND status = 'OPEN' AND deleted_at IS NULL LIMIT 1")
    suspend fun getOpenPromiseForTxn(txnId: String): PaymentPromiseEntity?
}

@Dao
interface ExpenseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity)

    @Query("SELECT * FROM expenses WHERE shop_id = :shopId AND deleted_at IS NULL ORDER BY expense_date DESC")
    fun observeExpenses(shopId: String): Flow<List<ExpenseEntity>>
}

@Dao
interface AppMetaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(meta: AppMetaEntity)

    @Query("SELECT * FROM app_meta WHERE id = 1 LIMIT 1")
    suspend fun getAppMeta(): AppMetaEntity?

    @Query("UPDATE app_meta SET receipt_seq = receipt_seq + 1 WHERE id = 1")
    suspend fun incrementReceiptSeq()
}

@Dao
interface SyncDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueueOutbox(outbox: SyncOutboxEntity)

    @Query("SELECT * FROM sync_outbox ORDER BY created_at ASC")
    suspend fun getPendingOutbox(): List<SyncOutboxEntity>

    @Query("DELETE FROM sync_outbox WHERE id = :outboxId")
    suspend fun deleteOutbox(outboxId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCursor(cursor: SyncCursorEntity)

    @Query("SELECT * FROM sync_cursor WHERE collection_path = :path LIMIT 1")
    suspend fun getCursor(path: String): SyncCursorEntity?
}
