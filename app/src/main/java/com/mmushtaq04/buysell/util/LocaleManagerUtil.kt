package com.mmushtaq04.buysell.util

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object LocaleManagerUtil {

    val supportedLanguages: List<String>
        get() = listOf("System Default (Mobile Locale)") + AppPreferencesManager.supportedLanguages.map { it.displayName }

    fun getLanguageTagForDisplayName(displayName: String): String? {
        val matched = AppPreferencesManager.supportedLanguages.find { it.displayName.equals(displayName, ignoreCase = true) }
        return matched?.tag
    }

    fun applyLocale(languageTag: String?) {
        if (languageTag.isNullOrBlank() || languageTag == "SYSTEM") {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
        } else {
            val cleanTag = when (languageTag) {
                "b+ur+Latn" -> "ur-Latn"
                "zh-Hans" -> "zh-CN"
                else -> languageTag
            }
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(cleanTag))
        }
    }
}
