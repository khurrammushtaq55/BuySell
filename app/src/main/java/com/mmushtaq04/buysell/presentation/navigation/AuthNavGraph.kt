package com.mmushtaq04.buysell.presentation.navigation

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.mmushtaq04.buysell.presentation.screens.auth.LoginScreen
import com.mmushtaq04.buysell.presentation.screens.lock.AppLockScreen
import com.mmushtaq04.buysell.presentation.screens.onboarding.ShopSetupScreen
import com.mmushtaq04.buysell.presentation.screens.onboarding.ShopSetupViewModel

fun NavGraphBuilder.authNavGraph(
    navController: NavHostController,
    onNavigateAfterLogin: () -> Unit
) {
    composable(NavRoutes.Login.route) {
        LoginScreen(
            onGoogleSignInClick = onNavigateAfterLogin,
            onEmailAuthSuccess = onNavigateAfterLogin
        )
    }

    composable(NavRoutes.ShopSetup.route) {
        val setupViewModel: ShopSetupViewModel = viewModel()

        ShopSetupScreen(
            onShopCreated = { name, role, shopName, shopPhone, shopAddress, selectedCategories ->
                setupViewModel.createShop(
                    name = name,
                    role = role,
                    shopName = shopName,
                    shopPhone = shopPhone,
                    shopAddress = shopAddress,
                    selectedCategories = selectedCategories.toList(),
                    onSuccess = {
                        navController.navigate(NavRoutes.Home.route) {
                            popUpTo(NavRoutes.ShopSetup.route) { inclusive = true }
                        }
                    }
                )
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
}
