package com.mmushtaq04.buysell.data.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import com.mmushtaq04.buysell.data.local.dao.*
import com.mmushtaq04.buysell.data.local.entity.*

@Database(
    entities = [
        ShopEntity::class,
        UserEntity::class,
        ShopMemberEntity::class,
        InviteEntity::class,
        CategoryEntity::class,
        PartyEntity::class,
        StockItemEntity::class,
        TxnEntity::class,
        TxnLineEntity::class,
        PaymentEntity::class,
        PaymentAccountEntity::class,
        PaymentPromiseEntity::class,
        AttachmentEntity::class,
        ExpenseEntity::class,
        AuditLogEntity::class,
        ConflictEntity::class,
        AppMetaEntity::class,
        SyncOutboxEntity::class,
        SyncCursorEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun shopDao(): ShopDao
    abstract fun categoryDao(): CategoryDao
    abstract fun partyDao(): PartyDao
    abstract fun stockItemDao(): StockItemDao
    abstract fun txnDao(): TxnDao
    abstract fun paymentDao(): PaymentDao
    abstract fun paymentPromiseDao(): PaymentPromiseDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun appMetaDao(): AppMetaDao
    abstract fun syncDao(): SyncDao

    companion object {
        @kotlin.jvm.JvmStatic
        fun getInstance(context: Any): AppDatabase = getAppDatabase(context)
    }
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

expect fun getAppDatabase(context: Any): AppDatabase
