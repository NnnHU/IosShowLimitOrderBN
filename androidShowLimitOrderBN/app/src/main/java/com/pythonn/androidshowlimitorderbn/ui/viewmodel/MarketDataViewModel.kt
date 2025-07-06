package com.pythonn.androidshowlimitorderbn.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pythonn.androidshowlimitorderbn.data.models.MarketDepthData
import com.pythonn.androidshowlimitorderbn.data.remote.BinanceApiServiceImpl
import com.pythonn.androidshowlimitorderbn.data.repository.OrderBookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MarketDataViewModel(
    private val repository: OrderBookRepository,
    private val apiService: BinanceApiServiceImpl
) : ViewModel() {

    private val _spotMarketData = MutableStateFlow<MarketDepthData?>(null)
    val spotMarketData: StateFlow<MarketDepthData?> = _spotMarketData.asStateFlow()

    private val _futuresMarketData = MutableStateFlow<MarketDepthData?>(null)
    val futuresMarketData: StateFlow<MarketDepthData?> = _futuresMarketData.asStateFlow()

    fun switchSymbol(newSymbol: String, threshold: Double = 50.0) {
        viewModelScope.launch {
            repository.switchSymbol(newSymbol, threshold)
        }
    }

    override fun onCleared() {
        super.onCleared()
        repository.stopWebSocketStream()
    }
}
