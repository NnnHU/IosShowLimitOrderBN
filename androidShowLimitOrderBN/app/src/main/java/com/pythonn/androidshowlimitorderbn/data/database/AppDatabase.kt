package com.pythonn.androidshowlimitorderbn.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [OrderBookEntryEntity::class, CacheMetadataEntity::class], 
    version = 2, 
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun orderBookDao(): OrderBookDao
    abstract fun cacheMetadataDao(): CacheMetadataDao
    
    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 创建新的order_book_entries表结构
                database.execSQL("""
                    CREATE TABLE order_book_entries_new (
                        id TEXT PRIMARY KEY NOT NULL,
                        symbol TEXT NOT NULL,
                        isFutures INTEGER NOT NULL,
                        price REAL NOT NULL,
                        quantity REAL NOT NULL,
                        isBid INTEGER NOT NULL,
                        lastUpdated INTEGER NOT NULL,
                        isActive INTEGER NOT NULL DEFAULT 1
                    )
                """)
                
                // 迁移现有数据
                database.execSQL("""
                    INSERT INTO order_book_entries_new (id, symbol, isFutures, price, quantity, isBid, lastUpdated, isActive)
                    SELECT 
                        symbol || '_' || isFutures || '_' || price || '_' || isBid as id,
                        symbol, isFutures, price, quantity, isBid, 
                        ${System.currentTimeMillis()} as lastUpdated,
                        1 as isActive
                    FROM order_book_entries
                """)
                
                // 删除旧表，重命名新表
                database.execSQL("DROP TABLE order_book_entries")
                database.execSQL("ALTER TABLE order_book_entries_new RENAME TO order_book_entries")
                
                // 创建缓存元数据表
                database.execSQL("""
                    CREATE TABLE cache_metadata (
                        key TEXT PRIMARY KEY NOT NULL,
                        lastFullUpdate INTEGER NOT NULL,
                        lastIncrementalUpdate INTEGER NOT NULL,
                        dataVersion INTEGER NOT NULL,
                        isValid INTEGER NOT NULL DEFAULT 1
                    )
                """)
            }
        }
    }
}