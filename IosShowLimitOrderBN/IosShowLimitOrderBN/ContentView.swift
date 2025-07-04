import SwiftUI

struct ContentView: View {
    @StateObject var viewModel = MarketDataViewModel()

    var body: some View {
        TabView {
            SpotMarketView(viewModel: viewModel)
                .tabItem {
                    Label("Spot", systemImage: "chart.bar.fill")
                }
            
            FuturesMarketView(viewModel: viewModel)
                .tabItem {
                    Label("Futures", systemImage: "chart.line.uptrend.xyaxis")
                }
        }
        .onAppear {
            // Start fetching data for the initially selected symbol (BTCUSDT)
            viewModel.startFetchingData(symbol: viewModel.selectedSymbol)
        }
    }
}

struct SpotMarketView: View {
    @ObservedObject var viewModel: MarketDataViewModel
    @State private var localSelectedSymbol: String // Local state for TextField
    @State private var localCurrentThreshold: Double // Local state for TextField

    init(viewModel: MarketDataViewModel) {
        self.viewModel = viewModel
        _localSelectedSymbol = State(initialValue: viewModel.selectedSymbol)
        _localCurrentThreshold = State(initialValue: viewModel.currentThreshold)
    }

    var body: some View {
        NavigationView {
            ScrollView {
                VStack {
                    // Currency Input and Threshold Section
                    HStack {
                        Text("Currency Pair:")
                        TextField("e.g., BTCUSDT", text: $localSelectedSymbol)
                            .textFieldStyle(RoundedBorderTextFieldStyle())
                            .autocapitalization(.allCharacters)
                            .disableAutocorrection(true)
                        
                        Text("Threshold:")
                        TextField("e.g., 50", value: $localCurrentThreshold, formatter: NumberFormatter())
                            .textFieldStyle(RoundedBorderTextFieldStyle())
                            .keyboardType(.numberPad)
                            .frame(width: 80)
                        
                        Button("Switch") {
                            viewModel.selectedSymbol = localSelectedSymbol.uppercased()
                            viewModel.updateThreshold(for: viewModel.selectedSymbol, threshold: localCurrentThreshold)
                        }
                    }
                    .padding()

                    // Sub-navigation for Spot Market
                    HStack {
                        NavigationLink(destination: SpotOrderBookDetailsView(viewModel: viewModel)) {
                            Text("Order Book Details")
                                .padding()
                                .background(Color.green)
                                .foregroundColor(.white)
                                .cornerRadius(8)
                        }
                    }
                    .padding(.bottom)

                    if let spotData = viewModel.spotMarketData {
                        Text("Binance \(spotData.symbol) Spot Market Depth (Big Orders > \(Int(viewModel.getThreshold(for: spotData.symbol))) \(spotData.symbol.dropLast(4))) ")
                            .font(.headline)
                            .padding(.bottom, 5)

                        // Combined Order List with Bars
                        VStack(alignment: .leading) {
                            Text("Asks")
                                .font(.subheadline)
                                .padding(.leading)
                            ForEach(spotData.asks.prefix(5)) { ask in
                                OrderRowWithBarView(entry: ask, maxQuantity: spotData.maxQuantity, isBid: false)
                            }
                            
                            Text("Bids")
                                .font(.subheadline)
                                .padding(.leading)
                                .padding(.top, 10)
                            ForEach(spotData.bids.prefix(5)) { bid in
                                OrderRowWithBarView(entry: bid, maxQuantity: spotData.maxQuantity, isBid: true)
                            }
                        }
                        .padding(.horizontal)
                        .padding(.bottom)

                        Text("Spot Buy/Sell Ratio")
                            .font(.headline)
                            .padding(.bottom, 5)

                        if !spotData.buySellRatio.isEmpty {
                            RatioChartView(data: spotData.buySellRatio, isSpot: true)
                                .frame(height: 150)
                                .padding(.bottom)
                        } else {
                            Text("Buy/Sell ratio data is not available.")
                                .frame(height: 150)
                                .padding()
                        }
                    } else {
                        ProgressView("Loading Spot Data...")
                            .frame(maxWidth: .infinity, minHeight: 300)
                    }
                }
            }
            .navigationTitle("Market Overview")
        }
    }
}

struct FuturesMarketView: View {
    @ObservedObject var viewModel: MarketDataViewModel
    @State private var localSelectedSymbol: String // Local state for TextField
    @State private var localCurrentThreshold: Double // Local state for TextField

    init(viewModel: MarketDataViewModel) {
        self.viewModel = viewModel
        _localSelectedSymbol = State(initialValue: viewModel.selectedSymbol)
        _localCurrentThreshold = State(initialValue: viewModel.currentThreshold)
    }

    var body: some View {
        NavigationView {
            ScrollView {
                VStack {
                    // Currency Input and Threshold Section
                    HStack {
                        Text("Currency Pair:")
                        TextField("e.g., BTCUSDT", text: $localSelectedSymbol)
                            .textFieldStyle(RoundedBorderTextFieldStyle())
                            .autocapitalization(.allCharacters)
                            .disableAutocorrection(true)
                        
                        Text("Threshold:")
                        TextField("e.g., 50", value: $localCurrentThreshold, formatter: NumberFormatter())
                            .textFieldStyle(RoundedBorderTextFieldStyle())
                            .keyboardType(.numberPad)
                            .frame(width: 80)
                        
                        Button("Switch") {
                            viewModel.selectedSymbol = localSelectedSymbol.uppercased()
                            viewModel.updateThreshold(for: viewModel.selectedSymbol, threshold: localCurrentThreshold)
                        }
                    }
                    .padding()

                    // Sub-navigation for Futures Market
                    HStack {
                        NavigationLink(destination: FuturesOrderBookDetailsView(viewModel: viewModel)) {
                            Text("Order Book Details")
                                .padding()
                                .background(Color.green)
                                .foregroundColor(.white)
                                .cornerRadius(8)
                        }
                    }
                    .padding(.bottom)

                    if let futuresData = viewModel.futuresMarketData {
                        Text("Binance \(futuresData.symbol) Futures Market Depth (Big Orders > \(Int(viewModel.getThreshold(for: futuresData.symbol))) \(futuresData.symbol.dropLast(4))) ")
                            .font(.headline)
                            .padding(.bottom, 5)

                        // Combined Order List with Bars
                        VStack(alignment: .leading) {
                            Text("Asks")
                                .font(.subheadline)
                                .padding(.leading)
                            ForEach(futuresData.asks.prefix(5)) { ask in
                                OrderRowWithBarView(entry: ask, maxQuantity: futuresData.maxQuantity, isBid: false)
                            }
                            
                            Text("Bids")
                                .font(.subheadline)
                                .padding(.leading)
                                .padding(.top, 10)
                            ForEach(futuresData.bids.prefix(5)) { bid in
                                OrderRowWithBarView(entry: bid, maxQuantity: futuresData.maxQuantity, isBid: true)
                            }
                        }
                        .padding(.horizontal)
                        .padding(.bottom)

                        Text("Futures Buy/Sell Ratio")
                            .font(.headline)
                            .padding(.bottom, 5)

                        if !futuresData.buySellRatio.isEmpty {
                            RatioChartView(data: futuresData.buySellRatio, isSpot: false)
                                .frame(height: 150)
                                .padding(.bottom)
                        } else {
                            Text("Buy/Sell ratio data is not available.")
                                .frame(height: 150)
                                .padding()
                        }
                    } else {
                        ProgressView("Loading Futures Data...")
                            .frame(maxWidth: .infinity, minHeight: 300)
                    }
                }
            }
            .navigationTitle("Market Overview")
        }
    }
}

struct SpotOrderBookDetailsView: View {
    @ObservedObject var viewModel: MarketDataViewModel

    var body: some View {
        ScrollView {
            VStack {
                Text("Spot Ask Book")
                    .font(.headline)
                    .padding(.bottom, 5)
                if let spotData = viewModel.spotMarketData {
                    // Header
                    HStack {
                        Text("Price").fontWeight(.bold)
                        Spacer()
                        Text("Quantity").fontWeight(.bold)
                    }
                    .padding(.horizontal)
                    Divider()

                    ForEach(spotData.asks) { ask in
                        HStack {
                            Text(String(format: "$%.2f", ask.price))
                            Spacer()
                            Text(String(format: "%.3f", ask.quantity)).multilineTextAlignment(.trailing)
                        }
                        .padding(.horizontal)
                        Divider()
                    }
                } else {
                    Text("No Spot Ask Data")
                }

                Text("Spot Bid Book")
                    .font(.headline)
                    .padding(.top, 20)
                    .padding(.bottom, 5)
                if let spotData = viewModel.spotMarketData {
                    // Header
                    HStack {
                        Text("Price").fontWeight(.bold)
                        Spacer()
                        Text("Quantity").fontWeight(.bold)
                    }
                    .padding(.horizontal)
                    Divider()

                    ForEach(spotData.bids) { bid in
                        HStack {
                            Text(String(format: "$%.2f", bid.price))
                            Spacer()
                            Text(String(format: "%.3f", bid.quantity)).multilineTextAlignment(.trailing)
                        }
                        .padding(.horizontal)
                        Divider()
                    }
                } else {
                    Text("No Spot Bid Data")
                }
            }
        }
        .navigationTitle("Spot Order Book Details")
    }
}

struct FuturesOrderBookDetailsView: View {
    @ObservedObject var viewModel: MarketDataViewModel

    var body: some View {
        ScrollView {
            VStack {
                Text("Futures Ask Book")
                    .font(.headline)
                    .padding(.bottom, 5)
                if let futuresData = viewModel.futuresMarketData {
                    // Header
                    HStack {
                        Text("Price").fontWeight(.bold)
                        Spacer()
                        Text("Quantity").fontWeight(.bold)
                    }
                    .padding(.horizontal)
                    Divider()

                    ForEach(futuresData.asks) { ask in
                        HStack {
                            Text(String(format: "$%.2f", ask.price))
                            Spacer()
                            Text(String(format: "%.3f", ask.quantity)).multilineTextAlignment(.trailing)
                        }
                        .padding(.horizontal)
                        Divider()
                    }
                } else {
                    Text("No Futures Ask Data")
                }

                Text("Futures Bid Book")
                    .font(.headline)
                    .padding(.top, 20)
                    .padding(.bottom, 5)
                if let futuresData = viewModel.futuresMarketData {
                    // Header
                    HStack {
                        Text("Price").fontWeight(.bold)
                        Spacer()
                        Text("Quantity").fontWeight(.bold)
                    }
                    .padding(.horizontal)
                    Divider()

                    ForEach(futuresData.bids) { bid in
                        HStack {
                            Text(String(format: "$%.2f", bid.price))
                            Spacer()
                            Text(String(format: "%.3f", bid.quantity)).multilineTextAlignment(.trailing)
                        }
                        .padding(.horizontal)
                        Divider()
                    }
                } else {
                    Text("No Futures Bid Data")
                }
            }
        }
        .navigationTitle("Futures Order Book Details")
    }
}

#Preview {
    ContentView()
}