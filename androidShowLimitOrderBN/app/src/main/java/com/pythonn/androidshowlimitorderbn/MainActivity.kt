package com.pythonn.androidshowlimitorderbn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.gson.Gson
import com.pythonn.androidshowlimitorderbn.data.database.AppDatabase
import com.pythonn.androidshowlimitorderbn.data.remote.BinanceApiServiceImpl
import com.pythonn.androidshowlimitorderbn.data.repository.OrderBookRepository
import com.pythonn.androidshowlimitorderbn.ui.navigation.AppNavigation
import com.pythonn.androidshowlimitorderbn.ui.theme.AndroidShowLimitOrderBNTheme
import com.pythonn.androidshowlimitorderbn.ui.viewmodel.MarketDataViewModel
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val okHttpClient = OkHttpClient.Builder()
                    .pingInterval(3, TimeUnit.MINUTES)
                    .build()
                val gson = Gson()
                val apiService = BinanceApiServiceImpl(okHttpClient, gson)
                val database = AppDatabase.getDatabase(applicationContext)
                val repository = OrderBookRepository(apiService, database.orderBookDao(), database.cacheMetadataDao())
                return MarketDataViewModel(repository, apiService) as T
            }
        }
        val marketDataViewModel = ViewModelProvider(this, factory).get(MarketDataViewModel::class.java)

        setContent {
            AndroidShowLimitOrderBNTheme {
                AppNavigation(viewModel = marketDataViewModel)
            }
        }
    }
}
