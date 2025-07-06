package com.pythonn.androidshowlimitorderbn.data.repository

import com.pythonn.androidshowlimitorderbn.data.database.OrderBookDao
import com.pythonn.androidshowlimitorderbn.data.database.OrderBookEntryEntity
import com.pythonn.androidshowlimitorderbn.data.database.CacheMetadataDao
import com.pythonn.androidshowlimitorderbn.data.database.CacheMetadataEntity
import com.pythonn.androidshowlimitorderbn.data.models.MarketDepthData
import com.pythonn.androidshowlimitorderbn.data.models.OrderBookEntry
import com.pythonn.androidshowlimitorderbn.data.remote.BinanceApiServiceImpl
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

// Extension functions for conversion
fun OrderBookEntry.toEntity(symbol: String, isFutures: Boolean, isBid: Boolean): OrderBookEntryEntity {
    val id = OrderBookEntryEntity.generateId(symbol, isFutures, price, isBid)
    return OrderBookEntryEntity(
        id = id,
        symbol = symbol, 
        isFutures = isFutures, 
        price = price, 
        quantity = quantity, 
        isBid = isBid,
        lastUpdated = System.currentTimeMillis(),
        isActive = true
    )
}

fun OrderBookEntryEntity.toModel(): OrderBookEntry {
    return OrderBookEntry(price = price, quantity = quantity)
}

class OrderBookRepository(
    private val apiService: BinanceApiServiceImpl,
    private val orderBookDao: OrderBookDao,
    private val cacheMetadataDao: CacheMetadataDao
) {
    
    private val cacheValidityDuration = 5 * 60 * 1000L // 5分钟缓存有效期
    private val cleanupInterval = 60 * 60 * 1000L // 1小时清理间隔

    val spotMarketData: StateFlow<MarketDepthData?> = apiService.spotMarketData
    val futuresMarketData: StateFlow<MarketDepthData?> = apiService.futuresMarketData

    suspend fun fetchAndCacheOrderBookSnapshot(symbol: String, isFutures: Boolean) {
        try {
            val snapshot = apiService.fetchOrderBookSnapshot(symbol, isFutures)
            val bids = snapshot.bids.map { it.toEntity(symbol, isFutures, true) }
            val asks = snapshot.asks.map { it.toEntity(symbol, isFutures, false) }
            orderBookDao.updateOrderBook(symbol, isFutures, bids, asks)
        } catch (e: Exception) {
            println("Error fetching and caching snapshot: ${e.localizedMessage}")
            // Optionally, load from cache if network fails
        }
    }

    fun startWebSocketStream(symbol: String, isFutures: Boolean) {
        apiService.startWebSocketStream(symbol, isFutures)
    }

    fun switchSymbol(newSymbol: String, threshold: Double = 50.0) {
        apiService.switchSymbol(newSymbol, threshold)
    }

    fun stopWebSocketStream() {
        apiService.stopWebSocketStream()
    }

    suspend fun getCachedBids(symbol: String, isFutures: Boolean): List<OrderBookEntry> {
        return orderBookDao.getBids(symbol, isFutures).map { it.toModel() }
    }

    suspend fun getCachedAsks(symbol: String, isFutures: Boolean): List<OrderBookEntry> {
        return orderBookDao.getAsks(symbol, isFutures).map { it.toModel() }
    }
    
    // 智能获取市场数据：优先使用缓存，缓存无效时获取新数据
    suspend fun getMarketDataSmart(symbol: String, isFutures: Boolean): MarketDepthData? {
        val cacheKey = CacheMetadataEntity.generateKey(symbol, isFutures)
        val currentTime = System.currentTimeMillis()
        
        // 检查缓存是否有效
        if (cacheMetadataDao.isCacheValid(cacheKey, currentTime - cacheValidityDuration)) {
            val cachedBids = orderBookDao.getCachedBids(symbol, isFutures, true)
            val cachedAsks = orderBookDao.getCachedAsks(symbol, isFutures, false)
            
            if (cachedBids.isNotEmpty() || cachedAsks.isNotEmpty()) {
                return buildMarketDataFromCache(cachedBids, cachedAsks, symbol, isFutures)
            }
        }
        
        // 缓存无效或为空，返回null让调用者决定是否获取新数据
        return null
    }
    
    // 预加载市场数据
    suspend fun preloadMarketData(symbol: String) {
        try {
            // 预加载现货和期货数据
            fetchAndCacheOrderBookSnapshot(symbol, false)
            fetchAndCacheOrderBookSnapshot(symbol, true)
        } catch (e: Exception) {
            // 预加载失败不影响主流程
        }
    }
    
    // 刷新市场数据（后台更新）
    suspend fun refreshMarketData(symbol: String, threshold: Double) {
        try {
            apiService.switchSymbol(symbol, threshold)
        } catch (e: Exception) {
            // 刷新失败不影响缓存数据的显示
        }
    }
    
    // 从缓存构建MarketDepthData
    private suspend fun buildMarketDataFromCache(
        cachedBids: List<OrderBookEntryEntity>, 
        cachedAsks: List<OrderBookEntryEntity>,
        symbol: String,
        isFutures: Boolean
    ): MarketDepthData {
        val bids = cachedBids.map { it.toModel() }
        val asks = cachedAsks.map { it.toModel() }
        
        // 计算当前价格（买一和卖一的中间价）
        val currentPrice = if (bids.isNotEmpty() && asks.isNotEmpty()) {
            (bids.first().price + asks.first().price) / 2.0
        } else {
            0.0
        }
        
        // 计算价差
        val spread = if (bids.isNotEmpty() && asks.isNotEmpty()) {
            asks.first().price - bids.first().price
        } else {
            0.0
        }
        
        // 计算最大数量（用于图表缩放）
        val allQuantities = (bids + asks).map { it.quantity }
        val maxQuantity = allQuantities.maxOrNull() ?: 1.0
        
        // 过滤大单（这里使用简单的阈值，实际应该根据业务需求调整）
        val bigOrderThreshold = maxQuantity * 0.1 // 10%以上的订单视为大单
        val bigOrders = (bids + asks).filter { it.quantity >= bigOrderThreshold }
        
        return MarketDepthData(
            symbol = symbol,
            isFutures = isFutures,
            bids = bids,
            asks = asks,
            currentPrice = currentPrice,
            spread = spread,
            maxQuantity = maxQuantity,
            buySellRatio = emptyList(), // 这个需要单独计算，暂时为空
            bigOrders = bigOrders
        )
    }
    
    // 智能更新订单簿（支持增量更新）
    suspend fun smartUpdateOrderBook(
        symbol: String, 
        isFutures: Boolean, 
        bids: List<OrderBookEntry>, 
        asks: List<OrderBookEntry>,
        isIncremental: Boolean = false
    ) {
        val currentTime = System.currentTimeMillis()
        val cacheKey = CacheMetadataEntity.generateKey(symbol, isFutures)
        
        val bidEntities = bids.map { it.toEntity(symbol, isFutures, true) }
        val askEntities = asks.map { it.toEntity(symbol, isFutures, false) }
        
        // 更新订单簿数据
        orderBookDao.smartUpdateOrderBook(symbol, isFutures, bidEntities, askEntities, isIncremental)
        
        // 更新缓存元数据
        val metadata = cacheMetadataDao.getMetadata(cacheKey) ?: CacheMetadataEntity(
            key = cacheKey,
            lastFullUpdate = if (!isIncremental) currentTime else 0L,
            lastIncrementalUpdate = currentTime,
            dataVersion = currentTime,
            isValid = true
        )
        
        val updatedMetadata = if (isIncremental) {
            metadata.copy(
                lastIncrementalUpdate = currentTime,
                dataVersion = currentTime
            )
        } else {
            metadata.copy(
                lastFullUpdate = currentTime,
                lastIncrementalUpdate = currentTime,
                dataVersion = currentTime
            )
        }
        
        cacheMetadataDao.insertOrUpdate(updatedMetadata)
        
        // 定期清理过期数据
        if (currentTime % cleanupInterval < 1000) {
            cleanupExpiredData()
        }
    }
    
    // 清理过期数据
    private suspend fun cleanupExpiredData() {
        val threshold = System.currentTimeMillis() - cleanupInterval
        orderBookDao.cleanupExpiredOrders(threshold)
    }
    
    // 检查是否有缓存数据
    suspend fun hasCachedData(symbol: String, isFutures: Boolean): Boolean {
        return orderBookDao.hasCachedData(symbol, isFutures)
    }
    
    // 获取缓存统计信息
    suspend fun getCacheStats(symbol: String, isFutures: Boolean): Pair<Int, Boolean> {
        val count = orderBookDao.getActiveOrderCount(symbol, isFutures)
        val cacheKey = CacheMetadataEntity.generateKey(symbol, isFutures)
        val isValid = cacheMetadataDao.isCacheValid(cacheKey, System.currentTimeMillis() - cacheValidityDuration)
        return Pair(count, isValid)
    }
}