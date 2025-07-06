package com.pythonn.androidshowlimitorderbn.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pythonn.androidshowlimitorderbn.ui.viewmodel.MarketDataViewModel
import com.pythonn.androidshowlimitorderbn.ui.components.OrderRowWithBarView
import com.pythonn.androidshowlimitorderbn.ui.components.RatioChartView
import java.util.Locale

@Composable
fun SpotMarketScreen(
    viewModel: MarketDataViewModel,
    onNavigateToOrderBookDetails: () -> Unit
) {
    val spotData by viewModel.spotMarketData.collectAsState()
    val currentThreshold by viewModel.currentThreshold.collectAsState()
    var localSelectedSymbol by remember { mutableStateOf("BTCUSDT") }
    var localCurrentThreshold by remember { mutableStateOf(currentThreshold.toString()) }

    LaunchedEffect(localSelectedSymbol, currentThreshold) {
        viewModel.switchSymbol(localSelectedSymbol, currentThreshold)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Currency Input and Threshold Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = localSelectedSymbol,
                onValueChange = { localSelectedSymbol = it.uppercase() },
                label = { Text("Currency Pair") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface)
            )

            OutlinedTextField(
                value = localCurrentThreshold,
                onValueChange = { localCurrentThreshold = it },
                label = { Text("Threshold") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier.width(120.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface)
            )

            Button(
                onClick = {
                    viewModel.switchSymbol(
                        localSelectedSymbol,
                        localCurrentThreshold.toDoubleOrNull() ?: 50.0
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00B894)
                )
            ) {
                Text("Switch", color = Color.White)
            }
        }

        Spacer(Modifier.height(8.dp))

        // Navigation to Order Book Details
        Button(
            onClick = { onNavigateToOrderBookDetails() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF00B894)
            )
        ) {
            Text("Order Book Details", color = Color.White)
        }

        Spacer(Modifier.height(16.dp))

        // Market Data Display
        if (spotData != null) {
            // Ratio Chart (placed at the top)
            if (spotData!!.buySellRatio.isNotEmpty()) {
                Text(
                    text = "Order Ratio",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                RatioChartView(data = spotData!!.buySellRatio, isSpot = true)
                Spacer(Modifier.height(16.dp))
            }

            // Current Price (placed in the middle)
            Text(
                text = "Current Price: ${spotData!!.currentPrice}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            )

            // Asks (Sell Orders) - placed above current price, sorted descending
            Text(
                text = "Asks (${spotData!!.asks.size})",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                reverseLayout = true // Display asks from bottom to top (highest price at top)
            ) {
                items(spotData!!.asks) { ask ->
                    OrderRowWithBarView(
                        entry = ask,
                        isBid = false,
                        maxQuantity = spotData!!.maxQuantity
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Bids (Buy Orders) - placed below current price, sorted descending
            Text(
                text = "Bids (${spotData!!.bids.size})",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                items(spotData!!.bids) { bid ->
                    OrderRowWithBarView(
                        entry = bid,
                        isBid = true,
                        maxQuantity = spotData!!.maxQuantity
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF2E86DE)
                    )
                    Text(
                        text = "Loading Spot Data...",
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}