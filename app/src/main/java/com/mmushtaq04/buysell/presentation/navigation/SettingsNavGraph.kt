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
import com.mmushtaq04.buysell.presentation.components.PremiumPaywallDialog
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

        val settingsViewModel: SettingsViewModel = viewModel()
        val shopEntity by settingsViewModel.primaryShop.collectAsState()
        val categories by settingsViewModel.categories.collectAsState()
        val currencySymbol by settingsViewModel.currencySymbol.collectAsState()

        LaunchedEffect(shopEntity?.id) {
            shopEntity?.id?.let { featureRepo.syncEntitlementAndConfig(it) }
        }

        if (showPaywallDialog) {
            PremiumPaywallDialog(
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
            isExportLocked = featureRepo.isFeatureLocked(PremiumFeature.DATA_EXPORT),
            isPartnerInviteLocked = featureRepo.isFeatureLocked(PremiumFeature.SLEEPING_PARTNER_INVITE),
            isMonthlyReportLocked = featureRepo.isFeatureLocked(PremiumFeature.MONTHLY_SUMMARY_REPORTS),
            onUnlockClick = { showPaywallDialog = true },
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
