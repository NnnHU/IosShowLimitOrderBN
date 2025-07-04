import SwiftUI

struct RatioChartView: View {
    let data: [PriceRangeRatio]
    let isSpot: Bool

    var body: some View {
        GeometryReader { geometry in
            ZStack(alignment: .center) {
                // Central horizontal line (representing 0 ratio)
                Rectangle()
                    .fill(Color.gray.opacity(0.5))
                    .frame(height: 1) // Thin line
                    .frame(maxWidth: .infinity)

                HStack(alignment: .center, spacing: 2) { // Changed alignment to center
                    ForEach(data) { ratioEntry in
                        VStack {
                            // Bar
                            Rectangle()
                                .fill(barColor(for: ratioEntry.ratio))
                                .frame(width: max(0, (geometry.size.width / CGFloat(data.count)) - 2),
                                       height: max(5, CGFloat(abs(ratioEntry.ratio)) * geometry.size.height * 0.6))
                                .cornerRadius(3)
                                // Adjust offset to make bars grow from the center
                                .offset(y: ratioEntry.ratio > 0 ?
                                        -max(5, CGFloat(abs(ratioEntry.ratio)) * geometry.size.height * 0.6) / 2 :
                                        max(5, CGFloat(abs(ratioEntry.ratio)) * geometry.size.height * 0.6) / 2)

                            // Range Text and Ratio Text
                            Text("\(ratioEntry.range) (\(String(format: "%.1f%%", ratioEntry.ratio * 100)))")
                                .font(.caption2)
                                .foregroundColor(barColor(for: ratioEntry.ratio))
                                .offset(y: ratioEntry.ratio < 0 ?
                                        -max(5, CGFloat(abs(ratioEntry.ratio)) * geometry.size.height * 0.6) - 5 : // Move up for negative bars
                                        5) // Move down for positive bars (some padding)
                        }
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