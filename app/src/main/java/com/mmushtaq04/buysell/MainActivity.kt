package com.mmushtaq04.buysell

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.LocaleListCompat
import com.mmushtaq04.buysell.data.local.AppDatabase
import com.mmushtaq04.buysell.presentation.navigation.AppNavigation
import com.mmushtaq04.buysell.ui.theme.BuySellTheme
import com.mmushtaq04.buysell.util.AppPreferencesManager
import java.util.Locale

class MainActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        val languageTag = AppPreferencesManager.getAppLanguageTag(newBase)
        val localeList = LocaleListCompat.forLanguageTags(languageTag)
        val locale: Locale = if (!localeList.isEmpty && localeList.get(0) != null) localeList.get(0)!! else Locale.getDefault()
        Locale.setDefault(locale)

        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        config.setLocales(android.os.LocaleList(locale))

        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Apply saved theme preference (System, Light, Dark)
        val themeMode = AppPreferencesManager.getAppThemeMode(this)
        AppPreferencesManager.applyAppThemeMode(themeMode)

        // Initialize local database instance
        AppDatabase.getInstance(this)

        setContent {
            BuySellTheme {
                AppNavigation()
            }
        }
    }
}
