package com.mmushtaq04.buysell.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mmushtaq04.buysell.data.auth.FirebaseAuthManager
import com.mmushtaq04.buysell.presentation.screens.auth.LoginScreen
import com.mmushtaq04.buysell.presentation.screens.buy.BuyWizardScreen
import com.mmushtaq04.buysell.presentation.screens.dashboard.OwnerDashboardScreen
import com.mmushtaq04.buysell.presentation.screens.help.HelpScreen
import com.mmushtaq04.buysell.presentation.screens.home.HomeScreen
import com.mmushtaq04.buysell.presentation.screens.lock.AppLockScreen
import com.mmushtaq04.buysell.presentation.screens.onboarding.ShopSetupScreen
import com.mmushtaq04.buysell.presentation.screens.party.PartyListScreen
import com.mmushtaq04.buysell.presentation.screens.sell.SellWizardScreen
import com.mmushtaq04.buysell.presentation.screens.settings.SettingsScreen
import com.mmushtaq04.buysell.presentation.screens.stock.StockListScreen
import com.mmushtaq04.buysell.util.AppPinManager

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val authManager = remember { FirebaseAuthManager() }
    val isUserLoggedIn = authManager.isUserLoggedIn
    val isPinSet = AppPinManager.isPinSet(context)

    val startDest = when {
        !isUserLoggedIn -> NavRoutes.Login.route
        isPinSet -> NavRoutes.AppLock.route
        else -> NavRoutes.Home.route
    }

    NavHost(
        navController = navController,
        startDestination = startDest
    ) {
        composable(NavRoutes.Login.route) {
            LoginScreen(
                onGoogleSignInClick = {
                    navController.navigate(NavRoutes.ShopSetup.route) {
                        popUpTo(NavRoutes.Login.route) { inclusive = true }
                    }
                },
                onEmailAuthSuccess = {
                    navController.navigate(NavRoutes.ShopSetup.route) {
                        popUpTo(NavRoutes.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.ShopSetup.route) {
            ShopSetupScreen(
                onShopCreated = {
                    navController.navigate(NavRoutes.Home.route) {
                        popUpTo(NavRoutes.ShopSetup.route) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.AppLock.route) {
            AppLockScreen(
                onUnlockSuccess = {
                    navController.navigate(NavRoutes.Home.route) {
                        popUpTo(NavRoutes.AppLock.route) { inclusive = true }
                    }
                }
            )
        }

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
                onNavigateBack = { navController.popBackStack() },
                onSignOutClick = {
                    authManager.signOut()
                    navController.navigate(NavRoutes.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
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
