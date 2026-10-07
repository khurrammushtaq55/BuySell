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

        SettingsScreen(
            userRole = registeredUserRole,
            shopName = shopEntity?.name ?: "Mera Buy/Sell Store",
            shopPhone = shopEntity?.phone ?: "",
            shopAddress = shopEntity?.address ?: "",
            allCategories = allCategories,
            onUpdateShopProfile = { newName, newPhone, newAddress ->
                settingsViewModel.updateShopProfile(
                    newName = newName,
                    newPhone = newPhone,
                    newAddress = newAddress,
                    updatedByUserName = registeredUserName
                )
            },
            onToggleCategory = { category ->
                categoryViewModel.toggleCategoryEnabled(category)
            },
            onNavigateBack = { navController.popBackStack() },
            onNavigateToHelp = { navController.navigate(NavRoutes.Help.route) },
            onSignOutClick = {
                authManager.signOut()
                navController.navigate(NavRoutes.Login.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        )
    }
}
