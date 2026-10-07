package com.mmushtaq04.buysell.data.local

import com.mmushtaq04.buysell.data.local.entity.CategoryEntity
import com.mmushtaq04.buysell.data.local.enums.IdentifierType
import com.mmushtaq04.buysell.data.local.enums.TrackingMode
import java.util.UUID

object CategoryPresets {
    fun getPresetCategories(
        shopId: String,
        selectedCategoryNames: Set<String> = emptySet(),
        userId: String = ""
    ): List<CategoryEntity> {
        val now = System.currentTimeMillis()
        val presets = listOf(
            Triple("mobile", "Mobile", Pair(IdentifierType.IMEI, TrackingMode.UNIQUE)),
            Triple("tablet", "Tablet / iPad", Pair(IdentifierType.SERIAL, TrackingMode.UNIQUE)),
            Triple("laptop", "Laptop", Pair(IdentifierType.SERIAL, TrackingMode.UNIQUE)),
            Triple("console", "Console", Pair(IdentifierType.SERIAL, TrackingMode.UNIQUE)),
            Triple("smartwatch", "Smartwatch", Pair(IdentifierType.SERIAL, TrackingMode.UNIQUE)),
            Triple("earbuds", "Earbuds / Audio", Pair(IdentifierType.SERIAL, TrackingMode.UNIQUE)),
            Triple("accessories", "Accessories", Pair(IdentifierType.NONE, TrackingMode.QUANTITY)),
            Triple("parts", "Parts", Pair(IdentifierType.NONE, TrackingMode.QUANTITY)),
            Triple("camera", "Camera", Pair(IdentifierType.SERIAL, TrackingMode.UNIQUE)),
            Triple("other", "Other", Pair(IdentifierType.NONE, TrackingMode.UNIQUE))
        )

        return presets.mapIndexed { index, (key, name, types) ->
            val isEnabled = if (selectedCategoryNames.isEmpty()) {
                true
            } else {
                selectedCategoryNames.any { it.equals(name, ignoreCase = true) || it.equals(key, ignoreCase = true) }
            }

            CategoryEntity(
                id = UUID.randomUUID().toString(),
                shopId = shopId,
                name = name,
                presetKey = key,
                identifierType = types.first,
                trackingMode = types.second,
                enabled = isEnabled,
                sortOrder = index,
                createdAt = now,
                updatedAt = now,
                createdBy = userId,
                updatedBy = userId
            )
        }
    }
}
