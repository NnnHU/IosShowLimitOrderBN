
import Foundation
import Combine

class MarketDataViewModel: ObservableObject {
    @Published var spotMarketData: MarketDepthData? // Published property for SwiftUI views
    @Published var futuresMarketData: MarketDepthData? // Published property for SwiftUI views
    
    @Published var selectedSymbol: String = "BTCUSDT" {
        didSet { // When symbol changes, update threshold and restart data fetching
            currentThreshold = thresholds[selectedSymbol] ?? 50.0 // Default to 50 if not found
            startFetchingData(symbol: selectedSymbol)
        }
    }
    @Published var currentThreshold: Double = 50.0 // Default for BTCUSDT
    
    private var thresholds: [String: Double] = [ // Remember thresholds for each symbol
        "BTCUSDT": 50.0,
        "ETHUSDT": 200.0,
        "SOLUSDT": 500.0
    ]
    
    private var cancellables = Set<AnyCancellable>()
    
    init() {
        setupSubscriptions()
    }
    
    private func setupSubscriptions() {
        BinanceAPIService.shared.spotMarketDataPublisher
            .receive(on: DispatchQueue.main) // Ensure updates are on the main thread for UI
            .sink { [weak self] data in
                self?.spotMarketData = data
            }
            .store(in: &cancellables)
        
        BinanceAPIService.shared.futuresMarketDataPublisher
            .receive(on: DispatchQueue.main)
            .sink { [weak self] data in
                self?.futuresMarketData = data
            }
            .store(in: &cancellables)
    }
    
    func startFetchingData(symbol: String) {
        // Stop existing streams before starting new ones
        BinanceAPIService.shared.stopWebSocketStream()
        
        // Fetch initial snapshots
        BinanceAPIService.shared.fetchOrderBookSnapshot(symbol: symbol, isFutures: false)
        BinanceAPIService.shared.fetchOrderBookSnapshot(symbol: symbol, isFutures: true)
        
        // Start WebSocket streams
        BinanceAPIService.shared.startWebSocketStream(symbols: [symbol], isFutures: false)
        BinanceAPIService.shared.startWebSocketStream(symbols: [symbol], isFutures: true)
    }
    
    func updateThreshold(for symbol: String, threshold: Double) {
        thresholds[symbol] = threshold
        currentThreshold = threshold
    }
    
    func getThreshold(for symbol: String) -> Double {
        return thresholds[symbol] ?? 50.0 // Default to 50 if not found
    }
    
    func stopFetchingData() {
        BinanceAPIService.shared.stopWebSocketStream()
    }
}
