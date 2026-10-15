package com.mmushtaq04.buysell.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mmushtaq04.buysell.R
import com.mmushtaq04.buysell.ui.theme.BuySellTheme

@Composable
fun ShopProfileFields(
    ownerName: String,
    onOwnerNameChange: (String) -> Unit,
    shopName: String,
    onShopNameChange: (String) -> Unit,
    shopPhone: String,
    onShopPhoneChange: (String) -> Unit,
    shopAddress: String,
    onShopAddressChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = ownerName,
            onValueChange = onOwnerNameChange,
            label = { Text(stringResource(R.string.label_owner_name)) },
            placeholder = { Text(stringResource(R.string.placeholder_owner_name)) },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            enabled = enabled,
            readOnly = readOnly,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                keyboardType = KeyboardType.Text
            ),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = shopName,
            onValueChange = onShopNameChange,
            label = { Text(stringResource(R.string.label_shop_name)) },
            placeholder = { Text(stringResource(R.string.placeholder_shop_name)) },
            leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = null) },
            enabled = enabled,
            readOnly = readOnly,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                keyboardType = KeyboardType.Text
            ),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = shopPhone,
            onValueChange = { newValue ->
                val digitsOnly = newValue.filter { it.isDigit() }
                onShopPhoneChange(digitsOnly)
            },
            label = { Text(stringResource(R.string.label_shop_phone)) },
            placeholder = { Text(stringResource(R.string.placeholder_shop_phone)) },
            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
            enabled = enabled,
            readOnly = readOnly,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = shopAddress,
            onValueChange = onShopAddressChange,
            label = { Text(stringResource(R.string.label_shop_address)) },
            placeholder = { Text(stringResource(R.string.placeholder_shop_address)) },
            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
            enabled = enabled,
            readOnly = readOnly,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                keyboardType = KeyboardType.Text
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ShopProfileFieldsPreview() {
    BuySellTheme {
        ShopProfileFields(
            ownerName = "Malik Mushtaq",
            onOwnerNameChange = {},
            shopName = "Mushtaq Mobile Trading",
            onShopNameChange = {},
            shopPhone = "03001234567",
            onShopPhoneChange = {},
            shopAddress = "Shop #12, Ground Floor, Hafeez Center",
            onShopAddressChange = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
