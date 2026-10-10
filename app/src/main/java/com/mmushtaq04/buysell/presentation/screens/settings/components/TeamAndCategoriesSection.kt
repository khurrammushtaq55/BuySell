package com.mmushtaq04.buysell.presentation.screens.settings.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mmushtaq04.buysell.R
import com.mmushtaq04.buysell.data.local.entity.CategoryEntity
import com.mmushtaq04.buysell.presentation.components.PremiumFeatureGate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TeamAndCategoriesSection(
    isOwner: Boolean,
    allCategories: List<CategoryEntity>,
    isPartnerInviteLocked: Boolean = false,
    onUnlockClick: () -> Unit = {},
    onToggleCategory: (CategoryEntity) -> Unit,
    onGenerateInvite: (role: String, onCodeGenerated: (String) -> Unit) -> Unit
) {
    val context = LocalContext.current

    var showTeamDialog by remember { mutableStateOf(false) }
    var activeInviteCode by remember { mutableStateOf("") }
    var inviteRole by remember { mutableStateOf("STAFF") }
    var isGeneratingCode by remember { mutableStateOf(false) }

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

                PremiumFeatureGate(
                    isLocked = isPartnerInviteLocked,
                    featureName = stringResource(R.string.settings_partner_invite_title),
                    onUnlockClick = onUnlockClick
                ) {
                    SettingClickableRow(
                        icon = Icons.Default.Handshake,
                        title = stringResource(R.string.settings_partner_invite_title),
                        subtitle = stringResource(R.string.settings_partner_invite_sub)
                    ) {
                        inviteRole = "PARTNER"
                        activeInviteCode = ""
                        showTeamDialog = true
                    }
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
}
