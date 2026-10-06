package com.mmushtaq04.buysell

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mmushtaq04.buysell.data.local.AppDatabase
import com.mmushtaq04.buysell.data.local.entity.TxnEntity
import com.mmushtaq04.buysell.data.local.enums.*
import com.mmushtaq04.buysell.data.repository.*
import com.mmushtaq04.buysell.domain.model.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var stockRepo: StockRepositoryImpl
    private lateinit var partyRepo: PartyRepositoryImpl
    private lateinit var paymentRepo: PaymentRepositoryImpl

    private val shopId = "test-shop-1"
    private val userId = "user-owner"

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        stockRepo = StockRepositoryImpl(db)
        partyRepo = PartyRepositoryImpl(db)
        paymentRepo = PaymentRepositoryImpl(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testBuyAndSellUniqueItem() = runBlocking {
        // 1. Create a party (Supplier/Customer)
        val party = Party(
            id = "party-1",
            shopId = shopId,
            name = "Ali Ahmed",
            phone = "03001234567",
            cnic = "3520112345671"
        )
        partyRepo.createParty(party)

        // 2. Buy UNIQUE Phone (e.g. iPhone 15, IMEI 123456789012345, PKR 150,000)
        val itemToBuy = StockItem(
            id = "item-1",
            shopId = shopId,
            categoryId = "cat-mobile",
            brand = "Apple",
            model = "iPhone 15",
            identifier = "123456789012345",
            quantity = 1,
            remainingQty = 1
        )

        val purchaseTxn = stockRepo.recordPurchase(
            stockItem = itemToBuy,
            purchasePrice = 15000000L, // PKR 150,000.00
            partyId = party.id,
            createdByUserId = userId
        )

        assertNotNull(purchaseTxn)
        assertEquals(TxnType.PURCHASE, purchaseTxn.type)

        // Verify stock item is IN_STOCK
        val boughtItem = stockRepo.getStockItemById("item-1")
        assertNotNull(boughtItem)
        assertEquals(ItemStatus.IN_STOCK, boughtItem?.status)
        assertEquals(1, boughtItem?.remainingQty)

        // 3. Sell UNIQUE Phone for PKR 170,000.00
        val saleTxn = stockRepo.recordSale(
            stockItemId = "item-1",
            salePrice = 17000000L,
            partyId = party.id,
            createdByUserId = userId
        )

        assertNotNull(saleTxn)
        assertEquals(TxnType.SALE, saleTxn.type)

        // Verify stock item is now SOLD with remainingQty = 0
        val soldItem = stockRepo.getStockItemById("item-1")
        assertNotNull(soldItem)
        assertEquals(ItemStatus.SOLD, soldItem?.status)
        assertEquals(0, soldItem?.remainingQty)

        // 4. Check Party Balance
        // Net balance: SALE (+170,000) - PURCHASE (-150,000) = +20,000 (Party owes shop 20,000)
        val balanceDto = partyRepo.getPartyBalance(shopId, party.id)
        assertNotNull(balanceDto)
        assertEquals(2000000L, balanceDto?.balance)
    }

    @Test
    fun testQuantityLotsFifo() = runBlocking {
        // Create 2 lots of chargers
        val lot1 = StockItem(
            id = "lot-1",
            shopId = shopId,
            categoryId = "cat-acc",
            brand = "Anker",
            model = "20W Charger",
            quantity = 10,
            remainingQty = 10,
            stockedAt = 1000L
        )

        val lot2 = StockItem(
            id = "lot-2",
            shopId = shopId,
            categoryId = "cat-acc",
            brand = "Anker",
            model = "20W Charger",
            quantity = 5,
            remainingQty = 5,
            stockedAt = 2000L
        )

        stockRepo.recordPurchase(lot1, 50000L, "supplier-1", userId)
        stockRepo.recordPurchase(lot2, 30000L, "supplier-1", userId)

        // Verify FIFO order: lot1 (stockedAt 1000) comes before lot2 (stockedAt 2000)
        val fifoLots = stockRepo.findAvailableLotsFifo(shopId, "cat-acc", "Anker", "20W Charger")
        assertEquals(2, fifoLots.size)
        assertEquals("lot-1", fifoLots[0].id)
        assertEquals("lot-2", fifoLots[1].id)

        // Sell 3 units from lot1
        stockRepo.recordSale("lot-1", 100000L, "cust-1", userId, qtyToSell = 3)

        val updatedLot1 = stockRepo.getStockItemById("lot-1")
        assertEquals(7, updatedLot1?.remainingQty)
        assertEquals(ItemStatus.IN_STOCK, updatedLot1?.status)
    }

    @Test
    fun testSaleReturnRefundStaffBalance() = runBlocking {
        // Create customer party
        val party = Party(
            id = "cust-100",
            shopId = shopId,
            name = "Tariq Mahmood"
        )
        partyRepo.createParty(party)

        // 1. Customer buys phone for PKR 100,000 (SALE)
        val itemToBuy = StockItem(
            id = "item-100",
            shopId = shopId,
            categoryId = "cat-mobile",
            brand = "Samsung",
            model = "Galaxy A55",
            quantity = 1
        )
        stockRepo.recordPurchase(itemToBuy, 8000000L, "sup-1", userId)
        val saleTxn = stockRepo.recordSale("item-100", 10000000L, party.id, userId)

        // Customer pays PKR 100,000 (Payment IN, PUBLIC)
        paymentRepo.recordPayment(Payment(
            id = "pay-in-1",
            shopId = shopId,
            txnId = saleTxn.id,
            partyId = party.id,
            direction = PaymentDirection.IN,
            amount = 10000000L,
            scope = Scope.PUBLIC
        ))

        // Balance should be 0
        var publicBal = partyRepo.getPublicPartyBalance(shopId, party.id)
        assertEquals(0L, publicBal?.balance)

        // 2. Customer returns device (SALE_RETURN)
        val returnTxnEntity = TxnEntity(
            id = "txn-return-1",
            shopId = shopId,
            type = TxnType.SALE_RETURN,
            partyId = party.id,
            txnDate = System.currentTimeMillis(),
            totalAmount = 10000000L,
            scope = Scope.PUBLIC,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            createdBy = userId,
            updatedBy = userId
        )
        db.txnDao().insertTxn(returnTxnEntity)

        // Shop refunds customer PKR 100,000 (Payment OUT with scope = PUBLIC because linked to SALE_RETURN)
        paymentRepo.recordPayment(Payment(
            id = "pay-out-1",
            shopId = shopId,
            txnId = "txn-return-1",
            partyId = party.id,
            direction = PaymentDirection.OUT,
            amount = 10000000L,
            scope = Scope.PUBLIC
        ))

        // Staff-visible public party balance must equal 0
        publicBal = partyRepo.getPublicPartyBalance(shopId, party.id)
        assertNotNull(publicBal)
        assertEquals(0L, publicBal?.balance)
    }
}
