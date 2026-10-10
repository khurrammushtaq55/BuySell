package com.mmushtaq04.buysell.presentation.navigation

import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.mmushtaq04.buysell.presentation.screens.dashboard.OwnerDashboardScreen
import com.mmushtaq04.buysell.presentation.screens.dashboard.OwnerDashboardViewModel
import com.mmushtaq04.buysell.presentation.screens.expense.ExpenseListScreen
import com.mmushtaq04.buysell.presentation.screens.expense.ExpenseViewModel
import com.mmushtaq04.buysell.presentation.screens.help.HelpScreen
import com.mmushtaq04.buysell.presentation.screens.home.HomeScreen
import com.mmushtaq04.buysell.presentation.screens.home.HomeViewModel
import com.mmushtaq04.buysell.presentation.screens.party.PartyLedgerScreen
import com.mmushtaq04.buysell.presentation.screens.party.PartyLedgerViewModel
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
            onSelectParty = { partyId ->
                navController.navigate(NavRoutes.PartyLedger.createRoute(partyId))
            },
            onAddParty = { name, phone, cnic ->
                partyListViewModel.addNewParty(name, phone, cnic)
            }
        )
    }

    composable(
        route = NavRoutes.PartyLedger.route,
        arguments = listOf(navArgument("partyId") { type = NavType.StringType })
    ) { backStackEntry ->
        val partyId = backStackEntry.arguments?.getString("partyId") ?: ""
        val partyLedgerViewModel: PartyLedgerViewModel = viewModel()

        LaunchedEffect(partyId) {
            partyLedgerViewModel.loadLedger(partyId)
        }

        val ledgerUiState by partyLedgerViewModel.uiState.collectAsState()

        PartyLedgerScreen(
            uiState = ledgerUiState,
            onNavigateBack = { navController.popBackStack() },
            onRecordPayment = { amountRs, methodStr, note, direction ->
                partyLedgerViewModel.recordWasooliPayment(partyId, amountRs, methodStr, note, direction)
            }
        )
    }

    composable(NavRoutes.OwnerDashboard.route) {
        val context = androidx.compose.ui.platform.LocalContext.current
        val scope = rememberCoroutineScope()
        val db = com.mmushtaq04.buysell.data.local.AppDatabase.getInstance(context)
        val featureRepo = remember { com.mmushtaq04.buysell.data.repository.FeatureAccessRepositoryImpl(context, db) }

        val ownerDashboardViewModel: OwnerDashboardViewModel = viewModel()
        val dashboardState by ownerDashboardViewModel.uiState.collectAsState()

        var showPaywallDialog by remember { mutableStateOf(false) }

        if (showPaywallDialog) {
            com.mmushtaq04.buysell.presentation.components.PremiumPaywallDialog(
                onDismiss = { showPaywallDialog = false },
                onUpgradeClick = { _ ->
                    featureRepo.unlockLocallyForTesting(context)
                    showPaywallDialog = false
                },
                onRestoreClick = {
                    featureRepo.unlockLocallyForTesting(context)
                    showPaywallDialog = false
                },
                onRedeemPromoCode = { code, onSuccess, onError ->
                    scope.launch {
                        val user = db.userDao().getPrimaryUser()
                        val success = featureRepo.redeemAdminPromoCode(code, user?.shopId ?: "")
                        if (success) onSuccess() else onError()
                    }
                }
            )
        }

        OwnerDashboardScreen(
            uiState = dashboardState,
            isDashboardLocked = featureRepo.isFeatureLocked(com.mmushtaq04.buysell.data.repository.PremiumFeature.OWNER_DASHBOARD),
            onUnlockClick = { showPaywallDialog = true },
            onTimeRangeSelect = { range ->
                ownerDashboardViewModel.setTimeRange(range)
            },
            onNavigateToExpenses = { navController.navigate(NavRoutes.Expenses.route) },
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable(NavRoutes.Expenses.route) {
        val expenseViewModel: ExpenseViewModel = viewModel()
        val expenseUiState by expenseViewModel.uiState.collectAsState()

        ExpenseListScreen(
            expenses = expenseUiState.expenses,
            totalExpenseRs = expenseUiState.totalExpenseRs,
            selectedCategory = expenseUiState.selectedCategoryFilter,
            onCategoryFilterSelect = { cat -> expenseViewModel.setCategoryFilter(cat) },
            onAddExpense = { amountRs, category, note ->
                expenseViewModel.addExpense(amountRs, category, note)
            },
            onDeleteExpense = { id -> expenseViewModel.deleteExpense(id) },
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable(NavRoutes.Help.route) {
        HelpScreen(
            onNavigateBack = { navController.popBackStack() }
        )
    }
}
