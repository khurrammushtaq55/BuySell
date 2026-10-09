package com.mmushtaq04.buysell.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mmushtaq04.buysell.R

val GoldStarColor = Color(0xFFFFC107)

@Composable
fun InAppRatingDialog(
    onRateClick: (ratingStars: Int, neverShowAgain: Boolean) -> Unit,
    onDismiss: (neverShowAgain: Boolean) -> Unit
) {
    var selectedStars by remember { mutableIntStateOf(5) }
    var neverShowAgain by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { onDismiss(neverShowAgain) },
        shape = RoundedCornerShape(20.dp),
        icon = {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = GoldStarColor,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        },
        title = {
            Text(
                text = stringResource(R.string.rating_dialog_title, stringResource(R.string.app_name)),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.rating_dialog_sub),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                // 5-Star Interactive Rating Row
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    (1..5).forEach { starIndex ->
                        val isSelected = starIndex <= selectedStars
                        val starColor by animateColorAsState(
                            targetValue = if (isSelected) GoldStarColor else Color.LightGray,
                            label = "star_color"
                        )

                        Icon(
                            imageVector = if (isSelected) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Star $starIndex",
                            tint = starColor,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .clickable { selectedStars = starIndex }
                                .padding(2.dp)
                        )
                    }
                }

                // "Don't show again" Checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { neverShowAgain = !neverShowAgain }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Checkbox(
                        checked = neverShowAgain,
                        onCheckedChange = { neverShowAgain = it }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.rating_checkbox_never_show),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onRateClick(selectedStars, neverShowAgain) },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.rating_btn_rate),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onDismiss(neverShowAgain) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.rating_btn_later),
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
        }
    )
}
