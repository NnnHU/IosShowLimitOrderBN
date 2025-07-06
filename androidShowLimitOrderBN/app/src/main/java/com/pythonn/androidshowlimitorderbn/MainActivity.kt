package com.pythonn.androidshowlimitorderbn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.google.gson.Gson
import com.pythonn.androidshowlimitorderbn.data.database.AppDatabase
import com.pythonn.androidshowlimitorderbn.data.remote.BinanceApiServiceImpl
import com.pythonn.androidshowlimitorderbn.data.repository.OrderBookRepository
import com.pythonn.androidshowlimitorderbn.ui.navigation.AppNavigation
import com.pythonn.androidshowlimitorderbn.ui.theme.AndroidShowLimitOrderBNTheme
import com.pythonn.androidshowlimitorderbn.ui.viewmodel.MarketDataViewModel
import com.pythonn.androidshowlimitorderbn.utils.SharedPreferencesHelper
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val apiService = BinanceApiServiceImpl()
                val database = Room.databaseBuilder(
                    applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                ).addMigrations(AppDatabase.MIGRATION_1_2).build()
                val repository = OrderBookRepository(apiService, database.orderBookDao(), database.cacheMetadataDao())
                val sharedPrefsHelper = SharedPreferencesHelper(applicationContext)
                return MarketDataViewModel(repository, sharedPrefsHelper) as T
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