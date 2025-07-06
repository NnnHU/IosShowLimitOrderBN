package com.pythonn.androidshowlimitorderbn.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pythonn.androidshowlimitorderbn.data.models.MarketDepthData
import com.pythonn.androidshowlimitorderbn.data.repository.OrderBookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import com.pythonn.androidshowlimitorderbn.utils.SharedPreferencesHelper

class MarketDataViewModel(
    private val repository: OrderBookRepository,
    private val sharedPrefsHelper: SharedPreferencesHelper
) : ViewModel() {

    private val _spotMarketData = MutableStateFlow<MarketDepthData?>(null)
    val spotMarketData: StateFlow<MarketDepthData?> = _spotMarketData.asStateFlow()

    private val _futuresMarketData = MutableStateFlow<MarketDepthData?>(null)
    val futuresMarketData: StateFlow<MarketDepthData?> = _futuresMarketData.asStateFlow()

    private val _currentThreshold = MutableStateFlow(50.0)
    val currentThreshold: StateFlow<Double> = _currentThreshold.asStateFlow()

    init {
        _currentThreshold.value = sharedPrefsHelper.getThreshold()
        viewModelScope.launch {
            repository.spotMarketData
                .collect {
                    _spotMarketData.value = it
                }
        }

        viewModelScope.launch {
            repository.futuresMarketData
                .collect {
                    _futuresMarketData.value = it
                }
        }
    }

    fun switchSymbol(newSymbol: String, threshold: Double = 50.0) {
        _currentThreshold.value = threshold
        sharedPrefsHelper.saveThreshold(threshold)
        viewModelScope.launch {
            repository.switchSymbol(newSymbol, threshold)
        }
    }

    override fun onCleared() {
        super.onCleared()
        repository.stopWebSocketStream()
    }
}