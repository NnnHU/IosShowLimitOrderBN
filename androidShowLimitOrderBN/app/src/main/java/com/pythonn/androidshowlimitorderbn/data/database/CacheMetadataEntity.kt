package com.pythonn.androidshowlimitorderbn.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cache_metadata")
data class CacheMetadataEntity(
    @PrimaryKey val key: String, // symbol_isFutures
    val lastFullUpdate: Long, // 最后完整更新时间
    val lastIncrementalUpdate: Long, // 最后增量更新时间
    val dataVersion: Long, // 数据版本号
    val isValid: Boolean = true // 缓存是否有效
) {
    companion object {
        fun generateKey(symbol: String, isFutures: Boolean): String {
            return "${symbol}_${isFutures}"
        }
    }
}