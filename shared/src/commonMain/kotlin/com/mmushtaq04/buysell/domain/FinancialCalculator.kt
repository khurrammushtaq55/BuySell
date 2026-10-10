package com.mmushtaq04.buysell.domain

data class FinancialSummary(
    val grossRevenueRs: Long = 0L,
    val netRevenueRs: Long = 0L,
    val grossCogsRs: Long = 0L,
    val netCogsRs: Long = 0L,
    val totalExpensesRs: Long = 0L,
    val returnsRefundsRs: Long = 0L,
    val netProfitRs: Long = 0L,
    val missingCostCount: Int = 0
)

object FinancialCalculator {

    fun calculateNetProfit(
        grossRevenueRs: Long,
        returnsRefundsRs: Long,
        grossCogsRs: Long,
        returnedCogsRs: Long,
        totalExpensesRs: Long
    ): FinancialSummary {
        val netRevenueRs = grossRevenueRs - returnsRefundsRs
        val netCogsRs = grossCogsRs - returnedCogsRs
        val netProfitRs = netRevenueRs - netCogsRs - totalExpensesRs

        return FinancialSummary(
            grossRevenueRs = grossRevenueRs,
            netRevenueRs = netRevenueRs,
            grossCogsRs = grossCogsRs,
            netCogsRs = netCogsRs,
            totalExpensesRs = totalExpensesRs,
            returnsRefundsRs = returnsRefundsRs,
            netProfitRs = netProfitRs
        )
    }
}
