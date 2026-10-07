package com.mmushtaq04.buysell.presentation.screens.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mmushtaq04.buysell.R
import com.mmushtaq04.buysell.ui.theme.BuySellTheme

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ShopSetupScreen(
    onShopCreated: (
        userName: String,
        role: String,
        shopName: String,
        shopPhone: String,
        shopAddress: String,
        selectedCategories: Set<String>
    ) -> Unit = { _, _, _, _, _, _ -> }
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Owner, 1 = Staff, 2 = Partner

    // Owner Form States
    var userName by remember { mutableStateOf("") }
    var shopName by remember { mutableStateOf("") }
    var shopPhone by remember { mutableStateOf("") }
    var shopNumberAddress by remember { mutableStateOf("") }

    // Staff/Partner Form States
    var joinUserName by remember { mutableStateOf("") }
    var joinPhone by remember { mutableStateOf("") }
    var inviteCode by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }

    val presetCategories = listOf(
        "Mobile", "Tablet / iPad", "Laptop", "Console",
        "Smartwatch", "Earbuds / Audio", "Accessories", "Parts"
    )

    var selectedCategories by remember { mutableStateOf(presetCategories.toSet()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.shop_setup_title_bar), fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Tab Selection: Owner vs Staff vs Partner
                SecondaryTabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Store, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.tab_owner), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.tab_staff), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.tab_partner), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    )
                }

                if (selectedTab == 0) {
                    // --- OWNER: CREATE NEW SHOP ---
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Store,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(stringResource(R.string.shop_setup_owner_header), fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            Text(stringResource(R.string.shop_setup_owner_sub), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    OutlinedTextField(
                        value = userName,
                        onValueChange = { userName = it },
                        label = { Text(stringResource(R.string.label_owner_name)) },
                        placeholder = { Text("e.g. Muhammad Ali") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = shopName,
                        onValueChange = { shopName = it },
                        label = { Text(stringResource(R.string.label_shop_name)) },
                        placeholder = { Text("e.g. Hafeez Center Mobiles") },
                        leadingIcon = { Icon(Icons.Default.Store, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = shopPhone,
                        onValueChange = { shopPhone = it },
                        label = { Text(stringResource(R.string.label_shop_phone)) },
                        placeholder = { Text("03001234567") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = shopNumberAddress,
                        onValueChange = { shopNumberAddress = it },
                        label = { Text(stringResource(R.string.label_shop_address)) },
                        placeholder = { Text("e.g. Shop #12, Hafeez Center, Lahore") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider()

                    Text(stringResource(R.string.shop_setup_categories_prompt), fontWeight = FontWeight.Bold, fontSize = 15.sp)

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        presetCategories.forEach { category ->
                            val isSelected = category in selectedCategories
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedCategories = if (isSelected) {
                                        selectedCategories - category
                                    } else {
                                        selectedCategories + category
                                    }
                                },
                                label = { Text(category, fontSize = 13.sp) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }
                } else if (selectedTab == 1) {
                    // --- STAFF: JOIN EXISTING SHOP WITH INVITE CODE ---
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(stringResource(R.string.shop_setup_staff_header), fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            Text(stringResource(R.string.shop_setup_staff_sub), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    OutlinedTextField(
                        value = joinUserName,
                        onValueChange = { joinUserName = it },
                        label = { Text(stringResource(R.string.label_staff_name)) },
                        placeholder = { Text("e.g. Usman Ahmed") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = joinPhone,
                        onValueChange = { joinPhone = it },
                        label = { Text(stringResource(R.string.label_mobile_number)) },
                        placeholder = { Text("03129876543") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = inviteCode,
                        onValueChange = { inviteCode = it.uppercase() },
                        label = { Text(stringResource(R.string.label_staff_code)) },
                        placeholder = { Text("e.g. K7M2A9P4") },
                        leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    // --- PARTNER (SLEEPING PARTNER): VIEW-ONLY DASHBOARD ---
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(stringResource(R.string.shop_setup_partner_header), fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            Text(stringResource(R.string.shop_setup_partner_sub), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    OutlinedTextField(
                        value = joinUserName,
                        onValueChange = { joinUserName = it },
                        label = { Text(stringResource(R.string.label_partner_name)) },
                        placeholder = { Text("e.g. Bilal Khan") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = joinPhone,
                        onValueChange = { joinPhone = it },
                        label = { Text(stringResource(R.string.label_mobile_number)) },
                        placeholder = { Text("03335554433") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = inviteCode,
                        onValueChange = { inviteCode = it.uppercase() },
                        label = { Text(stringResource(R.string.label_partner_code)) },
                        placeholder = { Text("e.g. P8X9R2Q5") },
                        leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    isLoading = true
                    val roleStr = when (selectedTab) {
                        0 -> "Owner"
                        1 -> "Staff"
                        else -> "Partner"
                    }
                    val nameToSave = if (selectedTab == 0) userName.trim() else joinUserName.trim()
                    onShopCreated(
                        nameToSave,
                        roleStr,
                        shopName.trim(),
                        shopPhone.trim(),
                        shopNumberAddress.trim(),
                        selectedCategories
                    )
                },
                enabled = if (selectedTab == 0) {
                    userName.isNotBlank() && shopName.isNotBlank() && shopPhone.isNotBlank() && shopNumberAddress.isNotBlank() && !isLoading
                } else {
                    joinUserName.isNotBlank() && joinPhone.isNotBlank() && inviteCode.length >= 6 && !isLoading
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    val btnLabel = when (selectedTab) {
                        0 -> stringResource(R.string.btn_create_shop)
                        1 -> stringResource(R.string.btn_join_staff)
                        else -> stringResource(R.string.btn_join_partner)
                    }
                    Text(text = btnLabel, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ShopSetupScreenPreview() {
    BuySellTheme {
        ShopSetupScreen()
    }
}
