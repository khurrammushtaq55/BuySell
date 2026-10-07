package com.mmushtaq04.buysell.presentation.navigation

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.mmushtaq04.buysell.data.auth.FirebaseAuthManager
import com.mmushtaq04.buysell.data.local.AppDatabase
import com.mmushtaq04.buysell.presentation.viewmodel.CategoryViewModel
import com.mmushtaq04.buysell.util.AppPinManager
import kotlinx.coroutines.launch

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getInstance(context) }
    val authManager = remember { FirebaseAuthManager() }
    val isUserLoggedIn = authManager.isUserLoggedIn
    val isPinSet = AppPinManager.isPinSet(context)

    var registeredUserName by remember {
        mutableStateOf(authManager.currentUser?.displayName ?: "Malik / Staff")
    }
    var registeredUserRole by remember { mutableStateOf("Owner") }

    val categoryViewModel: CategoryViewModel = viewModel()
    val enabledCategories by categoryViewModel.enabledCategories.collectAsState()
    val allCategories by categoryViewModel.allCategories.collectAsState()

    val startDest = when {
        !isUserLoggedIn -> NavRoutes.Login.route
        isPinSet -> NavRoutes.AppLock.route
        else -> NavRoutes.Home.route
    }

    fun navigateAfterLogin() {
        authManager.currentUser?.displayName?.let { name ->
            if (name.isNotBlank()) registeredUserName = name
        }
        scope.launch {
            val meta = db.appMetaDao().getAppMeta()
            if (meta?.activeShopId != null) {
                navController.navigate(NavRoutes.Home.route) {
                    popUpTo(NavRoutes.Login.route) { inclusive = true }
                }
            } else {
                navController.navigate(NavRoutes.ShopSetup.route) {
                    popUpTo(NavRoutes.Login.route) { inclusive = true }
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDest
    ) {
        authNavGraph(
            navController = navController,
            onNavigateAfterLogin = { navigateAfterLogin() }
        )

        mainNavGraph(
            navController = navController,
            registeredUserRole = registeredUserRole
        )

        wizardsNavGraph(
            navController = navController,
            registeredUserName = registeredUserName,
            enabledCategories = enabledCategories
        )

        settingsNavGraph(
            navController = navController,
            authManager = authManager,
            registeredUserName = registeredUserName,
            registeredUserRole = registeredUserRole,
            allCategories = allCategories,
            categoryViewModel = categoryViewModel
        )
    }
}
