package com.pythonn.androidshowlimitorderbn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pythonn.androidshowlimitorderbn.data.models.OrderBookEntry

@Composable
fun HorizontalBarChartView(
    data: List<OrderBookEntry>,
    isBids: Boolean,
    maxQuantity: Double,
    barColor: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        data.forEach { entry ->
            OrderRowWithBarView(entry = entry, maxQuantity = maxQuantity, isBid = isBids)
        }
    }
}