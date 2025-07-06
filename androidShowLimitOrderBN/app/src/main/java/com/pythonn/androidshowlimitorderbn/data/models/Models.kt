package com.pythonn.androidshowlimitorderbn.data.models

import com.google.gson.annotations.SerializedName
import java.util.UUID

// MARK: - Binance API Models

// Represents a single order in the order book
data class OrderBookEntry(
    val price: Double,
    val quantity: Double,
    val id: UUID = UUID.randomUUID() // For SwiftUI List identification, equivalent for Compose
)

// Represents the full order book snapshot
data class OrderBookSnapshot(
    @SerializedName("lastUpdateId") val lastUpdateId: Long,
    @SerializedName("bids") val bidsRaw: List<List<String>>, // Raw string arrays from API
    @SerializedName("asks") val asksRaw: List<List<String>>  // Raw string arrays from API
) {
    // Convert raw string arrays to OrderBookEntry objects
    val bids: List<OrderBookEntry>
        get() = bidsRaw.map { OrderBookEntry(it[0].toDouble(), it[1].toDouble()) }
    
    val asks: List<OrderBookEntry>
        get() = asksRaw.map { OrderBookEntry(it[0].toDouble(), it[1].toDouble()) }
}

// Represents a WebSocket depth update event
data class DepthUpdateEvent(
    @SerializedName("e") val eventType: String, // "depthUpdate"
    @SerializedName("E") val eventTime: Long, // Event time
    @SerializedName("s") val symbol: String,
    @SerializedName("U") val firstUpdateId: Long, // First update ID in event
    @SerializedName("u") val finalUpdateId: Long, // Final update ID in event
    @SerializedName("b") val bids: List<List<String>>, // Bids to be updated (price, quantity as strings)
    @SerializedName("a") val asks: List<List<String>> // Asks to be updated (price, quantity as strings)
) {
    // Helper to convert raw string lists to OrderBookEntry
    fun getBidsAsOrderBookEntries(): List<OrderBookEntry> {
        return bids.map { OrderBookEntry(it[0].toDouble(), it[1].toDouble()) }
    }

    fun getAsksAsOrderBookEntries(): List<OrderBookEntry> {
        return asks.map { OrderBookEntry(it[0].toDouble(), it[1].toDouble()) }
    }
}

// MARK: - Internal App Models (for processed data)

data class MarketDepthData(
    val symbol: String,
    val isFutures: Boolean,
    var bids: List<OrderBookEntry>,
    var asks: List<OrderBookEntry>,
    var currentPrice: Double,
    var spread: Double,
    var maxQuantity: Double, // Add maxQuantity for chart scaling
    var buySellRatio: List<PriceRangeRatio>, // For ratio charts
    var bigOrders: List<OrderBookEntry>, // Filtered big orders
    val id: UUID = UUID.randomUUID()
)

data class PriceRangeRatio(
    val range: String, // e.g., "0-1%", "1-2.5%"
    val ratio: Double,
    val bidsVolume: Double,
    val asksVolume: Double,
    val delta: Double,
    val id: UUID = UUID.randomUUID()
)
