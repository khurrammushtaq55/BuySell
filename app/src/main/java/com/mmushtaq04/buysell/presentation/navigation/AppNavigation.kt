package com.mmushtaq04.buysell.presentation.navigation

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.mmushtaq04.buysell.R
import com.mmushtaq04.buysell.data.auth.FirebaseAuthManager
import com.mmushtaq04.buysell.data.local.AppDatabase
import com.mmushtaq04.buysell.data.local.enums.Role
import com.mmushtaq04.buysell.data.sync.FirestoreSyncManager
import com.mmushtaq04.buysell.presentation.screens.splash.AppSplashScreen
import com.mmushtaq04.buysell.presentation.viewmodel.CategoryViewModel
import com.mmushtaq04.buysell.util.AppPinManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    val msgRecords = stringResource(R.string.restoration_msg_records)
    val msgSearching = stringResource(R.string.restoration_msg_searching)
    val msgParties = stringResource(R.string.restoration_msg_parties)
    val msgFinalizing = stringResource(R.string.restoration_msg_finalizing)

    var isRestoringData by remember { mutableStateOf(false) }
    var restorationMessage by remember { mutableStateOf(msgRecords) }

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

    var initialStartDestination by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val startTime = System.currentTimeMillis()
        val targetRoute = withContext(Dispatchers.IO) {
            if (!isUserLoggedIn) {
                NavRoutes.Welcome.route
            } else {
                val userId = authManager.currentUser?.uid
                val localUser = db.userDao().getPrimaryUser()

                // If logged-in user changed on device, wipe stale local Room DB state
                if (localUser != null && userId != null && localUser.id != userId) {
                    Log.i("AppNavigation", "New user login detected ($userId vs local ${localUser.id}). Clearing local DB.")
                    db.clearAllTables()
                }

                val meta = db.appMetaDao().getAppMeta()
                val user = db.userDao().getPrimaryUser()
                var activeShopId = meta?.activeShopId ?: user?.shopId

                if (activeShopId.isNullOrBlank() && userId != null) {
                    val restoredId = FirestoreSyncManager(db).restoreUserDataFromFirestore(userId).getOrDefault("")
                    if (restoredId.isNotBlank()) {
                        activeShopId = restoredId
                    }
                }

                when {
                    isPinSet -> NavRoutes.AppLock.route
                    activeShopId.isNullOrBlank() -> NavRoutes.ShopSetup.route
                    else -> NavRoutes.Home.route
                }
            }
        }

        val elapsedTime = System.currentTimeMillis() - startTime
        val minSplashDurationMs = 1000L
        if (elapsedTime < minSplashDurationMs) {
            delay(minSplashDurationMs - elapsedTime)
        }

        initialStartDestination = targetRoute
    }

    fun navigateAfterLogin() {
        val userId = authManager.currentUser?.uid
        scope.launch {
            isRestoringData = true
            restorationMessage = msgSearching

            val localUser = db.userDao().getPrimaryUser()
            if (localUser != null && localUser.id != userId) {
                Log.i("AppNavigation", "New user login detected ($userId vs local ${localUser.id}). Clearing stale local Room DB tables.")
                db.clearAllTables()
            }

            val meta = db.appMetaDao().getAppMeta()
            var activeShopId = meta?.activeShopId

            if (activeShopId.isNullOrBlank() && userId != null) {
                restorationMessage = msgSearching
                val restoredId = FirestoreSyncManager(db).restoreUserDataFromFirestore(userId).getOrDefault("")
                if (restoredId.isNotBlank()) {
                    activeShopId = restoredId
                }
            }

            if (!activeShopId.isNullOrBlank()) {
                restorationMessage = msgFinalizing
                val user = db.userDao().getPrimaryUser()
                val role = user?.role ?: Role.STAFF
                FirestoreSyncManager(db).pullChanges(activeShopId, role)

                isRestoringData = false
                navController.navigate(NavRoutes.Home.route) {
                    popUpTo(NavRoutes.Welcome.route) { inclusive = true }
                }
            } else {
                isRestoringData = false
                navController.navigate(NavRoutes.ShopSetup.route) {
                    popUpTo(NavRoutes.Welcome.route) { inclusive = true }
                }
            }
        }
    }

    if (initialStartDestination == null) {
        AppSplashScreen()
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = initialStartDestination!!
            ) {
                authNavGraph(
                    navController = navController,
                    onNavigateAfterLogin = { navigateAfterLogin() },
                    onNavigateAfterSignUp = {
                        navController.navigate(NavRoutes.ShopSetup.route) {
                            popUpTo(NavRoutes.Welcome.route) { inclusive = true }
                        }
                    }
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
}

@Composable
private fun DataRestorationOverlayScreen(message: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val iconScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.background
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
            .padding(32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                shadowElevation = 12.dp,
                modifier = Modifier
                    .size(108.dp)
                    .scale(iconScale)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_background),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = stringResource(R.string.restoration_overlay_title),
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.3.sp,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.restoration_overlay_sub),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f),
                tonalElevation = 2.dp,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.5.dp,
                        strokeCap = StrokeCap.Round
                    )

                    Text(
                        text = message,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
