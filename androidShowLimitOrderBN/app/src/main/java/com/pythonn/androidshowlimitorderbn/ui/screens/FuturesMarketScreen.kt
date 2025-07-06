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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pythonn.androidshowlimitorderbn.ui.viewmodel.MarketDataViewModel
import com.pythonn.androidshowlimitorderbn.ui.components.OrderRowWithBarView
import com.pythonn.androidshowlimitorderbn.ui.components.RatioChartView
import java.util.Locale

@Composable
fun FuturesMarketScreen(
    viewModel: MarketDataViewModel,
    onNavigateToOrderBookDetails: () -> Unit
) {
    val futuresData by viewModel.futuresMarketData.collectAsState()
    var localSelectedSymbol by remember { mutableStateOf("BTCUSDT") }
    var localCurrentThreshold by remember { mutableStateOf("50.0") }

    LaunchedEffect(Unit) {
        // Initial data fetch when the screen is first composed
        viewModel.switchSymbol(localSelectedSymbol, localCurrentThreshold.toDoubleOrNull() ?: 50.0)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
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
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = localCurrentThreshold,
                    onValueChange = { localCurrentThreshold = it },
                    label = { Text("Threshold") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.width(120.dp)
                )

                Button(
                    onClick = {
                        viewModel.switchSymbol(
                            localSelectedSymbol,
                            localCurrentThreshold.toDoubleOrNull() ?: 50.0
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2E86DE)
                    )
                ) {
                    Text("Switch", color = Color.White)
                }
            }
        }

        item {
            // Navigation to Order Book Details
            Button(
                onClick = { onNavigateToOrderBookDetails() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2E86DE)
                )
            ) {
                Text("Order Book Details", color = Color.White)
            }
        }

        // Market Data Display
        if (futuresData != null) {
            item {
                Text(
                    text = "Binance ${futuresData!!.symbol} Futures Market Depth (Big Orders > ${localCurrentThreshold.toDoubleOrNull()?.toInt() ?: 50} ${futuresData!!.symbol.dropLast(4)})",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            // Current Price and Spread Display
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF8F9FA)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Current Price",
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )
                                Text(
                                    text = String.format("%.2f USDT", futuresData!!.currentPrice),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E86DE)
                                )
                            }
                            Column(
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = "Spread",
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )
                                Text(
                                    text = String.format("%.2f USDT", futuresData!!.spread),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF00B894)
                                )
                            }
                        }
                    }
                }
            }

            // Asks Section
            item {
                Text(
                    text = "Asks",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }

            items(futuresData!!.asks.take(5)) { ask ->
                OrderRowWithBarView(
                    entry = ask,
                    maxQuantity = futuresData!!.maxQuantity,
                    isBid = false,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Bids Section
            item {
                Text(
                    text = "Bids",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp)
                )
            }

            items(futuresData!!.bids.take(5)) { bid ->
                OrderRowWithBarView(
                    entry = bid,
                    maxQuantity = futuresData!!.maxQuantity,
                    isBid = true,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Buy/Sell Ratio Chart
            item {
                Text(
                    text = "Futures Buy/Sell Ratio",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 16.dp)
                )

                if (futuresData!!.buySellRatio.isNotEmpty()) {
                    RatioChartView(
                        data = futuresData!!.buySellRatio,
                        isSpot = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(bottom = 16.dp)
                    )
                } else {
                    Text(
                        text = "Buy/Sell ratio data is not available.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            item {
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
                            text = "Loading Futures Data...",
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}