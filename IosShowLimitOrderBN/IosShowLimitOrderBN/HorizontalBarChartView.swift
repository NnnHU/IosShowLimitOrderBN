import SwiftUI

struct HorizontalBarChartView: View {
    let data: [OrderBookEntry]
    let isBids: Bool // True for bids (right side), False for asks (left side)
    let maxQuantity: Double
    let barColor: Color

    var body: some View {
        GeometryReader { geometry in
            VStack(spacing: 2) {
                ForEach(data) { entry in
                    HStack(spacing: 0) {
                        if isBids { // Bids (right side)
                            // Spacer to push content to the right
                            Spacer(minLength: 0)
                            
                            // Quantity Text
                            Text(String(format: "%.1f", entry.quantity))
                                .font(.caption2)
                                .frame(width: 40, alignment: .trailing)
                                .foregroundColor(.white)
                            
                            // Bar
                            Rectangle()
                                .fill(barColor)
                                .frame(width: (entry.quantity / maxQuantity) * (geometry.size.width * 0.4), height: 15)
                                .cornerRadius(3)
                        } else { // Asks (left side)
                            // Bar
                            Rectangle()
                                .fill(barColor)
                                .frame(width: (entry.quantity / maxQuantity) * (geometry.size.width * 0.4), height: 15)
                                .cornerRadius(3)
                            
                            // Quantity Text
                            Text(String(format: "%.1f", entry.quantity))
                                .font(.caption2)
                                .frame(width: 40, alignment: .leading)
                                .foregroundColor(.white)
                            
                            // Spacer to push content to the left
                            Spacer(minLength: 0)
                        }
                    }
                }
            }
        }
    }
}