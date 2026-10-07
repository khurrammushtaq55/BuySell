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
import androidx.compose.material.icons.automirrored.filled.Logout
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
    onSignOutClick: () -> Unit = {}
) {
    val context = LocalContext.current
    var editableShopName by remember(shopName) { mutableStateOf(shopName) }
    var editableShopPhone by remember(shopPhone) { mutableStateOf(shopPhone) }
    var editableShopAddress by remember(shopAddress) { mutableStateOf(shopAddress) }
    var isSavingShopProfile by remember { mutableStateOf(false) }

    var selectedLanguage by remember { mutableStateOf("Roman Urdu / رومن اردو") }
    var expandedLanguageDropdown by remember { mutableStateOf(false) }
    var slowStockDaysText by remember { mutableStateOf("30") }
    var receiptFooterText by remember { mutableStateOf("Shukriya! Visit again.") }

    var showPinDialog by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showTeamDialog by remember { mutableStateOf(false) }

    var activeInviteCode by remember { mutableStateOf("") }
    var inviteRole by remember { mutableStateOf("STAFF") }
    var isGeneratingCode by remember { mutableStateOf(false) }

    var pinInputText by remember { mutableStateOf("") }
    var isPinActive by remember { mutableStateOf(runCatching { AppPinManager.isPinSet(context) }.getOrDefault(false)) }

    val isOwner = userRole.equals("Owner", ignoreCase = true)

    val languages = listOf(
        "Roman Urdu / رومن اردو",
        "Urdu / اردو",
        "English",
        "Spanish / Español",
        "French / Français",
        "Hindi / हिंदी",
        "Arabic / العربية",
        "Chinese / 简体中文"
    )

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
                title = { Text("Settings & Shop Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            // Shop Details Editable Section (Owner Only)
            Text("Dukan ki Details:", fontWeight = FontWeight.Bold, fontSize = 16.sp)

            OutlinedTextField(
                value = editableShopName,
                onValueChange = { editableShopName = it },
                label = { Text("Dukan Ka Naam") },
                readOnly = !isOwner,
                leadingIcon = { Icon(Icons.Default.Store, contentDescription = null) },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = editableShopPhone,
                onValueChange = { editableShopPhone = it },
                label = { Text("Dukan Mobile Number") },
                readOnly = !isOwner,
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = editableShopAddress,
                onValueChange = { editableShopAddress = it },
                label = { Text("Dukan / Shop Address") },
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
                            Toast.makeText(context, "Dukan ki detail update ho gayi ✓", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = editableShopName.isNotBlank() && !isSavingShopProfile,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Dukan Details Save Karein ✓", fontWeight = FontWeight.Bold)
                }
            } else {
                Text(
                    text = "Note: Dukan ki detail sirf Shop Owner tabdeeli kar sakta hai.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider()

            // Language Selection
            Text("Aap ki Zaban (Language):", fontWeight = FontWeight.Bold, fontSize = 16.sp)

            Box {
                OutlinedTextField(
                    value = selectedLanguage,
                    onValueChange = {},
                    label = { Text("App Language") },
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
                    languages.forEach { lang ->
                        DropdownMenuItem(
                            text = { Text(lang) },
                            onClick = {
                                selectedLanguage = lang
                                expandedLanguageDropdown = false
                            }
                        )
                    }
                }
            }

            HorizontalDivider()

            // Team Management (Owner Only)
            if (isOwner) {
                Text("Team & Staff Management:", fontWeight = FontWeight.Bold, fontSize = 16.sp)

                SettingClickableRow(
                    icon = Icons.Default.GroupAdd,
                    title = "Team & Staff Invites / Code",
                    subtitle = "Staff members ya Sleeping Partner ke liye invite code banayein"
                ) {
                    showTeamDialog = true
                }

                HorizontalDivider()
            }

            // Shop Categories Management
            Text("Trading Categories (Check/Uncheck Karein):", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Jin categories mein dukan deal karti hai, unhein select karein:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

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
                            onClick = {
                                onToggleCategory(categoryItem)
                            },
                            label = { Text(categoryItem.name, fontSize = 13.sp) },
                            leadingIcon = if (categoryItem.enabled) {
                                { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }
            }

            HorizontalDivider()

            // Shop Receipt & Slow Stock Settings
            Text("Receipt & Stock Settings:", fontWeight = FontWeight.Bold, fontSize = 16.sp)

            OutlinedTextField(
                value = slowStockDaysText,
                onValueChange = { slowStockDaysText = it },
                label = { Text("Slow Stock Warning Days") },
                supportingText = { Text("Default: 30 days (jab phone 30 din se zyada pada rahe)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = receiptFooterText,
                onValueChange = { receiptFooterText = it },
                label = { Text("Receipt Footer Note") },
                modifier = Modifier.fillMaxWidth()
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Receipt Shop Logo", fontWeight = FontWeight.Bold)
                        Text("PNG / JPG max 200 KB — Receipt par print hoga", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }

            HorizontalDivider()

            // Account & Security
            Text("Account & Security:", fontWeight = FontWeight.Bold, fontSize = 16.sp)

            SettingClickableRow(
                icon = Icons.Default.Lock,
                title = "App PIN Lock",
                subtitle = if (isPinActive) "4-Digit PIN Enabled ✓ (Tap to change/remove)" else "Set 4-digit PIN for cold start"
            ) {
                showPinDialog = true
            }

            if (isOwner || userRole.equals("Partner", ignoreCase = true)) {
                SettingClickableRow(
                    icon = Icons.Default.Download,
                    title = "Export All Data (ZIP)",
                    subtitle = "Save JSON + CSV + Photos on phone"
                ) { }
            }

            SettingClickableRow(
                icon = Icons.AutoMirrored.Filled.Logout,
                title = "Sign Out",
                subtitle = "Logout active session"
            ) {
                showSignOutDialog = true
            }

            Spacer(modifier = Modifier.height(16.dp))

            // App Version Info
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
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(subtitle, fontSize = 12.sp, color = Color.Gray)
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
