package com.pythonn.androidshowlimitorderbn.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "order_book_entries")
data class OrderBookEntryEntity(
    @PrimaryKey val id: String, // 复合主键：symbol_isFutures_price_isBid
    val symbol: String,
    val isFutures: Boolean,
    val price: Double,
    val quantity: Double,
    val isBid: Boolean, // true for bid, false for ask
    val lastUpdated: Long = System.currentTimeMillis(), // 最后更新时间戳
    val isActive: Boolean = true // 标记订单是否仍然有效
) {
    companion object {
        fun generateId(symbol: String, isFutures: Boolean, price: Double, isBid: Boolean): String {
            return "${symbol}_${isFutures}_${price}_${isBid}"
        }
    }
}