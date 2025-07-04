
import SwiftUI

struct OrderRowWithBarView: View {
    let entry: OrderBookEntry
    let maxQuantity: Double
    let isBid: Bool

    var body: some View {
        HStack(spacing: 5) {
            if isBid {
                // For Bids: Bar on the left, text on the right
                barView
                Spacer()
                quantityText
                priceText
            } else {
                // For Asks: Text on the left, bar on the right
                priceText
                quantityText
                Spacer()
                barView
            }
        }
        .padding(.vertical, 2)
    }

    // Extracted subviews for clarity
    private var priceText: some View {
        Text(String(format: "$%.2f", entry.price))
            .font(.caption)
            .frame(width: 80, alignment: isBid ? .trailing : .leading)
            .foregroundColor(.black)
    }

    private var quantityText: some View {
        Text(String(format: "%.3f", entry.quantity))
            .font(.caption)
            .frame(width: 60, alignment: isBid ? .trailing : .leading)
            .foregroundColor(.black)
    }

    private var barView: some View {
        ZStack(alignment: isBid ? .trailing : .leading) {
            Rectangle()
                .fill(Color.gray.opacity(0.2))
                .frame(height: 10)

            Rectangle()
                .fill(isBid ? Color(red: 0/255, green: 184/255, blue: 148/255) : Color(red: 255/255, green: 118/255, blue: 117/255))
                .frame(width: calculateBarWidth(), height: 10)
        }
        .frame(height: 10)
        .cornerRadius(2)
    }

    private func calculateBarWidth() -> CGFloat {
        guard maxQuantity > 0 else { return 5 } // Avoid division by zero
        let width = (entry.quantity / maxQuantity) * 200 // Use a fixed width for calculation
        return max(5, CGFloat(width)) // Return a minimum width of 5
    }
}
