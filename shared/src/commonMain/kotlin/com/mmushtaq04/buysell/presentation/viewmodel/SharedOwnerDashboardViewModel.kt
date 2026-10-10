package com.mmushtaq04.buysell.presentation.viewmodel

import com.mmushtaq04.buysell.domain.FinancialCalculator
import com.mmushtaq04.buysell.domain.FinancialSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SharedDashboardUiState(
    val selectedTimeRange: String = "TODAY",
    val financialSummary: FinancialSummary = FinancialSummary(),
    val isLoading: Boolean = false
)

class SharedOwnerDashboardViewModel {

    private val _uiState = MutableStateFlow(SharedDashboardUiState())
    val uiState: StateFlow<SharedDashboardUiState> = _uiState.asStateFlow()

    fun updateMetrics(
        grossRevenueRs: Long,
        returnsRefundsRs: Long,
        grossCogsRs: Long,
        returnedCogsRs: Long,
        totalExpensesRs: Long
    ) {
        val summary = FinancialCalculator.calculateNetProfit(
            grossRevenueRs = grossRevenueRs,
            returnsRefundsRs = returnsRefundsRs,
            grossCogsRs = grossCogsRs,
            returnedCogsRs = returnedCogsRs,
            totalExpensesRs = totalExpensesRs
        )
        _uiState.value = _uiState.value.copy(
            financialSummary = summary,
            isLoading = false
        )
    }

    fun setTimeRange(range: String) {
        _uiState.value = _uiState.value.copy(selectedTimeRange = range)
    }
}
