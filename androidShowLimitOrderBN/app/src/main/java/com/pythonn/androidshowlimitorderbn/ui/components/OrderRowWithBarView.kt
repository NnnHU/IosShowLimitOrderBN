package com.pythonn.androidshowlimitorderbn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pythonn.androidshowlimitorderbn.data.models.OrderBookEntry
import kotlin.math.max

@Composable
fun OrderRowWithBarView(
    entry: OrderBookEntry,
    maxQuantity: Double,
    isBid: Boolean,
    modifier: Modifier = Modifier
) {
    val bidColor = Color(0xFF00B894) // Green for bids (matching iOS)
    val askColor = Color(0xFFFF7675) // Red for asks (matching iOS)
    val backgroundColor = Color.Gray.copy(alpha = 0.2f)
    val barColor = if (isBid) bidColor else askColor
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        // Unified layout for both Bids and Asks: Price on the left, quantity in middle, bar on the right
        PriceText(
            price = entry.price,
            textAlign = TextAlign.Start
        )
        QuantityText(
            quantity = entry.quantity,
            textAlign = TextAlign.Start
        )
        Spacer(modifier = Modifier.weight(1f))
        BarView(
            quantity = entry.quantity,
            maxQuantity = maxQuantity,
            color = barColor,
            backgroundColor = backgroundColor
        )
    }
}

@Composable
private fun PriceText(
    price: Double,
    textAlign: TextAlign,
    modifier: Modifier = Modifier
) {
    Text(
        text = String.format("$%.2f", price),
        fontSize = 12.sp,
        color = Color.Black,
        textAlign = textAlign,
        modifier = modifier.width(80.dp)
    )
}

@Composable
private fun QuantityText(
    quantity: Double,
    textAlign: TextAlign,
    modifier: Modifier = Modifier
) {
    Text(
        text = String.format("%.3f", quantity),
        fontSize = 12.sp,
        color = Color.Black,
        textAlign = textAlign,
        modifier = modifier.width(60.dp)
    )
}

@Composable
private fun BarView(
    quantity: Double,
    maxQuantity: Double,
    color: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    val barWidth = calculateBarWidth(quantity, maxQuantity)
    
    Box(
        modifier = modifier
            .width(200.dp)
            .height(10.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(backgroundColor),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .width(barWidth.dp)
                .height(10.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
    }
}

private fun calculateBarWidth(quantity: Double, maxQuantity: Double): Float {
    if (maxQuantity <= 0) return 5f
    val width = (quantity / maxQuantity) * 200f
    return max(5f, width.toFloat())
}