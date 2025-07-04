
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
        // 使用 switchSymbol 方法，它会自动处理断开连接和重新连接
        let threshold = getThreshold(for: symbol)
        BinanceAPIService.shared.switchSymbol(newSymbol: symbol, threshold: threshold)
    }
    
    func stopFetchingData() {
        // 添加一个新的公共方法来停止所有连接
        BinanceAPIService.shared.stopWebSocketStream()
    }
    
    func updateThreshold(for symbol: String, threshold: Double) {
        thresholds[symbol] = threshold
        currentThreshold = threshold
    }
    
    func getThreshold(for symbol: String) -> Double {
        return thresholds[symbol] ?? 50.0 // Default to 50 if not found
    }
}
