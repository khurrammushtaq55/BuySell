package com.mmushtaq04.buysell.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mmushtaq04.buysell.R

object CategoryLocalization {

    @Composable
    fun getLocalizedCategoryName(codeOrName: String): String {
        return when (codeOrName.lowercase().trim()) {
            "mobile", "mobile phones" -> stringResource(R.string.category_mobile)
            "tablet", "tablet / ipad", "ipad" -> stringResource(R.string.category_tablet)
            "laptop" -> stringResource(R.string.category_laptop)
            "console" -> stringResource(R.string.category_console)
            "smartwatch" -> stringResource(R.string.category_smartwatch)
            "earbuds", "earbuds / audio", "audio" -> stringResource(R.string.category_earbuds)
            "accessories" -> stringResource(R.string.category_accessories)
            "parts" -> stringResource(R.string.category_parts)
            "camera" -> stringResource(R.string.category_camera)
            "other" -> stringResource(R.string.category_other)
            else -> codeOrName
        }
    }
}
