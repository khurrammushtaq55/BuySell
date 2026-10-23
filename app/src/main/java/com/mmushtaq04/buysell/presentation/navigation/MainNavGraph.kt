package com.mmushtaq04.buysell.presentation.navigation

import com.mmushtaq04.buysell.R
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

        LaunchedEffect(Unit) {
            homeViewModel.loadHomeData()
        }

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

        val billingManager = remember { com.mmushtaq04.buysell.domain.billing.PlayBillingManager.getInstance(context) }
        val productDetailsMap by billingManager.productDetailsMap.collectAsState()
        val isBillingReady by billingManager.isBillingReady.collectAsState()
        val billingEventMsg by billingManager.billingEventMessage.collectAsState()

        LaunchedEffect(billingEventMsg) {
            billingEventMsg?.let { msg ->
                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                billingManager.clearEventMessage()
            }
        }

        var userEntity by remember { mutableStateOf<com.mmushtaq04.buysell.data.local.entity.UserEntity?>(null) }
        LaunchedEffect(Unit) {
            val user = db.userDao().getPrimaryUser()
            userEntity = user
            if (user != null) {
                billingManager.initializeSession(featureRepo, user.shopId, user.id)
            }
        }

        var showPaywallDialog by remember { mutableStateOf(false) }

        LaunchedEffect(showPaywallDialog) {
            if (showPaywallDialog) {
                billingManager.queryAvailableProducts()
            }
        }

        if (showPaywallDialog) {
            com.mmushtaq04.buysell.presentation.components.PremiumPaywallDialog(
                isOwner = userEntity?.role == com.mmushtaq04.buysell.data.local.enums.Role.OWNER,
                productDetailsMap = productDetailsMap,
                isBillingReady = isBillingReady,
                onDismiss = { showPaywallDialog = false },
                onUpgradeClick = { selectedProductId ->
                    val activity = context as? android.app.Activity
                    if (activity != null && userEntity != null) {
                        val details = productDetailsMap[selectedProductId]
                        if (details == null) {
                            android.widget.Toast.makeText(
                                context,
                                context.getString(R.string.billing_product_not_active, selectedProductId),
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        } else {
                            billingManager.launchBillingFlow(
                                activity = activity,
                                productId = selectedProductId,
                                shopId = userEntity!!.shopId,
                                userUid = userEntity!!.id
                            )
                            showPaywallDialog = false
                        }
                    }
                },
                onRestoreClick = {
                    if (userEntity != null) {
                        billingManager.restorePurchases(userEntity!!.shopId, featureRepo) { isRestored ->
                            val msgRes = if (isRestored) R.string.billing_restore_success else R.string.billing_restore_empty
                            android.widget.Toast.makeText(context, context.getString(msgRes), android.widget.Toast.LENGTH_LONG).show()
                        }
                    }
                    showPaywallDialog = false
                },
                onRedeemPromoCode = { code, onSuccess, onError ->
                    scope.launch {
                        val user = userEntity ?: db.userDao().getPrimaryUser()
                        val res = featureRepo.redeemAdminPromoCodeResult(code, user?.shopId ?: "")
                        res.onSuccess { onSuccess() }
                           .onFailure { err -> onError(err.localizedMessage ?: "Invalid promo code.") }
                    }
                }
            )
        }

        OwnerDashboardScreen(
            viewModel = ownerDashboardViewModel,
            isAdvancedDashboardLocked = featureRepo.isFeatureLocked(com.mmushtaq04.buysell.data.repository.PremiumFeature.DASHBOARD_ADVANCED),
            isInventoryInsightsLocked = featureRepo.isFeatureLocked(com.mmushtaq04.buysell.data.repository.PremiumFeature.INVENTORY_INSIGHTS),
            onUnlockClick = { showPaywallDialog = true },
            onNavigateToExpenses = { navController.navigate(NavRoutes.Expenses.route) },
            onNavigateToPartyList = { navController.navigate(NavRoutes.PartyList.route) },
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
