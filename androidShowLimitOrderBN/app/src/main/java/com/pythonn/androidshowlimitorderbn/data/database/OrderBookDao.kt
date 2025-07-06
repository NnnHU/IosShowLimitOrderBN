package com.pythonn.androidshowlimitorderbn.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface OrderBookDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<OrderBookEntryEntity>)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entry: OrderBookEntryEntity)

    // 获取缓存的买盘数据（只返回有效的订单）
    @Query("SELECT * FROM order_book_entries WHERE symbol = :symbol AND isFutures = :isFutures AND isActive = 1 AND isBid = :isBid ORDER BY price DESC")
    suspend fun getCachedBids(symbol: String, isFutures: Boolean, isBid: Boolean = true): List<OrderBookEntryEntity>

    // 获取缓存的卖盘数据（只返回有效的订单）
    @Query("SELECT * FROM order_book_entries WHERE symbol = :symbol AND isFutures = :isFutures AND isActive = 1 AND isBid = :isBid ORDER BY price ASC")
    suspend fun getCachedAsks(symbol: String, isFutures: Boolean, isBid: Boolean = false): List<OrderBookEntryEntity>
    
    // 兼容旧方法
    @Query("SELECT * FROM order_book_entries WHERE symbol = :symbol AND isFutures = :isFutures AND isBid = :isBid ORDER BY price DESC")
    suspend fun getBids(symbol: String, isFutures: Boolean, isBid: Boolean = true): List<OrderBookEntryEntity>

    @Query("SELECT * FROM order_book_entries WHERE symbol = :symbol AND isFutures = :isFutures AND isBid = :isBid ORDER BY price ASC")
    suspend fun getAsks(symbol: String, isFutures: Boolean, isBid: Boolean = false): List<OrderBookEntryEntity>
    
    // 检查是否有缓存数据
    @Query("SELECT COUNT(*) > 0 FROM order_book_entries WHERE symbol = :symbol AND isFutures = :isFutures AND isActive = 1")
    suspend fun hasCachedData(symbol: String, isFutures: Boolean): Boolean
    
    // 增量更新订单数量
    @Query("UPDATE order_book_entries SET quantity = :quantity, lastUpdated = :timestamp, isActive = 1 WHERE id = :id")
    suspend fun updateOrderQuantity(id: String, quantity: Double, timestamp: Long)
    
    // 标记订单为无效（价格为0时）
    @Query("UPDATE order_book_entries SET isActive = 0, lastUpdated = :timestamp WHERE id = :id")
    suspend fun deactivateOrder(id: String, timestamp: Long)
    
    // 批量插入或更新订单
    @Transaction
    suspend fun batchInsertOrUpdate(entries: List<OrderBookEntryEntity>) {
        entries.forEach { entry ->
            insertOrUpdate(entry)
        }
    }
    
    // 清理过期数据（超过1小时的无效订单）
    @Query("DELETE FROM order_book_entries WHERE isActive = 0 AND lastUpdated < :threshold")
    suspend fun cleanupExpiredOrders(threshold: Long)
    
    // 清理特定交易对的所有数据
    @Query("DELETE FROM order_book_entries WHERE symbol = :symbol AND isFutures = :isFutures")
    suspend fun deleteAll(symbol: String, isFutures: Boolean)
    
    // 获取特定交易对的数据统计
    @Query("SELECT COUNT(*) FROM order_book_entries WHERE symbol = :symbol AND isFutures = :isFutures AND isActive = 1")
    suspend fun getActiveOrderCount(symbol: String, isFutures: Boolean): Int

    @Transaction
    suspend fun updateOrderBook(symbol: String, isFutures: Boolean, bids: List<OrderBookEntryEntity>, asks: List<OrderBookEntryEntity>) {
        deleteAll(symbol, isFutures)
        insertAll(bids)
        insertAll(asks)
    }
    
    // 智能更新：如果有缓存则增量更新，否则全量替换
    @Transaction
    suspend fun smartUpdateOrderBook(symbol: String, isFutures: Boolean, bids: List<OrderBookEntryEntity>, asks: List<OrderBookEntryEntity>, isIncremental: Boolean = false) {
        if (isIncremental) {
            // 增量更新
            batchInsertOrUpdate(bids + asks)
        } else {
            // 全量替换
            deleteAll(symbol, isFutures)
            insertAll(bids + asks)
        }
    }
}