package com.mmushtaq04.buysell.presentation.navigation

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.mmushtaq04.buysell.data.auth.FirebaseAuthManager
import com.mmushtaq04.buysell.data.local.AppDatabase
import com.mmushtaq04.buysell.data.local.entity.CategoryEntity
import com.mmushtaq04.buysell.data.repository.FeatureAccessRepositoryImpl
import com.mmushtaq04.buysell.data.repository.PremiumFeature
import com.mmushtaq04.buysell.presentation.components.BuyCoffeeDialog
import com.mmushtaq04.buysell.presentation.components.PremiumPaywallDialog
import com.mmushtaq04.buysell.presentation.components.RedeemPromoDialog
import com.mmushtaq04.buysell.presentation.screens.settings.SettingsScreen
import com.mmushtaq04.buysell.presentation.screens.settings.SettingsViewModel
import com.mmushtaq04.buysell.presentation.viewmodel.CategoryViewModel
import kotlinx.coroutines.launch

fun NavGraphBuilder.settingsNavGraph(
    navController: NavHostController,
    authManager: FirebaseAuthManager,
    registeredUserName: String,
    registeredUserRole: String,
    allCategories: List<CategoryEntity>,
    categoryViewModel: CategoryViewModel
) {
    composable(NavRoutes.Settings.route) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val db = AppDatabase.getInstance(context)

        val featureRepo = remember { FeatureAccessRepositoryImpl(context, db) }
        val isPremiumUnlocked by featureRepo.isPremiumUnlocked.collectAsState()

        var showPaywallDialog by remember { mutableStateOf(false) }
        var showCoffeeDialog by remember { mutableStateOf(false) }
        var showPromoDialog by remember { mutableStateOf(false) }

        val settingsViewModel: SettingsViewModel = viewModel()
        val shopEntity by settingsViewModel.primaryShop.collectAsState()
        val categories by settingsViewModel.categories.collectAsState()
        val currencySymbol by settingsViewModel.currencySymbol.collectAsState()

        LaunchedEffect(shopEntity?.id) {
            shopEntity?.id?.let { featureRepo.syncEntitlementAndConfig(it) }
        }

        val billingManager = remember { com.mmushtaq04.buysell.domain.billing.PlayBillingManager.getInstance(context) }
        val productDetailsMap by billingManager.productDetailsMap.collectAsState()
        val isBillingReady by billingManager.isBillingReady.collectAsState()

        var userEntity by remember { mutableStateOf<com.mmushtaq04.buysell.data.local.entity.UserEntity?>(null) }
        LaunchedEffect(Unit) {
            val db = com.mmushtaq04.buysell.data.local.AppDatabase.getInstance(context)
            val user = db.userDao().getPrimaryUser()
            userEntity = user
            if (user != null) {
                billingManager.initializeSession(featureRepo, user.shopId, user.id)
            }
        }

        if (showPaywallDialog) {
            PremiumPaywallDialog(
                isOwner = userEntity?.role == com.mmushtaq04.buysell.data.local.enums.Role.OWNER,
                productDetailsMap = productDetailsMap,
                isBillingReady = isBillingReady,
                onDismiss = { showPaywallDialog = false },
                onUpgradeClick = { selectedProductId ->
                    val activity = context as? android.app.Activity
                    if (activity != null && userEntity != null) {
                        billingManager.launchBillingFlow(
                            activity = activity,
                            productId = selectedProductId,
                            shopId = userEntity!!.shopId,
                            userUid = userEntity!!.id
                        )
                    }
                    showPaywallDialog = false
                },
                onRestoreClick = {
                    if (userEntity != null) {
                        billingManager.restorePurchases(userEntity!!.shopId, featureRepo)
                    }
                    showPaywallDialog = false
                },
                onRedeemPromoCode = { code, onSuccess, onError ->
                    scope.launch {
                        val success = featureRepo.redeemAdminPromoCode(code, shopEntity?.id ?: "")
                        if (success) {
                            onSuccess()
                        } else {
                            onError()
                        }
                    }
                }
            )
        }

        if (showCoffeeDialog) {
            BuyCoffeeDialog(
                onDismiss = { showCoffeeDialog = false },
                onBuyCoffeeClick = {
                    showCoffeeDialog = false
                }
            )
        }

        if (showPromoDialog) {
            RedeemPromoDialog(
                onDismiss = { showPromoDialog = false },
                onRedeemCode = { code, onSuccess, onError ->
                    scope.launch {
                        val success = featureRepo.redeemAdminPromoCode(code, shopEntity?.id ?: "")
                        if (success) {
                            onSuccess()
                        } else {
                            onError()
                        }
                    }
                }
            )
        }

        SettingsScreen(
            userRole = registeredUserRole,
            ownerName = registeredUserName,
            shopName = shopEntity?.name ?: "Mera Buy/Sell Store",
            shopPhone = shopEntity?.phone ?: "",
            shopAddress = shopEntity?.address ?: "",
            allCategories = categories,
            currencySymbol = currencySymbol,
            isExportLocked = featureRepo.isFeatureLocked(PremiumFeature.REPORTS_EXPORT_PRO),
            isPartnerInviteLocked = featureRepo.isFeatureLocked(PremiumFeature.SLEEPING_PARTNER_INVITE),
            isMonthlyReportLocked = featureRepo.isFeatureLocked(PremiumFeature.MONTHLY_SUMMARY_REPORTS),
            onUnlockClick = { showPaywallDialog = true },
            onBuyCoffeeClick = { showCoffeeDialog = true },
            onRedeemPromoClick = { showPromoDialog = true },
            onUpdateCurrency = { symbol, code ->
                settingsViewModel.updateCurrency(symbol, code)
            },
            onUpdateShopProfile = { newOwnerName, newShopName, newPhone, newAddress ->
                settingsViewModel.updateShopProfile(
                    ownerName = newOwnerName,
                    shopName = newShopName,
                    phone = newPhone,
                    address = newAddress
                )
            },
            onToggleCategory = { category ->
                settingsViewModel.toggleCategory(category)
            },
            onGenerateInvite = { role, onCodeGenerated ->
                settingsViewModel.generateInviteCode(role, onCodeGenerated)
            },
            onNavigateBack = { navController.popBackStack() },
            onNavigateToHelp = { navController.navigate(NavRoutes.Help.route) },
            onAttemptSignOut = { onRequireWarning, _ ->
                settingsViewModel.handleSignOut(
                    authManager = authManager,
                    onRequireUnsyncedWarning = onRequireWarning,
                    onReadyToSignOut = {
                        navController.navigate(NavRoutes.Welcome.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            },
            onForceSignOut = { _ ->
                settingsViewModel.executeForceSignOut(
                    authManager = authManager,
                    onReadyToSignOut = {
                        navController.navigate(NavRoutes.Welcome.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        )
    }
}
