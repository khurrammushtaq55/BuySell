package com.mmushtaq04.buysell.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mmushtaq04.buysell.presentation.screens.buy.BuyWizardScreen
import com.mmushtaq04.buysell.presentation.screens.dashboard.OwnerDashboardScreen
import com.mmushtaq04.buysell.presentation.screens.help.HelpScreen
import com.mmushtaq04.buysell.presentation.screens.home.HomeScreen
import com.mmushtaq04.buysell.presentation.screens.party.PartyListScreen
import com.mmushtaq04.buysell.presentation.screens.sell.SellWizardScreen
import com.mmushtaq04.buysell.presentation.screens.settings.SettingsScreen
import com.mmushtaq04.buysell.presentation.screens.stock.StockListScreen

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.Home.route
    ) {
        composable(NavRoutes.Home.route) {
            HomeScreen(
                onNavigateToBuy = { navController.navigate(NavRoutes.BuyWizard.route) },
                onNavigateToSell = { navController.navigate(NavRoutes.SellWizard.route) },
                onNavigateToExchange = { navController.navigate(NavRoutes.BuyWizard.route) },
                onNavigateToStock = { navController.navigate(NavRoutes.StockList.route) },
                onNavigateToParties = { navController.navigate(NavRoutes.PartyList.route) },
                onNavigateToSettings = { navController.navigate(NavRoutes.Settings.route) }
            )
        }

        composable(NavRoutes.BuyWizard.route) {
            BuyWizardScreen(
                onNavigateBack = { navController.popBackStack() },
                onSaveSuccess = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.SellWizard.route) {
            SellWizardScreen(
                onNavigateBack = { navController.popBackStack() },
                onSaveSuccess = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.StockList.route) {
            StockListScreen(
                onNavigateBack = { navController.popBackStack() },
                onSelectItem = { /* Open detail */ }
            )
        }

        composable(NavRoutes.PartyList.route) {
            PartyListScreen(
                onNavigateBack = { navController.popBackStack() },
                onSelectParty = { /* Open party ledger */ }
            )
        }

        composable(NavRoutes.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.OwnerDashboard.route) {
            OwnerDashboardScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.Help.route) {
            HelpScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
