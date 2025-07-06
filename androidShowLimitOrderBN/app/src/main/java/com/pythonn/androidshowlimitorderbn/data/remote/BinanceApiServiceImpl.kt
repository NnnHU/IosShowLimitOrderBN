package com.pythonn.androidshowlimitorderbn.data.remote

import com.google.gson.GsonBuilder
import com.pythonn.androidshowlimitorderbn.data.api.BinanceApiService
import com.pythonn.androidshowlimitorderbn.data.models.DepthUpdateEvent
import com.pythonn.androidshowlimitorderbn.data.models.MarketDepthData
import com.pythonn.androidshowlimitorderbn.data.models.OrderBookSnapshot
import com.pythonn.androidshowlimitorderbn.data.models.PriceRangeRatio
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.math.pow
import kotlin.random.Random

// Connection status data class
data class ConnectionStatus(
    val isConnected: Boolean,
    val lastUpdateTime: Long,
    val reconnectAttempts: Int,
    val dataQuality: DataQuality,
    val symbol: String,
    val isFutures: Boolean
)

enum class DataQuality {
    REAL_TIME,    // Real-time data
    CACHED,       // Cached data
    DEGRADED      // Degraded data
}

class BinanceApiServiceImpl {

    private val spotBaseUrl = "https://api.binance.com/api/v3/"
    private val futuresBaseUrl = "https://fapi.binance.com/fapi/v1/"

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val webSocketClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS) // Disable read timeout for websockets
        .connectTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .dns(object : okhttp3.Dns {
            override fun lookup(hostname: String): List<InetAddress> {
                return try {
                    println("Attempting DNS lookup for: $hostname")
                    val addresses = InetAddress.getAllByName(hostname).toList()
                    println("DNS lookup successful for $hostname: ${addresses.map { it.hostAddress }}")
                    addresses
                } catch (e: Exception) {
                    println("DNS resolution failed for $hostname: ${e.localizedMessage}")
                    
                    // Try alternative DNS servers
                    val fallbackAddresses = tryAlternativeDns(hostname)
                    if (fallbackAddresses.isNotEmpty()) {
                        println("Fallback DNS successful for $hostname: ${fallbackAddresses.map { it.hostAddress }}")
                        return fallbackAddresses
                    }
                    
                    // If all DNS fails, try hardcoded IPs as last resort
                    val hardcodedIps = getHardcodedIps(hostname)
                    if (hardcodedIps.isNotEmpty()) {
                        println("Using hardcoded IPs for $hostname: ${hardcodedIps.map { it.hostAddress }}")
                        return hardcodedIps
                    }
                    
                    println("All DNS resolution methods failed for $hostname")
                    throw e
                }
            }
        })
        .build()

    private val gson = GsonBuilder().create()

    private val spotRetrofit: Retrofit = Retrofit.Builder()
        .baseUrl(spotBaseUrl)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    private val futuresRetrofit: Retrofit = Retrofit.Builder()
        .baseUrl(futuresBaseUrl)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    private val spotApiService: BinanceApiService = spotRetrofit.create(BinanceApiService::class.java)
    private val futuresApiService: BinanceApiService = futuresRetrofit.create(BinanceApiService::class.java)

    private val managers: ConcurrentHashMap<String, OrderBookManager> = ConcurrentHashMap()

    private var spotWebSocket: WebSocket? = null
    private var futuresWebSocket: WebSocket? = null
    private var currentSpotSymbol: String? = null
    private var currentFuturesSymbol: String? = null

    private val _spotMarketData = MutableStateFlow<MarketDepthData?>(null)
    val spotMarketData: StateFlow<MarketDepthData?> = _spotMarketData

    private val _futuresMarketData = MutableStateFlow<MarketDepthData?>(null)
    val futuresMarketData: StateFlow<MarketDepthData?> = _futuresMarketData

    // 数据稳定性控制
    private var lastSpotPublishTime = 0L
    private var lastFuturesPublishTime = 0L
    private val minPublishInterval = 200L // 最小发布间隔200ms，提高响应速度

    private val scope = CoroutineScope(Dispatchers.IO)
    private val reconnectAttempts: ConcurrentHashMap<String, Int> = ConcurrentHashMap()
    private val maxReconnectAttempts = 5
    private val reconnectDelays = listOf(1000L, 2000L, 5000L, 10000L, 30000L)
    
    // Connection status tracking
    private val _connectionStatus = MutableStateFlow<ConnectionStatus?>(null)
    val connectionStatus: StateFlow<ConnectionStatus?> = _connectionStatus
    
    // Adaptive threshold management
    private val adaptiveThresholds: ConcurrentHashMap<String, Double> = ConcurrentHashMap()

    private val ANALYSIS_RANGES: List<Pair<Double, Double>> = listOf(
        Pair(0.0, 1.0),
        Pair(1.0, 2.5),
        Pair(2.5, 5.0),
        Pair(5.0, 10.0)
    )

    init {
        // Initialize with default symbols if needed
        managers["BTCUSDT_SPOT"] = OrderBookManager(symbol = "BTCUSDT", isFutures = false, minQuantity = 50.0)
        managers["BTCUSDT_FUTURES"] = OrderBookManager(symbol = "BTCUSDT", isFutures = true, minQuantity = 50.0)
    }
    
    private fun tryAlternativeDns(hostname: String): List<InetAddress> {
        val alternativeDnsServers = listOf("8.8.8.8", "8.8.4.4", "1.1.1.1", "1.0.0.1")
        
        for (dnsServer in alternativeDnsServers) {
            try {
                println("Trying alternative DNS server $dnsServer for $hostname")
                // This is a simplified approach - in a real implementation you'd use a proper DNS client
                val addresses = InetAddress.getAllByName(hostname).toList()
                if (addresses.isNotEmpty()) {
                    return addresses
                }
            } catch (e: Exception) {
                println("Alternative DNS $dnsServer failed for $hostname: ${e.localizedMessage}")
            }
        }
        return emptyList()
    }
    
    private fun getHardcodedIps(hostname: String): List<InetAddress> {
        // These are example IPs - in production you'd need to get current IPs
        // Note: Binance IPs change frequently, so this is just for emergency fallback
        val hardcodedMappings = mapOf(
            "api.binance.com" to listOf("76.223.126.88"),
            "fapi.binance.com" to listOf("76.223.126.88"),
            "stream.binance.com" to listOf("76.223.126.88"),
            "fstream.binance.com" to listOf("76.223.126.88")
        )
        
        return hardcodedMappings[hostname]?.mapNotNull { ip ->
            try {
                InetAddress.getByName(ip)
            } catch (e: Exception) {
                println("Failed to create InetAddress for hardcoded IP $ip: ${e.localizedMessage}")
                null
            }
        } ?: emptyList()
    }

    suspend fun fetchOrderBookSnapshot(symbol: String, isFutures: Boolean, limit: Int = 1000): OrderBookSnapshot {
        return if (isFutures) {
            futuresApiService.getFuturesOrderBookSnapshot(symbol, limit)
        } else {
            spotApiService.getSpotOrderBookSnapshot(symbol, limit)
        }
    }

    fun startWebSocketStream(symbol: String, isFutures: Boolean) {
        val streamName = symbol.lowercase() + "@depth"
        // Try alternative URLs if primary fails
        val baseUrl = if (isFutures) {
            "wss://fstream.binance.com/ws"
        } else {
            // Use port 443 as fallback for better firewall compatibility
            "wss://stream.binance.com/ws"
        }
        val urlString = "$baseUrl/$streamName"

        val request = Request.Builder()
            .url(urlString)
            .addHeader("User-Agent", "AndroidApp/1.0")
            .build()

        val listener = BinanceWebSocketListener(
            onMessageReceived = { event ->
                processWebSocketMessage(event, isFutures)
            },
            onConnectionOpened = {
                val key = if (isFutures) "futures" else "spot"
                reconnectAttempts[key] = 0
                // Update connection status
                _connectionStatus.value = ConnectionStatus(
                    isConnected = true,
                    lastUpdateTime = System.currentTimeMillis(),
                    reconnectAttempts = 0,
                    dataQuality = DataQuality.REAL_TIME,
                    symbol = symbol,
                    isFutures = isFutures
                )
                println("WebSocket opened for $symbol (isFutures: $isFutures)")
            },
            onConnectionClosed = {
                println("WebSocket closed for $symbol (isFutures: $isFutures)")
            },
            onFailure = { t, response ->
                println("WebSocket failure for $symbol (isFutures: $isFutures): ${t.localizedMessage}")
                println("Error type: ${t.javaClass.simpleName}")
                if (t.message?.contains("No address associated with hostname") == true) {
                    println("DNS resolution failed. Check internet connection and DNS settings.")
                } else if (t.message?.contains("timeout") == true) {
                    println("Connection timeout. Network may be slow or blocked.")
                } else if (t.message?.contains("refused") == true) {
                    println("Connection refused. Server may be down or port blocked.")
                }
                response?.let { println("Response code: ${it.code}, message: ${it.message}") }
                reconnectWebSocketStream(isFutures)
            }
        )

        if (isFutures) {
            futuresWebSocket?.cancel()
            futuresWebSocket = webSocketClient.newWebSocket(request, listener)
            currentFuturesSymbol = symbol
        } else {
            spotWebSocket?.cancel()
            spotWebSocket = webSocketClient.newWebSocket(request, listener)
            currentSpotSymbol = symbol
        }
        println("Starting WebSocket stream for $symbol (isFutures: $isFutures) at $urlString")
    }

    // Adaptive threshold calculation
    private fun getAdaptiveThreshold(symbol: String): Double {
        val baseThreshold = 50.0
        val cachedThreshold = adaptiveThresholds[symbol]
        
        if (cachedThreshold != null) {
            return cachedThreshold
        }
        
        // For now, use base threshold. In future, can analyze market volume
        val adaptiveThreshold = baseThreshold
        adaptiveThresholds[symbol] = adaptiveThreshold
        return adaptiveThreshold
    }
    
    // Enhanced error handling for network issues
    private fun handleNetworkError(symbol: String, isFutures: Boolean, error: Throwable) {
        println("Network error for $symbol (isFutures: $isFutures): ${error.localizedMessage}")
        
        // Update connection status
        _connectionStatus.value = ConnectionStatus(
            isConnected = false,
            lastUpdateTime = System.currentTimeMillis(),
            reconnectAttempts = reconnectAttempts.getOrDefault(if (isFutures) "futures" else "spot", 0),
            dataQuality = DataQuality.DEGRADED,
            symbol = symbol,
            isFutures = isFutures
        )
        
        // Attempt to use cached data if available
        scope.launch {
            // Future implementation: load from cache
            // val cachedData = getCachedMarketData(symbol, isFutures)
            // if (cachedData != null && isDataFresh(cachedData)) {
            //     publishCachedData(cachedData, isOffline = true)
            // }
        }
    }

    fun switchSymbol(newSymbol: String, threshold: Double = 50.0) {
        println("Switching symbol to: $newSymbol with threshold: $threshold")

        val adaptiveThreshold = if (threshold == 50.0) getAdaptiveThreshold(newSymbol) else threshold
        println("BinanceApiServiceImpl: Using adaptive threshold: $adaptiveThreshold for symbol: $newSymbol")

        val spotManagerKey = "${newSymbol.uppercase()}_SPOT"
        val futuresManagerKey = "${newSymbol.uppercase()}_FUTURES"

        val spotManagerExists = managers.containsKey(spotManagerKey)
        val futuresManagerExists = managers.containsKey(futuresManagerKey)

        // Only disconnect and reconnect if the symbol is actually changing
        val spotSymbolChanged = currentSpotSymbol != newSymbol
        val futuresSymbolChanged = currentFuturesSymbol != newSymbol

        if (spotSymbolChanged) {
            spotWebSocket?.cancel()
            _spotMarketData.value = null // Clear data for old symbol
            currentSpotSymbol = newSymbol
            managers.remove(spotManagerKey) // Remove old manager
        }
        if (futuresSymbolChanged) {
            futuresWebSocket?.cancel()
            _futuresMarketData.value = null // Clear data for old symbol
            currentFuturesSymbol = newSymbol
            managers.remove(futuresManagerKey) // Remove old manager
        }

        // Initialize or update managers
        managers.computeIfAbsent(spotManagerKey) {
            OrderBookManager(symbol = newSymbol, isFutures = false, minQuantity = adaptiveThreshold)
        }.minQuantity = adaptiveThreshold // Update threshold for existing manager
        managers.computeIfAbsent(futuresManagerKey) {
            OrderBookManager(symbol = newSymbol, isFutures = true, minQuantity = adaptiveThreshold)
        }.minQuantity = adaptiveThreshold // Update threshold for existing manager

        scope.launch(Dispatchers.IO) {
            // Fetch and set initial snapshots if symbol changed or manager was just created
            if (spotSymbolChanged || !spotManagerExists) {
                try {
                    println("Attempting to fetch spot snapshot for $newSymbol")
                    val spotSnapshot = fetchOrderBookSnapshot(newSymbol, false)
                    println("Successfully fetched spot snapshot for $newSymbol with ${spotSnapshot.bids.size} bids and ${spotSnapshot.asks.size} asks")
                    managers[spotManagerKey]?.setInitialOrderBook(spotSnapshot)
                    publishMarketData(managers[spotManagerKey]!!)
                    println("Fetched initial spot snapshot for $newSymbol, lastUpdateId: ${managers[spotManagerKey]?.lastUpdateId}")
                } catch (e: Exception) {
                    println("Error fetching spot snapshot: ${e.localizedMessage}")
                    println("Exception type: ${e.javaClass.simpleName}")
                    handleNetworkError(newSymbol, false, e)
                    e.printStackTrace()
                }
            }

            if (futuresSymbolChanged || !futuresManagerExists) {
                try {
                    println("Attempting to fetch futures snapshot for $newSymbol")
                    val futuresSnapshot = fetchOrderBookSnapshot(newSymbol, true)
                    println("Successfully fetched futures snapshot for $newSymbol with ${futuresSnapshot.bids.size} bids and ${futuresSnapshot.asks.size} asks")
                    managers[futuresManagerKey]?.setInitialOrderBook(futuresSnapshot)
                    publishMarketData(managers[futuresManagerKey]!!)
                    println("Fetched initial futures snapshot for $newSymbol, lastUpdateId: ${managers[futuresManagerKey]?.lastUpdateId}")
                } catch (e: Exception) {
                    println("Error fetching futures snapshot: ${e.localizedMessage}")
                    println("Exception type: ${e.javaClass.simpleName}")
                    handleNetworkError(newSymbol, true, e)
                    e.printStackTrace()
                }
            }

            // Start WebSockets only if symbol changed or they were not running
            if (spotSymbolChanged || spotWebSocket == null || spotWebSocket?.request()?.url?.host != webSocketClient.newBuilder().build().newWebSocket(Request.Builder().url("wss://stream.binance.com/ws/${newSymbol.lowercase()}@depth").build(), object : okhttp3.WebSocketListener(){}).request().url.host) {
                startWebSocketStream(newSymbol, false)
            }
            if (futuresSymbolChanged || futuresWebSocket == null || futuresWebSocket?.request()?.url?.host != webSocketClient.newBuilder().build().newWebSocket(Request.Builder().url("wss://fstream.binance.com/ws/${newSymbol.lowercase()}@depth").build(), object : okhttp3.WebSocketListener(){}).request().url.host) {
                startWebSocketStream(newSymbol, true)
            }
        }
    }

    fun stopWebSocketStream() {
        spotWebSocket?.cancel()
        futuresWebSocket?.cancel()
        println("All WebSocket connections disconnected.")
    }

    private fun reconnectWebSocketStream(isFutures: Boolean) {
        val key = if (isFutures) "futures" else "spot"
        val attempts = reconnectAttempts.getOrDefault(key, 0)
        val symbol = if (isFutures) currentFuturesSymbol else currentSpotSymbol

        if (attempts >= maxReconnectAttempts) {
            println("Max reconnect attempts reached for $key")
            // Update connection status to indicate failure
            symbol?.let {
                _connectionStatus.value = ConnectionStatus(
                    isConnected = false,
                    lastUpdateTime = System.currentTimeMillis(),
                    reconnectAttempts = attempts,
                    dataQuality = DataQuality.DEGRADED,
                    symbol = it,
                    isFutures = isFutures
                )
            }
            return
        }

        reconnectAttempts[key] = attempts + 1

        // Use improved backoff strategy with jitter
        val baseDelay = reconnectDelays.getOrElse(attempts) { 30000L }
        val jitter = Random.nextLong(0, 1000L)
        val delayMillis = baseDelay + jitter

        scope.launch {
            delay(delayMillis)
            if (isFutures) {
                currentFuturesSymbol?.let { symbol ->
                    println("Reconnecting Futures WebSocket (attempt ${attempts + 1})...")
                    startWebSocketStream(symbol, true)
                }
            } else {
                currentSpotSymbol?.let { symbol ->
                    println("Reconnecting Spot WebSocket (attempt ${attempts + 1})...")
                    startWebSocketStream(symbol, false)
                }
            }
        }
    }



    private fun processWebSocketMessage(event: DepthUpdateEvent, isFutures: Boolean) {
        val managerKey = "${event.symbol.uppercase()}_${if (isFutures) "FUTURES" else "SPOT"}"
        managers[managerKey]?.let { manager ->
            manager.applyUpdate(event.getBidsAsOrderBookEntries(), event.getAsksAsOrderBookEntries(), event.finalUpdateId)
            publishMarketData(manager)
        }
    }

    private fun publishMarketData(manager: OrderBookManager) {
        val currentTime = System.currentTimeMillis()
        
        // 增加发布时间间隔，减少UI跳动
        val publishInterval = if (manager.isFutures) 500L else 300L // 期货500ms，现货300ms
        
        if (manager.isFutures) {
            if (currentTime - lastFuturesPublishTime < publishInterval) {
                return
            }
            lastFuturesPublishTime = currentTime
        } else {
            if (currentTime - lastSpotPublishTime < publishInterval) {
                return
            }
            lastSpotPublishTime = currentTime
        }
        
        println("Publishing market data for ${manager.symbol} (isFutures: ${manager.isFutures})")
        val (filteredBids, filteredAsks) = manager.getFilteredOrders(100)
        val currentPrice = manager.getCurrentPrice()
        
        // 添加详细的数据状态日志
        println("Data status - Current price: $currentPrice, Filtered bids: ${filteredBids.size}, Filtered asks: ${filteredAsks.size}")
        if (currentPrice == null) {
            println("WARNING: Current price is null - this may cause ratio calculation issues")
        }

        val allQuantities = (filteredBids + filteredAsks).map { it.quantity }
        val maxQuantity = allQuantities.maxOrNull() ?: 1.0

        val calculatedRatios = ANALYSIS_RANGES.map { (lower, upper) ->
            manager.calculateDepthRatioRange(lower, upper) ?: PriceRangeRatio(
                range = "${lower}-${upper}%",
                ratio = 0.0,
                bidsVolume = 0.0,
                asksVolume = 0.0,
                delta = 0.0
            )
        }
        
        // 检查数据是否有实质性变化（包括比率变化检测）
        val currentData = if (manager.isFutures) _futuresMarketData.value else _spotMarketData.value
        if (currentData != null && currentPrice != null && currentData.symbol == manager.symbol && !hasSignificantChange(currentData, filteredBids, filteredAsks, currentPrice, calculatedRatios)) {
            // 数据变化不大，跳过更新以减少UI跳动
            return
        }

        val marketData = MarketDepthData(
            symbol = manager.symbol,
            isFutures = manager.isFutures,
            bids = filteredBids,
            asks = filteredAsks,
            currentPrice = currentPrice ?: 0.0,
            spread = manager.getSpread() ?: 0.0,
            maxQuantity = maxQuantity,
            buySellRatio = calculatedRatios,
            bigOrders = manager.getBigOrders()
        )

        if (manager.isFutures) {
            _futuresMarketData.value = marketData
            println("BinanceApiServiceImpl: _futuresMarketData updated. Value: ${_futuresMarketData.value?.symbol}, Bids: ${_futuresMarketData.value?.bids?.size}, Asks: ${_futuresMarketData.value?.asks?.size}")
        } else {
            _spotMarketData.value = marketData
            println("BinanceApiServiceImpl: _spotMarketData updated. Value: ${_spotMarketData.value?.symbol}, Bids: ${_spotMarketData.value?.bids?.size}, Asks: ${_spotMarketData.value?.asks?.size}")
        }
    }
    
    private fun hasSignificantChange(
        currentData: MarketDepthData,
        newBids: List<com.pythonn.androidshowlimitorderbn.data.models.OrderBookEntry>,
        newAsks: List<com.pythonn.androidshowlimitorderbn.data.models.OrderBookEntry>,
        newPrice: Double,
        newRatios: List<PriceRangeRatio> = emptyList()
    ): Boolean {
        // 更严格的价格变化阈值，减少跳动
        val priceChangePercent = kotlin.math.abs(newPrice - currentData.currentPrice) / currentData.currentPrice
        if (priceChangePercent > 0.001) { // 提高到0.1%
            return true // 有显著变化，需要更新
        }
        
        // 检查订单簿前3档是否有变化（减少检查范围）
        val topBids = newBids.take(3)
        val topAsks = newAsks.take(3)
        val currentTopBids = currentData.bids.take(3)
        val currentTopAsks = currentData.asks.take(3)
        
        if (topBids.size != currentTopBids.size || topAsks.size != currentTopAsks.size) {
            return true // 有变化
        }
        
        // 更严格的价格和数量变化阈值
        for (i in topBids.indices) {
            val bidPriceChange = kotlin.math.abs(topBids[i].price - currentTopBids[i].price) / currentTopBids[i].price
            val bidQuantityChange = kotlin.math.abs(topBids[i].quantity - currentTopBids[i].quantity) / currentTopBids[i].quantity
            if (bidPriceChange > 0.001 || bidQuantityChange > 0.2) { // 提高阈值
                return true // 有显著变化
            }
        }
        
        for (i in topAsks.indices) {
            val askPriceChange = kotlin.math.abs(topAsks[i].price - currentTopAsks[i].price) / currentTopAsks[i].price
            val askQuantityChange = kotlin.math.abs(topAsks[i].quantity - currentTopAsks[i].quantity) / currentTopAsks[i].quantity
            if (askPriceChange > 0.001 || askQuantityChange > 0.2) { // 提高阈值
                return true // 有显著变化
            }
        }
        
        // 检查比率变化（如果提供了新比率数据）
        if (newRatios.isNotEmpty() && currentData.buySellRatio.isNotEmpty()) {
            for (i in newRatios.indices.take(kotlin.math.min(newRatios.size, currentData.buySellRatio.size))) {
                val ratioChange = kotlin.math.abs(newRatios[i].ratio - currentData.buySellRatio[i].ratio)
                if (ratioChange > 0.05) { // 比率变化超过5%才更新
                    return true
                }
            }
        }
        
        return false // 没有显著变化，可以跳过更新
    }
}