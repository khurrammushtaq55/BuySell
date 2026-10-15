package com.mmushtaq04.buysell.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mmushtaq04.buysell.R
import com.mmushtaq04.buysell.ui.theme.BuySellTheme

@Composable
fun PremiumFeatureGate(
    isLocked: Boolean,
    featureName: String,
    onUnlockClick: () -> Unit,
    placeholder: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    if (!isLocked) {
        content()
        return
    }

    val lockedDesc = stringResource(R.string.paywall_gate_locked_desc, featureName)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clearAndSetSemantics {
                contentDescription = lockedDesc
            }
            .clickable { onUnlockClick() }
    ) {
        // T1: Render neutral placeholder when locked, NOT the real content!
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            if (placeholder != null) {
                placeholder()
            } else {
                DefaultLockPlaceholder()
            }
        }

        // Light Translucent Overlay Scrim
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.65f))
        )

        // Compact Lock Pill Badge displaying featureName
        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(6.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 4.dp,
            tonalElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = stringResource(R.string.paywall_gate_unlock_feature, featureName),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun DefaultLockPlaceholder() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Box(modifier = Modifier.fillMaxSize())
    }
}

@Preview(showBackground = true)
@Composable
fun PremiumFeatureGatePreview() {
    BuySellTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            PremiumFeatureGate(
                isLocked = true,
                featureName = "Monthly Report",
                onUnlockClick = {}
            ) {
                Text("Secret real data that should not be composed when locked")
            }
        }
    }
}
