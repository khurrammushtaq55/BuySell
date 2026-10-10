package com.mmushtaq04.buysell.presentation.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mmushtaq04.buysell.R
import com.mmushtaq04.buysell.data.local.entity.CategoryEntity
import com.mmushtaq04.buysell.presentation.screens.settings.components.*
import com.mmushtaq04.buysell.ui.theme.BuySellTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    userRole: String = "Owner",
    ownerName: String = "Malik / Staff",
    shopName: String = "Mera Buy/Sell Store",
    shopPhone: String = "",
    shopAddress: String = "",
    allCategories: List<CategoryEntity> = emptyList(),
    currencySymbol: String = "Rs",
    isExportLocked: Boolean = false,
    isPartnerInviteLocked: Boolean = false,
    isMonthlyReportLocked: Boolean = false,
    onUnlockClick: () -> Unit = {},
    onUpdateCurrency: (symbol: String, code: String) -> Unit = { _, _ -> },
    onUpdateShopProfile: (ownerName: String, shopName: String, phone: String, address: String) -> Unit = { _, _, _, _ -> },
    onToggleCategory: (CategoryEntity) -> Unit = {},
    onGenerateInvite: (role: String, onCodeGenerated: (String) -> Unit) -> Unit = { _, _ -> },
    onNavigateBack: () -> Unit = {},
    onNavigateToHelp: () -> Unit = {},
    onAttemptSignOut: (onRequireWarning: (Int) -> Unit, onReadyToSignOut: () -> Unit) -> Unit = { _, _ -> },
    onForceSignOut: (onReadyToSignOut: () -> Unit) -> Unit = { _ -> }
) {
    val isOwner = userRole.equals("Owner", ignoreCase = true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToHelp) {
                        Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "Madad / Help")
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
            // CARD 1: Store Profile Section
            StoreProfileSection(
                isOwner = isOwner,
                ownerName = ownerName,
                shopName = shopName,
                shopPhone = shopPhone,
                shopAddress = shopAddress,
                onUpdateShopProfile = onUpdateShopProfile
            )

            // CARD 2: Language & Preferences Section
            LanguageAndPreferencesSection(
                currencySymbol = currencySymbol,
                isMonthlyReportLocked = isMonthlyReportLocked,
                onUnlockClick = onUnlockClick,
                onUpdateCurrency = onUpdateCurrency,
                onNavigateToHelp = onNavigateToHelp
            )

            // CARD 3: Team Management & Categories Section
            TeamAndCategoriesSection(
                isOwner = isOwner,
                allCategories = allCategories,
                isPartnerInviteLocked = isPartnerInviteLocked,
                onUnlockClick = onUnlockClick,
                onToggleCategory = onToggleCategory,
                onGenerateInvite = onGenerateInvite
            )

            // CARD 4: Security & Account Section
            SecurityAndAccountSection(
                isOwner = isOwner,
                userRole = userRole,
                isExportLocked = isExportLocked,
                onUnlockClick = onUnlockClick,
                onAttemptSignOut = onAttemptSignOut,
                onForceSignOut = onForceSignOut
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Version Footer
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.settings_footer_version, stringResource(R.string.app_name), userRole),
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    BuySellTheme {
        SettingsScreen()
    }
}
