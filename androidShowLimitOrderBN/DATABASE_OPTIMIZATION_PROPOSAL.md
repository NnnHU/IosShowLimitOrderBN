# 数据库优化方案

## 问题分析

当前应用存在以下效率问题：
1. **频繁网络请求**：每次切换交易对都需要重新获取完整的订单簿数据
2. **用户体验差**：切换标签时需要等待数据加载，界面显示空白
3. **资源浪费**：重复获取相同的数据，增加服务器负担和流量消耗
4. **数据不连续**：切换时会丢失之前的数据状态

## 优化方案

### 1. 数据缓存策略优化

#### 1.1 本地数据持久化
- **当前状态**：数据库仅用于临时存储，每次切换都清空重新获取
- **优化方案**：实现智能缓存机制，保留多个交易对的历史数据

#### 1.2 增量更新机制
- **WebSocket增量更新**：只更新变化的订单，而不是替换整个订单簿
- **差异化存储**：记录订单的变化历史，支持数据回溯

### 2. 数据库结构优化

#### 2.1 扩展OrderBookEntryEntity
```kotlin
@Entity(tableName = "order_book_entries")
data class OrderBookEntryEntity(
    @PrimaryKey val id: String, // 使用复合主键：symbol_isFutures_price_isBid
    val symbol: String,
    val isFutures: Boolean,
    val price: Double,
    val quantity: Double,
    val isBid: Boolean,
    val lastUpdated: Long, // 最后更新时间戳
    val isActive: Boolean = true // 标记订单是否仍然有效
)
```

#### 2.2 添加缓存元数据表
```kotlin
@Entity(tableName = "cache_metadata")
data class CacheMetadataEntity(
    @PrimaryKey val key: String, // symbol_isFutures
    val lastFullUpdate: Long, // 最后完整更新时间
    val lastIncrementalUpdate: Long, // 最后增量更新时间
    val dataVersion: Long, // 数据版本号
    val isValid: Boolean = true // 缓存是否有效
)
```

### 3. DAO层优化

#### 3.1 增加智能查询方法
```kotlin
@Dao
interface OrderBookDao {
    // 获取缓存的订单数据（如果存在且有效）
    @Query("SELECT * FROM order_book_entries WHERE symbol = :symbol AND isFutures = :isFutures AND isActive = 1 AND isBid = :isBid ORDER BY price DESC")
    suspend fun getCachedBids(symbol: String, isFutures: Boolean, isBid: Boolean = true): List<OrderBookEntryEntity>
    
    // 检查缓存是否有效（最近5分钟内更新）
    @Query("SELECT COUNT(*) > 0 FROM cache_metadata WHERE key = :key AND lastIncrementalUpdate > :threshold AND isValid = 1")
    suspend fun isCacheValid(key: String, threshold: Long): Boolean
    
    // 增量更新订单
    @Query("UPDATE order_book_entries SET quantity = :quantity, lastUpdated = :timestamp WHERE symbol = :symbol AND isFutures = :isFutures AND price = :price AND isBid = :isBid")
    suspend fun updateOrderQuantity(symbol: String, isFutures: Boolean, price: Double, isBid: Boolean, quantity: Double, timestamp: Long)
    
    // 标记订单为无效（价格为0时）
    @Query("UPDATE order_book_entries SET isActive = 0, lastUpdated = :timestamp WHERE symbol = :symbol AND isFutures = :isFutures AND price = :price AND isBid = :isBid")
    suspend fun deactivateOrder(symbol: String, isFutures: Boolean, price: Double, isBid: Boolean, timestamp: Long)
    
    // 清理过期数据（超过1小时的无效订单）
    @Query("DELETE FROM order_book_entries WHERE isActive = 0 AND lastUpdated < :threshold")
    suspend fun cleanupExpiredOrders(threshold: Long)
}
```

### 4. Repository层优化

#### 4.1 智能数据获取策略
```kotlin
class OrderBookRepository {
    private val cacheValidityDuration = 5 * 60 * 1000L // 5分钟
    
    suspend fun getMarketData(symbol: String, isFutures: Boolean): MarketDepthData? {
        val cacheKey = "${symbol}_${isFutures}"
        val currentTime = System.currentTimeMillis()
        
        // 1. 首先检查缓存是否有效
        if (orderBookDao.isCacheValid(cacheKey, currentTime - cacheValidityDuration)) {
            // 使用缓存数据
            val cachedBids = orderBookDao.getCachedBids(symbol, isFutures, true)
            val cachedAsks = orderBookDao.getCachedBids(symbol, isFutures, false)
            
            if (cachedBids.isNotEmpty() || cachedAsks.isNotEmpty()) {
                return buildMarketDataFromCache(cachedBids, cachedAsks)
            }
        }
        
        // 2. 缓存无效或为空，获取新数据
        return fetchFreshData(symbol, isFutures)
    }
    
    private suspend fun applyIncrementalUpdate(update: OrderBookUpdate) {
        val timestamp = System.currentTimeMillis()
        
        update.bids.forEach { bid ->
            if (bid.quantity == 0.0) {
                orderBookDao.deactivateOrder(update.symbol, update.isFutures, bid.price, true, timestamp)
            } else {
                orderBookDao.updateOrderQuantity(update.symbol, update.isFutures, bid.price, true, bid.quantity, timestamp)
            }
        }
        
        update.asks.forEach { ask ->
            if (ask.quantity == 0.0) {
                orderBookDao.deactivateOrder(update.symbol, update.isFutures, ask.price, false, timestamp)
            } else {
                orderBookDao.updateOrderQuantity(update.symbol, update.isFutures, ask.price, false, ask.quantity, timestamp)
            }
        }
        
        // 更新缓存元数据
        updateCacheMetadata("${update.symbol}_${update.isFutures}", timestamp)
    }
}
```

### 5. ViewModel层优化

#### 5.1 预加载和后台更新
```kotlin
class MarketDataViewModel {
    private val preloadedSymbols = mutableSetOf<String>()
    
    fun switchSymbol(newSymbol: String, threshold: Double = 50.0) {
        viewModelScope.launch {
            // 1. 立即显示缓存数据（如果有）
            val cachedSpotData = repository.getCachedMarketData(newSymbol, false)
            val cachedFuturesData = repository.getCachedMarketData(newSymbol, true)
            
            if (cachedSpotData != null) {
                _spotMarketData.value = cachedSpotData
            }
            if (cachedFuturesData != null) {
                _futuresMarketData.value = cachedFuturesData
            }
            
            // 2. 后台更新数据
            repository.refreshMarketData(newSymbol, threshold)
        }
    }
    
    fun preloadSymbol(symbol: String) {
        if (!preloadedSymbols.contains(symbol)) {
            viewModelScope.launch {
                repository.preloadMarketData(symbol)
                preloadedSymbols.add(symbol)
            }
        }
    }
}
```

### 6. 实施计划

#### 阶段一：数据库结构优化（1-2天）
1. 扩展OrderBookEntryEntity添加时间戳和状态字段
2. 创建CacheMetadataEntity
3. 更新数据库版本和迁移脚本

#### 阶段二：DAO层增强（1天）
1. 添加缓存有效性检查方法
2. 实现增量更新方法
3. 添加数据清理方法

#### 阶段三：Repository层重构（2-3天）
1. 实现智能缓存策略
2. 优化数据获取逻辑
3. 实现增量更新机制

#### 阶段四：ViewModel层优化（1天）
1. 添加预加载功能
2. 实现即时缓存显示
3. 优化用户体验

#### 阶段五：测试和优化（1-2天）
1. 单元测试
2. 性能测试
3. 用户体验测试

### 7. 预期效果

#### 7.1 性能提升
- **切换速度**：从2-3秒降低到<500ms
- **网络请求**：减少70-80%的重复请求
- **数据连续性**：保持历史数据，支持快速回溯

#### 7.2 用户体验改善
- **即时响应**：切换标签立即显示缓存数据
- **平滑过渡**：数据更新时保持界面稳定
- **离线支持**：网络不佳时仍可查看缓存数据

#### 7.3 资源优化
- **内存使用**：智能清理过期数据
- **存储空间**：压缩存储，定期清理
- **电池续航**：减少网络请求，降低功耗

### 8. 风险评估

#### 8.1 技术风险
- **数据一致性**：需要确保缓存与实时数据的同步
- **存储空间**：长期使用可能积累大量数据
- **复杂性增加**：代码逻辑变得更复杂

#### 8.2 缓解措施
- **版本控制**：使用数据版本号确保一致性
- **定期清理**：自动清理过期和无效数据
- **分层设计**：保持代码模块化和可测试性

## 总结

这个优化方案将显著提升应用的性能和用户体验，通过智能缓存和增量更新机制，实现快速响应和数据连续性。建议按阶段实施，确保每个阶段都经过充分测试后再进入下一阶段。