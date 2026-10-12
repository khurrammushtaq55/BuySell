package com.mmushtaq04.buysell.data.local

actual fun getAppDatabase(context: Any): AppDatabase {
    error("iOS AppDatabase instance factory")
}
