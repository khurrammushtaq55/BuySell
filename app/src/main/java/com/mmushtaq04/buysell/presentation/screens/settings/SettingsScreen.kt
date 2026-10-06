package com.mmushtaq04.buysell.presentation.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    var selectedLanguage by remember { mutableStateOf("Roman Urdu / رومن اردو") }
    var expandedLanguageDropdown by remember { mutableStateOf(false) }
    var slowStockDaysText by remember { mutableStateOf("30") }
    var receiptFooterText by remember { mutableStateOf("Shukriya! Visit again.") }

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

            // Shop Profile Settings
            Text("Shop Detail:", fontWeight = FontWeight.Bold, fontSize = 16.sp)

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
                subtitle = "Set 4-digit PIN for cold start"
            ) { }

            SettingClickableRow(
                icon = Icons.Default.Download,
                title = "Export All Data (ZIP)",
                subtitle = "Save JSON + CSV + Photos on phone"
            ) { }

            SettingClickableRow(
                icon = Icons.AutoMirrored.Filled.Logout,
                title = "Sign Out",
                subtitle = "Logout active session"
            ) { }

            Spacer(modifier = Modifier.height(16.dp))

            // App Version Info
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Hafeez Center Tracker v1.0 (Build 1)",
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
