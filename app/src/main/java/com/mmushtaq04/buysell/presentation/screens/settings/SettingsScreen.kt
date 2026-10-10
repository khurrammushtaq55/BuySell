package com.mmushtaq04.buysell.presentation.screens.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Logout
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mmushtaq04.buysell.R
import com.mmushtaq04.buysell.data.local.entity.CategoryEntity
import com.mmushtaq04.buysell.data.sync.DailySummaryWorker
import com.mmushtaq04.buysell.data.sync.MonthlySummaryWorker
import com.mmushtaq04.buysell.ui.theme.BuySellTheme
import com.mmushtaq04.buysell.util.AppPinManager
import com.mmushtaq04.buysell.util.AppPreferencesManager
import com.mmushtaq04.buysell.util.DataExporter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    userRole: String = "Owner",
    ownerName: String = "Malik / Staff",
    shopName: String = "Mera Buy/Sell Store",
    shopPhone: String = "",
    shopAddress: String = "",
    allCategories: List<CategoryEntity> = emptyList(),
    currencySymbol: String = "Rs",
    onUpdateCurrency: (symbol: String, code: String) -> Unit = { _, _ -> },
    onUpdateShopProfile: (ownerName: String, shopName: String, phone: String, address: String) -> Unit = { _, _, _, _ -> },
    onToggleCategory: (CategoryEntity) -> Unit = {},
    onGenerateInvite: (role: String, onCodeGenerated: (String) -> Unit) -> Unit = { _, _ -> },
    onNavigateBack: () -> Unit = {},
    onNavigateToHelp: () -> Unit = {},
    onAttemptSignOut: (onRequireWarning: (Int) -> Unit, onReadyToSignOut: () -> Unit) -> Unit = { _, _ -> },
    onForceSignOut: (onReadyToSignOut: () -> Unit) -> Unit = { _ -> }
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var editableOwnerName by remember(ownerName) { mutableStateOf(ownerName) }
    var editableShopName by remember(shopName) { mutableStateOf(shopName) }
    var editableShopPhone by remember(shopPhone) { mutableStateOf(shopPhone) }
    var editableShopAddress by remember(shopAddress) { mutableStateOf(shopAddress) }
    var isSavingShopProfile by remember { mutableStateOf(false) }

    val languages = AppPreferencesManager.supportedLanguages
    val currentTag = remember { AppPreferencesManager.getAppLanguageTag(context) }
    var selectedLanguageOption by remember {
        mutableStateOf(languages.find { it.tag.equals(currentTag, ignoreCase = true) } ?: languages.first())
    }
    var expandedLanguageDropdown by remember { mutableStateOf(false) }

    // Theme Selection State
    val themeOptions = AppPreferencesManager.supportedThemes
    val currentThemeMode = remember { AppPreferencesManager.getAppThemeMode(context) }
    var selectedThemeOption by remember {
        mutableStateOf(themeOptions.find { it.mode.equals(currentThemeMode, ignoreCase = true) } ?: themeOptions.first())
    }
    var expandedThemeDropdown by remember { mutableStateOf(false) }

    var showBuyCostInSell by remember {
        mutableStateOf(runCatching { AppPreferencesManager.isShowBuyCostInSellEnabled(context) }.getOrDefault(true))
    }

    var showPinDialog by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showUnsyncedWarningDialog by remember { mutableStateOf(false) }
    var unsyncedCount by remember { mutableIntStateOf(0) }
    var showTeamDialog by remember { mutableStateOf(false) }

    var activeInviteCode by remember { mutableStateOf("") }
    var inviteRole by remember { mutableStateOf("STAFF") }
    var isGeneratingCode by remember { mutableStateOf(false) }

    var pinInputText by remember { mutableStateOf("") }
    var isPinActive by remember { mutableStateOf(runCatching { AppPinManager.isPinSet(context) }.getOrDefault(false)) }

    val isOwner = userRole.equals("Owner", ignoreCase = true)

    if (showTeamDialog) {
        val isStaffInvite = inviteRole == "STAFF"
        val dialogTitle = if (isStaffInvite) stringResource(R.string.settings_staff_invite_title) else stringResource(R.string.settings_partner_invite_title)
        val dialogSub = if (isStaffInvite) stringResource(R.string.settings_staff_invite_sub) else stringResource(R.string.settings_partner_invite_sub)
        val iconVector = if (isStaffInvite) Icons.Default.PersonAdd else Icons.Default.Handshake

        AlertDialog(
            onDismissRequest = { showTeamDialog = false },
            icon = { Icon(iconVector, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text(dialogTitle, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(dialogSub, fontSize = 14.sp)

                    if (activeInviteCode.isNotBlank()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(stringResource(R.string.settings_invite_code_label, inviteRole), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = activeInviteCode,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(stringResource(R.string.settings_code_expires_hint), fontSize = 11.sp, color = Color.Gray)
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Invite Code", activeInviteCode)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Code copied!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.settings_btn_copy_code))
                            }

                            Button(
                                onClick = {
                                    val roleLabel = if (isStaffInvite) "Staff Member" else "Sleeping Partner"
                                    val appNameStr = context.getString(R.string.app_name)
                                    val shareMsg = context.getString(R.string.settings_invite_share_msg, appNameStr, roleLabel, activeInviteCode)
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, shareMsg)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share Code"))
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.settings_btn_whatsapp_share))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isGeneratingCode = true
                        onGenerateInvite(inviteRole) { generatedCode ->
                            activeInviteCode = generatedCode
                            isGeneratingCode = false
                        }
                    },
                    enabled = !isGeneratingCode
                ) {
                    Text(if (activeInviteCode.isBlank()) stringResource(R.string.settings_btn_generate_new_code) else stringResource(R.string.settings_btn_regenerate_code))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTeamDialog = false }) {
                    Text(stringResource(R.string.stock_close_dialog))
                }
            }
        )
    }

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text(if (isPinActive) "App PIN Lock Badlein / Khatam Karein" else "4-Digit PIN Set Karein") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.settings_pin_dialog_msg), fontSize = 14.sp)
                    OutlinedTextField(
                        value = pinInputText,
                        onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) pinInputText = it },
                        label = { Text(stringResource(R.string.settings_label_4digit_pin)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinInputText.length == 4) {
                            AppPinManager.savePin(context, pinInputText)
                            isPinActive = true
                            showPinDialog = false
                            pinInputText = ""
                        }
                    },
                    enabled = pinInputText.length == 4
                ) {
                    Text(stringResource(R.string.settings_btn_set_pin))
                }
            },
            dismissButton = {
                if (isPinActive) {
                    TextButton(
                        onClick = {
                            AppPinManager.clearPin(context)
                            isPinActive = false
                            showPinDialog = false
                            pinInputText = ""
                        }
                    ) {
                        Text(stringResource(R.string.settings_btn_remove_pin), color = MaterialTheme.colorScheme.error)
                    }
                } else {
                    TextButton(onClick = { showPinDialog = false }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            }
        )
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text(stringResource(R.string.settings_signout_dialog_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.settings_signout_dialog_msg), fontSize = 15.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutDialog = false
                        onAttemptSignOut(
                            { count ->
                                unsyncedCount = count
                                showUnsyncedWarningDialog = true
                            },
                            {
                                // Clean sign-out completed
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.settings_btn_signout))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showUnsyncedWarningDialog) {
        AlertDialog(
            onDismissRequest = { showUnsyncedWarningDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.settings_unsynced_warning_title),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            text = {
                Text(
                    text = stringResource(R.string.settings_unsynced_warning_msg, unsyncedCount),
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showUnsyncedWarningDialog = false
                        onForceSignOut {
                            // Force sign-out completed
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.settings_btn_discard_signout))
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { showUnsyncedWarningDialog = false }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                    Button(
                        onClick = {
                            showUnsyncedWarningDialog = false
                            onAttemptSignOut(
                                { count ->
                                    unsyncedCount = count
                                    showUnsyncedWarningDialog = true
                                },
                                {
                                    // Clean sign-out completed
                                }
                            )
                        }
                    ) {
                        Text(stringResource(R.string.settings_btn_try_sync))
                    }
                }
            }
        )
    }

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
            SettingsSectionCard(
                title = stringResource(R.string.settings_section_store),
                icon = Icons.Default.Store
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (!isOwner) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = stringResource(R.string.settings_shop_profile_owner_only_notice),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = editableOwnerName,
                        onValueChange = { editableOwnerName = it },
                        label = { Text(stringResource(R.string.label_owner_name)) },
                        readOnly = !isOwner,
                        enabled = isOwner,
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editableShopName,
                        onValueChange = { editableShopName = it },
                        label = { Text(stringResource(R.string.label_shop_name)) },
                        readOnly = !isOwner,
                        enabled = isOwner,
                        leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editableShopPhone,
                        onValueChange = { editableShopPhone = it },
                        label = { Text(stringResource(R.string.label_shop_phone)) },
                        readOnly = !isOwner,
                        enabled = isOwner,
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editableShopAddress,
                        onValueChange = { editableShopAddress = it },
                        label = { Text(stringResource(R.string.label_shop_address)) },
                        readOnly = !isOwner,
                        enabled = isOwner,
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (isOwner) {
                        Button(
                            onClick = {
                                if (editableShopName.isNotBlank() && editableOwnerName.isNotBlank()) {
                                    isSavingShopProfile = true
                                    onUpdateShopProfile(
                                        editableOwnerName.trim(),
                                        editableShopName.trim(),
                                        editableShopPhone.trim(),
                                        editableShopAddress.trim()
                                    )
                                    isSavingShopProfile = false
                                    Toast.makeText(context, "Dukan details updated ✓", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = editableShopName.isNotBlank() && editableOwnerName.isNotBlank() && !isSavingShopProfile,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.action_save), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // CARD 2: Language & Preferences Section
            SettingsSectionCard(
                title = stringResource(R.string.settings_section_lang),
                icon = Icons.Default.Translate
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.settings_app_language), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)

                    Box {
                        OutlinedTextField(
                            value = selectedLanguageOption.displayName,
                            onValueChange = {},
                            label = { Text(stringResource(R.string.settings_label_selected_language)) },
                            modifier = Modifier.fillMaxWidth().clickable { expandedLanguageDropdown = true },
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = { expandedLanguageDropdown = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            }
                        )

                        DropdownMenu(
                            expanded = expandedLanguageDropdown,
                            onDismissRequest = { expandedLanguageDropdown = false }
                        ) {
                            languages.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.displayName) },
                                    onClick = {
                                        selectedLanguageOption = option
                                        expandedLanguageDropdown = false
                                        AppPreferencesManager.setAppLanguageTag(context, option.tag)
                                        Toast.makeText(context, "Language updated: ${option.displayName}", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Text(stringResource(R.string.settings_app_theme), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)

                    Box {
                        OutlinedTextField(
                            value = stringResource(selectedThemeOption.displayNameResId),
                            onValueChange = {},
                            label = { Text(stringResource(R.string.settings_app_theme)) },
                            modifier = Modifier.fillMaxWidth().clickable { expandedThemeDropdown = true },
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = { expandedThemeDropdown = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            }
                        )

                        DropdownMenu(
                            expanded = expandedThemeDropdown,
                            onDismissRequest = { expandedThemeDropdown = false }
                        ) {
                            themeOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(option.displayNameResId)) },
                                    onClick = {
                                        selectedThemeOption = option
                                        expandedThemeDropdown = false
                                        AppPreferencesManager.setAppThemeMode(context, option.mode)
                                    }
                                )
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Text(stringResource(R.string.settings_store_currency), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(stringResource(R.string.settings_store_currency_sub), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    var expandedCurrencyDropdown by remember { mutableStateOf(false) }
                    var showCustomCurrencyDialog by remember { mutableStateOf(false) }
                    var customCurrencyInput by remember { mutableStateOf("") }

                    Box {
                        OutlinedTextField(
                            value = currencySymbol,
                            onValueChange = {},
                            label = { Text(stringResource(R.string.settings_store_currency)) },
                            modifier = Modifier.fillMaxWidth().clickable { expandedCurrencyDropdown = true },
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = { expandedCurrencyDropdown = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            }
                        )

                        DropdownMenu(
                            expanded = expandedCurrencyDropdown,
                            onDismissRequest = { expandedCurrencyDropdown = false }
                        ) {
                            AppPreferencesManager.supportedCurrencies.forEach { preset ->
                                DropdownMenuItem(
                                    text = { Text("${preset.flag} ${preset.name} (${preset.symbol})") },
                                    onClick = {
                                        expandedCurrencyDropdown = false
                                        onUpdateCurrency(preset.symbol, preset.code)
                                    }
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("✍️ Custom Symbol...") },
                                onClick = {
                                    expandedCurrencyDropdown = false
                                    showCustomCurrencyDialog = true
                                }
                            )
                        }
                    }

                    if (showCustomCurrencyDialog) {
                        AlertDialog(
                            onDismissRequest = { showCustomCurrencyDialog = false },
                            title = { Text(stringResource(R.string.settings_custom_currency_dialog_title), fontWeight = FontWeight.Bold) },
                            text = {
                                OutlinedTextField(
                                    value = customCurrencyInput,
                                    onValueChange = { customCurrencyInput = it },
                                    label = { Text(stringResource(R.string.settings_custom_currency_label)) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        if (customCurrencyInput.isNotBlank()) {
                                            onUpdateCurrency(customCurrencyInput.trim(), "CUSTOM")
                                            showCustomCurrencyDialog = false
                                            customCurrencyInput = ""
                                        }
                                    },
                                    enabled = customCurrencyInput.isNotBlank()
                                ) {
                                    Text(stringResource(R.string.action_save))
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showCustomCurrencyDialog = false }) {
                                    Text(stringResource(R.string.action_cancel))
                                }
                            }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val updated = !showBuyCostInSell
                                showBuyCostInSell = updated
                                AppPreferencesManager.setShowBuyCostInSellEnabled(context, updated)
                            },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.settings_show_buy_price), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(stringResource(R.string.settings_show_buy_price_sub), fontSize = 12.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = showBuyCostInSell,
                            onCheckedChange = { updated ->
                                showBuyCostInSell = updated
                                AppPreferencesManager.setShowBuyCostInSellEnabled(context, updated)
                            }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    var isDailySummaryEnabled by remember { mutableStateOf(AppPreferencesManager.isDailySummaryEnabled(context)) }
                    var dailySummaryTime by remember { mutableStateOf(AppPreferencesManager.getDailySummaryTime(context)) }
                    var showTimePickerDialog by remember { mutableStateOf(false) }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val updated = !isDailySummaryEnabled
                                isDailySummaryEnabled = updated
                                AppPreferencesManager.setDailySummaryEnabled(context, updated)
                                DailySummaryWorker.scheduleDailySummary(context)
                            },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.settings_daily_summary_title), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(stringResource(R.string.settings_daily_summary_sub), fontSize = 12.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = isDailySummaryEnabled,
                            onCheckedChange = { updated ->
                                isDailySummaryEnabled = updated
                                AppPreferencesManager.setDailySummaryEnabled(context, updated)
                                DailySummaryWorker.scheduleDailySummary(context)
                            }
                        )
                    }

                    if (isDailySummaryEnabled) {
                        val formattedTime = remember(dailySummaryTime) {
                            val h = dailySummaryTime.first
                            val m = dailySummaryTime.second
                            val ampm = if (h >= 12) "PM" else "AM"
                            val displayHour = when {
                                h == 0 -> 12
                                h > 12 -> h - 12
                                else -> h
                            }
                            String.format(java.util.Locale.getDefault(), "%02d:%02d %s", displayHour, m, ampm)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showTimePickerDialog = true }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.settings_daily_summary_time_label), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = formattedTime,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        if (showTimePickerDialog) {
                            DisposableEffect(Unit) {
                                val timePicker = android.app.TimePickerDialog(
                                    context,
                                    { _, hourOfDay, minute ->
                                        dailySummaryTime = hourOfDay to minute
                                        AppPreferencesManager.setDailySummaryTime(context, hourOfDay, minute)
                                        DailySummaryWorker.scheduleDailySummary(context, hourOfDay, minute)
                                        showTimePickerDialog = false
                                    },
                                    dailySummaryTime.first,
                                    dailySummaryTime.second,
                                    false
                                )
                                timePicker.setOnCancelListener { showTimePickerDialog = false }
                                timePicker.setOnDismissListener { showTimePickerDialog = false }
                                timePicker.show()
                                onDispose {
                                    timePicker.dismiss()
                                }
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    var isMonthlySummaryEnabled by remember { mutableStateOf(AppPreferencesManager.isMonthlySummaryEnabled(context)) }
                    var monthlySchedule by remember { mutableStateOf(AppPreferencesManager.getMonthlySummarySchedule(context)) }
                    var showMonthlyTimePicker by remember { mutableStateOf(false) }
                    var showMonthlyDayDialog by remember { mutableStateOf(false) }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val updated = !isMonthlySummaryEnabled
                                isMonthlySummaryEnabled = updated
                                AppPreferencesManager.setMonthlySummaryEnabled(context, updated)
                                MonthlySummaryWorker.scheduleMonthlySummary(context)
                            },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.settings_monthly_summary_title), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(stringResource(R.string.settings_monthly_summary_sub), fontSize = 12.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = isMonthlySummaryEnabled,
                            onCheckedChange = { updated ->
                                isMonthlySummaryEnabled = updated
                                AppPreferencesManager.setMonthlySummaryEnabled(context, updated)
                                MonthlySummaryWorker.scheduleMonthlySummary(context)
                            }
                        )
                    }

                    if (isMonthlySummaryEnabled) {
                        val (currentDay, currentHour, currentMin) = monthlySchedule

                        val formattedDayText = if (currentDay == 31) "Day 31 (Last Day)" else "Day $currentDay"

                        val formattedTimeText = remember(currentHour, currentMin) {
                            val ampm = if (currentHour >= 12) "PM" else "AM"
                            val displayHour = when {
                                currentHour == 0 -> 12
                                currentHour > 12 -> currentHour - 12
                                else -> currentHour
                            }
                            String.format(java.util.Locale.getDefault(), "%02d:%02d %s", displayHour, currentMin, ampm)
                        }

                        // Row 1: Day Selector
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showMonthlyDayDialog = true }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.settings_monthly_summary_day_label), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = formattedDayText,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }

                        // Row 2: Time Selector
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showMonthlyTimePicker = true }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Summary Notification Time", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = formattedTimeText,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        // Independent Dialog 1: Day Selector Dialog (1 to 31)
                        if (showMonthlyDayDialog) {
                            var tempDay by remember { mutableStateOf(currentDay) }
                            val daysList = (1..31).toList()

                            AlertDialog(
                                onDismissRequest = { showMonthlyDayDialog = false },
                                title = { Text("Select Day of Month", fontWeight = FontWeight.Bold) },
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("Choose the day of the month for your business report (1 to 31):", fontSize = 13.sp, color = Color.Gray)
                                        Box(modifier = Modifier.fillMaxWidth().height(220.dp)) {
                                            LazyColumn {
                                                items(daysList) { dayNum ->
                                                    val labelText = if (dayNum == 31) "31 (Last Day of Month)" else "$dayNum"
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clickable { tempDay = dayNum }
                                                            .padding(10.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        RadioButton(
                                                            selected = tempDay == dayNum,
                                                            onClick = { tempDay = dayNum }
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(labelText, fontWeight = FontWeight.SemiBold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            monthlySchedule = Triple(tempDay, currentHour, currentMin)
                                            AppPreferencesManager.setMonthlySummarySchedule(context, tempDay, currentHour, currentMin)
                                            MonthlySummaryWorker.scheduleMonthlySummary(context, tempDay, currentHour, currentMin)
                                            showMonthlyDayDialog = false
                                        }
                                    ) {
                                        Text(stringResource(R.string.action_save))
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showMonthlyDayDialog = false }) {
                                        Text(stringResource(R.string.action_cancel))
                                    }
                                }
                            )
                        }

                        // Independent Dialog 2: Time Picker Dialog
                        if (showMonthlyTimePicker) {
                            DisposableEffect(Unit) {
                                val timePicker = android.app.TimePickerDialog(
                                    context,
                                    { _, hourOfDay, minute ->
                                        monthlySchedule = Triple(currentDay, hourOfDay, minute)
                                        AppPreferencesManager.setMonthlySummarySchedule(context, currentDay, hourOfDay, minute)
                                        MonthlySummaryWorker.scheduleMonthlySummary(context, currentDay, hourOfDay, minute)
                                        showMonthlyTimePicker = false
                                    },
                                    currentHour,
                                    currentMin,
                                    false
                                )
                                timePicker.setOnCancelListener { showMonthlyTimePicker = false }
                                timePicker.setOnDismissListener { showMonthlyTimePicker = false }
                                timePicker.show()
                                onDispose {
                                    timePicker.dismiss()
                                }
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    SettingClickableRow(
                        icon = Icons.AutoMirrored.Filled.HelpOutline,
                        title = stringResource(R.string.help_title),
                        subtitle = "App istemal karne ke aasan tareeqay aur hidayat dekhein"
                    ) {
                        onNavigateToHelp()
                    }
                }
            }

            // CARD 3: Team Management & Categories Section
            SettingsSectionCard(
                title = stringResource(R.string.settings_section_team),
                icon = Icons.Default.Group
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (isOwner) {
                        SettingClickableRow(
                            icon = Icons.Default.PersonAdd,
                            title = stringResource(R.string.settings_staff_invite_title),
                            subtitle = stringResource(R.string.settings_staff_invite_sub)
                        ) {
                            inviteRole = "STAFF"
                            activeInviteCode = ""
                            showTeamDialog = true
                        }

                        SettingClickableRow(
                            icon = Icons.Default.Handshake,
                            title = stringResource(R.string.settings_partner_invite_title),
                            subtitle = stringResource(R.string.settings_partner_invite_sub)
                        ) {
                            inviteRole = "PARTNER"
                            activeInviteCode = ""
                            showTeamDialog = true
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }

                    Text(stringResource(R.string.settings_trading_categories), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)

                    if (allCategories.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            allCategories.forEach { categoryItem ->
                                FilterChip(
                                    selected = categoryItem.enabled,
                                    enabled = isOwner,
                                    onClick = { onToggleCategory(categoryItem) },
                                    label = { Text(categoryItem.name, fontSize = 13.sp) },
                                    leadingIcon = if (categoryItem.enabled) {
                                        { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                            }
                        }
                    }
                }
            }

            // CARD 5: Security & Account Section
            SettingsSectionCard(
                title = stringResource(R.string.settings_section_security),
                icon = Icons.Default.Security
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SettingClickableRow(
                        icon = Icons.Default.Lock,
                        title = stringResource(R.string.settings_app_pin),
                        subtitle = if (isPinActive) "4-Digit PIN Active ✓" else "Set 4-digit PIN for cold start"
                    ) {
                        showPinDialog = true
                    }

                    if (isOwner || userRole.equals("Partner", ignoreCase = true)) {
                        SettingClickableRow(
                            icon = Icons.Default.Download,
                            title = "Export All Data (ZIP)",
                            subtitle = "Save JSON + CSV records on phone"
                        ) {
                            scope.launch {
                                DataExporter.exportDataZip(context)
                            }
                        }
                    }

                    SettingClickableRow(
                        icon = Icons.AutoMirrored.Filled.Logout,
                        title = stringResource(R.string.settings_sign_out),
                        subtitle = "Logout active session"
                    ) {
                        showSignOutDialog = true
                    }
                }
            }

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

@Composable
private fun SettingsSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            HorizontalDivider()

            content()
        }
    }
}

@Composable
private fun SettingClickableRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(subtitle, fontSize = 11.sp, color = Color.Gray)
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
