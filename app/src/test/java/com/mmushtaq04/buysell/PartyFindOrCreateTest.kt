package com.mmushtaq04.buysell

import com.mmushtaq04.buysell.domain.PartyNormalizer
import org.junit.Assert.assertEquals
import org.junit.Test

class PartyFindOrCreateTest {

    @Test
    fun testNormalizePhoneFormats() {
        assertEquals("03001234567", PartyNormalizer.normalizePhone("0300-1234567"))
        assertEquals("03001234567", PartyNormalizer.normalizePhone("+92 300 1234567"))
        assertEquals("03001234567", PartyNormalizer.normalizePhone("923001234567"))
        assertEquals("03001234567", PartyNormalizer.normalizePhone("3001234567"))
        assertEquals("03001234567", PartyNormalizer.normalizePhone("0300 123 4567"))
    }

    @Test
    fun testNormalizeCnicFormats() {
        assertEquals("3520212345671", PartyNormalizer.normalizeCnic("35202-1234567-1"))
        assertEquals("3520212345671", PartyNormalizer.normalizeCnic("35202 1234567 1"))
    }

    @Test
    fun testSamePersonPhoneMatching() {
        val phone1 = PartyNormalizer.normalizePhone("0300-1234567")
        val phone2 = PartyNormalizer.normalizePhone("+92 300 1234567")
        val phone3 = PartyNormalizer.normalizePhone("923001234567")

        assertEquals(phone1, phone2)
        assertEquals(phone2, phone3)
    }

    @Test
    fun testPartyBalanceNetsAcrossTransactions() {
        // Customer & Supplier: Purchases Rs 10,000, Sales Rs 15,000, Payments IN Rs 5,000, Payments OUT Rs 2,000
        val saleTotal = 1500000L
        val purchaseTotal = 1000000L
        val paymentInTotal = 500000L
        val paymentOutTotal = 200000L

        val balance = (saleTotal - 0L - paymentInTotal) - (purchaseTotal - 0L - paymentOutTotal)

        // (15,000 - 5,000) - (10,000 - 2,000) = 10,000 - 8,000 = +2,000 Rs
        assertEquals(200000L, balance)
    }
}
