package com.example.rangesplitter.ws

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.TimeUnit

object BybitLinearTickerWebSocket : WebSocketListener() {

    private const val TAG = "BybitLinearWS"
    private const val WS_URL = "wss://stream-testnet.bybit.com/v5/public/linear"

    // -------- DATA MODEL --------
    data class TickerUpdate(
        val symbol: String,
        val lastPrice: Double? = null,
        val indexPrice: Double? = null,
        val markPrice: Double? = null,
        val price24hPcnt: Double? = null,
        val high24h: Double? = null,
        val low24h: Double? = null,
        val volume24h: Double? = null,
        val openInterest: Double? = null,
        val openInterestValue: Double? = null
    ) {
        /** Merge a delta update into this full snapshot. */
        fun merge(delta: TickerUpdate): TickerUpdate = copy(
            lastPrice =       delta.lastPrice       ?: this.lastPrice,
            indexPrice =      delta.indexPrice      ?: this.indexPrice,
            markPrice =       delta.markPrice       ?: this.markPrice,
            price24hPcnt =    delta.price24hPcnt    ?: this.price24hPcnt,
            high24h =         delta.high24h         ?: this.high24h,
            low24h =          delta.low24h          ?: this.low24h,
            volume24h =       delta.volume24h       ?: this.volume24h,
            openInterest =    delta.openInterest    ?: this.openInterest,
            openInterestValue = delta.openInterestValue ?: this.openInterestValue
        )
    }

    interface TickerListener {
        fun onTicker(update: TickerUpdate)
    }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    private var webSocket: WebSocket? = null

    private val subscribedSymbols = mutableSetOf<String>()
    private val listeners = CopyOnWriteArraySet<TickerListener>()

    // cache of *full* merged ticker state per symbol
    private val tickerCache = mutableMapOf<String, TickerUpdate>()

    // -------- PUBLIC API --------

    fun addListener(listener: TickerListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: TickerListener) {
        listeners.remove(listener)
    }

    fun subscribe(symbol: String) {
        if (subscribedSymbols.contains(symbol)) return
        subscribedSymbols.add(symbol)
        ensureConnected()

        val msg = """
            {
              "op": "subscribe",
              "args": ["tickers.$symbol"]
            }
        """.trimIndent()
        webSocket?.send(msg)
        Log.d(TAG, "Sent subscribe for $symbol")
    }

    fun unsubscribe(symbol: String) {
        if (!subscribedSymbols.contains(symbol)) return
        subscribedSymbols.remove(symbol)

        val msg = """
            {
              "op": "unsubscribe",
              "args": ["tickers.$symbol"]
            }
        """.trimIndent()
        webSocket?.send(msg)
        Log.d(TAG, "Sent unsubscribe for $symbol")

        tickerCache.remove(symbol)
    }

    fun disconnect() {
        Log.d(TAG, "Disconnecting WebSocket")
        webSocket?.close(1000, "Normal Closure")
        webSocket = null
        tickerCache.clear()
    }

    // -------- INTERNAL --------

    private fun ensureConnected() {
        if (webSocket != null) return

        val request = Request.Builder()
            .url(WS_URL)
            .build()

        webSocket = client.newWebSocket(request, this)
        Log.d(TAG, "Connecting WebSocket...")
    }

    // -------- CALLBACKS --------

    override fun onOpen(webSocket: WebSocket, response: Response) {
        Log.d(TAG, "WebSocket opened")

        if (subscribedSymbols.isNotEmpty()) {
            val args = subscribedSymbols.joinToString(
                separator = "\",\"tickers.",
                prefix = "tickers.",
                postfix = ""
            )
            val msg = """
                {
                  "op": "subscribe",
                  "args": ["$args"]
                }
            """.trimIndent()
            webSocket.send(msg)
            Log.d(TAG, "Re-subscribed to: $subscribedSymbols")
        }
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        Log.d(TAG, "WS message: $text")

        try {
            val json = JSONObject(text)

            val topic = json.optString("topic", "")
            if (!topic.startsWith("tickers.")) return

            // "data" is an OBJECT in v5 ticker (delta or snapshot)
            val dataObj = json.optJSONObject("data") ?: return

            val symbol = dataObj.optString("symbol", "")
            if (symbol.isEmpty()) return

            val partial = TickerUpdate(
                symbol = symbol,
                lastPrice        = dataObj.optString("lastPrice", null)?.toDoubleOrNull(),
                indexPrice       = dataObj.optString("indexPrice", null)?.toDoubleOrNull(),
                markPrice        = dataObj.optString("markPrice", null)?.toDoubleOrNull(),
                price24hPcnt     = dataObj.optString("price24hPcnt", null)?.toDoubleOrNull(),
                high24h          = dataObj.optString("highPrice24h", null)?.toDoubleOrNull(),
                low24h           = dataObj.optString("lowPrice24h", null)?.toDoubleOrNull(),
                volume24h        = dataObj.optString("volume24h", null)?.toDoubleOrNull(),
                openInterest     = dataObj.optString("openInterest", null)?.toDoubleOrNull(),
                openInterestValue= dataObj.optString("openInterestValue", null)?.toDoubleOrNull()
            )

            // Merge delta with previous full state
            val merged = tickerCache[symbol]?.merge(partial) ?: partial
            tickerCache[symbol] = merged

            // Notify listeners with the merged state
            for (listener in listeners) {
                try {
                    listener.onTicker(merged)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in listener: ${e.message}", e)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse WS message: $text", e)
        }
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        Log.e(TAG, "WebSocket failure: ${t.message}", t)
        this.webSocket = null
        // (optional) reconnection logic here
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        Log.d(TAG, "WebSocket closed: $code $reason")
        this.webSocket = null
    }
}
