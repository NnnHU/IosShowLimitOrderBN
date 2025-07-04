import SwiftUI

struct RatioChartView: View {
    let data: [PriceRangeRatio]
    let isSpot: Bool

    var body: some View {
        GeometryReader { geometry in
            HStack(alignment: .bottom, spacing: 5) {
                ForEach(data) { ratioEntry in
                    VStack {
                        Spacer()
                        Rectangle()
                            .fill(barColor(for: ratioEntry.ratio))
                            .frame(width: (geometry.size.width / CGFloat(data.count)) * 0.6, height: max(5, CGFloat(abs(ratioEntry.ratio)) * geometry.size.height * 0.8)) // Scale height based on ratio, with a minimum of 5
                            .cornerRadius(3)
                        Text(ratioEntry.range)
                            .font(.caption2)
                            .foregroundColor(.white)
                    }
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(Color.clear)
        }
    }
    
    private func barColor(for ratio: Double) -> Color {
        if ratio > 0 {
            return isSpot ? Color(red: 0/255, green: 184/255, blue: 148/255) : Color(red: 46/255, green: 134/255, blue: 222/255) // Greenish for spot bids, Blue for futures bids
        } else if ratio < 0 {
            return isSpot ? Color(red: 255/255, green: 118/255, blue: 117/255) : Color(red: 255/255, green: 165/255, blue: 2/255) // Reddish for spot asks, Orange for futures asks
        } else {
            return Color.gray // Neutral
        }
    }
}