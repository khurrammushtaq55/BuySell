package com.mmushtaq04.buysell.presentation.navigation

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.mmushtaq04.buysell.presentation.screens.dashboard.OwnerDashboardScreen
import com.mmushtaq04.buysell.presentation.screens.dashboard.OwnerDashboardViewModel
import com.mmushtaq04.buysell.presentation.screens.help.HelpScreen
import com.mmushtaq04.buysell.presentation.screens.home.HomeScreen
import com.mmushtaq04.buysell.presentation.screens.home.HomeViewModel
import com.mmushtaq04.buysell.presentation.screens.party.PartyListScreen
import com.mmushtaq04.buysell.presentation.screens.party.PartyListViewModel
import com.mmushtaq04.buysell.presentation.screens.stock.StockListScreen
import com.mmushtaq04.buysell.presentation.screens.stock.StockListViewModel

fun NavGraphBuilder.mainNavGraph(
    navController: NavHostController,
    registeredUserRole: String
) {
    composable(NavRoutes.Home.route) {
        val homeViewModel: HomeViewModel = viewModel()
        val homeUiState by homeViewModel.uiState.collectAsState()

        HomeScreen(
            shopName = homeUiState.shopName,
            userRole = registeredUserRole,
            todaySalesCount = homeUiState.todaySalesCount,
            todaySalesAmountPaisa = homeUiState.todaySalesAmountPaisa,
            onNavigateToBuy = { navController.navigate(NavRoutes.BuyWizard.route) },
            onNavigateToSell = { navController.navigate(NavRoutes.SellWizard.route) },
            onNavigateToExchange = { navController.navigate(NavRoutes.ExchangeWizard.route) },
            onNavigateToStock = { navController.navigate(NavRoutes.StockList.route) },
            onNavigateToParties = { navController.navigate(NavRoutes.PartyList.route) },
            onNavigateToDashboard = { navController.navigate(NavRoutes.OwnerDashboard.route) },
            onNavigateToSettings = { navController.navigate(NavRoutes.Settings.route) },
            onNavigateToHelp = { navController.navigate(NavRoutes.Help.route) }
        )
    }

    composable(NavRoutes.StockList.route) {
        val stockListViewModel: StockListViewModel = viewModel()
        val stockItems by stockListViewModel.stockItems.collectAsState()

        StockListScreen(
            stockItems = stockItems,
            onNavigateBack = { navController.popBackStack() },
            onSelectItem = { /* Open detail */ }
        )
    }

    composable(NavRoutes.PartyList.route) {
        val partyListViewModel: PartyListViewModel = viewModel()
        val parties by partyListViewModel.parties.collectAsState()

        PartyListScreen(
            parties = parties,
            onNavigateBack = { navController.popBackStack() },
            onSelectParty = { /* Open party ledger */ }
        )
    }

    composable(NavRoutes.OwnerDashboard.route) {
        val ownerDashboardViewModel: OwnerDashboardViewModel = viewModel()
        val dashboardState by ownerDashboardViewModel.uiState.collectAsState()

        OwnerDashboardScreen(
            todaySalesCount = dashboardState.todaySalesCount,
            todaySalesTotalRs = dashboardState.todaySalesTotalRs,
            monthlyNetProfitRs = dashboardState.monthlyNetProfitRs,
            capitalInStockRs = dashboardState.capitalInStockRs,
            slowStockCount = dashboardState.slowStockCount,
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable(NavRoutes.Help.route) {
        HelpScreen(
            onNavigateBack = { navController.popBackStack() }
        )
    }
}
