package com.mmushtaq04.buysell.presentation.components

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mmushtaq04.buysell.R

@Composable
fun PremiumPaywallDialog(
    onDismiss: () -> Unit,
    onUpgradeClick: (plan: String) -> Unit,
    onRestoreClick: () -> Unit,
    onRedeemPromoCode: (code: String, onSuccess: () -> Unit, onError: () -> Unit) -> Unit
) {
    val context = LocalContext.current
    var selectedPlan by remember { mutableStateOf("ANNUAL") }
    var showPromoDialog by remember { mutableStateOf(false) }
    var promoCodeInput by remember { mutableStateOf("") }
    var isRedeeming by remember { mutableStateOf(false) }

    if (showPromoDialog) {
        AlertDialog(
            onDismissRequest = { showPromoDialog = false },
            icon = { Icon(Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text(stringResource(R.string.paywall_promo_dialog_title), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.paywall_promo_dialog_msg), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = promoCodeInput,
                        onValueChange = { promoCodeInput = it.uppercase() },
                        label = { Text(stringResource(R.string.paywall_promo_label)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (promoCodeInput.isNotBlank()) {
                            isRedeeming = true
                            onRedeemPromoCode(
                                promoCodeInput.trim(),
                                {
                                    isRedeeming = false
                                    showPromoDialog = false
                                    Toast.makeText(context, context.getString(R.string.paywall_toast_unlocked), Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                },
                                {
                                    isRedeeming = false
                                    Toast.makeText(context, context.getString(R.string.paywall_toast_invalid_code), Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    },
                    enabled = promoCodeInput.isNotBlank() && !isRedeeming
                ) {
                    Text(stringResource(R.string.action_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPromoDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        icon = {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(id = R.mipmap.ic_launcher_round),
                        contentDescription = null,
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                    )
                }
            }
        },
        title = {
            Text(
                text = stringResource(R.string.paywall_title, stringResource(R.string.app_name)),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = stringResource(R.string.paywall_sub),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                // Feature Highlights List
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PaywallFeatureRow(stringResource(R.string.paywall_feature_dashboard))
                    PaywallFeatureRow(stringResource(R.string.paywall_feature_export))
                    PaywallFeatureRow(stringResource(R.string.paywall_feature_partner))
                    PaywallFeatureRow(stringResource(R.string.paywall_feature_currency))
                    PaywallFeatureRow(stringResource(R.string.paywall_feature_monthly))
                }

                HorizontalDivider()

                // Pricing Options Selection
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedPlan = "ANNUAL" },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedPlan == "ANNUAL") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.paywall_plan_annual),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        RadioButton(
                            selected = selectedPlan == "ANNUAL",
                            onClick = { selectedPlan = "ANNUAL" }
                        )
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedPlan = "MONTHLY" },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedPlan == "MONTHLY") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.paywall_plan_monthly),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        RadioButton(
                            selected = selectedPlan == "MONTHLY",
                            onClick = { selectedPlan = "MONTHLY" }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { onUpgradeClick(selectedPlan) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.paywall_btn_upgrade),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onRestoreClick) {
                        Text(
                            text = stringResource(R.string.paywall_btn_restore),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    TextButton(onClick = { showPromoDialog = true }) {
                        Text(
                            text = stringResource(R.string.paywall_btn_promo),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.action_cancel),
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
        }
    )
}

@Composable
private fun PaywallFeatureRow(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
