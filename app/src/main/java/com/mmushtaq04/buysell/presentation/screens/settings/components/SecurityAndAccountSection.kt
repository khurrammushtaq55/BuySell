package com.mmushtaq04.buysell.presentation.screens.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mmushtaq04.buysell.R
import com.mmushtaq04.buysell.presentation.components.PremiumFeatureGate
import com.mmushtaq04.buysell.util.AppPinManager
import com.mmushtaq04.buysell.util.DataExporter
import kotlinx.coroutines.launch

@Composable
fun SecurityAndAccountSection(
    isOwner: Boolean,
    userRole: String,
    isExportLocked: Boolean = false,
    onUnlockClick: () -> Unit = {},
    onAttemptSignOut: (onRequireWarning: (Int) -> Unit, onReadyToSignOut: () -> Unit) -> Unit,
    onForceSignOut: (onReadyToSignOut: () -> Unit) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showPinDialog by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showUnsyncedWarningDialog by remember { mutableStateOf(false) }
    var unsyncedCount by remember { mutableIntStateOf(0) }

    var pinInputText by remember { mutableStateOf("") }
    var isPinActive by remember { mutableStateOf(runCatching { AppPinManager.isPinSet(context) }.getOrDefault(false)) }

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text(if (isPinActive) stringResource(R.string.settings_pin_title_active) else stringResource(R.string.settings_pin_title_inactive)) },
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

    SettingsSectionCard(
        title = stringResource(R.string.settings_section_security),
        icon = Icons.Default.Security
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingClickableRow(
                icon = Icons.Default.Lock,
                title = stringResource(R.string.settings_app_pin),
                subtitle = if (isPinActive) stringResource(R.string.settings_pin_sub_active) else stringResource(R.string.settings_pin_sub_inactive)
            ) {
                showPinDialog = true
            }

            if (isOwner || userRole.equals("Partner", ignoreCase = true)) {
                PremiumFeatureGate(
                    isLocked = isExportLocked,
                    featureName = stringResource(R.string.settings_export_data_title),
                    onUnlockClick = onUnlockClick
                ) {
                    SettingClickableRow(
                        icon = Icons.Default.Download,
                        title = stringResource(R.string.settings_export_data_title),
                        subtitle = stringResource(R.string.settings_export_data_sub)
                    ) {
                        scope.launch {
                            DataExporter.exportDataZip(context)
                        }
                    }
                }
            }

            SettingClickableRow(
                icon = Icons.AutoMirrored.Filled.Logout,
                title = stringResource(R.string.settings_sign_out),
                subtitle = stringResource(R.string.settings_sign_out_sub)
            ) {
                showSignOutDialog = true
            }
        }
    }
}
