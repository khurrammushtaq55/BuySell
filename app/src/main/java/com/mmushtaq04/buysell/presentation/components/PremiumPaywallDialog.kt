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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.billingclient.api.ProductDetails
import com.mmushtaq04.buysell.R
import com.mmushtaq04.buysell.domain.billing.PlayBillingManager

@Composable
fun PremiumPaywallDialog(
    isOwner: Boolean = true,
    productDetailsMap: Map<String, ProductDetails> = emptyMap(),
    isBillingReady: Boolean = true,
    onDismiss: () -> Unit,
    onUpgradeClick: (plan: String) -> Unit,
    onRestoreClick: () -> Unit,
    onRedeemPromoCode: (code: String, onSuccess: () -> Unit, onError: () -> Unit) -> Unit
) {
    val context = LocalContext.current
    var selectedPlan by remember { mutableStateOf(PlayBillingManager.SKU_PRO_ANNUAL) }
    var showPromoDialog by remember { mutableStateOf(false) }
    var promoCodeInput by remember { mutableStateOf("") }
    var isRedeeming by remember { mutableStateOf(false) }

    val annualDetails = productDetailsMap[PlayBillingManager.SKU_PRO_ANNUAL]
    val monthlyDetails = productDetailsMap[PlayBillingManager.SKU_PRO_MONTHLY]

    val annualPriceFormatted = annualDetails?.subscriptionOfferDetails?.firstOrNull()?.pricingPhases
        ?.pricingPhaseList?.firstOrNull()?.formattedPrice ?: "$9.99"
    val annualPriceText = stringResource(R.string.paywall_plan_annual_sub, annualPriceFormatted)

    val monthlyPriceFormatted = monthlyDetails?.subscriptionOfferDetails?.firstOrNull()?.pricingPhases
        ?.pricingPhaseList?.firstOrNull()?.formattedPrice ?: "$0.99"
    val monthlyPriceText = stringResource(R.string.paywall_plan_monthly_sub, monthlyPriceFormatted)

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
                                    Toast.makeText(context, context.getString(R.string.promo_error_invalid), Toast.LENGTH_SHORT).show()
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
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
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

                if (!isOwner) {
                    // T7: Non-owner notice
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = stringResource(R.string.paywall_owner_only_notice),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                } else {
                    // Pricing Options Selection (Owner only)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedPlan = PlayBillingManager.SKU_PRO_ANNUAL },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedPlan == PlayBillingManager.SKU_PRO_ANNUAL) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
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
                                    text = stringResource(R.string.paywall_plan_annual_title),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = annualPriceText,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            RadioButton(
                                selected = selectedPlan == PlayBillingManager.SKU_PRO_ANNUAL,
                                onClick = { selectedPlan = PlayBillingManager.SKU_PRO_ANNUAL }
                            )
                        }
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedPlan = PlayBillingManager.SKU_PRO_MONTHLY },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedPlan == PlayBillingManager.SKU_PRO_MONTHLY) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
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
                                    text = stringResource(R.string.paywall_plan_monthly_title),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = monthlyPriceText,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            RadioButton(
                                selected = selectedPlan == PlayBillingManager.SKU_PRO_MONTHLY,
                                onClick = { selectedPlan = PlayBillingManager.SKU_PRO_MONTHLY }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isOwner) {
                    Button(
                        onClick = { onUpgradeClick(selectedPlan) },
                        enabled = isBillingReady,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isBillingReady) stringResource(R.string.paywall_btn_upgrade) else stringResource(R.string.billing_btn_loading),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onRestoreClick, enabled = isBillingReady) {
                            Text(
                                text = stringResource(R.string.paywall_btn_restore),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        TextButton(onClick = { showPromoDialog = true }) {
                            Text(
                                text = stringResource(R.string.paywall_btn_promo),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }

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
        },
        dismissButton = null
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

@Preview(showBackground = true)
@Composable
fun PremiumPaywallDialogPreview() {
    com.mmushtaq04.buysell.ui.theme.BuySellTheme {
        PremiumPaywallDialog(
            isOwner = true,
            isBillingReady = true,
            onDismiss = {},
            onUpgradeClick = {},
            onRestoreClick = {},
            onRedeemPromoCode = { _, _, _ -> }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PremiumPaywallDialogNonOwnerPreview() {
    com.mmushtaq04.buysell.ui.theme.BuySellTheme {
        PremiumPaywallDialog(
            isOwner = false,
            isBillingReady = true,
            onDismiss = {},
            onUpgradeClick = {},
            onRestoreClick = {},
            onRedeemPromoCode = { _, _, _ -> }
        )
    }
}
