package com.mmushtaq04.buysell.util

import android.app.Activity
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

data class LanguageOption(val displayName: String, val tag: String)

object AppPreferencesManager {
    private const val PREFS_NAME = "buysell_app_prefs"
    private const val KEY_SHOW_BUY_COST_IN_SELL = "show_buy_cost_in_sell"
    private const val KEY_APP_LANGUAGE_TAG = "app_language_tag"

    val supportedLanguages = listOf(
        LanguageOption("English", "en"),
        LanguageOption("Roman Urdu / رومن اردو", "b+ur+Latn"),
        LanguageOption("Urdu / اردو", "ur"),
        LanguageOption("Spanish / Español", "es"),
        LanguageOption("French / Français", "fr"),
        LanguageOption("Hindi / हिंदी", "hi"),
        LanguageOption("Arabic / العربية", "ar"),
        LanguageOption("Chinese / 简体中文", "zh-CN")
    )

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
        if (prefs.contains(KEY_APP_LANGUAGE_TAG)) {
            return prefs.getString(KEY_APP_LANGUAGE_TAG, "en") ?: "en"
        }

        // Auto-detect Mobile System Locale on first launch (minSdk = 24)
        val deviceLocale = context.resources.configuration.locales.get(0)
        val language = deviceLocale?.language ?: "en"
        val script = deviceLocale?.script ?: ""

        val matchedTag = when {
            language.equals("ur", ignoreCase = true) && script.equals("Latn", ignoreCase = true) -> "b+ur+Latn"
            language.equals("ur", ignoreCase = true) -> "ur"
            language.equals("es", ignoreCase = true) -> "es"
            language.equals("fr", ignoreCase = true) -> "fr"
            language.equals("hi", ignoreCase = true) -> "hi"
            language.equals("ar", ignoreCase = true) -> "ar"
            language.equals("zh", ignoreCase = true) -> "zh-CN"
            language.equals("en", ignoreCase = true) -> "en"
            else -> "en" // Default fallback if mobile locale is unsupported
        }

        return matchedTag
    }

    fun setAppLanguageTag(context: Context, languageTag: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_APP_LANGUAGE_TAG, languageTag).apply()

        val localeList = LocaleListCompat.forLanguageTags(languageTag)
        AppCompatDelegate.setApplicationLocales(localeList)

        (context as? Activity)?.recreate()
    }
}
