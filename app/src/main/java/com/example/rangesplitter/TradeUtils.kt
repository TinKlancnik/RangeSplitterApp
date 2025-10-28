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
                        cancelOrder(order, recyclerView,
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

}
