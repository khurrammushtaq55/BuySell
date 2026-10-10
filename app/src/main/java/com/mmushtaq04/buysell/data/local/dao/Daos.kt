package com.mmushtaq04.buysell.data.local.dao

import androidx.room.*
import com.mmushtaq04.buysell.data.local.entity.*
import com.mmushtaq04.buysell.data.local.enums.ItemStatus
import com.mmushtaq04.buysell.data.local.enums.PromiseStatus
import com.mmushtaq04.buysell.data.local.enums.Role
import com.mmushtaq04.buysell.data.local.enums.Scope
import com.mmushtaq04.buysell.data.local.enums.TxnType
import kotlinx.coroutines.flow.Flow

@Dao
interface ShopDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShop(shop: ShopEntity)

    @Query("SELECT * FROM shops WHERE id = :shopId LIMIT 1")
    suspend fun getShopById(shopId: String): ShopEntity?

    @Query("SELECT * FROM shops WHERE id = :shopId LIMIT 1")
    fun observeShopById(shopId: String): Flow<ShopEntity?>

    @Query("SELECT * FROM shops WHERE owner_user_id = :userId LIMIT 1")
    suspend fun getShopByOwner(userId: String): ShopEntity?

    @Update
    suspend fun updateShop(shop: ShopEntity)
}

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
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Query("SELECT * FROM categories WHERE shop_id = :shopId AND enabled = 1 ORDER BY sort_order ASC")
    fun observeCategories(shopId: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE shop_id = :shopId ORDER BY sort_order ASC")
    fun observeAllCategories(shopId: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE shop_id = :shopId AND enabled = 1 ORDER BY sort_order ASC")
    suspend fun getEnabledCategories(shopId: String): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE shop_id = :shopId ORDER BY sort_order ASC")
    suspend fun getCategories(shopId: String): List<CategoryEntity>
}

@Dao
interface PartyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParty(party: PartyEntity)

    @Query("DELETE FROM parties WHERE id = :partyId")
    suspend fun deletePartyById(partyId: String)

    @Query("SELECT * FROM parties WHERE shop_id = :shopId AND deleted_at IS NULL ORDER BY name ASC")
    fun observeParties(shopId: String): Flow<List<PartyEntity>>

    @Query("SELECT * FROM parties WHERE shop_id = :shopId AND deleted_at IS NULL")
    suspend fun getAllParties(shopId: String): List<PartyEntity>

    @Query("SELECT * FROM parties WHERE id = :partyId LIMIT 1")
    suspend fun getPartyById(partyId: String): PartyEntity?

    // Total Party Balance = (Sales - SaleReturns - PaymentsIn) - (Purchases - PurchaseReturns - PaymentsOut)
    @Query("""
        SELECT p.id, p.name, p.phone, p.cnic,
               (
                (COALESCE((SELECT SUM(t.total_amount) FROM txns t WHERE t.party_id = p.id AND t.type = 'SALE' AND t.deleted_at IS NULL), 0) -
                 COALESCE((SELECT SUM(t.total_amount) FROM txns t WHERE t.party_id = p.id AND t.type = 'SALE_RETURN' AND t.deleted_at IS NULL), 0) -
                 COALESCE((SELECT SUM(pay.amount) FROM payments pay WHERE pay.party_id = p.id AND pay.direction = 'IN' AND pay.deleted_at IS NULL), 0))
                -
                (COALESCE((SELECT SUM(t.total_amount) FROM txns t WHERE t.party_id = p.id AND t.type = 'PURCHASE' AND t.deleted_at IS NULL), 0) -
                 COALESCE((SELECT SUM(t.total_amount) FROM txns t WHERE t.party_id = p.id AND t.type = 'PURCHASE_RETURN' AND t.deleted_at IS NULL), 0) -
                 COALESCE((SELECT SUM(pay.amount) FROM payments pay WHERE pay.party_id = p.id AND pay.direction = 'OUT' AND pay.deleted_at IS NULL), 0))
               ) AS balance
        FROM parties p
        WHERE p.shop_id = :shopId AND p.deleted_at IS NULL
        ORDER BY p.name ASC
    """)
    fun observePartiesWithBalances(shopId: String): Flow<List<PartyBalanceDto>>

    @Query("""
        SELECT p.id, p.name, p.phone, p.cnic,
               (
                (COALESCE((SELECT SUM(t.total_amount) FROM txns t WHERE t.party_id = p.id AND t.type = 'SALE' AND t.deleted_at IS NULL), 0) -
                 COALESCE((SELECT SUM(t.total_amount) FROM txns t WHERE t.party_id = p.id AND t.type = 'SALE_RETURN' AND t.deleted_at IS NULL), 0) -
                 COALESCE((SELECT SUM(pay.amount) FROM payments pay WHERE pay.party_id = p.id AND pay.direction = 'IN' AND pay.deleted_at IS NULL), 0))
                -
                (COALESCE((SELECT SUM(t.total_amount) FROM txns t WHERE t.party_id = p.id AND t.type = 'PURCHASE' AND t.deleted_at IS NULL), 0) -
                 COALESCE((SELECT SUM(t.total_amount) FROM txns t WHERE t.party_id = p.id AND t.type = 'PURCHASE_RETURN' AND t.deleted_at IS NULL), 0) -
                 COALESCE((SELECT SUM(pay.amount) FROM payments pay WHERE pay.party_id = p.id AND pay.direction = 'OUT' AND pay.deleted_at IS NULL), 0))
               ) AS balance
        FROM parties p
        WHERE p.shop_id = :shopId AND p.id = :partyId AND p.deleted_at IS NULL
        LIMIT 1
    """)
    suspend fun getPartyBalance(shopId: String, partyId: String): PartyBalanceDto?

    @Query("""
        SELECT p.id, p.name, p.phone, p.cnic,
               (
                (COALESCE((SELECT SUM(t.total_amount) FROM txns t WHERE t.party_id = p.id AND t.type = 'SALE' AND t.deleted_at IS NULL), 0) -
                 COALESCE((SELECT SUM(t.total_amount) FROM txns t WHERE t.party_id = p.id AND t.type = 'SALE_RETURN' AND t.deleted_at IS NULL), 0) -
                 COALESCE((SELECT SUM(pay.amount) FROM payments pay WHERE pay.party_id = p.id AND pay.direction = 'IN' AND pay.deleted_at IS NULL), 0))
                -
                (COALESCE((SELECT SUM(t.total_amount) FROM txns t WHERE t.party_id = p.id AND t.type = 'PURCHASE' AND t.deleted_at IS NULL), 0) -
                 COALESCE((SELECT SUM(t.total_amount) FROM txns t WHERE t.party_id = p.id AND t.type = 'PURCHASE_RETURN' AND t.deleted_at IS NULL), 0) -
                 COALESCE((SELECT SUM(pay.amount) FROM payments pay WHERE pay.party_id = p.id AND pay.direction = 'OUT' AND pay.deleted_at IS NULL), 0))
               ) AS balance
        FROM parties p
        WHERE p.shop_id = :shopId AND p.id = :partyId AND p.deleted_at IS NULL
        LIMIT 1
    """)
    fun observePartyBalance(shopId: String, partyId: String): Flow<PartyBalanceDto?>

    @Query("""
        SELECT p.id, p.name, p.phone, p.cnic,
               (
                (COALESCE((SELECT SUM(t.total_amount) FROM txns t WHERE t.party_id = p.id AND t.type = 'SALE' AND t.scope = 'PUBLIC' AND t.deleted_at IS NULL), 0) -
                 COALESCE((SELECT SUM(t.total_amount) FROM txns t WHERE t.party_id = p.id AND t.type = 'SALE_RETURN' AND t.scope = 'PUBLIC' AND t.deleted_at IS NULL), 0) -
                 COALESCE((SELECT SUM(pay.amount) FROM payments pay WHERE pay.party_id = p.id AND pay.direction = 'IN' AND pay.scope = 'PUBLIC' AND pay.deleted_at IS NULL), 0))
                -
                (COALESCE((SELECT SUM(t.total_amount) FROM txns t WHERE t.party_id = p.id AND t.type = 'PURCHASE' AND t.scope = 'PUBLIC' AND t.deleted_at IS NULL), 0) -
                 COALESCE((SELECT SUM(t.total_amount) FROM txns t WHERE t.party_id = p.id AND t.type = 'PURCHASE_RETURN' AND t.scope = 'PUBLIC' AND t.deleted_at IS NULL), 0) -
                 COALESCE((SELECT SUM(pay.amount) FROM payments pay WHERE pay.party_id = p.id AND pay.direction = 'OUT' AND pay.scope = 'PUBLIC' AND pay.deleted_at IS NULL), 0))
               ) AS balance
        FROM parties p
        WHERE p.shop_id = :shopId AND p.id = :partyId AND p.deleted_at IS NULL
        LIMIT 1
    """)
    suspend fun getPublicPartyBalance(shopId: String, partyId: String): PartyBalanceDto?
}

data class PartyBalanceDto(
    val id: String,
    val name: String,
    val phone: String?,
    val cnic: String?,
    val balance: Long
)

@Dao
interface StockItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockItem(item: StockItemEntity)

    @Update
    suspend fun updateStockItem(item: StockItemEntity)

    @Query("SELECT * FROM stock_items WHERE id = :id LIMIT 1")
    suspend fun getStockItemById(id: String): StockItemEntity?

    @Query("SELECT * FROM stock_items WHERE shop_id = :shopId AND deleted_at IS NULL")
    suspend fun getAllStockItems(shopId: String): List<StockItemEntity>

    @Query("SELECT * FROM stock_items WHERE shop_id = :shopId AND status = 'IN_STOCK' AND remaining_qty > 0 AND deleted_at IS NULL ORDER BY stocked_at DESC")
    fun observeInStockItems(shopId: String): Flow<List<StockItemEntity>>

    @Query("SELECT * FROM stock_items WHERE shop_id = :shopId AND identifier = :identifier AND status = 'IN_STOCK' AND deleted_at IS NULL LIMIT 1")
    suspend fun findInStockByIdentifier(shopId: String, identifier: String): StockItemEntity?

    @Query("SELECT * FROM stock_items WHERE shop_id = :shopId AND identifier = :identifier AND deleted_at IS NULL")
    suspend fun findByIdentifierHistory(shopId: String, identifier: String): List<StockItemEntity>

    // FIFO Lot query: Pick oldest stocked lot with remaining_qty > 0
    @Query("""
        SELECT * FROM stock_items 
        WHERE shop_id = :shopId 
          AND status = 'IN_STOCK' 
          AND category_id = :categoryId 
          AND brand = :brand 
          AND model = :model 
          AND remaining_qty > 0
          AND deleted_at IS NULL
        ORDER BY stocked_at ASC
    """)
    suspend fun findAvailableLotsFifo(shopId: String, categoryId: String, brand: String, model: String): List<StockItemEntity>
}

@Dao
interface TxnDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTxn(txn: TxnEntity)

    @Query("DELETE FROM txns WHERE id = :txnId")
    suspend fun deleteTxn(txnId: String)

    @Query("DELETE FROM txns WHERE id = :txnId")
    suspend fun deleteTxnById(txnId: String)

    @Query("DELETE FROM txn_lines WHERE txn_id = :txnId")
    suspend fun deleteTxnLinesByTxnId(txnId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTxnLines(lines: List<TxnLineEntity>)

    @Transaction
    suspend fun insertTxnWithLines(txn: TxnEntity, lines: List<TxnLineEntity>) {
        insertTxn(txn)
        insertTxnLines(lines)
    }

    @Query("SELECT * FROM txns WHERE shop_id = :shopId AND deleted_at IS NULL ORDER BY txn_date DESC")
    fun observeTxns(shopId: String): Flow<List<TxnEntity>>

    @Query("SELECT * FROM txns WHERE shop_id = :shopId AND party_id = :partyId AND deleted_at IS NULL ORDER BY txn_date DESC")
    fun observeTxnsByParty(shopId: String, partyId: String): Flow<List<TxnEntity>>

    @Query("SELECT * FROM txns WHERE shop_id = :shopId AND txn_date >= :startTimeMs AND txn_date <= :endTimeMs AND deleted_at IS NULL ORDER BY txn_date DESC")
    suspend fun getTxnsInTimeRange(shopId: String, startTimeMs: Long, endTimeMs: Long): List<TxnEntity>

    @Query("SELECT * FROM txns WHERE shop_id = :shopId AND deleted_at IS NULL")
    suspend fun getAllTxns(shopId: String): List<TxnEntity>

    @Query("SELECT * FROM txns WHERE id = :txnId LIMIT 1")
    suspend fun getTxnById(txnId: String): TxnEntity?

    @Query("SELECT COUNT(*) FROM txns WHERE shop_id = :shopId AND type = 'SALE' AND txn_date >= :startOfDayMs AND txn_date <= :endOfDayMs AND deleted_at IS NULL")
    suspend fun getTodaySalesCount(shopId: String, startOfDayMs: Long, endOfDayMs: Long): Int

    @Query("SELECT COALESCE(SUM(total_amount), 0) FROM txns WHERE shop_id = :shopId AND type = 'SALE' AND txn_date >= :startOfDayMs AND txn_date <= :endOfDayMs AND deleted_at IS NULL")
    suspend fun getTodaySalesAmountPaisa(shopId: String, startOfDayMs: Long, endOfDayMs: Long): Long

    @Query("SELECT * FROM txn_lines WHERE txn_id = :txnId AND deleted_at IS NULL")
    suspend fun getTxnLinesForTxn(txnId: String): List<TxnLineEntity>

    @Query("SELECT * FROM txn_lines WHERE shop_id = :shopId AND deleted_at IS NULL")
    suspend fun getAllTxnLines(shopId: String): List<TxnLineEntity>

    @Transaction
    suspend fun getTxnWithLinesById(txnId: String): TxnWithLines? {
        val txn = getTxnById(txnId) ?: return null
        val lines = getTxnLinesForTxn(txnId)
        return TxnWithLines(txn, lines)
    }

    @Query("SELECT unit_price FROM txn_lines WHERE id = :lineId LIMIT 1")
    suspend fun getUnitPriceByLineId(lineId: String): Long?
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
interface PaymentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity)

    @Query("DELETE FROM payments WHERE id = :paymentId")
    suspend fun deletePayment(paymentId: String)

    @Query("SELECT * FROM payments WHERE shop_id = :shopId AND party_id = :partyId AND deleted_at IS NULL ORDER BY pay_date DESC")
    fun observePaymentsByParty(shopId: String, partyId: String): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE shop_id = :shopId AND deleted_at IS NULL")
    suspend fun getAllPayments(shopId: String): List<PaymentEntity>

    @Query("SELECT SUM(amount) FROM payments WHERE shop_id = :shopId AND txn_id = :txnId AND deleted_at IS NULL")
    suspend fun getSumPaymentsForTxn(shopId: String, txnId: String): Long?
}

@Dao
interface PaymentPromiseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPromise(promise: PaymentPromiseEntity)

    @Update
    suspend fun updatePromise(promise: PaymentPromiseEntity)

    @Query("SELECT * FROM payment_promises WHERE shop_id = :shopId AND promised_date <= :nowMs AND status = 'OPEN' AND deleted_at IS NULL ORDER BY promised_date ASC")
    fun observeOverduePromises(shopId: String, nowMs: Long): Flow<List<PaymentPromiseEntity>>

    @Query("SELECT * FROM payment_promises WHERE txn_id = :txnId AND status = 'OPEN' AND deleted_at IS NULL LIMIT 1")
    suspend fun getOpenPromiseForTxn(txnId: String): PaymentPromiseEntity?
}

@Dao
interface ExpenseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity)

    @Query("SELECT * FROM expenses WHERE id = :expenseId LIMIT 1")
    suspend fun getExpenseById(expenseId: String): ExpenseEntity?

    @Query("DELETE FROM expenses WHERE id = :expenseId")
    suspend fun hardDeleteExpense(expenseId: String)

    @Query("SELECT * FROM expenses WHERE shop_id = :shopId AND deleted_at IS NULL ORDER BY expense_date DESC")
    fun observeExpenses(shopId: String): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE shop_id = :shopId AND expense_date >= :startTimeMs AND expense_date <= :endTimeMs AND deleted_at IS NULL ORDER BY expense_date DESC")
    suspend fun getExpensesInTimeRange(shopId: String, startTimeMs: Long, endTimeMs: Long): List<ExpenseEntity>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE shop_id = :shopId AND expense_date >= :startTimeMs AND expense_date <= :endTimeMs AND deleted_at IS NULL")
    suspend fun getTotalExpensesPaisa(shopId: String, startTimeMs: Long, endTimeMs: Long): Long
}

@Dao
interface AppMetaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(meta: AppMetaEntity)

    @Query("SELECT * FROM app_meta LIMIT 1")
    suspend fun getAppMeta(): AppMetaEntity?

    @Query("SELECT * FROM app_meta LIMIT 1")
    fun observeAppMeta(): Flow<AppMetaEntity?>
}

@Dao
interface SyncDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueueOutbox(outbox: SyncOutboxEntity)

    @Query("SELECT * FROM sync_outbox ORDER BY created_at ASC")
    suspend fun getPendingOutbox(): List<SyncOutboxEntity>

    @Query("DELETE FROM sync_outbox WHERE id = :id")
    suspend fun deleteOutbox(id: String)

    @Query("SELECT * FROM sync_cursor WHERE collection_path = :collectionPath LIMIT 1")
    suspend fun getCursor(collectionPath: String): SyncCursorEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCursor(cursor: SyncCursorEntity)
}
