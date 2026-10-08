package com.mmushtaq04.buysell.presentation.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.mmushtaq04.buysell.data.auth.FirebaseAuthManager
import com.mmushtaq04.buysell.data.local.AppDatabase
import com.mmushtaq04.buysell.data.local.enums.Role
import com.mmushtaq04.buysell.data.sync.FirestoreSyncManager
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

    var isRestoringData by remember { mutableStateOf(false) }
    var restorationMessage by remember { mutableStateOf("Restoring shop records...") }

    val primaryUserFlow = remember { db.userDao().observePrimaryUser() }
    val primaryUser by primaryUserFlow.collectAsState(initial = null)

    val firebaseUser = authManager.currentUser
    val registeredUserName = remember(primaryUser, firebaseUser) {
        when {
            !primaryUser?.displayName.isNullOrBlank() -> primaryUser!!.displayName
            !firebaseUser?.displayName.isNullOrBlank() -> firebaseUser.displayName!!
            !firebaseUser?.email.isNullOrBlank() -> firebaseUser.email!!.substringBefore("@")
            else -> "Malik / Staff"
        }
    }

    val registeredUserRole = remember(primaryUser) {
        primaryUser?.role?.name ?: "STAFF"
    }

    val categoryViewModel: CategoryViewModel = viewModel()
    val allCategories by categoryViewModel.allCategories.collectAsState()

    LaunchedEffect(isUserLoggedIn) {
        if (isUserLoggedIn) {
            val userId = authManager.currentUser?.uid ?: return@LaunchedEffect
            val meta = db.appMetaDao().getAppMeta()
            if (meta?.activeShopId.isNullOrBlank()) {
                isRestoringData = true
                restorationMessage = "Downloading shop stock & ledger records..."
                FirestoreSyncManager(db).restoreUserDataFromFirestore(userId)
                isRestoringData = false
            }
        }
    }

    val startDest = when {
        !isUserLoggedIn -> NavRoutes.Login.route
        isPinSet -> NavRoutes.AppLock.route
        else -> NavRoutes.Home.route
    }

    fun navigateAfterLogin() {
        val userId = authManager.currentUser?.uid
        scope.launch {
            isRestoringData = true
            restorationMessage = "Searching existing shop records..."

            val meta = db.appMetaDao().getAppMeta()
            var activeShopId = meta?.activeShopId

            if (activeShopId.isNullOrBlank() && userId != null) {
                restorationMessage = "Downloading stock items, transactions & parties..."
                val restoredId = FirestoreSyncManager(db).restoreUserDataFromFirestore(userId).getOrDefault("")
                if (restoredId.isNotBlank()) {
                    activeShopId = restoredId
                }
            }

            if (!activeShopId.isNullOrBlank()) {
                restorationMessage = "Finalizing store setup..."
                val user = db.userDao().getPrimaryUser()
                val role = user?.role ?: Role.STAFF
                FirestoreSyncManager(db).pullChanges(activeShopId, role)

                isRestoringData = false
                navController.navigate(NavRoutes.Home.route) {
                    popUpTo(NavRoutes.Login.route) { inclusive = true }
                }
            } else {
                isRestoringData = false
                navController.navigate(NavRoutes.ShopSetup.route) {
                    popUpTo(NavRoutes.Login.route) { inclusive = true }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                registeredUserName = registeredUserName
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

        AnimatedVisibility(
            visible = isRestoringData,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            DataRestorationOverlayScreen(message = restorationMessage)
        }
    }
}

@Composable
private fun DataRestorationOverlayScreen(message: String) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(96.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(52.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Restoring Your Store & Stock...",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 4.dp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Please wait a moment while your devices and ledgers are synchronized.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center
            )
        }
    }
}
