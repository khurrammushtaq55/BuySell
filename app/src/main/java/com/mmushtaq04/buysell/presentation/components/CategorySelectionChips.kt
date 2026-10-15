package com.mmushtaq04.buysell.presentation.components

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mmushtaq04.buysell.R
import com.mmushtaq04.buysell.data.local.entity.CategoryEntity
import com.mmushtaq04.buysell.ui.theme.BuySellTheme

/**
 * Reusable category selection grid component that displays a FlowRow of FilterChips
 * and enforces the rule that at least 1 category must remain selected/enabled.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategorySelectionChips(
    categories: List<String>,
    selectedCategories: Set<String>,
    onCategoriesChange: (Set<String>) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val context = LocalContext.current

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        categories.forEach { category ->
            val isSelected = selectedCategories.contains(category)
            FilterChip(
                selected = isSelected,
                enabled = enabled,
                onClick = {
                    if (isSelected && selectedCategories.size <= 1) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.toast_min_category_required),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        val updated = if (isSelected) {
                            selectedCategories - category
                        } else {
                            selectedCategories + category
                        }
                        onCategoriesChange(updated)
                    }
                },
                label = { Text(category, fontSize = 13.sp) },
                leadingIcon = if (isSelected) {
                    { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null
            )
        }
    }
}

/**
 * Entity-based overload for CategoryEntity items (used in Settings screen).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EntityCategorySelectionChips(
    categoryEntities: List<CategoryEntity>,
    onToggleCategory: (CategoryEntity) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val context = LocalContext.current
    val enabledCount = categoryEntities.count { it.enabled }

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        categoryEntities.forEach { categoryItem ->
            FilterChip(
                selected = categoryItem.enabled,
                enabled = enabled,
                onClick = {
                    if (categoryItem.enabled && enabledCount <= 1) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.toast_min_category_required),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        onToggleCategory(categoryItem)
                    }
                },
                label = { Text(categoryItem.name, fontSize = 13.sp) },
                leadingIcon = if (categoryItem.enabled) {
                    { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CategorySelectionChipsPreview() {
    val presetCategories = listOf(
        "Mobile", "Tablet / iPad", "Laptop", "Console",
        "Smartwatch", "Earbuds / Audio", "Accessories", "Parts"
    )
    BuySellTheme {
        CategorySelectionChips(
            categories = presetCategories,
            selectedCategories = setOf("Mobile", "Laptop"),
            onCategoriesChange = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
