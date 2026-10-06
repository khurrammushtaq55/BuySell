package com.mmushtaq04.buysell.util

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object LocaleManagerUtil {

    val supportedLanguages = listOf(
        "System Default (Mobile Locale)",
        "Roman Urdu / رومن اردو",
        "Urdu / اردو",
        "English",
        "Spanish / Español",
        "French / Français",
        "Hindi / हिंदी",
        "Arabic / العربية",
        "Chinese / 简体中文"
    )

    fun getLanguageTagForDisplayName(displayName: String): String? {
        return when (displayName) {
            "Roman Urdu / رومن اردو" -> "ur-Latn"
            "Urdu / اردو" -> "ur"
            "English" -> "en"
            "Spanish / Español" -> "es"
            "French / Français" -> "fr"
            "Hindi / हिंदी" -> "hi"
            "Arabic / العربية" -> "ar"
            "Chinese / 简体中文" -> "zh-Hans"
            else -> null // System Default
        }
    }

    fun applyLocale(languageTag: String?) {
        if (languageTag.isNullOrBlank() || languageTag == "SYSTEM") {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
        } else {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageTag))
        }
    }
}
