package com.mmushtaq04.buysell.data.local

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import platform.Foundation.NSHomeDirectory

private var INSTANCE: AppDatabase? = null

actual fun getAppDatabase(context: Any): AppDatabase {
    return INSTANCE ?: run {
        val dbFile = NSHomeDirectory() + "/Documents/hafeez_center_db.db"
        val instance = Room.databaseBuilder<AppDatabase>(
            name = dbFile,
            factory = { AppDatabaseConstructor.initialize() }
        )
            .setDriver(BundledSQLiteDriver())
            .build()
        INSTANCE = instance
        instance
    }
}
