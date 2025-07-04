//
//  BinanceAPIService.swift
//  IosShowLimitOrderBN
//
//  Created by k on 2024/12/19.
//

import Foundation
import Combine

// MARK: - BinanceAPIService uses Models.swift for data structures

// MARK: - OrderBookManager

class OrderBookManager {
    let symbol: String
    let isFutures: Bool
    let minQuantity: Double
    
    private var bids: [Double: Double] = [:] // [Price: Quantity]
    private var asks: [Double: Double] = [:] // [Price: Quantity]
    
    var lastUpdateId: Int = 0
    
    init(symbol: String, isFutures: Bool, minQuantity: Double) {
        self.symbol = symbol
        self.isFutures = isFutures
        self.minQuantity = minQuantity
    }
    
    func setInitialOrderBook(snapshot: OrderBookSnapshot) {
        bids.removeAll()
        asks.removeAll()
        
        for bid in snapshot.bids {
            bids[bid.price] = bid.quantity
        }
        
        for ask in snapshot.asks {
            asks[ask.price] = ask.quantity
        }
        
        lastUpdateId = snapshot.lastUpdateId
    }
    
    func applyUpdate(bids: [OrderBookEntry], asks: [OrderBookEntry], newLastUpdateId: Int) {
        // Apply bid updates
        for bid in bids {
            if bid.quantity == 0 {
                self.bids.removeValue(forKey: bid.price)
            } else {
                self.bids[bid.price] = bid.quantity
            }
        }
        
        // Apply ask updates
        for ask in asks {
            if ask.quantity == 0 {
                self.asks.removeValue(forKey: ask.price)
            } else {
                self.asks[ask.price] = ask.quantity
            }
        }
        
        lastUpdateId = newLastUpdateId
    }
    
    func getFilteredOrders(limit: Int) -> ([OrderBookEntry], [OrderBookEntry]) {
        let filteredBids = bids
            .filter { $0.value >= minQuantity }
            .sorted { $0.key > $1.key } // Descending price order
            .prefix(limit)
            .map { OrderBookEntry(price: $0.key, quantity: $0.value) }
        
        let filteredAsks = asks
            .filter { $0.value >= minQuantity }
            .sorted { $0.key < $1.key } // Ascending price order
            .prefix(limit)
            .map { OrderBookEntry(price: $0.key, quantity: $0.value) }
        
        return (Array(filteredBids), Array(filteredAsks))
    }
    
    func getCurrentPrice() -> Double? {
        guard let highestBid = bids.keys.max(),
              let lowestAsk = asks.keys.min() else {
            return nil
        }
        return (highestBid + lowestAsk) / 2.0
    }
    
    func calculateDepthRatioRange(lowerPercent: Double, upperPercent: Double) -> PriceRangeRatio? {
        guard let currentPrice = getCurrentPrice() else { return nil }
        
        let lowerBound = currentPrice * (1 - lowerPercent / 100)
        let upperBound = currentPrice * (1 + upperPercent / 100)
        
        let bidsInRange = bids.filter { $0.key >= lowerBound && $0.key <= currentPrice }
        let asksInRange = asks.filter { $0.key <= upperBound && $0.key >= currentPrice }
        
        let totalBidQuantity = bidsInRange.values.reduce(0, +)
        let totalAskQuantity = asksInRange.values.reduce(0, +)
        let totalQuantity = totalBidQuantity + totalAskQuantity
        
        guard totalQuantity > 0 else {
            return PriceRangeRatio(
                range: "\(lowerPercent)-\(upperPercent)%",
                ratio: 0,
                bidsVolume: 0,
                asksVolume: 0,
                delta: 0
            )
        }
        
        let delta = totalBidQuantity - totalAskQuantity
        let ratio = delta / totalQuantity
        
        return PriceRangeRatio(
            range: "\(lowerPercent)-\(upperPercent)%",
            ratio: ratio,
            bidsVolume: totalBidQuantity,
            asksVolume: totalAskQuantity,
            delta: delta
        )
    }
}

// MARK: - BinanceAPIService

class BinanceAPIService: NSObject, URLSessionWebSocketDelegate {
    static let shared = BinanceAPIService()
    
    private var spotWebSocketTask: URLSessionWebSocketTask?
    private var futuresWebSocketTask: URLSessionWebSocketTask?
    private var currentSpotSymbol: String?
    private var currentFuturesSymbol: String?
    private var managers: [String: OrderBookManager] = [:]
    private var session: URLSession!
    
    // Publishers for real-time data updates
    let spotMarketDataPublisher = PassthroughSubject<MarketDepthData, Never>()
    let futuresMarketDataPublisher = PassthroughSubject<MarketDepthData, Never>()
    
    // Heart beat and reconnection properties
    private var heartbeatTimer: Timer?
    private var reconnectAttempts: [String: Int] = [:]
    private let maxReconnectAttempts = 5
    
    private override init() {
        super.init()
        
        let config = URLSessionConfiguration.default
        config.waitsForConnectivity = true
        config.timeoutIntervalForRequest = 30
        config.timeoutIntervalForResource = 60
        
        self.session = URLSession(configuration: config, delegate: self, delegateQueue: OperationQueue())
        
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
        if isFutures {
            futuresWebSocketTask?.cancel(with: .goingAway, reason: nil)
            futuresWebSocketTask = nil
        } else {
            spotWebSocketTask?.cancel(with: .goingAway, reason: nil)
            spotWebSocketTask = nil
        }
        
        let streamNames = symbols.map { $0.lowercased() + "@depth" }
        let baseURL = isFutures ? "wss://fstream.binance.com/ws" : "wss://stream.binance.com:9443/ws"
        let urlString = "\(baseURL)/\(streamNames.joined(separator: "/"))"
        
        guard let url = URL(string: urlString) else {
            print("Invalid WebSocket URL: \(urlString)")
            return
        }
        
        let webSocketTask = session.webSocketTask(with: url)
        
        if isFutures {
            futuresWebSocketTask = webSocketTask
            currentFuturesSymbol = symbols.first
        } else {
            spotWebSocketTask = webSocketTask
            currentSpotSymbol = symbols.first
        }
        
        webSocketTask.resume()
        
        print("Starting WebSocket stream for \(symbols) (\(isFutures ? "Futures" : "Spot")) at \(urlString)")
    }
    
    func switchSymbol(newSymbol: String, threshold: Double = 50.0) {
        print("Switching symbol to: \(newSymbol) with threshold: \(threshold)")
        disconnectAllWebSockets()
        
        managers.removeAll()
        managers["\(newSymbol.uppercased())_SPOT"] = OrderBookManager(symbol: newSymbol, isFutures: false, minQuantity: threshold)
        managers["\(newSymbol.uppercased())_FUTURES"] = OrderBookManager(symbol: newSymbol, isFutures: true, minQuantity: threshold)

        currentSpotSymbol = newSymbol
        currentFuturesSymbol = newSymbol
        
        fetchOrderBookSnapshot(symbol: newSymbol, isFutures: false)
        fetchOrderBookSnapshot(symbol: newSymbol, isFutures: true)
        
        startWebSocketStream(symbols: [newSymbol], isFutures: false)
        startWebSocketStream(symbols: [newSymbol], isFutures: true)
    }
    
    func stopWebSocketStream() {
        disconnectAllWebSockets()
    }
    
    private func disconnectAllWebSockets() {
        spotWebSocketTask?.cancel(with: .goingAway, reason: nil)
        spotWebSocketTask = nil
        futuresWebSocketTask?.cancel(with: .goingAway, reason: nil)
        futuresWebSocketTask = nil
        heartbeatTimer?.invalidate()
        print("All WebSocket connections disconnected.")
    }
    
    private func receiveWebSocketMessage(for task: URLSessionWebSocketTask?, isFutures: Bool) {
        guard let task = task, task.state == .running else {
            print("WebSocket task is not running, skipping receive")
            return
        }
        
        task.receive { [weak self] result in
            guard let self = self else { return }
            
            switch result {
            case .failure(let error):
                print("WebSocket receive error: \(error.localizedDescription)")
                if let urlError = error as? URLError {
                    print("URLError Code: \(urlError.code.rawValue)")
                }
                
                if let wsError = error as? URLError {
                    switch wsError.code {
                    case .networkConnectionLost, .notConnectedToInternet:
                        print("Network connection lost. Attempting to reconnect...")
                        DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) {
                            self.reconnectWebSocketStream(isFutures: isFutures)
                        }
                    case .timedOut:
                        print("Connection timed out. Attempting to reconnect...")
                        DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
                            self.reconnectWebSocketStream(isFutures: isFutures)
                        }
                    default:
                        print("Other WebSocket error, canceling task")
                        task.cancel()
                    }
                }
            case .success(let message):
                switch message {
                case .string(let text):
                    self.processWebSocketMessage(text: text, isFutures: isFutures)
                case .data(let data):
                    print("Received binary message: \(data)")
                @unknown default:
                    fatalError("Unknown WebSocket message type")
                }
                self.receiveWebSocketMessage(for: task, isFutures: isFutures)
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
    
    private func reconnectWebSocketStream(isFutures: Bool) {
        let key = isFutures ? "futures" : "spot"
        let attempts = reconnectAttempts[key] ?? 0
        
        guard attempts < maxReconnectAttempts else {
            print("Max reconnect attempts reached for \(key)")
            return
        }
        
        reconnectAttempts[key] = attempts + 1
        
        let delay = min(pow(2.0, Double(attempts)), 30.0)
        
        DispatchQueue.main.asyncAfter(deadline: .now() + delay) {
            if isFutures, let symbol = self.currentFuturesSymbol {
                print("Reconnecting Futures WebSocket (attempt \(attempts + 1))...")
                self.startWebSocketStream(symbols: [symbol], isFutures: true)
            } else if !isFutures, let symbol = self.currentSpotSymbol {
                print("Reconnecting Spot WebSocket (attempt \(attempts + 1))...")
                self.startWebSocketStream(symbols: [symbol], isFutures: false)
            }
        }
    }
    
    private func startHeartbeat(for task: URLSessionWebSocketTask?, isFutures: Bool) {
        heartbeatTimer?.invalidate()
        heartbeatTimer = Timer.scheduledTimer(withTimeInterval: 30.0, repeats: true) { [weak self] _ in
            guard let self = self, let task = task, task.state == .running else { return }
            
            task.sendPing { error in
                if let error = error {
                    print("Ping failed: \(error.localizedDescription)")
                    self.reconnectWebSocketStream(isFutures: isFutures)
                }
            }
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
        let (filteredBids, filteredAsks) = manager.getFilteredOrders(limit: 100)
        let currentPrice = manager.getCurrentPrice() ?? 0.0
        
        let allQuantities = (filteredBids + filteredAsks).map { $0.quantity }
        let maxQuantity = allQuantities.max() ?? 1.0
        
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
            spread: 0.0,
            maxQuantity: maxQuantity,
            buySellRatio: calculatedRatios,
            bigOrders: []
        )
        
        if manager.isFutures {
            futuresMarketDataPublisher.send(marketData)
        } else {
            spotMarketDataPublisher.send(marketData)
        }
    }
    
    // MARK: - URLSessionWebSocketDelegate
    
    func urlSession(_ session: URLSession, webSocketTask: URLSessionWebSocketTask, didOpenWithProtocol protocol: String?) {
        if webSocketTask == spotWebSocketTask {
            print("Spot WebSocket did open")
            reconnectAttempts["spot"] = 0
            self.receiveWebSocketMessage(for: self.spotWebSocketTask, isFutures: false)
            self.startHeartbeat(for: self.spotWebSocketTask, isFutures: false)
        } else if webSocketTask == futuresWebSocketTask {
            print("Futures WebSocket did open")
            reconnectAttempts["futures"] = 0
            self.receiveWebSocketMessage(for: self.futuresWebSocketTask, isFutures: true)
            self.startHeartbeat(for: self.futuresWebSocketTask, isFutures: true)
        }
    }
    
    func urlSession(_ session: URLSession, webSocketTask: URLSessionWebSocketTask, didCloseWith closeCode: URLSessionWebSocketTask.CloseCode, reason: Data?) {
        heartbeatTimer?.invalidate()
        
        let reasonString = reason.map { String(data: $0, encoding: .utf8) ?? "" } ?? ""
        
        if webSocketTask == spotWebSocketTask {
            print("Spot WebSocket did close with code: \(closeCode), reason: \(reasonString)")
            if closeCode != .goingAway {
                reconnectWebSocketStream(isFutures: false)
            }
        } else if webSocketTask == futuresWebSocketTask {
            print("Futures WebSocket did close with code: \(closeCode), reason: \(reasonString)")
            if closeCode != .goingAway {
                reconnectWebSocketStream(isFutures: true)
            }
        }
    }
}