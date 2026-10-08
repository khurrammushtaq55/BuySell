package com.mmushtaq04.buysell.presentation.navigation

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.mmushtaq04.buysell.presentation.screens.auth.LoginScreen
import com.mmushtaq04.buysell.presentation.screens.auth.WelcomeLandingScreen
import com.mmushtaq04.buysell.presentation.screens.lock.AppLockScreen
import com.mmushtaq04.buysell.presentation.screens.onboarding.ShopSetupScreen
import com.mmushtaq04.buysell.presentation.screens.onboarding.ShopSetupViewModel

fun NavGraphBuilder.authNavGraph(
    navController: NavHostController,
    onNavigateAfterLogin: () -> Unit
) {
    composable(NavRoutes.Welcome.route) {
        WelcomeLandingScreen(
            onNavigateToRegister = {
                navController.navigate(NavRoutes.Login.createRoute(isRegister = true))
            },
            onNavigateToLogin = {
                navController.navigate(NavRoutes.Login.createRoute(isRegister = false))
            },
            onGoogleSignInClick = onNavigateAfterLogin
        )
    }

    composable(
        route = NavRoutes.Login.route,
        arguments = listOf(
            navArgument("isRegister") {
                type = NavType.BoolType
                defaultValue = false
            }
        )
    ) { backStackEntry ->
        val isRegister = backStackEntry.arguments?.getBoolean("isRegister") ?: false

        LoginScreen(
            initialRegisterMode = isRegister,
            onNavigateBackToWelcome = {
                navController.popBackStack()
            },
            onGoogleSignInClick = onNavigateAfterLogin,
            onEmailAuthSuccess = onNavigateAfterLogin
        )
    }

    composable(NavRoutes.ShopSetup.route) {
        val setupViewModel: ShopSetupViewModel = viewModel()

        ShopSetupScreen(
            onNavigateToHelp = {
                navController.navigate(NavRoutes.Help.route)
            },
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
            },
            onJoinWithInvite = { inviteCode, userName, onError ->
                setupViewModel.joinShopWithInvite(
                    inviteCode = inviteCode,
                    userName = userName,
                    onSuccess = {
                        navController.navigate(NavRoutes.Home.route) {
                            popUpTo(NavRoutes.ShopSetup.route) { inclusive = true }
                        }
                    },
                    onError = onError
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
