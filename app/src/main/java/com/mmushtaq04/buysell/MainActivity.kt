package com.mmushtaq04.buysell

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mmushtaq04.buysell.data.local.AppDatabase
import com.mmushtaq04.buysell.presentation.navigation.AppNavigation
import com.mmushtaq04.buysell.ui.theme.BuySellTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize local database instance
        AppDatabase.getInstance(this)

        setContent {
            BuySellTheme {
                AppNavigation()
            }
        }
    }
}
