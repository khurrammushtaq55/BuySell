package com.mmushtaq04.buysell.presentation.screens.settings.components

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mmushtaq04.buysell.R

@Composable
fun StoreProfileSection(
    isOwner: Boolean,
    ownerName: String,
    shopName: String,
    shopPhone: String,
    shopAddress: String,
    onUpdateShopProfile: (ownerName: String, shopName: String, phone: String, address: String) -> Unit
) {
    val context = LocalContext.current

    var editableOwnerName by remember(ownerName) { mutableStateOf(ownerName) }
    var editableShopName by remember(shopName) { mutableStateOf(shopName) }
    var editableShopPhone by remember(shopPhone) { mutableStateOf(shopPhone) }
    var editableShopAddress by remember(shopAddress) { mutableStateOf(shopAddress) }
    var isSavingShopProfile by remember { mutableStateOf(false) }

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
                            Toast.makeText(context, context.getString(R.string.settings_toast_shop_updated), Toast.LENGTH_SHORT).show()
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
}
