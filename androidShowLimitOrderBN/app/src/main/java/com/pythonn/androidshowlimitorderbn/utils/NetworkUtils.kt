package com.pythonn.androidshowlimitorderbn.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

object NetworkUtils {
    
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
    
    suspend fun testDnsResolution(hostname: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            InetAddress.getByName(hostname)
            true
        } catch (e: Exception) {
            println("DNS resolution failed for $hostname: ${e.localizedMessage}")
            false
        }
    }
    
    suspend fun testConnectivity(hostname: String, port: Int, timeoutMs: Int = 5000): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(hostname, port), timeoutMs)
                true
            }
        } catch (e: Exception) {
            println("Connectivity test failed for $hostname:$port - ${e.localizedMessage}")
            false
        }
    }
    
    suspend fun testBinanceConnectivity(): Map<String, Boolean> = withContext(Dispatchers.IO) {
        val results = mutableMapOf<String, Boolean>()
        
        // Test DNS resolution
        results["api.binance.com_dns"] = testDnsResolution("api.binance.com")
        results["fapi.binance.com_dns"] = testDnsResolution("fapi.binance.com")
        results["stream.binance.com_dns"] = testDnsResolution("stream.binance.com")
        results["fstream.binance.com_dns"] = testDnsResolution("fstream.binance.com")
        
        // Test connectivity
        results["api.binance.com_443"] = testConnectivity("api.binance.com", 443)
        results["fapi.binance.com_443"] = testConnectivity("fapi.binance.com", 443)
        results["stream.binance.com_443"] = testConnectivity("stream.binance.com", 443)
        results["fstream.binance.com_443"] = testConnectivity("fstream.binance.com", 443)
        
        // Test Google DNS as baseline
        results["google_dns_8.8.8.8"] = testConnectivity("8.8.8.8", 53)
        results["google.com_dns"] = testDnsResolution("google.com")
        
        return@withContext results
    }
    
    fun printConnectivityReport(results: Map<String, Boolean>) {
        println("=== Network Connectivity Report ===")
        results.forEach { (test, result) ->
            val status = if (result) "✓ PASS" else "✗ FAIL"
            println("$test: $status")
        }
        println("===================================")
    }
}