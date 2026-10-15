package com.mmushtaq04.buysell

import com.mmushtaq04.buysell.domain.FinancialCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardCorrectnessTest {

    @Test
    fun testFinancialCalculatorCalculatesNetProfitAccurately() {
        // Gross Revenue: Rs 5000 (500000 paisa)
        // Returns/Refunds: Rs 500 (50000 paisa)
        // Gross COGS: Rs 3000 (300000 paisa)
        // Returned COGS: Rs 300 (30000 paisa)
        // Total Expenses: Rs 800 (80000 paisa)

        val summary = FinancialCalculator.calculateNetProfit(
            grossRevenueRs = 5000L,
            returnsRefundsRs = 500L,
            grossCogsRs = 3000L,
            returnedCogsRs = 300L,
            totalExpensesRs = 800L
        )

        // Net Revenue = 5000 - 500 = 4500
        assertEquals(4500L, summary.netRevenueRs)
        // Net COGS = 3000 - 300 = 2700
        assertEquals(2700L, summary.netCogsRs)
        // Net Profit = 4500 - 2700 - 800 = 1000
        assertEquals(1000L, summary.netProfitRs)
    }

    @Test
    fun testNegativeProfitCalculatedWithoutClamping() {
        // Revenue: Rs 1000, COGS: Rs 1500, Expenses: Rs 500 -> Net Profit = -1000
        val summary = FinancialCalculator.calculateNetProfit(
            grossRevenueRs = 1000L,
            returnsRefundsRs = 0L,
            grossCogsRs = 1500L,
            returnedCogsRs = 0L,
            totalExpensesRs = 500L
        )

        assertEquals(1000L, summary.netRevenueRs)
        assertEquals(1500L, summary.netCogsRs)
        assertEquals(-1000L, summary.netProfitRs)
    }

    @Test
    fun testChunkingVariableLimitsHelper() {
        val largeTxnList = (1..2500).map { "txn_id_$it" }
        val chunks = largeTxnList.chunked(800)

        assertEquals(4, chunks.size)
        assertEquals(800, chunks[0].size)
        assertEquals(800, chunks[1].size)
        assertEquals(800, chunks[2].size)
        assertEquals(100, chunks[3].size)
        assertEquals(2500, chunks.sumOf { it.size })
    }
}
