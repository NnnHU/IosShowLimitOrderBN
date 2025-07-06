package com.pythonn.androidshowlimitorderbn.ui.viewmodel

import com.pythonn.androidshowlimitorderbn.data.repository.OrderBookRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlinx.coroutines.flow.MutableStateFlow
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import com.pythonn.androidshowlimitorderbn.utils.SharedPreferencesHelper
import android.content.Context

@ExperimentalCoroutinesApi
class MarketDataViewModelTest {

    private lateinit var viewModel: MarketDataViewModel
    private val mockRepository: OrderBookRepository = mock()
    private val mockSharedPrefsHelper: SharedPreferencesHelper = mock()
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        // Mock the StateFlows returned by the repository
        whenever(mockRepository.spotMarketData).thenReturn(MutableStateFlow(null))
        whenever(mockRepository.futuresMarketData).thenReturn(MutableStateFlow(null))
        whenever(mockSharedPrefsHelper.getThreshold()).thenReturn(50.0)
        viewModel = MarketDataViewModel(mockRepository, mockSharedPrefsHelper)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `switchSymbol calls repository switchSymbol`() = runTest {
        val symbol = "BTCUSDT"
        val threshold = 50.0

        viewModel.switchSymbol(symbol, threshold)

        testDispatcher.scheduler.advanceUntilIdle() // Ensure coroutine launched by viewModelScope is executed

        verify(mockRepository).switchSymbol(symbol, threshold)
        verify(mockSharedPrefsHelper).saveThreshold(threshold)
    }
}