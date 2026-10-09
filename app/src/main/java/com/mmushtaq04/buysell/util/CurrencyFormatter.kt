package com.mmushtaq04.buysell.util

import android.content.Context
import java.text.NumberFormat
import java.util.Locale

object CurrencyFormatter {

    fun getSymbol(context: Context): String {
        return AppPreferencesManager.getCurrencySymbol(context)
    }

    fun formatAmount(context: Context, amountRs: Long): String {
        val symbol = getSymbol(context)
        val formattedNum = NumberFormat.getNumberInstance(Locale.getDefault()).format(amountRs)
        return "$symbol $formattedNum"
    }

    fun formatPaisa(context: Context, amountPaisa: Long): String {
        return formatAmount(context, amountPaisa / 100)
    }
}
