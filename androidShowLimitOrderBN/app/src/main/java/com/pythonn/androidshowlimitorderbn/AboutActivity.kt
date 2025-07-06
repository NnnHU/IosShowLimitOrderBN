package com.pythonn.androidshowlimitorderbn

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.pythonn.androidshowlimitorderbn.data.remote.BinanceApiServiceImpl
import com.pythonn.androidshowlimitorderbn.data.remote.ConnectionStatus
import com.pythonn.androidshowlimitorderbn.data.remote.DataQuality
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AboutActivity : AppCompatActivity() {
    
    private lateinit var connectionStatusText: TextView
    private lateinit var closeButton: Button
    private var apiService: BinanceApiServiceImpl? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        println("AboutActivity: onCreate started")
        
        try {
            setContentView(R.layout.activity_about)
            println("AboutActivity: setContentView completed")
            
            initializeViews()
            println("AboutActivity: initializeViews completed")
            
            setupClickListeners()
            println("AboutActivity: setupClickListeners completed")
            
            observeConnectionStatus()
            println("AboutActivity: observeConnectionStatus completed")
            
            println("AboutActivity: onCreate completed successfully")
        } catch (e: Exception) {
            println("AboutActivity: Error in onCreate: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }
    
    private fun initializeViews() {
        connectionStatusText = findViewById(R.id.connectionStatusText)
        closeButton = findViewById(R.id.closeButton)
    }
    
    private fun setupClickListeners() {
        closeButton.setOnClickListener {
            finish()
        }
    }
    
    private fun observeConnectionStatus() {
        // Try to get the API service instance from the application or MainActivity
        // For now, we'll show a generic status
        updateConnectionStatus(null)
        
        // If we had access to the API service, we could observe real status:
        // lifecycleScope.launch {
        //     apiService?.connectionStatus?.collect { status ->
        //         updateConnectionStatus(status)
        //     }
        // }
    }
    
    private fun updateConnectionStatus(status: ConnectionStatus?) {
        val statusText = if (status != null) {
            val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val lastUpdate = timeFormat.format(Date(status.lastUpdateTime))
            
            val connectionText = if (status.isConnected) "✅ Connected" else "❌ Disconnected"
            val qualityText = when (status.dataQuality) {
                DataQuality.REAL_TIME -> "📡 Real-time"
                DataQuality.CACHED -> "💾 Cached"
                DataQuality.DEGRADED -> "⚠️ Degraded"
            }
            
            "$connectionText\n" +
            "Symbol: ${status.symbol}\n" +
            "Market: ${if (status.isFutures) "Futures" else "Spot"}\n" +
            "Data Quality: $qualityText\n" +
            "Last Update: $lastUpdate\n" +
            "Reconnect Attempts: ${status.reconnectAttempts}"
        } else {
            "📊 Monitoring Binance WebSocket connections\n" +
            "🔄 Automatic reconnection enabled\n" +
            "⚡ Real-time market data streaming\n" +
            "🛡️ Smart error handling active\n" +
            "💾 Intelligent caching system\n\n" +
            "Note: Detailed connection status available when actively trading"
        }
        
        connectionStatusText.text = statusText
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh connection status when activity resumes
        observeConnectionStatus()
    }
}