package com.mmushtaq04.buysell

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.mmushtaq04.buysell.data.local.AppDatabase
import com.mmushtaq04.buysell.data.local.entity.*
import com.mmushtaq04.buysell.data.local.enums.*
import com.mmushtaq04.buysell.data.repository.PartyRepositoryImpl
import com.mmushtaq04.buysell.data.repository.StockRepositoryImpl
import com.mmushtaq04.buysell.data.sync.ConflictDetector
import com.mmushtaq04.buysell.data.sync.scopeFor
import com.mmushtaq04.buysell.domain.InviteManager
import com.mmushtaq04.buysell.domain.model.Party
import com.mmushtaq04.buysell.domain.model.StockItem
import com.mmushtaq04.buysell.util.AppPinManager
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CoreLogicTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var stockRepo: StockRepositoryImpl
    private lateinit var partyRepo: PartyRepositoryImpl

    private val shopId = "shop-test-123"
    private val userId = "owner-uid-456"

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        stockRepo = StockRepositoryImpl(db)
        partyRepo = PartyRepositoryImpl(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testAppPinManagerSaltingAndVerification() {
        // Clear existing PIN
        AppPinManager.clearPin(context)
        assertFalse(AppPinManager.isPinSet(context))

        // Set 4-digit PIN "1234"
        AppPinManager.savePin(context, "1234")
        assertTrue(AppPinManager.isPinSet(context))

        // Verify correct PIN
        assertTrue(AppPinManager.verifyPin(context, "1234"))

        // Verify incorrect PIN fails
        assertFalse(AppPinManager.verifyPin(context, "9999"))
        assertFalse(AppPinManager.verifyPin(context, "0000"))

        // Remove PIN
        AppPinManager.clearPin(context)
        assertFalse(AppPinManager.isPinSet(context))
    }

    @Test
    fun testInviteCodeGenerationFormat() {
        // Generate invite code
        val inviteCode = InviteManager.generateInviteCode()
        assertNotNull(inviteCode)
        assertEquals(8, inviteCode.length)

        // Ensure uppercase alphanumeric without ambiguous characters (0, O, 1, I, L)
        val validCharRegex = Regex("^[A-HJ-NP-Z2-9]{8}$")
        assertTrue("Code '$inviteCode' does not match valid character set", validCharRegex.matches(inviteCode))
    }

    @Test
    fun testAppMetaDeviceCodeAndShopPersistence() = runBlocking {
        val meta = AppMetaEntity(
            id = 1,
            deviceCode = "DEV01",
            deviceId = "device_test_uuid",
            activeShopId = shopId,
            schemaVersion = 1,
            receiptSeq = 100
        )

        db.appMetaDao().insertOrUpdate(meta)

        val retrieved = db.appMetaDao().getAppMeta()
        assertNotNull(retrieved)
        assertEquals(shopId, retrieved?.activeShopId)
        assertEquals("DEV01", retrieved?.deviceCode)
        assertEquals(100, retrieved?.receiptSeq)
    }

    @Test
    fun testExchangeLinkedTransactionFlow() = runBlocking {
        // 1. Setup Customer
        val customer = Party(
            id = "party-cust-1",
            shopId = shopId,
            name = "Tariq Mahmood",
            phone = "03211234567"
        )
        partyRepo.createParty(customer)

        // 2. Setup Shop Stock Item (New Phone: Samsung Galaxy S24, PKR 200,000)
        val newPhone = StockItem(
            id = "item-new-s24",
            shopId = shopId,
            categoryId = "cat-mobile",
            brand = "Samsung",
            model = "Galaxy S24",
            identifier = "987654321098765",
            quantity = 1,
            remainingQty = 1
        )
        stockRepo.recordPurchase(newPhone, 18000000L, "supplier-x", userId)

        // 3. Trade-In Device (Old Phone: iPhone 11, Value PKR 60,000)
        val oldPhone = StockItem(
            id = "item-old-i11",
            shopId = shopId,
            categoryId = "cat-mobile",
            brand = "Apple",
            model = "iPhone 11",
            identifier = "111222333444555",
            quantity = 1,
            remainingQty = 1
        )

        // Record Sale of New Phone
        val saleTxn = stockRepo.recordSale(
            stockItemId = newPhone.id,
            salePrice = 20000000L, // 200,000 PKR
            partyId = customer.id,
            createdByUserId = userId
        )

        // Record Purchase of Old Phone (Trade-in)
        val purchaseTxn = stockRepo.recordPurchase(
            stockItem = oldPhone,
            purchasePrice = 6000000L, // 60,000 PKR
            partyId = customer.id,
            createdByUserId = userId
        )

        assertNotNull(saleTxn)
        assertNotNull(purchaseTxn)

        // Verify status of stock items
        val soldNew = stockRepo.getStockItemById(newPhone.id)
        val boughtOld = stockRepo.getStockItemById(oldPhone.id)

        assertEquals(ItemStatus.SOLD, soldNew?.status)
        assertEquals(0, soldNew?.remainingQty)

        assertEquals(ItemStatus.IN_STOCK, boughtOld?.status)
        assertEquals(1, boughtOld?.remainingQty)

        // Verify Net Customer Balance: 200,000 (Sale) - 60,000 (Trade-In Purchase) = 140,000 PKR
        val partyBal = partyRepo.getPartyBalance(shopId, customer.id)
        assertNotNull(partyBal)
        assertEquals(14000000L, partyBal?.balance)
    }

    @Test
    fun testTxnDaoVaultPurgeQueries() = runBlocking {
        // Create stock purchase transaction
        val item = StockItem(
            id = "item-purge-1",
            shopId = shopId,
            categoryId = "cat-mobile",
            brand = "Xiaomi",
            model = "Redmi Note 13",
            identifier = "555666777888999",
            quantity = 1,
            remainingQty = 1
        )

        val purchaseTxn = stockRepo.recordPurchase(item, 3500000L, "party-supplier-2", userId)
        assertNotNull(purchaseTxn)

        val txnId = purchaseTxn.id

        // Verify transaction and lines exist in Room
        val savedTxn = db.txnDao().getTxnById(txnId)
        val linesBefore = db.txnDao().getTxnLinesForTxn(txnId)
        assertNotNull(savedTxn)
        assertTrue(linesBefore.isNotEmpty())

        // Execute Staff Vault Purge DAO operations
        db.txnDao().deleteTxnLinesByTxnId(txnId)
        db.txnDao().deleteTxn(txnId)

        // Verify deletion
        val deletedTxn = db.txnDao().getTxnById(txnId)
        val linesAfter = db.txnDao().getTxnLinesForTxn(txnId)

        assertNull(deletedTxn)
        assertTrue(linesAfter.isEmpty())
    }

    @Test
    fun testPartyLedgerCalculationsWithPayments() = runBlocking {
        val party = Party(
            id = "party-ledger-1",
            shopId = shopId,
            name = "Bilal Khan",
            phone = "03009876543"
        )
        partyRepo.createParty(party)

        // Item 1: Sold for 100,000 PKR (10,000,000 paisa)
        val phone1 = StockItem(
            id = "phone-1",
            shopId = shopId,
            categoryId = "cat-mobile",
            brand = "Google",
            model = "Pixel 8",
            identifier = "333444555666777",
            quantity = 1,
            remainingQty = 1
        )
        stockRepo.recordPurchase(phone1, 8000000L, "supplier-1", userId)
        stockRepo.recordSale(phone1.id, 10000000L, party.id, userId)

        // Record Cash Payment from Customer: 40,000 PKR (4,000,000 paisa)
        val now = System.currentTimeMillis()
        val payment = PaymentEntity(
            id = UUID.randomUUID().toString(),
            shopId = shopId,
            partyId = party.id,
            direction = PaymentDirection.IN,
            amount = 4000000L,
            method = PaymentMethod.CASH,
            payDate = now,
            scope = Scope.VAULT,
            createdAt = now,
            updatedAt = now,
            createdBy = userId,
            updatedBy = userId
        )
        db.paymentDao().insertPayment(payment)

        // Verify Balance: 100,000 (Sale) - 40,000 (Payment) = 60,000 PKR (6,000,000 paisa)
        val bal = partyRepo.getPartyBalance(shopId, party.id)
        assertNotNull(bal)
        assertEquals(6000000L, bal?.balance)
    }

    @Test
    fun testConflictDetectorDoubleSaleFlagging() = runBlocking {
        val conflictDetector = ConflictDetector(db)

        val item = StockItem(
            id = "item-conflict-1",
            shopId = shopId,
            categoryId = "cat-mobile",
            brand = "Apple",
            model = "iPhone 14",
            identifier = "123123123123123",
            quantity = 1,
            remainingQty = 1
        )
        stockRepo.recordPurchase(item, 10000000L, "supplier-1", userId)

        // Ensure initially no conflict
        assertFalse(conflictDetector.checkConflictForStockItem(shopId, item.id))

        // Record double-sale conflict
        conflictDetector.recordConflict(shopId, item.id, "txn-sale-a", "txn-sale-b")

        // Verify stock item now flags hasConflict = true
        val updatedItem = stockRepo.getStockItemById(item.id)
        assertNotNull(updatedItem)
        assertTrue(updatedItem?.hasConflict == true)
        assertTrue(conflictDetector.checkConflictForStockItem(shopId, item.id))
    }

    @Test
    fun testStandalonePaymentScope() {
        assertEquals(Scope.PUBLIC, standalonePaymentScope(PaymentDirection.IN))
        assertEquals(Scope.VAULT, standalonePaymentScope(PaymentDirection.OUT))
    }

    @Test
    fun testScopeForResolution() {
        assertEquals(Scope.VAULT, scopeFor("expenses", null))
        assertEquals(Scope.VAULT, scopeFor("expense", "PUBLIC"))
        assertEquals(Scope.VAULT, scopeFor("audit_logs", null))
        assertEquals(Scope.VAULT, scopeFor("parties", "VAULT"))
        assertEquals(Scope.PUBLIC, scopeFor("parties", null))
        assertEquals(Scope.PUBLIC, scopeFor("parties", "PUBLIC"))
    }

    @Test
    fun testDaoSoftDeleteExclusion() = runBlocking {
        val now = System.currentTimeMillis()

        // 1. Party
        val p = PartyEntity(id = "p1", shopId = shopId, name = "Soft Party", createdAt = now, updatedAt = now, createdBy = userId, updatedBy = userId, deletedAt = now)
        db.partyDao().insertParty(p)
        assertTrue(db.partyDao().getAllParties(shopId).isEmpty())
        assertNull(db.partyDao().getPartyBalance(shopId, "p1"))

        // 2. Txn
        val t = TxnEntity(id = "t1", shopId = shopId, partyId = "p1", type = TxnType.SALE, totalAmount = 1000L, txnDate = now, scope = Scope.PUBLIC, createdAt = now, updatedAt = now, createdBy = userId, updatedBy = userId, deletedAt = now)
        db.txnDao().insertTxn(t)
        assertTrue(db.txnDao().getAllTxns(shopId).isEmpty())
        assertTrue(db.txnDao().getTxnsInTimeRange(shopId, 0L, now + 1000L).isEmpty())

        // 3. Payment
        val pay = PaymentEntity(id = "pay1", shopId = shopId, partyId = "p1", direction = PaymentDirection.IN, amount = 500L, method = PaymentMethod.CASH, payDate = now, scope = Scope.PUBLIC, createdAt = now, updatedAt = now, createdBy = userId, updatedBy = userId, deletedAt = now)
        db.paymentDao().insertPayment(pay)
        assertTrue(db.paymentDao().getAllPayments(shopId).isEmpty())

        // 4. Expense
        val exp = ExpenseEntity(id = "e1", shopId = shopId, categoryId = "RENT", amount = 2000L, expenseDate = now, createdAt = now, updatedAt = now, createdBy = userId, updatedBy = userId, deletedAt = now)
        db.expenseDao().insertExpense(exp)
        assertTrue(db.expenseDao().getExpensesInTimeRange(shopId, 0L, now + 1000L).isEmpty())
        assertEquals(0L, db.expenseDao().getTotalExpensesPaisa(shopId, 0L, now + 1000L))
    }

    @Test
    fun testStaffPurgeHardDelete() = runBlocking {
        val syncManager = com.mmushtaq04.buysell.data.sync.FirestoreSyncManager(db)
        val now = System.currentTimeMillis()

        val t = TxnEntity(id = "purge-t1", shopId = shopId, partyId = "p1", type = TxnType.PURCHASE, totalAmount = 5000L, txnDate = now, scope = Scope.VAULT, createdAt = now, updatedAt = now, createdBy = userId, updatedBy = userId)
        val line = TxnLineEntity(id = "purge-l1", shopId = shopId, txnId = "purge-t1", stockItemId = "item1", quantity = 1, unitPrice = 5000L, lineTotal = 5000L, createdAt = now, updatedAt = now, createdBy = userId, updatedBy = userId)

        db.txnDao().insertTxnWithLines(t, listOf(line))
        assertNotNull(db.txnDao().getTxnById("purge-t1"))
        assertFalse(db.txnDao().getTxnLinesForTxn("purge-t1").isEmpty())

        syncManager.purgeLocalVaultRow("txns", "purge-t1")

        assertNull(db.txnDao().getTxnById("purge-t1"))
        assertTrue(db.txnDao().getTxnLinesForTxn("purge-t1").isEmpty())
    }

    @Test
    fun testExpenseSoftDeleteAndOutboxUpsert() = runBlocking {
        val now = System.currentTimeMillis()
        val exp = ExpenseEntity(id = "exp-soft-1", shopId = shopId, categoryId = "RENT", amount = 10000L, expenseDate = now, createdAt = now, updatedAt = now, createdBy = userId, updatedBy = userId, rev = 1)
        db.expenseDao().insertExpense(exp)

        val expense = db.expenseDao().getExpenseById("exp-soft-1")
        assertNotNull(expense)

        val updatedExpense = expense!!.copy(
            deletedAt = now,
            deletedBy = userId,
            updatedAt = now,
            updatedBy = userId,
            rev = expense.rev + 1,
            syncState = SyncState.PENDING
        )

        db.withTransaction {
            db.expenseDao().insertExpense(updatedExpense)
            db.syncDao().enqueueOutbox(
                SyncOutboxEntity(
                    id = UUID.randomUUID().toString(),
                    entityType = "expenses",
                    entityId = updatedExpense.id,
                    op = SyncOp.UPSERT,
                    payloadJson = com.google.gson.Gson().toJson(updatedExpense),
                    createdAt = now
                )
            )
        }

        val stored = db.expenseDao().getExpenseById("exp-soft-1")
        assertNotNull(stored)
        assertNotNull(stored?.deletedAt)
        assertEquals(2L, stored?.rev)

        val pendingOutbox = db.syncDao().getPendingOutbox()
        val outboxItem = pendingOutbox.find { it.entityId == "exp-soft-1" }
        assertNotNull(outboxItem)
        assertEquals(com.mmushtaq04.buysell.data.local.enums.SyncOp.UPSERT, outboxItem?.op)
        assertTrue(outboxItem?.payloadJson?.contains("deleted_at") == true)
    }

    @Test
    fun testLedgerRecordWasooliPaymentWithOutDirection() = runBlocking {
        val now = System.currentTimeMillis()

        val paymentEntity = PaymentEntity(
            id = UUID.randomUUID().toString(),
            shopId = shopId,
            txnId = null,
            partyId = "party-1",
            direction = PaymentDirection.OUT,
            amount = 500000L,
            method = PaymentMethod.CASH,
            referenceNo = null,
            note = "Payment to Supplier",
            payDate = now,
            createdAt = now,
            updatedAt = now,
            createdBy = userId,
            updatedBy = userId,
            scope = standalonePaymentScope(PaymentDirection.OUT)
        )

        db.withTransaction {
            db.paymentDao().insertPayment(paymentEntity)
            db.syncDao().enqueueOutbox(
                SyncOutboxEntity(
                    id = UUID.randomUUID().toString(),
                    entityType = "payments",
                    entityId = paymentEntity.id,
                    op = SyncOp.UPSERT,
                    payloadJson = com.google.gson.Gson().toJson(paymentEntity),
                    createdAt = now
                )
            )
        }

        val pendingOutbox = db.syncDao().getPendingOutbox()
        val outboxItem = pendingOutbox.find { it.entityType == "payments" }
        assertNotNull(outboxItem)

        val payment = db.paymentDao().getAllPayments(shopId).firstOrNull()
        assertNotNull(payment)
        assertEquals(Scope.VAULT, payment?.scope)
        assertEquals("Payment to Supplier", payment?.note)
        assertNull(payment?.referenceNo)
    }

    @Test
    fun testFeatureAccessRepositoryAndLocking() {
        val repo = com.mmushtaq04.buysell.data.repository.FeatureAccessRepositoryImpl(context, db)

        // Initially locked for default locked features
        assertTrue(repo.isFeatureLocked(com.mmushtaq04.buysell.data.repository.PremiumFeature.OWNER_DASHBOARD))
        assertTrue(repo.isFeatureLocked(com.mmushtaq04.buysell.data.repository.PremiumFeature.DATA_EXPORT))

        // Test local unlock simulation
        repo.unlockLocallyForTesting(context)

        assertFalse(repo.isFeatureLocked(com.mmushtaq04.buysell.data.repository.PremiumFeature.OWNER_DASHBOARD))
        assertFalse(repo.isFeatureLocked(com.mmushtaq04.buysell.data.repository.PremiumFeature.DATA_EXPORT))
        assertTrue(repo.isPremiumUnlocked.value)
    }

    @Test
    fun testValidateAllStringFormatSpecifiers() {
        val rootDir = java.io.File("src/main/res")
        if (!rootDir.exists()) {
            System.err.println("Skipping testValidateAllStringFormatSpecifiers in non-standard test directory")
            return
        }

        val valueFolders = rootDir.listFiles { file -> file.isDirectory && file.name.startsWith("values") }
        assertNotNull("No values folders found in res", valueFolders)
        assertTrue("Expected values folders in res", valueFolders!!.isNotEmpty())

        val badPattern = java.util.regex.Pattern.compile("%(\\d+)(?!\\$[a-zA-Z])")
        val errors = mutableListOf<String>()

        for (folder in valueFolders) {
            val stringsFile = java.io.File(folder, "strings.xml")
            if (!stringsFile.exists()) continue

            try {
                val factory = javax.xml.parsers.DocumentBuilderFactory.newInstance()
                val builder = factory.newDocumentBuilder()
                val doc = builder.parse(stringsFile)
                val stringNodes = doc.getElementsByTagName("string")

                for (i in 0 until stringNodes.length) {
                    val node = stringNodes.item(i)
                    val keyName = node.attributes.getNamedItem("name")?.nodeValue ?: "unknown"
                    val textContent = node.textContent ?: ""

                    val matcher = badPattern.matcher(textContent)
                    if (matcher.find()) {
                        errors.add("${folder.name}/strings.xml -> key '$keyName': \"$textContent\"")
                    }
                }
            } catch (e: Exception) {
                fail("Failed to parse ${folder.name}/strings.xml: ${e.message}")
            }
        }

        if (errors.isNotEmpty()) {
            fail("Found ${errors.size} invalid format specifier(s) without type conversion specifiers (\$s, \$d, etc.):\n" + errors.joinToString("\n"))
        }
    }
}
