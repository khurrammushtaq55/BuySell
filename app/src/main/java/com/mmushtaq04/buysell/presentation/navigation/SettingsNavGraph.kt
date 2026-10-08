package com.mmushtaq04.buysell.presentation.navigation

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.mmushtaq04.buysell.data.auth.FirebaseAuthManager
import com.mmushtaq04.buysell.data.local.entity.CategoryEntity
import com.mmushtaq04.buysell.presentation.screens.settings.SettingsScreen
import com.mmushtaq04.buysell.presentation.screens.settings.SettingsViewModel
import com.mmushtaq04.buysell.presentation.viewmodel.CategoryViewModel

fun NavGraphBuilder.settingsNavGraph(
    navController: NavHostController,
    authManager: FirebaseAuthManager,
    registeredUserName: String,
    registeredUserRole: String,
    allCategories: List<CategoryEntity>,
    categoryViewModel: CategoryViewModel
) {
    composable(NavRoutes.Settings.route) {
        val settingsViewModel: SettingsViewModel = viewModel()
        val shopEntity by settingsViewModel.primaryShop.collectAsState()
        val categories by settingsViewModel.categories.collectAsState()

        SettingsScreen(
            userRole = registeredUserRole,
            ownerName = registeredUserName,
            shopName = shopEntity?.name ?: "Mera Buy/Sell Store",
            shopPhone = shopEntity?.phone ?: "",
            shopAddress = shopEntity?.address ?: "",
            allCategories = categories,
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
