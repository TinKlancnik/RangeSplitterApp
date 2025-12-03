package com.example.rangesplitter

import OpenPositionsAdapter
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import bybit.sdk.rest.ByBitRestApiCallback
import bybit.sdk.rest.account.WalletBalanceParams
import bybit.sdk.rest.market.KLineParams
import bybit.sdk.rest.market.KLineResponse
import bybit.sdk.rest.order.CancelOrderParams
import bybit.sdk.rest.order.CancelOrderResponse
import bybit.sdk.rest.order.OrdersOpenParams
import bybit.sdk.rest.order.OrdersOpenResponse
import bybit.sdk.rest.position.PositionInfoParams
import bybit.sdk.rest.position.PositionInfoResponse
import bybit.sdk.shared.AccountType
import bybit.sdk.shared.Category
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.util.Locale

data class Coin(
    val symbol: String,
    val priceText: String,
    val changeText: String,
    val changeValue: Double
)

enum class SortMode {
    VOLUME, CHANGE, VOLATILITY, ALPHA
}

// ---------- other models you already had ----------

data class OpenOrder(
    val symbol: String,
    val triggerPrice: String,
    val side: String,
    val quantity: String,
    val orderId: String,
    val takeProfit: String?,
    val stopLoss: String?
)

data class OpenPosition(
    val symbol: String,
    val side: String,
    val size: String,
    val avgPrice: String,
    val leverage: String,
    val unrealisedPnl: String
)

data class Candle(
    val time: Long,
    val open: Float,
    val high: Float,
    val low: Float,
    val close: Float
)

object TradeUtils {

    // reuse single client + main-thread handler
    private val httpClient = OkHttpClient()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun fetchOpenOrders(
        recyclerView: RecyclerView
    ) {
        val client = BybitClientManager.createClient()
        val params = OrdersOpenParams(category = Category.linear, settleCoin = "USDT")

        client.orderClient.ordersOpen(params, object : ByBitRestApiCallback<OrdersOpenResponse> {
            override fun onSuccess(result: OrdersOpenResponse) {
                val openOrders = result.result.list.map {
                    OpenOrder(
                        symbol = it.symbol,
                        triggerPrice = it.price,
                        side = it.side.toString(),
                        quantity = it.qty,
                        orderId = it.orderId,
                        takeProfit = it.takeProfit,
                        stopLoss = it.stopLoss
                    )
                }

                recyclerView.post {
                    val adapter = OpenOrdersAdapter(openOrders) { order ->
                        cancelOrder(
                            order,
                            recyclerView,
                            { Log.d("CancelOrder", it) },
                            { Log.e("CancelOrder", it) }
                        )
                    }
                    recyclerView.adapter = adapter
                }
            }

            override fun onError(error: Throwable) {
                Log.e("OpenOrders", "Error: ${error.message}")
            }
        })
    }

    fun cancelOrder(
        order: OpenOrder,
        recyclerView: RecyclerView,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val client = BybitClientManager.createClient()
        val params = CancelOrderParams(
            category = Category.linear,
            symbol = order.symbol,
            orderId = order.orderId
        )

        client.orderClient.cancelOrder(params, object : ByBitRestApiCallback<CancelOrderResponse> {
            override fun onSuccess(result: CancelOrderResponse) {
                Log.d("CancelOrder", "Success: ${result.result.orderId}")
                onSuccess("Successfully cancelled order: ${order.orderId}")
                fetchOpenOrders(recyclerView)
            }

            override fun onError(error: Throwable) {
                Log.e("CancelOrder", "Error: ${error.message}")
                onError("Failed to cancel order: ${error.message}")
            }
        })
    }

    fun fetchOpenPositions(recyclerView: RecyclerView) {
        val client = BybitClientManager.createClient()
        val params = PositionInfoParams(category = Category.linear, settleCoin = "USDT")

        client.positionClient.getPositionInfo(params, object : ByBitRestApiCallback<PositionInfoResponse> {
            override fun onSuccess(result: PositionInfoResponse) {
                val openPositions = result.result.list.map {
                    OpenPosition(
                        symbol = it.symbol,
                        avgPrice = it.avgPrice,
                        side = it.side.toString(),
                        leverage = it.leverage,
                        size = it.size,
                        unrealisedPnl = it.unrealisedPnl
                    )
                }

                recyclerView.post {
                    val adapter = OpenPositionsAdapter(openPositions)
                    recyclerView.adapter = adapter
                }
            }

            override fun onError(error: Throwable) {
                Log.e("OpenPositions", "Error: ${error.message}")
            }
        })
    }

    fun startPeriodicUpdate(recyclerView: RecyclerView): Handler {
        val handler = Handler(Looper.getMainLooper())
        val updateInterval = 5000L

        val updateRunnable = object : Runnable {
            override fun run() {
                fetchOpenPositions(recyclerView)
                handler.postDelayed(this, updateInterval)
            }
        }

        handler.post(updateRunnable)
        return handler
    }

    fun stopPeriodicUpdate(handler: Handler) {
        handler.removeCallbacksAndMessages(null)
    }

    fun fetchKlines(
        symbol: String,
        interval: String = "15",
        limit: Int = 100,
        onSuccess: (List<Candle>) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val client = BybitClientManager.createClient()
        val params = KLineParams(
            category = Category.linear,
            symbol = symbol,
            interval = interval,
            limit = limit
        )

        client.marketClient.getKline(params, object : ByBitRestApiCallback<KLineResponse> {
            override fun onSuccess(result: KLineResponse) {
                val candles = result.result.list.map { item ->
                    Candle(
                        time = item[0].toLong() / 1000,
                        open = item[1].toFloat(),
                        high = item[2].toFloat(),
                        low = item[3].toFloat(),
                        close = item[4].toFloat()
                    )
                }
                onSuccess(candles)
            }

            override fun onError(error: Throwable) {
                Log.e("KlineFetch", "Error: ${error.message}", error)
                onError(error)
            }
        })
    }

    fun fetchBalance(onResult: (String) -> Unit) {
        val client = BybitClientManager.createClient()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = client.accountClient.getWalletBalanceBlocking(
                    WalletBalanceParams(AccountType.UNIFIED, listOf("USDT"))
                )
                Log.d("FetchBalance", "Wallet response: $response")

                val totalEquity = response.result?.list?.firstOrNull()?.totalEquity
                val formattedBalance = totalEquity?.toDoubleOrNull()?.let {
                    String.format("%.2f", it)
                } ?: "Error"

                withContext(Dispatchers.Main) {
                    onResult(formattedBalance)
                }
            } catch (e: Exception) {
                Log.e("FetchBalance", "Exception: ${e.message}")
                withContext(Dispatchers.Main) {
                    onResult("Error")
                }
            }
        }
    }

    fun fetchTopCoinsWithPrices(
        sortMode: SortMode,
        limit: Int = 50,
        onSuccess: (List<Coin>) -> Unit,
        onError: (String) -> Unit
    ) {
        val url = "https://api-testnet.bybit.com/v5/market/tickers?category=linear"

        val request = Request.Builder()
            .url(url)
            .build()

        Thread {
            try {
                val response: Response = httpClient.newCall(request).execute()

                if (response.isSuccessful) {
                    val jsonResponse = JSONObject(response.body?.string() ?: "")
                    val result = jsonResponse.getJSONObject("result")
                    val list = result.getJSONArray("list")

                    data class RawCoin(
                        val symbol: String,
                        val lastPrice: String,
                        val turnover24h: Double,
                        val volume24h: Double,
                        val change24hRate: Double,
                        val volatility: Double
                    )

                    val rawCoins = mutableListOf<RawCoin>()

                    for (i in 0 until list.length()) {
                        val ticker = list.getJSONObject(i)
                        val symbol = ticker.getString("symbol")
                        val lastPrice = ticker.getString("lastPrice")

                        val turnoverStr = ticker.optString("turnover24h", "0")
                        val volumeStr = ticker.optString("volume24h", "0")
                        val changeStr = ticker.optString("price24hPcnt", "0")
                        val highStr = ticker.optString("highPrice24h", "0")
                        val lowStr = ticker.optString("lowPrice24h", "0")

                        val turnover = turnoverStr.toDoubleOrNull() ?: 0.0
                        val volume = volumeStr.toDoubleOrNull() ?: 0.0
                        val changeRate = changeStr.toDoubleOrNull() ?: 0.0
                        val high = highStr.toDoubleOrNull() ?: 0.0
                        val low = lowStr.toDoubleOrNull() ?: 0.0
                        val volatility = high - low

                        rawCoins.add(
                            RawCoin(
                                symbol = symbol,
                                lastPrice = lastPrice,
                                turnover24h = turnover,
                                volume24h = volume,
                                change24hRate = changeRate,
                                volatility = volatility
                            )
                        )
                    }

                    val filtered = rawCoins.filter { it.symbol.endsWith("USDT") }

                    val sorted = when (sortMode) {
                        SortMode.VOLUME -> filtered.sortedByDescending { it.turnover24h }
                        SortMode.CHANGE -> filtered.sortedByDescending { it.change24hRate }
                        SortMode.VOLATILITY -> filtered.sortedByDescending { it.volatility }
                        SortMode.ALPHA -> filtered.sortedBy { it.symbol }
                    }

                    val top = sorted.take(limit)

                    val newCoins = top.map { raw ->
                        val pct = raw.change24hRate * 100.0
                        val pctText = if (pct >= 0) {
                            String.format(Locale.US, "+%.2f%%", pct)
                        } else {
                            String.format(Locale.US, "%.2f%%", pct)
                        }

                        Coin(
                            symbol = raw.symbol,
                            priceText = raw.lastPrice,
                            changeText = pctText,
                            changeValue = pct
                        )
                    }

                    mainHandler.post {
                        onSuccess(newCoins)
                    }

                    Log.d("CoinList", "Loaded ${newCoins.size} coins, mode=$sortMode")
                } else {
                    val msg = "Failed to fetch tickers: ${response.code}"
                    Log.d("CoinList", msg)
                    mainHandler.post { onError(msg) }
                }
            } catch (e: Exception) {
                val msg = "Error: ${e.message}"
                Log.d("CoinList", msg)
                mainHandler.post { onError(msg) }
            }
        }.start()
    }
}