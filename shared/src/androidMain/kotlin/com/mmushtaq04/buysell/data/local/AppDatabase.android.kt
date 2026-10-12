package com.mmushtaq04.buysell.data.local

import android.content.Context
import androidx.room.Room

private var INSTANCE: AppDatabase? = null

actual fun getAppDatabase(context: Any): AppDatabase {
    return INSTANCE ?: synchronized(AppDatabase::class) {
        val androidContext = context as Context
        val instance = Room.databaseBuilder(
            androidContext.applicationContext,
            AppDatabase::class.java,
            "hafeez_center_db"
        ).fallbackToDestructiveMigration().build()
        INSTANCE = instance
        instance
    }
}
