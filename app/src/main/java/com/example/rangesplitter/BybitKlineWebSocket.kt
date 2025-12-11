package com.example.rangesplitter

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject

// At top of BybitKlineWebSocket.kt
data class KlineBar(
    val time: Long,   // SECONDS (to match Candle.time)
    val open: Float,
    val high: Float,
    val low: Float,
    val close: Float
)


/**
 * Minimal Bybit kline WebSocket helper, meant to be used like
 * Lightweight Charts streaming:
 *
 * - First call setData([...]) with history
 * - Then call subscribe(...) and in callback call `series.update(...)`
 */
class BybitKlineWebSocket(
    // Testnet endpoint. Change to mainnet if needed.
    private val endpointUrl: String = "wss://stream-testnet.bybit.com/v5/public/linear"
) {

    private var client: OkHttpClient? = null
    private var webSocket: WebSocket? = null

    private var currentSymbol: String? = null
    private var currentInterval: String? = null

    private var onBarCallback: ((KlineBar) -> Unit)? = null
    private var onErrorCallback: ((Throwable) -> Unit)? = null

    /**
     * Subscribe to kline.<interval>.<symbol>
     * interval examples: "1", "5", "15", "60", "240", "D"
     * symbol: "BTCUSDT", "ETHUSDT", ...
     */
    fun subscribe(
        symbol: String,
        interval: String,
        onBar: (KlineBar) -> Unit,
        onError: (Throwable) -> Unit = {}
    ) {
        // If already connected to same topic, keep it
        if (symbol == currentSymbol && interval == currentInterval && webSocket != null) {
            return
        }

        disconnect()

        currentSymbol = symbol
        currentInterval = interval
        onBarCallback = onBar
        onErrorCallback = onError

        client = OkHttpClient()

        val request = Request.Builder()
            .url(endpointUrl)
            .build()

        webSocket = client!!.newWebSocket(request, object : WebSocketListener() {

            override fun onOpen(ws: WebSocket, response: Response) {
                super.onOpen(ws, response)
                Log.d("BybitWS", "WebSocket opened")

                val topic = "kline.$interval.$symbol"
                val subMsg = JSONObject().apply {
                    put("op", "subscribe")
                    put("args", JSONArray().put(topic))
                }

                ws.send(subMsg.toString())
                Log.d("BybitWS", "Subscribed to $topic")
            }

            override fun onMessage(ws: WebSocket, text: String) {
                super.onMessage(ws, text)
                Log.d("BybitWS", "Message: $text")

                try {
                    val json = JSONObject(text)

                    // Ignore non-kline messages (like success/sub replies)
                    if (!json.has("topic")) return

                    val topic = json.getString("topic")
                    if (!topic.startsWith("kline.")) return

                    val dataArr = json.optJSONArray("data") ?: return
                    if (dataArr.length() == 0) return

                    val obj = dataArr.getJSONObject(0)

                    // Your payload:
                    // "start":1765479600000, "open":"252854.1", "high":"259779.3", "low":"252854.1", "close":"259154.9", ...
                    val startMs = obj.getLong("start")          // milliseconds
                    val open = obj.getString("open").toFloat()
                    val high = obj.getString("high").toFloat()
                    val low  = obj.getString("low").toFloat()
                    val close = obj.getString("close").toFloat()

                    // REST fetchKlines does: item[0].toLong() / 1000  → SECONDS
                    val barTime = startMs / 1000L                // convert ms → seconds

                    val bar = KlineBar(
                        time = barTime,
                        open = open,
                        high = high,
                        low = low,
                        close = close
                    )

                    onBarCallback?.invoke(bar)

                } catch (e: Exception) {
                    Log.e("BybitWS", "Error parsing WS message", e)
                    onErrorCallback?.invoke(e)
                }
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                super.onFailure(ws, t, response)
                Log.e("BybitWS", "WebSocket failure: ${t.message}", t)
                onErrorCallback?.invoke(t)
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                super.onClosed(ws, code, reason)
                Log.d("BybitWS", "WebSocket closed: $code / $reason")
            }
        })
    }

    fun disconnect() {
        try {
            webSocket?.close(1000, "Closing chart socket")
        } catch (_: Exception) {
        } finally {
            webSocket = null
        }

        try {
            client?.dispatcher?.executorService?.shutdown()
        } catch (_: Exception) {
        } finally {
            client = null
        }
    }
}
