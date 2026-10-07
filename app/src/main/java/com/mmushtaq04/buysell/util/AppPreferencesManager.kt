package com.mmushtaq04.buysell.util

import android.app.Activity
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object AppPreferencesManager {
    private const val PREFS_NAME = "buysell_app_prefs"
    private const val KEY_SHOW_BUY_COST_IN_SELL = "show_buy_cost_in_sell"
    private const val KEY_APP_LANGUAGE_TAG = "app_language_tag"

    fun isShowBuyCostInSellEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_SHOW_BUY_COST_IN_SELL, true)
    }

    fun setShowBuyCostInSellEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_SHOW_BUY_COST_IN_SELL, enabled).apply()
    }

    fun getAppLanguageTag(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_APP_LANGUAGE_TAG, "b+ur+Latn") ?: "b+ur+Latn"
    }

    fun setAppLanguageTag(context: Context, languageTag: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_APP_LANGUAGE_TAG, languageTag).apply()

        val localeList = LocaleListCompat.forLanguageTags(languageTag)
        AppCompatDelegate.setApplicationLocales(localeList)

        (context as? Activity)?.recreate()
    }
}
