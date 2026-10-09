package com.mmushtaq04.buysell.util

import android.app.Activity
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.mmushtaq04.buysell.R

data class LanguageOption(val displayName: String, val tag: String)
data class ThemeOption(val mode: String, val displayNameResId: Int)
data class CurrencyPreset(val name: String, val code: String, val symbol: String, val flag: String)

object AppPreferencesManager {
    private const val PREFS_NAME = "buysell_app_prefs"
    private const val KEY_SHOW_BUY_COST_IN_SELL = "show_buy_cost_in_sell"
    private const val KEY_APP_LANGUAGE_TAG = "app_language_tag"
    private const val KEY_APP_THEME_MODE = "app_theme_mode"
    private const val KEY_CURRENCY_SYMBOL = "currency_symbol"
    private const val KEY_CURRENCY_CODE = "currency_code"

    val supportedLanguages = listOf(
        LanguageOption("English", "en"),
        LanguageOption("Roman Urdu / رومن اردو", "ur-Latn"),
        LanguageOption("Urdu / اردو", "ur"),
        LanguageOption("Spanish / Español", "es"),
        LanguageOption("French / Français", "fr"),
        LanguageOption("Hindi / हिंदी", "hi"),
        LanguageOption("Arabic / العربية", "ar"),
        LanguageOption("Chinese / 简体中文", "zh-CN")
    )

    val supportedThemes = listOf(
        ThemeOption("system", R.string.theme_system),
        ThemeOption("light", R.string.theme_light),
        ThemeOption("dark", R.string.theme_dark)
    )

    val supportedCurrencies = listOf(
        CurrencyPreset("Pakistani Rupee", "PKR", "Rs", "🇵🇰"),
        CurrencyPreset("US Dollar", "USD", "$", "🇺🇸"),
        CurrencyPreset("Euro", "EUR", "€", "🇪🇺"),
        CurrencyPreset("British Pound", "GBP", "£", "🇬🇧"),
        CurrencyPreset("UAE Dirham", "AED", "AED", "🇦🇪"),
        CurrencyPreset("Saudi Riyal", "SAR", "SAR", "🇸🇦"),
        CurrencyPreset("Indian Rupee", "INR", "₹", "🇮🇳"),
        CurrencyPreset("Canadian Dollar", "CAD", "$", "🇨🇦"),
        CurrencyPreset("Australian Dollar", "AUD", "$", "🇦🇺")
    )

    fun isShowBuyCostInSellEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_SHOW_BUY_COST_IN_SELL, true)
    }

    fun setShowBuyCostInSellEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_SHOW_BUY_COST_IN_SELL, enabled).apply()
    }

    fun getAppThemeMode(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_APP_THEME_MODE, "system") ?: "system"
    }

    fun setAppThemeMode(context: Context, themeMode: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_APP_THEME_MODE, themeMode).apply()

        applyAppThemeMode(themeMode)
        (context as? Activity)?.recreate()
    }

    fun applyAppThemeMode(themeMode: String) {
        when (themeMode.lowercase().trim()) {
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    fun getAppLanguageTag(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val rawTag = prefs.getString(KEY_APP_LANGUAGE_TAG, null)

        if (!rawTag.isNullOrBlank()) {
            val cleanTag = when (rawTag) {
                "b+ur+Latn" -> "ur-Latn"
                "zh-Hans" -> "zh-CN"
                else -> rawTag
            }
            if (cleanTag != rawTag) {
                prefs.edit().putString(KEY_APP_LANGUAGE_TAG, cleanTag).apply()
            }
            return cleanTag
        }

        val deviceLocale = context.resources.configuration.locales.get(0)
        val language = deviceLocale?.language ?: "en"
        val script = deviceLocale?.script ?: ""

        val matchedTag = when {
            language.equals("ur", ignoreCase = true) && script.equals("Latn", ignoreCase = true) -> "ur-Latn"
            language.equals("ur", ignoreCase = true) -> "ur"
            language.equals("es", ignoreCase = true) -> "es"
            language.equals("fr", ignoreCase = true) -> "fr"
            language.equals("hi", ignoreCase = true) -> "hi"
            language.equals("ar", ignoreCase = true) -> "ar"
            language.equals("zh", ignoreCase = true) -> "zh-CN"
            language.equals("en", ignoreCase = true) -> "en"
            else -> "en"
        }

        prefs.edit().putString(KEY_APP_LANGUAGE_TAG, matchedTag).apply()
        return matchedTag
    }

    fun setAppLanguageTag(context: Context, languageTag: String) {
        val cleanTag = when (languageTag) {
            "b+ur+Latn" -> "ur-Latn"
            "zh-Hans" -> "zh-CN"
            else -> languageTag
        }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_APP_LANGUAGE_TAG, cleanTag).apply()

        val localeList = LocaleListCompat.forLanguageTags(cleanTag)
        AppCompatDelegate.setApplicationLocales(localeList)

        (context as? Activity)?.recreate()
    }

    fun getCurrencySymbol(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val symbol = prefs.getString(KEY_CURRENCY_SYMBOL, null)
        if (!symbol.isNullOrBlank()) return symbol

        val (autoSymbol, autoCode) = autoDetectCurrency(context)
        prefs.edit().putString(KEY_CURRENCY_SYMBOL, autoSymbol).putString(KEY_CURRENCY_CODE, autoCode).apply()
        return autoSymbol
    }

    fun getCurrencyCode(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val code = prefs.getString(KEY_CURRENCY_CODE, null)
        if (!code.isNullOrBlank()) return code

        val (autoSymbol, autoCode) = autoDetectCurrency(context)
        prefs.edit().putString(KEY_CURRENCY_SYMBOL, autoSymbol).putString(KEY_CURRENCY_CODE, autoCode).apply()
        return autoCode
    }

    fun setCurrency(context: Context, symbol: String, code: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_CURRENCY_SYMBOL, symbol).putString(KEY_CURRENCY_CODE, code).apply()
    }

    private fun autoDetectCurrency(context: Context): Pair<String, String> {
        val deviceLocale = context.resources.configuration.locales.get(0)
        val country = deviceLocale?.country?.uppercase() ?: ""

        return when (country) {
            "PK" -> "Rs" to "PKR"
            "US" -> "$" to "USD"
            "GB" -> "£" to "GBP"
            "AE" -> "AED" to "AED"
            "SA" -> "SAR" to "SAR"
            "IN" -> "₹" to "INR"
            "CA" -> "$" to "CAD"
            "AU" -> "$" to "AUD"
            "DE", "FR", "ES", "IT", "NL", "BE", "AT", "GR", "PT", "FI", "IE" -> "€" to "EUR"
            else -> {
                runCatching {
                    val curr = java.util.Currency.getInstance(deviceLocale)
                    curr.symbol to curr.currencyCode
                }.getOrDefault("Rs" to "PKR")
            }
        }
    }
}
