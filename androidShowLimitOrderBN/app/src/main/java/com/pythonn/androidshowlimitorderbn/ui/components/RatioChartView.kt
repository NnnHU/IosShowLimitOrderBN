package com.pythonn.androidshowlimitorderbn.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import com.pythonn.androidshowlimitorderbn.data.models.PriceRangeRatio

@Composable
fun RatioChartView(
    data: List<PriceRangeRatio>,
    isSpot: Boolean,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val textPaint = remember(density) {
        android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textAlign = android.graphics.Paint.Align.CENTER
            textSize = with(density) { 8.sp.toPx() }
            isAntiAlias = true
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Chart area with more space for negative values
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val barWidth = (canvasWidth / data.size) - 4.dp.toPx()
                // Reserve space for labels at bottom (40dp) and some padding
                val chartAreaHeight = canvasHeight - 40.dp.toPx()
                val maxBarHeight = chartAreaHeight * 0.4f // 40% of chart area for each direction
                val centerY = chartAreaHeight * 0.5f // Center line position

                // Central horizontal line (representing 0 ratio)
                drawLine(
                    color = Color.Gray.copy(alpha = 0.5f),
                    start = Offset(0f, centerY),
                    end = Offset(canvasWidth, centerY),
                    strokeWidth = 1.dp.toPx()
                )

                data.forEachIndexed { index, ratioEntry ->
                    val normalizedRatio = kotlin.math.abs(ratioEntry.ratio).coerceAtMost(1.0)
                    val barHeight = (normalizedRatio * maxBarHeight).toFloat().coerceAtLeast(3.dp.toPx())
                    val barColor = getBarColor(ratioEntry.ratio, isSpot)

                    val x = index * (canvasWidth / data.size) + 2.dp.toPx()
                    
                    val y = if (ratioEntry.ratio > 0) {
                        centerY - barHeight // Positive ratio, bar grows upwards
                    } else {
                        centerY // Negative ratio, bar grows downwards
                    }

                    // Draw rounded rectangle for bar
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx())
                    )
                    
                    // Draw value text on the bar
                    val valueText = String.format("%.1f%%", ratioEntry.ratio * 100)
                    val textY = if (ratioEntry.ratio > 0) {
                        y - 8.dp.toPx() // Above positive bars
                    } else {
                        y + barHeight + 12.dp.toPx() // Below negative bars
                    }
                    
                    drawIntoCanvas { canvas ->
                        canvas.nativeCanvas.drawText(
                            valueText,
                            x + barWidth / 2f,
                            textY,
                            textPaint
                        )
                    }
                }
            }
        }
        
        // Price range labels below the chart
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            data.forEach { ratioEntry ->
                Text(
                    text = ratioEntry.range,
                    fontSize = 10.sp,
                    color = getBarColor(ratioEntry.ratio, isSpot),
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

private fun getBarColor(ratio: Double, isSpot: Boolean): Color {
    return if (ratio > 0) {
        if (isSpot) Color(0xFF00B894) else Color(0xFF2E86DE) // Green for spot bids, Blue for futures bids
    } else if (ratio < 0) {
        if (isSpot) Color(0xFFFF7675) else Color(0xFFFFA502) // Red for spot asks, Orange for futures asks
    } else {
        Color(0xFF6C757D) // Gray for neutral
    }
}
