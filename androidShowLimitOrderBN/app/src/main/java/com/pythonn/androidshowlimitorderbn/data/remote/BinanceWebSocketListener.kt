package com.pythonn.androidshowlimitorderbn.data.remote

import com.google.gson.Gson
import com.pythonn.androidshowlimitorderbn.data.models.DepthUpdateEvent
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

class BinanceWebSocketListener(
    private val onMessageReceived: (DepthUpdateEvent) -> Unit,
    private val onConnectionOpened: () -> Unit,
    private val onConnectionClosed: () -> Unit,
    private val onFailure: (Throwable, Response?) -> Unit
) : WebSocketListener() {

    private val gson = Gson()

    override fun onOpen(webSocket: WebSocket, response: Response) {
        super.onOpen(webSocket, response)
        onConnectionOpened()
        println("WebSocket Opened: ${response.message}")
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        super.onMessage(webSocket, text)
        try {
            // Check if this is an error message from Binance
            if (text.contains("\"error\"")) {
                println("Received WebSocket error message: $text")
                return
            }
            
            // Check if this is a ping/pong or other non-data message
            if (text == "pong" || text.isEmpty()) {
                println("Received WebSocket ping/pong: $text")
                return
            }
            
            val event = gson.fromJson(text, DepthUpdateEvent::class.java)
            
            // Validate that required fields are not null
            if (event.symbol.isNullOrEmpty() || event.eventType.isNullOrEmpty()) {
                println("Received invalid WebSocket message with missing required fields: $text")
                return
            }
            
            onMessageReceived(event)
        } catch (e: Exception) {
            println("Error parsing WebSocket message: ${e.localizedMessage}")
            println("Received text: $text")
        }
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        super.onMessage(webSocket, bytes)
        println("Received bytes: ${bytes.hex()}")
    }

    // Note: OkHttp automatically handles ping/pong frames
    // The server sends ping frames every 3 minutes and OkHttp responds with pong frames automatically

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        super.onClosing(webSocket, code, reason)
        println("WebSocket Closing: $code / $reason")
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        super.onClosed(webSocket, code, reason)
        onConnectionClosed()
        println("WebSocket Closed: $code / $reason")
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        super.onFailure(webSocket, t, response)
        onFailure(t, response)
        println("WebSocket Failure: ${t.localizedMessage}")
        response?.let { println("Response: ${it.message}") }
    }
}