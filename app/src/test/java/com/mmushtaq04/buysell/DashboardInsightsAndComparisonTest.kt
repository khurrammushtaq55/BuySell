package com.mmushtaq04.buysell

import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardInsightsAndComparisonTest {

    @Test
    fun testMarginAndAverageCalculations() {
        val netRevenuePaisa = 1000000L // Rs 10,000
        val netProfitPaisa = 250000L   // Rs 2,500
        val saleTxnCount = 5
        val totalUnitsSold = 10

        val marginPercent = (netProfitPaisa.toFloat() / netRevenuePaisa.toFloat()) * 100f
        val avgSaleValueRs = (netRevenuePaisa / saleTxnCount) / 100
        val avgProfitPerUnitRs = (netProfitPaisa / 100) / totalUnitsSold

        assertEquals(25.0f, marginPercent, 0.01f)
        assertEquals(2000L, avgSaleValueRs) // Rs 2,000 per sale
        assertEquals(250L, avgProfitPerUnitRs) // Rs 250 per unit
    }

    @Test
    fun testZeroDivideGuardsForMarginsAndAverages() {
        val netRevenuePaisa = 0L
        val netProfitPaisa = 0L
        val saleTxnCount = 0
        val totalUnitsSold = 0

        val marginPercent = if (netRevenuePaisa > 0) (netProfitPaisa.toFloat() / netRevenuePaisa.toFloat()) * 100f else 0f
        val avgSaleValueRs = if (saleTxnCount > 0) (netRevenuePaisa / saleTxnCount) / 100 else 0L
        val avgProfitPerUnitRs = if (totalUnitsSold > 0) (netProfitPaisa / 100) / totalUnitsSold else 0L

        assertEquals(0f, marginPercent, 0.01f)
        assertEquals(0L, avgSaleValueRs)
        assertEquals(0L, avgProfitPerUnitRs)
    }

    @Test
    fun testPeriodComparisonPercentageChange() {
        val currentRevenuePaisa = 1500000L // Rs 15,000
        val previousRevenuePaisa = 1000000L // Rs 10,000

        val changePercent = ((currentRevenuePaisa.toFloat() - previousRevenuePaisa.toFloat()) / previousRevenuePaisa.toFloat()) * 100f

        assertEquals(50.0f, changePercent, 0.01f) // +50%
    }

    @Test
    fun testAgedStockBucketClassification() {
        val nowMs = 1000000000000L
        val oneDayMs = 24 * 60 * 60 * 1000L

        val stocked10DaysAgo = nowMs - (10 * oneDayMs)
        val stocked45DaysAgo = nowMs - (45 * oneDayMs)
        val stocked75DaysAgo = nowMs - (75 * oneDayMs)
        val stocked120DaysAgo = nowMs - (120 * oneDayMs)

        fun getBucket(stockedAt: Long): String {
            val ageDays = ((nowMs - stockedAt) / oneDayMs).toInt()
            return when {
                ageDays <= 30 -> "0-30"
                ageDays in 31..60 -> "31-60"
                ageDays in 61..90 -> "61-90"
                else -> "90+"
            }
        }

        assertEquals("0-30", getBucket(stocked10DaysAgo))
        assertEquals("31-60", getBucket(stocked45DaysAgo))
        assertEquals("61-90", getBucket(stocked75DaysAgo))
        assertEquals("90+", getBucket(stocked120DaysAgo))
    }
}
