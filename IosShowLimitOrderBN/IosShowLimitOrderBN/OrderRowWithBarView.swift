
import SwiftUI

struct OrderRowWithBarView: View {
    let entry: OrderBookEntry
    let maxQuantity: Double
    let isBid: Bool

    var body: some View {
        HStack(spacing: 5) {
            // Price - Always on the left
            Text(String(format: "$%.2f", entry.price))
                .font(.caption)
                .frame(width: 80, alignment: .leading) // Fixed width for price
                .foregroundColor(.white)

            // Quantity - Next to price
            Text(String(format: "%.3f", entry.quantity))
                .font(.caption)
                .frame(width: 60, alignment: .leading) // Fixed width for quantity
                .foregroundColor(.white)

            // Bar - Flexible width, extends to the right
            GeometryReader { geometry in
                Rectangle()
                    .fill(isBid ? Color(red: 0/255, green: 184/255, blue: 148/255) : Color(red: 255/255, green: 118/255, blue: 117/255))
                    .frame(width: max(5, (entry.quantity / maxQuantity) * geometry.size.width), height: 10)
                    .cornerRadius(2)
            }
            .frame(height: 10) // Fixed height for the bar area, width will be flexible
            
            Spacer(minLength: 0) // Pushes everything to the left
        }
        .padding(.vertical, 2)
    }
}
