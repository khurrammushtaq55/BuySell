package com.mmushtaq04.buysell.presentation.screens.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.app.Activity
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.mmushtaq04.buysell.R
import com.mmushtaq04.buysell.presentation.components.InAppRatingDialog
import com.mmushtaq04.buysell.ui.theme.BuySellTheme
import com.mmushtaq04.buysell.util.RatingManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    shopName: String = "Mera Buy/Sell Store",
    userRole: String = "Owner",
    todaySalesCount: Int = 0,
    todaySalesAmountPaisa: Long = 0L,
    onNavigateToBuy: () -> Unit = {},
    onNavigateToSell: () -> Unit = {},
    onNavigateToExchange: () -> Unit = {},
    onNavigateToStock: () -> Unit = {},
    onNavigateToParties: () -> Unit = {},
    onNavigateToDashboard: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToHelp: () -> Unit = {}
) {
    val context = LocalContext.current
    val isPartner = userRole.equals("Partner", ignoreCase = true)

    var showRatingDialog by remember {
        mutableStateOf(RatingManager.shouldShowRatingPrompt(context))
    }

    if (showRatingDialog) {
        InAppRatingDialog(
            onRateClick = { ratingStars, neverShowAgain ->
                showRatingDialog = false
                RatingManager.resetInteractionCount(context)
                if (neverShowAgain) {
                    RatingManager.setNeverShowAgain(context, true)
                }
                Toast.makeText(context, context.getString(R.string.rating_toast_thank_you), Toast.LENGTH_SHORT).show()
                val activity = context as? Activity
                if (activity != null) {
                    RatingManager.launchGoogleInAppReview(activity)
                } else {
                    RatingManager.openPlayStorePage(context)
                }
            },
            onDismiss = { neverShowAgain ->
                showRatingDialog = false
                RatingManager.resetInteractionCount(context)
                if (neverShowAgain) {
                    RatingManager.setNeverShowAgain(context, true)
                }
            }
        )
    }

    // Android 13+ (API 33+) Runtime Notification Permission Request
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                Log.i("HomeScreen", "POST_NOTIFICATIONS permission granted ✓")
            } else {
                Log.w("HomeScreen", "POST_NOTIFICATIONS permission denied by user")
            }
        }

        LaunchedEffect(Unit) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = shopName, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            text = stringResource(R.string.home_role_cloud_sync, userRole),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToHelp) {
                        Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "Madad / Help")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isPartner) {
                // Partner Banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(stringResource(R.string.home_partner_banner_title), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(stringResource(R.string.home_partner_banner_sub), fontSize = 12.sp)
                        }
                    }
                }
            }

            // Today Summary Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { if (isPartner || userRole.equals("Owner", ignoreCase = true)) onNavigateToDashboard() },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = stringResource(R.string.home_today_sales), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val formattedAmount = com.mmushtaq04.buysell.util.CurrencyFormatter.formatPaisa(context, todaySalesAmountPaisa)
                        val zeroFormatted = com.mmushtaq04.buysell.util.CurrencyFormatter.formatAmount(context, 0L)
                        Text(
                            text = if (todaySalesCount > 0) "$todaySalesCount sales • $formattedAmount" else zeroFormatted,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (todaySalesCount > 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = if (todaySalesCount > 0) Color(0xFF2E7D32) else Color.Gray,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // Prominent Dashboard Card for Owner and Partner
            if (isPartner || userRole.equals("Owner", ignoreCase = true)) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToDashboard() },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Assessment,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.dashboard_title),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Munafa, sales analytics, aur stock reports dekhein",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            Text(
                text = if (isPartner) stringResource(R.string.home_view_only_sections) else stringResource(R.string.home_what_to_do),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            // Primary 2x2 Grid Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ActionCard(
                    title = stringResource(R.string.action_sell),
                    subtitle = if (isPartner) "Read-Only" else "Farokht",
                    icon = Icons.Default.Sell,
                    backgroundColor = if (isPartner) Color.LightGray.copy(alpha = 0.3f) else Color(0xFFE8F5E9),
                    contentColor = if (isPartner) Color.Gray else Color(0xFF2E7D32),
                    modifier = Modifier.weight(1f),
                    onClick = { if (!isPartner) onNavigateToSell() }
                )
                ActionCard(
                    title = stringResource(R.string.action_buy),
                    subtitle = if (isPartner) "Read-Only" else "Khareedari",
                    icon = Icons.Default.ShoppingCart,
                    backgroundColor = if (isPartner) Color.LightGray.copy(alpha = 0.3f) else Color(0xFFE3F2FD),
                    contentColor = if (isPartner) Color.Gray else Color(0xFF1565C0),
                    modifier = Modifier.weight(1f),
                    onClick = { if (!isPartner) onNavigateToBuy() }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ActionCard(
                    title = stringResource(R.string.action_khata),
                    subtitle = "Baqi paisay",
                    icon = Icons.Default.MenuBook,
                    backgroundColor = Color(0xFFFFF8E1),
                    contentColor = Color(0xFFF57F17),
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToParties
                )
                ActionCard(
                    title = stringResource(R.string.action_stock),
                    subtitle = "Available phones",
                    icon = Icons.Default.Inventory2,
                    backgroundColor = Color(0xFFF3E5F5),
                    contentColor = Color(0xFF7B1FA2),
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToStock
                )
            }

            HorizontalDivider()

            // Exchange Option
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { if (!isPartner) onNavigateToExchange() },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isPartner) Color.LightGray.copy(alpha = 0.3f) else MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = if (isPartner) Color.Gray else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.action_exchange),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = if (isPartner) Color.Gray else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = if (isPartner) "Partner mode — entry disabled" else "Customer purana phone de kar naya le raha hai",
                            fontSize = 12.sp,
                            color = if (isPartner) Color.Gray else MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    backgroundColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(130.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(36.dp)
            )
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = contentColor
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = contentColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    BuySellTheme {
        HomeScreen(
            userRole = "Partner",
            todaySalesCount = 3,
            todaySalesAmountPaisa = 12500000L
        )
    }
}
