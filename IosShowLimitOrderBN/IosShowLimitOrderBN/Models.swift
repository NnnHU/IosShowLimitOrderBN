
import Foundation

// MARK: - Binance API Models

// Represents a single order in the order book
struct OrderBookEntry: Codable, Identifiable {
    let id = UUID() // For SwiftUI List identification
    let price: Double
    let quantity: Double

    init(price: String, quantity: String) {
        self.price = Double(price) ?? 0.0
        self.quantity = Double(quantity) ?? 0.0
    }
    
    init(price: Double, quantity: Double) {
        self.price = price
        self.quantity = quantity
    }
    
    // For decoding from Binance API (array of arrays)
    init(from decoder: Decoder) throws {
        var container = try decoder.unkeyedContainer()
        let priceString = try container.decode(String.self)
        let quantityString = try container.decode(String.self)
        self.price = Double(priceString) ?? 0.0
        self.quantity = Double(quantityString) ?? 0.0
    }
    
    func encode(to encoder: Encoder) throws {
        var container = encoder.unkeyedContainer()
        try container.encode(String(price))
        try container.encode(String(quantity))
    }
}

// Represents the full order book snapshot
struct OrderBookSnapshot: Codable {
    let lastUpdateId: Int
    let bids: [OrderBookEntry]
    let asks: [OrderBookEntry]
}

// Represents a WebSocket depth update event
struct DepthUpdateEvent: Codable {
    let eventType: String // "depthUpdate"
    let eventTime: Int // Event time
    let symbol: String
    let firstUpdateId: Int // First update ID in event
    let finalUpdateId: Int // Final update ID in event
    let bids: [OrderBookEntry] // Bids to be updated
    let asks: [OrderBookEntry] // Asks to be updated

    enum CodingKeys: String, CodingKey {
        case eventType = "e"
        case eventTime = "E"
        case symbol = "s"
        case firstUpdateId = "U"
        case finalUpdateId = "u"
        case bids = "b"
        case asks = "a"
    }
}

// MARK: - Internal App Models (for processed data)

struct MarketDepthData: Identifiable {
    let id = UUID()
    let symbol: String
    let isFutures: Bool
    var bids: [OrderBookEntry]
    var asks: [OrderBookEntry]
    var currentPrice: Double
    var spread: Double
    var maxQuantity: Double // Add maxQuantity for chart scaling
    var buySellRatio: [PriceRangeRatio] // For ratio charts
    var bigOrders: [OrderBookEntry] // Filtered big orders
}

struct PriceRangeRatio: Identifiable {
    let id = UUID()
    let range: String // e.g., "0-1%", "1-2.5%"
    let ratio: Double
    let bidsVolume: Double
    let asksVolume: Double
    let delta: Double
}

