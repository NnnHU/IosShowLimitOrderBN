package com.pythonn.androidshowlimitorderbn.data.remote

import com.pythonn.androidshowlimitorderbn.data.models.OrderBookEntry
import com.pythonn.androidshowlimitorderbn.data.models.OrderBookSnapshot
import com.pythonn.androidshowlimitorderbn.data.models.PriceRangeRatio
import kotlin.math.pow

class OrderBookManager(
    val symbol: String,
    val isFutures: Boolean,
    var minQuantity: Double
) {

    private var bids: MutableMap<Double, Double> = mutableMapOf() // [Price: Quantity]
    private var asks: MutableMap<Double, Double> = mutableMapOf() // [Price: Quantity]

    var lastUpdateId: Long = 0

    fun setInitialOrderBook(snapshot: OrderBookSnapshot) {
        bids.clear()
        asks.clear()

        snapshot.bids.forEach { bid ->
            bids[bid.price] = bid.quantity
        }

        snapshot.asks.forEach { ask ->
            asks[ask.price] = ask.quantity
        }

        lastUpdateId = snapshot.lastUpdateId
    }

    fun applyUpdate(bidsUpdate: List<OrderBookEntry>, asksUpdate: List<OrderBookEntry>, newLastUpdateId: Long) {
        // Apply bid updates
        bidsUpdate.forEach { bid ->
            if (bid.quantity == 0.0) {
                bids.remove(bid.price)
            } else {
                bids[bid.price] = bid.quantity
            }
        }

        // Apply ask updates
        asksUpdate.forEach { ask ->
            if (ask.quantity == 0.0) {
                asks.remove(ask.price)
            } else {
                asks[ask.price] = ask.quantity
            }
        }

        lastUpdateId = newLastUpdateId
    }

    fun getFilteredOrders(limit: Int): Pair<List<OrderBookEntry>, List<OrderBookEntry>> {
        println("OrderBookManager: Filtering orders for ${symbol} (isFutures: ${isFutures}) with minQuantity: ${minQuantity}")
        println("OrderBookManager: Raw bids count: ${bids.size}, Raw asks count: ${asks.size}")
        val filteredBids = bids
            .filter { it.value >= minQuantity }
            .entries
            .sortedByDescending { it.key } // Descending price order
            .take(limit)
            .map { OrderBookEntry(price = it.key, quantity = it.value) }

        val filteredAsks = asks
            .filter { it.value >= minQuantity }
            .entries
            .sortedBy { it.key } // Ascending price order
            .take(limit)
            .map { OrderBookEntry(price = it.key, quantity = it.value) }

        return Pair(filteredBids, filteredAsks)
    }

    fun getBigOrders(): List<OrderBookEntry> {
        val bigBids = bids
            .filter { it.value >= minQuantity }
            .entries
            .map { OrderBookEntry(price = it.key, quantity = it.value) }

        val bigAsks = asks
            .filter { it.value >= minQuantity }
            .entries
            .map { OrderBookEntry(price = it.key, quantity = it.value) }

        return (bigBids + bigAsks).sortedBy { it.price } // Sort by price for consistency
    }

    fun getCurrentPrice(): Double? {
        val highestBid = bids.keys.maxOrNull()
        val lowestAsk = asks.keys.minOrNull()

        return when {
            highestBid != null && lowestAsk != null -> (highestBid + lowestAsk) / 2.0
            highestBid != null -> highestBid // 只有买盘数据时使用最高买价
            lowestAsk != null -> lowestAsk   // 只有卖盘数据时使用最低卖价
            else -> null
        }
    }

    fun getSpread(): Double? {
        val highestBid = bids.keys.maxOrNull()
        val lowestAsk = asks.keys.minOrNull()

        return if (highestBid != null && lowestAsk != null) {
            lowestAsk - highestBid
        } else {
            null
        }
    }

    fun calculateDepthRatioRange(lowerPercent: Double, upperPercent: Double): PriceRangeRatio? {
        val currentPrice = getCurrentPrice() ?: return null

        val lowerBound = currentPrice * (1 - lowerPercent / 100)
        val upperBound = currentPrice * (1 + upperPercent / 100)

        val bidsInRange = bids.filter { it.key >= lowerBound && it.key <= currentPrice }
        val asksInRange = asks.filter { it.key <= upperBound && it.key >= currentPrice }

        val totalBidQuantity = bidsInRange.values.sum()
        val totalAskQuantity = asksInRange.values.sum()
        val totalQuantity = totalBidQuantity + totalAskQuantity

        if (totalQuantity == 0.0) {
            return PriceRangeRatio(
                range = "${lowerPercent}-${upperPercent}%",
                ratio = 0.0,
                bidsVolume = 0.0,
                asksVolume = 0.0,
                delta = 0.0
            )
        }

        val delta = totalBidQuantity - totalAskQuantity
        val ratio = delta / totalQuantity

        return PriceRangeRatio(
            range = "${lowerPercent}-${upperPercent}%",
            ratio = ratio,
            bidsVolume = totalBidQuantity,
            asksVolume = totalAskQuantity,
            delta = delta
        )
    }
}