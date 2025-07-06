package com.pythonn.androidshowlimitorderbn.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pythonn.androidshowlimitorderbn.ui.viewmodel.MarketDataViewModel
import com.pythonn.androidshowlimitorderbn.ui.components.OrderRowWithBarView

@Composable
fun SpotOrderBookDetailsScreen(viewModel: MarketDataViewModel) {
    val spotData by viewModel.spotMarketData.collectAsState()
    
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
                Text(
                    text = "Spot Order Book Details",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
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
            
            items(stableSpotData!!.asks) { ask ->
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
            
            items(stableSpotData!!.bids) { bid ->
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

@Composable
fun FuturesOrderBookDetailsScreen(viewModel: MarketDataViewModel) {
    val futuresData by viewModel.futuresMarketData.collectAsState()
    
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
                Text(
                    text = "Futures Order Book Details",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
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
            
            items(stableFuturesData!!.asks) { ask ->
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
            
            items(stableFuturesData!!.bids) { bid ->
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