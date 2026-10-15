package com.mmushtaq04.buysell

import com.mmushtaq04.buysell.data.local.dao.PartyBalanceDto
import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardReceivablesPayablesTest {

    @Test
    fun testReceivablesAndPayablesSumEqualPartyBalances() {
        val partyBalances = listOf(
            PartyBalanceDto("p1", "Ali", "03001234567", null, 500000L),  // +5000 Rs (Receivable)
            PartyBalanceDto("p2", "Usman", "03007654321", null, 300000L), // +3000 Rs (Receivable)
            PartyBalanceDto("p3", "Supplier A", "03119876543", null, -400000L), // -4000 Rs (Payable)
            PartyBalanceDto("p4", "Settled Party", null, null, 0L)
        )

        val totalReceivablesPaisa = partyBalances.filter { it.balance > 0 }.sumOf { it.balance }
        val totalPayablesPaisa = partyBalances.filter { it.balance < 0 }.sumOf { kotlin.math.abs(it.balance) }

        assertEquals(800000L, totalReceivablesPaisa) // Rs 8000
        assertEquals(400000L, totalPayablesPaisa)    // Rs 4000
    }

    @Test
    fun testTopDebtorsOrder() {
        val partyBalances = listOf(
            PartyBalanceDto("p1", "Ali", "03001234567", null, 500000L),
            PartyBalanceDto("p2", "Usman", "03007654321", null, 1200000L),
            PartyBalanceDto("p3", "Bilal", "03211112223", null, 300000L)
        )

        val topDebtors = partyBalances.filter { it.balance > 0 }.sortedByDescending { it.balance }

        assertEquals("Usman", topDebtors[0].name)
        assertEquals(1200000L, topDebtors[0].balance)
        assertEquals("Ali", topDebtors[1].name)
        assertEquals(500000L, topDebtors[1].balance)
        assertEquals("Bilal", topDebtors[2].name)
        assertEquals(300000L, topDebtors[2].balance)
    }
}
