package com.mmushtaq04.buysell.presentation.screens.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import com.mmushtaq04.buysell.data.local.entity.CategoryEntity
import com.mmushtaq04.buysell.domain.InviteManager
import com.mmushtaq04.buysell.ui.theme.BuySellTheme
import com.mmushtaq04.buysell.util.AppPinManager
import com.mmushtaq04.buysell.util.AppPreferencesManager

data class LanguageOption(val displayName: String, val tag: String)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    userRole: String = "Owner",
    shopName: String = "Mera Buy/Sell Store",
    shopPhone: String = "",
    shopAddress: String = "",
    allCategories: List<CategoryEntity> = emptyList(),
    onUpdateShopProfile: (name: String, phone: String, address: String) -> Unit = { _, _, _ -> },
    onToggleCategory: (CategoryEntity) -> Unit = {},
    onNavigateBack: () -> Unit = {},
    onNavigateToHelp: () -> Unit = {},
    onSignOutClick: () -> Unit = {}
) {
    val context = LocalContext.current
    var editableShopName by remember(shopName) { mutableStateOf(shopName) }
    var editableShopPhone by remember(shopPhone) { mutableStateOf(shopPhone) }
    var editableShopAddress by remember(shopAddress) { mutableStateOf(shopAddress) }
    var isSavingShopProfile by remember { mutableStateOf(false) }

    val languages = listOf(
        LanguageOption("Roman Urdu / رومن اردو", "b+ur+Latn"),
        LanguageOption("Urdu / اردو", "ur"),
        LanguageOption("English", "en"),
        LanguageOption("Spanish / Español", "es"),
        LanguageOption("French / Français", "fr"),
        LanguageOption("Hindi / हिंदी", "hi"),
        LanguageOption("Arabic / العربية", "ar"),
        LanguageOption("Chinese / 简体中文", "zh-CN")
    )

    val currentTag = remember { AppPreferencesManager.getAppLanguageTag(context) }
    var selectedLanguageOption by remember {
        mutableStateOf(languages.find { it.tag.equals(currentTag, ignoreCase = true) } ?: languages.first())
    }

    var expandedLanguageDropdown by remember { mutableStateOf(false) }
    var slowStockDaysText by remember { mutableStateOf("30") }
    var receiptFooterText by remember { mutableStateOf("Shukriya! Visit again.") }

    var showBuyCostInSell by remember {
        mutableStateOf(runCatching { AppPreferencesManager.isShowBuyCostInSellEnabled(context) }.getOrDefault(true))
    }

    var showPinDialog by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showTeamDialog by remember { mutableStateOf(false) }

    var activeInviteCode by remember { mutableStateOf("") }
    var inviteRole by remember { mutableStateOf("STAFF") }
    var isGeneratingCode by remember { mutableStateOf(false) }

    var pinInputText by remember { mutableStateOf("") }
    var isPinActive by remember { mutableStateOf(runCatching { AppPinManager.isPinSet(context) }.getOrDefault(false)) }

    val isOwner = userRole.equals("Owner", ignoreCase = true)

    if (showTeamDialog) {
        AlertDialog(
            onDismissRequest = { showTeamDialog = false },
            icon = { Icon(Icons.Default.Group, contentDescription = null) },
            title = { Text("Team & Member Invites", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Role chunay aur 8-digit join code banayein:", fontSize = 14.sp)

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilterChip(
                            selected = inviteRole == "STAFF",
                            onClick = { inviteRole = "STAFF" },
                            label = { Text("Staff Member", fontSize = 12.sp) },
                            leadingIcon = if (inviteRole == "STAFF") {
                                { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            } else null
                        )

                        FilterChip(
                            selected = inviteRole == "PARTNER",
                            onClick = { inviteRole = "PARTNER" },
                            label = { Text("Sleeping Partner", fontSize = 12.sp) },
                            leadingIcon = if (inviteRole == "PARTNER") {
                                { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            } else null
                        )
                    }

                    if (activeInviteCode.isNotBlank()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Invite Code ($inviteRole):", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = activeInviteCode,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text("Expires in 7 days", fontSize = 11.sp, color = Color.Gray)
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
                                Text("Copy Code")
                            }

                            Button(
                                onClick = {
                                    val roleLabel = if (inviteRole == "STAFF") "Staff Member" else "Sleeping Partner"
                                    val shareMsg = "Aap ko Hafeez Center App par $roleLabel join karne ka Code bheja gaya hai: $activeInviteCode. App download karein aur 'Join with Code' chunay."
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, shareMsg)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share Code"))
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("WhatsApp Share")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isGeneratingCode = true
                        val newCode = InviteManager.generateInviteCode()
                        activeInviteCode = newCode

                        runCatching {
                            val db = FirebaseFirestore.getInstance()
                            val inviteData = mapOf(
                                "code" to newCode,
                                "role" to inviteRole,
                                "created_at" to System.currentTimeMillis()
                            )
                            db.collection("invites").document(newCode).set(inviteData)
                        }
                        isGeneratingCode = false
                    },
                    enabled = !isGeneratingCode
                ) {
                    Text(if (activeInviteCode.isBlank()) "Naya Code Banayein" else "Code Dobara Banayein")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTeamDialog = false }) {
                    Text("Close")
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
                    Text("Cold start par app kholne ke liye 4 digit PIN:", fontSize = 14.sp)
                    OutlinedTextField(
                        value = pinInputText,
                        onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) pinInputText = it },
                        label = { Text("4 Digit PIN") },
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
                    Text("PIN Set Karein")
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
                        Text("PIN Remove Karein", color = MaterialTheme.colorScheme.error)
                    }
                } else {
                    TextButton(onClick = { showPinDialog = false }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Sign Out Karein?", fontWeight = FontWeight.Bold) },
            text = { Text("Kaya aap apni dukan ke account se logout karna chahte hain?", fontSize = 15.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutDialog = false
                        onSignOutClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Logout Karein")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Preferences", fontWeight = FontWeight.Bold) },
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
                title = "Store Profile (Dukan Details)",
                icon = Icons.Default.Store
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editableShopName,
                        onValueChange = { editableShopName = it },
                        label = { Text("Dukan Ka Naam") },
                        readOnly = !isOwner,
                        leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editableShopPhone,
                        onValueChange = { editableShopPhone = it },
                        label = { Text("Mobile Number") },
                        readOnly = !isOwner,
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editableShopAddress,
                        onValueChange = { editableShopAddress = it },
                        label = { Text("Shop Address") },
                        readOnly = !isOwner,
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (isOwner) {
                        Button(
                            onClick = {
                                if (editableShopName.isNotBlank()) {
                                    isSavingShopProfile = true
                                    onUpdateShopProfile(
                                        editableShopName.trim(),
                                        editableShopPhone.trim(),
                                        editableShopAddress.trim()
                                    )
                                    isSavingShopProfile = false
                                    Toast.makeText(context, "Dukan details updated ✓", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = editableShopName.isNotBlank() && !isSavingShopProfile,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Save Shop Profile ✓", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // CARD 2: Language & Preferences Section
            SettingsSectionCard(
                title = "Language & Display (Zaban Aur Preferences)",
                icon = Icons.Default.Translate
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("App Language (Aap ki Zaban):", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)

                    Box {
                        OutlinedTextField(
                            value = selectedLanguageOption.displayName,
                            onValueChange = {},
                            label = { Text("Selected Language") },
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
                            Text("Show Buy Price in Sell Wizard", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Sale karte waqt khareed price eye icon par dikhayein", fontSize = 12.sp, color = Color.Gray)
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

                    SettingClickableRow(
                        icon = Icons.AutoMirrored.Filled.HelpOutline,
                        title = "Madad / App Guide (In-App Help)",
                        subtitle = "App istemal karne ke aasan tareeqay aur hidayat dekhein"
                    ) {
                        onNavigateToHelp()
                    }
                }
            }

            // CARD 3: Team Management & Categories Section
            SettingsSectionCard(
                title = "Team & Trading Categories",
                icon = Icons.Default.Group
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (isOwner) {
                        SettingClickableRow(
                            icon = Icons.Default.GroupAdd,
                            title = "Staff / Partner Join Code",
                            subtitle = "Staff ya Sleeping Partner ko join karwane ke liye code banayein"
                        ) {
                            showTeamDialog = true
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }

                    Text("Trading Categories (Product Types):", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)

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

            // CARD 4: Receipt & Stock Rules
            SettingsSectionCard(
                title = "Receipt & Stock Rules",
                icon = Icons.AutoMirrored.Filled.ReceiptLong
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = slowStockDaysText,
                        onValueChange = { slowStockDaysText = it },
                        label = { Text("Slow Stock Warning (Days)") },
                        supportingText = { Text("Default: 30 days old stock warning") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = receiptFooterText,
                        onValueChange = { receiptFooterText = it },
                        label = { Text("Receipt Footer Note") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // CARD 5: Security & Account Section
            SettingsSectionCard(
                title = "Security & Account",
                icon = Icons.Default.Security
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SettingClickableRow(
                        icon = Icons.Default.Lock,
                        title = "App PIN Lock",
                        subtitle = if (isPinActive) "4-Digit PIN Active ✓" else "Set 4-digit PIN for cold start"
                    ) {
                        showPinDialog = true
                    }

                    if (isOwner || userRole.equals("Partner", ignoreCase = true)) {
                        SettingClickableRow(
                            icon = Icons.Default.Download,
                            title = "Export All Data (ZIP)",
                            subtitle = "Save JSON + CSV records on phone"
                        ) { }
                    }

                    SettingClickableRow(
                        icon = Icons.AutoMirrored.Filled.Logout,
                        title = "Sign Out",
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
                    text = "Hafeez Center Tracker v1.0 (Build 1) • Role: $userRole",
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
