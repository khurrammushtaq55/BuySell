package com.mmushtaq04.buysell.presentation.navigation

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mmushtaq04.buysell.data.auth.FirebaseAuthManager
import com.mmushtaq04.buysell.presentation.screens.auth.LoginScreen
import com.mmushtaq04.buysell.presentation.screens.buy.BuyViewModel
import com.mmushtaq04.buysell.presentation.screens.buy.BuyWizardScreen
import com.mmushtaq04.buysell.presentation.screens.dashboard.OwnerDashboardScreen
import com.mmushtaq04.buysell.presentation.screens.dashboard.OwnerDashboardViewModel
import com.mmushtaq04.buysell.presentation.screens.exchange.ExchangeViewModel
import com.mmushtaq04.buysell.presentation.screens.exchange.ExchangeWizardScreen
import com.mmushtaq04.buysell.presentation.screens.help.HelpScreen
import com.mmushtaq04.buysell.presentation.screens.home.HomeScreen
import com.mmushtaq04.buysell.presentation.screens.home.HomeViewModel
import com.mmushtaq04.buysell.presentation.screens.lock.AppLockScreen
import com.mmushtaq04.buysell.presentation.screens.onboarding.ShopSetupScreen
import com.mmushtaq04.buysell.presentation.screens.party.PartyListScreen
import com.mmushtaq04.buysell.presentation.screens.party.PartyListViewModel
import com.mmushtaq04.buysell.presentation.screens.sell.SellViewModel
import com.mmushtaq04.buysell.presentation.screens.sell.SellWizardScreen
import com.mmushtaq04.buysell.presentation.screens.settings.SettingsScreen
import com.mmushtaq04.buysell.presentation.screens.stock.StockListScreen
import com.mmushtaq04.buysell.presentation.screens.stock.StockListViewModel
import com.mmushtaq04.buysell.presentation.viewmodel.CategoryViewModel
import com.mmushtaq04.buysell.util.AppPinManager

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val authManager = remember { FirebaseAuthManager() }
    val isUserLoggedIn = authManager.isUserLoggedIn
    val isPinSet = AppPinManager.isPinSet(context)

    var registeredUserName by remember {
        mutableStateOf(authManager.currentUser?.displayName ?: "Malik / Staff")
    }

    val categoryViewModel: CategoryViewModel = viewModel()
    val enabledCategories by categoryViewModel.enabledCategories.collectAsState()
    val allCategories by categoryViewModel.allCategories.collectAsState()

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
                    authManager.currentUser?.displayName?.let { name ->
                        if (name.isNotBlank()) registeredUserName = name
                    }
                    navController.navigate(NavRoutes.ShopSetup.route) {
                        popUpTo(NavRoutes.Login.route) { inclusive = true }
                    }
                },
                onEmailAuthSuccess = {
                    authManager.currentUser?.displayName?.let { name ->
                        if (name.isNotBlank()) registeredUserName = name
                    }
                    navController.navigate(NavRoutes.ShopSetup.route) {
                        popUpTo(NavRoutes.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.ShopSetup.route) {
            ShopSetupScreen(
                onShopCreated = { name ->
                    if (name.isNotBlank()) {
                        registeredUserName = name
                    }
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
            val homeViewModel: HomeViewModel = viewModel()
            val homeUiState by homeViewModel.uiState.collectAsState()

            HomeScreen(
                shopName = homeUiState.shopName,
                userRole = homeUiState.userRole,
                todaySalesCount = homeUiState.todaySalesCount,
                todaySalesAmountPaisa = homeUiState.todaySalesAmountPaisa,
                onNavigateToBuy = { navController.navigate(NavRoutes.BuyWizard.route) },
                onNavigateToSell = { navController.navigate(NavRoutes.SellWizard.route) },
                onNavigateToExchange = { navController.navigate(NavRoutes.ExchangeWizard.route) },
                onNavigateToStock = { navController.navigate(NavRoutes.StockList.route) },
                onNavigateToParties = { navController.navigate(NavRoutes.PartyList.route) },
                onNavigateToSettings = { navController.navigate(NavRoutes.Settings.route) }
            )
        }

        composable(NavRoutes.BuyWizard.route) {
            val buyViewModel: BuyViewModel = viewModel()

            BuyWizardScreen(
                currentUserName = registeredUserName,
                enabledCategories = enabledCategories.map { it.name },
                onNavigateBack = { navController.popBackStack() },
                onSavePurchase = { cat, brand, model, imei, color, issue, price, name, phone, cnic, recordedBy, paid, method, details, promised ->
                    buyViewModel.savePurchase(
                        categoryName = cat,
                        brand = brand,
                        model = model,
                        imei = imei,
                        color = color,
                        issue = issue,
                        priceRs = price,
                        sellerName = name,
                        sellerPhone = phone,
                        sellerCnic = cnic,
                        recordedBy = recordedBy,
                        paidAmountRs = paid,
                        paymentMethodStr = method,
                        paymentDetails = details,
                        promisedDateStr = promised,
                        onSuccess = {}
                    )
                },
                onSaveSuccess = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.SellWizard.route) {
            val sellViewModel: SellViewModel = viewModel()
            val stockList by sellViewModel.stockList.collectAsState()

            SellWizardScreen(
                stockList = stockList,
                currentUserName = registeredUserName,
                onNavigateBack = { navController.popBackStack() },
                onSaveSale = { itemId, price, name, phone, recordedBy, received, method, details, promised ->
                    sellViewModel.saveSale(
                        stockItemId = itemId,
                        salePriceRs = price,
                        buyerName = name,
                        buyerPhone = phone,
                        recordedBy = recordedBy,
                        receivedAmountRs = received,
                        paymentMethodStr = method,
                        paymentDetails = details,
                        promisedDateStr = promised,
                        onSuccess = {}
                    )
                },
                onSaveSuccess = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.ExchangeWizard.route) {
            val exchangeViewModel: ExchangeViewModel = viewModel()
            val stockList by exchangeViewModel.stockList.collectAsState()

            ExchangeWizardScreen(
                stockList = stockList,
                currentUserName = registeredUserName,
                onNavigateBack = { navController.popBackStack() },
                onSaveExchange = { soldItemId, newPrice, oldCat, oldBrand, oldModel, oldImei, oldColor, oldIssue, oldPrice, name, phone, recordedBy, cash, method ->
                    exchangeViewModel.saveExchange(
                        soldStockItemId = soldItemId,
                        newPhonePriceRs = newPrice,
                        oldCategory = oldCat,
                        oldBrand = oldBrand,
                        oldModel = oldModel,
                        oldImei = oldImei,
                        oldColor = oldColor,
                        oldIssue = oldIssue,
                        oldPhoneValueRs = oldPrice,
                        customerName = name,
                        customerPhone = phone,
                        recordedBy = recordedBy,
                        cashPaidRs = cash,
                        paymentMethodStr = method,
                        onSuccess = {}
                    )
                },
                onSaveSuccess = { navController.popBackStack() }
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

        composable(NavRoutes.Settings.route) {
            SettingsScreen(
                allCategories = allCategories,
                onToggleCategory = { category ->
                    categoryViewModel.toggleCategoryEnabled(category)
                },
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
}
