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
import androidx.navigation.NavController
import com.pythonn.androidshowlimitorderbn.ui.viewmodel.MarketDataViewModel
import com.pythonn.androidshowlimitorderbn.ui.components.OrderRowWithBarView
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotOrderBookDetailsScreen(viewModel: MarketDataViewModel, navController: NavController, initialThreshold: Double) {
    val spotData by viewModel.spotMarketData.collectAsState()
    var localCurrentThreshold by remember { mutableStateOf(initialThreshold.toString()) }

    // 添加数据稳定性缓冲，避免频繁重组
    val stableSpotData by remember(spotData) {
        derivedStateOf {
            spotData?.let { data ->
                // 只有当数据真正有意义变化时才更新UI
                if (data.bids.isNotEmpty() && data.asks.isNotEmpty()) {
                    data
                } else {
                    null
                }
            }
        }
    }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (stableSpotData != null) {
            item {
                TopAppBar(
                    title = { Text("Spot Order Book Details") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = localCurrentThreshold,
                        onValueChange = { localCurrentThreshold = it },
                        label = { Text("Filter Threshold") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface)
                    )

                    Button(
                        onClick = {
                            viewModel.switchSymbol(
                                stableSpotData!!.symbol,
                                localCurrentThreshold.toDoubleOrNull() ?: 50.0
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00B894)
                        )
                    ) {
                        Text("Apply", color = Color.White)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Asks Section
            item {
                Text(
                    text = "Ask Book (${stableSpotData!!.asks.size} orders)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFFF7675),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            // Ask headers
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Price",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Quantity",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1f)
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            }
            
            items(stableSpotData!!.asks.filter { it.quantity >= (localCurrentThreshold.toDoubleOrNull() ?: 50.0) }) { ask ->
                OrderRowWithBarView(
                    entry = ask,
                    maxQuantity = stableSpotData!!.maxQuantity,
                    isBid = false,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // Bids Section
            item {
                Text(
                    text = "Bid Book (${stableSpotData!!.bids.size} orders)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF00B894),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            // Bid headers
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Price",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Quantity",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1f)
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            }
            
            items(stableSpotData!!.bids.filter { it.quantity >= (localCurrentThreshold.toDoubleOrNull() ?: 50.0) }) { bid ->
                OrderRowWithBarView(
                    entry = bid,
                    maxQuantity = stableSpotData!!.maxQuantity,
                    isBid = true,
                    modifier = Modifier.fillMaxWidth()
                )
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
                            text = "Loading Order Book Details...",
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FuturesOrderBookDetailsScreen(viewModel: MarketDataViewModel, navController: NavController, initialThreshold: Double) {
    val futuresData by viewModel.futuresMarketData.collectAsState()
    var localCurrentThreshold by remember { mutableStateOf(initialThreshold.toString()) }

    // 添加数据稳定性缓冲，避免频繁重组
    val stableFuturesData by remember(futuresData) {
        derivedStateOf {
            futuresData?.let { data ->
                // 只有当数据真正有意义变化时才更新UI
                if (data.bids.isNotEmpty() && data.asks.isNotEmpty()) {
                    data
                } else {
                    null
                }
            }
        }
    }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (stableFuturesData != null) {
            item {
                TopAppBar(
                    title = { Text("Futures Order Book Details") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = localCurrentThreshold,
                        onValueChange = { localCurrentThreshold = it },
                        label = { Text("Filter Threshold") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface)
                    )

                    Button(
                        onClick = {
                            viewModel.switchSymbol(
                                stableFuturesData!!.symbol,
                                localCurrentThreshold.toDoubleOrNull() ?: 50.0
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2E86DE)
                        )
                    ) {
                        Text("Apply", color = Color.White)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Asks Section
            item {
                Text(
                    text = "Ask Book (${stableFuturesData!!.asks.size} orders)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFFFA502),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            // Ask headers
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Price",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Quantity",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1f)
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            }
            
            items(stableFuturesData!!.asks.filter { it.quantity >= (localCurrentThreshold.toDoubleOrNull() ?: 50.0) }) { ask ->
                OrderRowWithBarView(
                    entry = ask,
                    maxQuantity = stableFuturesData!!.maxQuantity,
                    isBid = false,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // Bids Section
            item {
                Text(
                    text = "Bid Book (${stableFuturesData!!.bids.size} orders)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF2E86DE),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            // Bid headers
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Price",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Quantity",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1f)
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            }
            
            items(stableFuturesData!!.bids.filter { it.quantity >= (localCurrentThreshold.toDoubleOrNull() ?: 50.0) }) { bid ->
                OrderRowWithBarView(
                    entry = bid,
                    maxQuantity = stableFuturesData!!.maxQuantity,
                    isBid = true,
                    modifier = Modifier.fillMaxWidth()
                )
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
                            text = "Loading Order Book Details...",
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}