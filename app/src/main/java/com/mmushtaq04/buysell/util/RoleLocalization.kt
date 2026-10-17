package com.mmushtaq04.buysell.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mmushtaq04.buysell.R

object RoleLocalization {

    @Composable
    fun getLocalizedRoleName(roleStr: String): String {
        return when (roleStr.lowercase().trim()) {
            "owner", "shop_owner" -> stringResource(R.string.role_owner)
            "staff", "staff_member" -> stringResource(R.string.role_staff)
            "partner", "sleeping_partner" -> stringResource(R.string.role_partner)
            else -> roleStr
        }
    }
}
