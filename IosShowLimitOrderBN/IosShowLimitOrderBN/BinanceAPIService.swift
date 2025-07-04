import Foundation
import Combine

// MARK: - OrderBookManager (Internal Helper)

class OrderBookManager {
    let symbol: String
    let isFutures: Bool
    private var _bids: [Double: Double] = [:] // [Price: Quantity]
    private var _asks: [Double: Double] = [:] // [Price: Quantity]
    var lastUpdateId: Int = 0
    
    private let minQuantity: Double // This will come from Config later
    private let queue = DispatchQueue(label: "com.app.orderBookQueue", attributes: .concurrent)

    init(symbol: String, isFutures: Bool, minQuantity: Double) {
        self.symbol = symbol
        self.isFutures = isFutures
        self.minQuantity = minQuantity
    }

    func setInitialOrderBook(snapshot: OrderBookSnapshot) {
        queue.sync(flags: .barrier) {
            _bids = [:]
            _asks = [:]
            snapshot.bids.forEach { _bids[$0.price] = $0.quantity }
            snapshot.asks.forEach { _asks[$0.price] = $0.quantity }
            lastUpdateId = snapshot.lastUpdateId
        }
    }

    func applyUpdate(bids: [OrderBookEntry], asks: [OrderBookEntry], newLastUpdateId: Int) {
        queue.sync(flags: .barrier) { // Use barrier for writes
            // Ensure updates are sequential
            guard newLastUpdateId > lastUpdateId else { return }
            
            for bid in bids {
                if bid.quantity == 0 {
                    _bids[bid.price] = nil // Remove
                } else {
                    _bids[bid.price] = bid.quantity // Add or update
                }
            }
            
            for ask in asks {
                if ask.quantity == 0 {
                    _asks[ask.price] = nil // Remove
                } else {
                    _asks[ask.price] = ask.quantity // Add or update
                }
            }
            
            self.lastUpdateId = newLastUpdateId
        }
    }
    
    func getFilteredOrders(limit: Int) -> (bids: [OrderBookEntry], asks: [OrderBookEntry]) {
        var filteredBids: [OrderBookEntry] = []
        var filteredAsks: [OrderBookEntry] = []
        
        queue.sync { // Use sync for reads
            filteredBids = Array(_bids
                .filter { $0.value >= minQuantity && $0.key > 0 }
                .map { OrderBookEntry(price: String($0.key), quantity: String($0.value)) }
                .sorted { $0.price > $1.price } // Sort bids descending
                .prefix(limit))
            
            filteredAsks = Array(_asks
                .filter { $0.value >= minQuantity && $0.key > 0 }
                .map { OrderBookEntry(price: String($0.key), quantity: String($0.value)) }
                .sorted { $0.price > $1.price } // Sort asks descending
                .prefix(limit))
        }
        return (filteredBids, filteredAsks)
    }
    
    func getCurrentPrice() -> Double? {
        var price: Double? = nil
        queue.sync { // Use sync for reads
            guard let highestBid = _bids.filter({ $0.key > 0 }).keys.max(),
                  let lowestAsk = _asks.filter({ $0.key > 0 }).keys.min() else {
                return
            }
            price = (highestBid + lowestAsk) / 2
        }
        return price
    }

    func calculateDepthRatioRange(lowerPercent: Double, upperPercent: Double) -> PriceRangeRatio? {
        var ratioData: PriceRangeRatio? = nil
        queue.sync { // Use sync for reads
            guard let midPrice = getCurrentPrice() else { return }

            let lowerBound = midPrice * (1 - upperPercent / 100)
            let upperBound = midPrice * (1 + upperPercent / 100)
            let innerLowerBound = midPrice * (1 - lowerPercent / 100)
            let innerUpperBound = midPrice * (1 + lowerPercent / 100)

            let bidsInRange = _bids.filter { (price, _) in
                price >= lowerBound && price < midPrice
            }
            let bidsVolume = bidsInRange.values.reduce(0, +)

            let asksInRange = _asks.filter { (price, _) in
                price <= upperBound && price > midPrice
            }
            let asksVolume = asksInRange.values.reduce(0, +)

            let delta = bidsVolume - asksVolume
            let total = bidsVolume + asksVolume
            let ratio = total > 0 ? delta / total : 0.0
            
            let rangeName: String = {
                let lowerStr = (lowerPercent == floor(lowerPercent)) ? String(format: "%.0f", lowerPercent) : String(format: "%.1f", lowerPercent)
                let upperStr = (upperPercent == floor(upperPercent)) ? String(format: "%.0f", upperPercent) : String(format: "%.1f", upperPercent)
                
                if lowerPercent == 0 {
                    return "0-\(upperStr)%"
                } else {
                    return "\(lowerStr)-\(upperStr)%"
                }
            }()

            ratioData = PriceRangeRatio(range: rangeName, ratio: ratio, bidsVolume: bidsVolume, asksVolume: asksVolume, delta: delta)
        }
        return ratioData
    }
}

// MARK: - BinanceAPIService

class BinanceAPIService: NSObject, URLSessionWebSocketDelegate {
    static let shared = BinanceAPIService() // Singleton
    
    private var spotWebSocketTask: URLSessionWebSocketTask?
    private var futuresWebSocketTask: URLSessionWebSocketTask?
    private var managers: [String: OrderBookManager] = [:] // [Symbol: Manager]
    
    // Publishers for real-time data updates
    let spotMarketDataPublisher = PassthroughSubject<MarketDepthData, Never>()
    let futuresMarketDataPublisher = PassthroughSubject<MarketDepthData, Never>()
    
    private override init() {
        super.init()
        // Initialize managers for default symbols (e.g., BTCUSDT)
        // In a real app, this would be dynamic based on user selection
        managers["BTCUSDT_SPOT"] = OrderBookManager(symbol: "BTCUSDT", isFutures: false, minQuantity: 50.0)
        managers["BTCUSDT_FUTURES"] = OrderBookManager(symbol: "BTCUSDT", isFutures: true, minQuantity: 50.0)
    }
    
    // MARK: - REST API
    
    func fetchOrderBookSnapshot(symbol: String, isFutures: Bool, limit: Int = 1000) {
        let baseURL = isFutures ? "https://fapi.binance.com" : "https://api.binance.com"
        let endpoint = isFutures ? "/fapi/v1/depth" : "/api/v3/depth"
        let urlString = "\(baseURL)\(endpoint)?symbol=\(symbol.uppercased())&limit=\(limit)"
        
        guard let url = URL(string: urlString) else {
            print("Invalid URL: \(urlString)")
            return
        }
        
        URLSession.shared.dataTask(with: url) { [weak self] data, response, error in
            guard let self = self else { return }
            
            if let error = error {
                print("Error fetching snapshot for \(symbol): \(error.localizedDescription)")
                return
            }
            
            guard let data = data else {
                print("No data received for snapshot for \(symbol)")
                return
            }
            
            do {
                let snapshot = try JSONDecoder().decode(OrderBookSnapshot.self, from: data)
                let managerKey = "\(symbol.uppercased())_\(isFutures ? "FUTURES" : "SPOT")"
                if let manager = self.managers[managerKey] {
                    manager.setInitialOrderBook(snapshot: snapshot)
                    print("Fetched initial snapshot for \(symbol) (\(isFutures ? "Futures" : "Spot")), lastUpdateId: \(manager.lastUpdateId)")
                    self.publishMarketData(for: manager)
                }
            } catch {
                print("Error decoding snapshot for \(symbol): \(error.localizedDescription)")
                if let jsonString = String(data: data, encoding: .utf8) {
                    print("Received JSON: \(jsonString)")
                }
            }
        }.resume()
    }
    
    // MARK: - WebSocket
    
    func startWebSocketStream(symbols: [String], isFutures: Bool) {
        let streamNames = symbols.map { $0.lowercased() + "@depth" }
        let baseURL = isFutures ? "wss://fstream.binance.com/ws" : "wss://stream.binance.com:9443/ws"
        let urlString = "\(baseURL)/\(streamNames.joined(separator: "/"))"
        
        guard let url = URL(string: urlString) else {
            print("Invalid WebSocket URL: \(urlString)")
            return
        }
        
        let session = URLSession(configuration: .default, delegate: self, delegateQueue: OperationQueue())
        let webSocketTask = session.webSocketTask(with: url)
        
        if isFutures {
            futuresWebSocketTask = webSocketTask
        } else {
            spotWebSocketTask = webSocketTask
        }
        
        webSocketTask.resume()
        
        print("Starting WebSocket stream for \(symbols) (\(isFutures ? "Futures" : "Spot")) at \(urlString)")
        
        
    }
    
    func stopWebSocketStream() {
        spotWebSocketTask?.cancel(with: .goingAway, reason: nil)
        spotWebSocketTask = nil
        futuresWebSocketTask?.cancel(with: .goingAway, reason: nil)
        futuresWebSocketTask = nil
        print("WebSocket streams stopped.")
    }
    
    private func receiveWebSocketMessage(for task: URLSessionWebSocketTask?) {
        task?.receive { [weak self] result in
            guard let self = self else { return }
            
            switch result {
            case .failure(let error):
                print("WebSocket receive error: \(error.localizedDescription)")
                // Attempt to reconnect or handle error
            case .success(let message):
                switch message {
                case .string(let text):
                    let isFutures = (task == self.futuresWebSocketTask)
                    self.processWebSocketMessage(text: text, isFutures: isFutures)
                case .data(let data):
                    print("Received binary message: \(data)")
                @unknown default:
                    fatalError("Unknown WebSocket message type")
                }
                // Continue receiving messages
                self.receiveWebSocketMessage(for: task)
            }
        }
    }
    
    private func processWebSocketMessage(text: String, isFutures: Bool) {
        guard let data = text.data(using: .utf8) else { return }
        
        do {
            let decoder = JSONDecoder()
            let event = try decoder.decode(DepthUpdateEvent.self, from: data)
            
            let managerKey = "\(event.symbol.uppercased())_\(isFutures ? "FUTURES" : "SPOT")"
            if let manager = managers[managerKey] {
                manager.applyUpdate(bids: event.bids, asks: event.asks, newLastUpdateId: event.finalUpdateId)
                self.publishMarketData(for: manager)
            }
        } catch {
            print("Error decoding WebSocket message: \(error.localizedDescription)")
            if let jsonString = String(data: data, encoding: .utf8) {
                print("Received JSON: \(jsonString)")
            }
        }
    }
    
    // MARK: - URLSessionWebSocketDelegate
    
    func urlSession(_ session: URLSession, webSocketTask: URLSessionWebSocketTask, didOpenWithProtocol protocol: String?) {
        if webSocketTask == spotWebSocketTask {
            print("Spot WebSocket did open")
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) { // Small delay
                self.receiveWebSocketMessage(for: spotWebSocketTask)
            }
        } else if webSocketTask == futuresWebSocketTask {
            print("Futures WebSocket did open")
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) { // Small delay
                self.receiveWebSocketMessage(for: futuresWebSocketTask)
            }
        }
    }
    
    func urlSession(_ session: URLSession, webSocketTask: URLSessionWebSocketTask, didCloseWith closeCode: URLSessionWebSocketTask.CloseCode, reason: Data?) {
        if webSocketTask == spotWebSocketTask {
            print("Spot WebSocket did close with code: \(closeCode), reason: \(reason.map { String(data: $0, encoding: .utf8) ?? "" } ?? "")")
        } else if webSocketTask == futuresWebSocketTask {
            print("Futures WebSocket did close with code: \(closeCode), reason: \(reason.map { String(data: $0, encoding: .utf8) ?? "" } ?? "")")
        }
    }
    
    // MARK: - Data Publishing
    
    private let ANALYSIS_RANGES: [(Double, Double)] = [
        (0, 1),
        (1, 2.5),
        (2.5, 5),
        (5, 10)
    ]

    private func publishMarketData(for manager: OrderBookManager) {
        let (filteredBids, filteredAsks) = manager.getFilteredOrders(limit: 100) // Limit to 100 for display
        let currentPrice = manager.getCurrentPrice() ?? 0.0
        
        let allQuantities = (filteredBids + filteredAsks).map { $0.quantity }
        let maxQuantity = allQuantities.max() ?? 1.0 // Ensure it's not zero to avoid division by zero
        
        var calculatedRatios: [PriceRangeRatio] = []
        for (lower, upper) in ANALYSIS_RANGES {
            if let ratio = manager.calculateDepthRatioRange(lowerPercent: lower, upperPercent: upper) {
                calculatedRatios.append(ratio)
            }
        }
        
        let marketData = MarketDepthData(
            symbol: manager.symbol,
            isFutures: manager.isFutures,
            bids: filteredBids,
            asks: filteredAsks,
            currentPrice: currentPrice,
            spread: 0.0, // Placeholder, calculate later
            maxQuantity: maxQuantity,
            buySellRatio: calculatedRatios,
            bigOrders: [] // Placeholder, filter big orders later
        )
        
        if manager.isFutures {
            futuresMarketDataPublisher.send(marketData)
        } else {
            spotMarketDataPublisher.send(marketData)
        }
    }
}