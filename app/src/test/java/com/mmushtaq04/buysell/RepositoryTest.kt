package com.mmushtaq04.buysell

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mmushtaq04.buysell.data.local.AppDatabase
import com.mmushtaq04.buysell.data.local.enums.ItemStatus
import com.mmushtaq04.buysell.data.local.enums.TxnType
import com.mmushtaq04.buysell.data.repository.PartyRepositoryImpl
import com.mmushtaq04.buysell.data.repository.StockRepositoryImpl
import com.mmushtaq04.buysell.domain.model.Party
import com.mmushtaq04.buysell.domain.model.StockItem
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var stockRepo: StockRepositoryImpl
    private lateinit var partyRepo: PartyRepositoryImpl

    private val shopId = "shop-test"
    private val userId = "user-1"

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        stockRepo = StockRepositoryImpl(db)
        partyRepo = PartyRepositoryImpl(db)
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
        val balanceDto = partyRepo.getPartyBalance(shopId, party.id)
        assertNotNull(balanceDto)
        assertEquals(17000000L, balanceDto?.balance)
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

        stockRepo.recordPurchase(lot1, 100000L, "supplier-1", userId)
        stockRepo.recordPurchase(lot2, 50000L, "supplier-1", userId)

        // Verify FIFO query picks lot1 first
        val fifoLots = stockRepo.findAvailableLotsFifo(shopId, "cat-acc", "Anker", "20W Charger")
        assertEquals(2, fifoLots.size)
        assertEquals("lot-1", fifoLots[0].id)
        assertEquals("lot-2", fifoLots[1].id)
    }
}
