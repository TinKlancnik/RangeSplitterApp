import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import bybit.sdk.rest.ByBitRestApiCallback
import bybit.sdk.rest.ByBitRestClient
import bybit.sdk.rest.account.WalletBalanceParams
import bybit.sdk.rest.market.InstrumentsInfoParams
import bybit.sdk.rest.market.InstrumentsInfoResponse
import bybit.sdk.rest.market.InstrumentsInfoResultItem
import bybit.sdk.rest.okHttpClientProvider
import bybit.sdk.rest.order.CancelOrderParams
import bybit.sdk.rest.order.CancelOrderResponse
import bybit.sdk.rest.order.PlaceOrderParams
import bybit.sdk.rest.order.PlaceOrderResponse
import bybit.sdk.shared.Category
import bybit.sdk.shared.OrderType
import bybit.sdk.shared.Side
import bybit.sdk.shared.TimeInForce
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import bybit.sdk.rest.order.OrdersOpenParams
import bybit.sdk.rest.order.OrdersOpenResponse
import bybit.sdk.rest.position.ByBitPositionClient
import bybit.sdk.rest.position.PositionInfoParams
import bybit.sdk.rest.position.PositionInfoResponse
import com.example.rangesplitter.OpenOrdersAdapter
import com.example.rangesplitter.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import bybit.sdk.rest.market.KLineParams
import bybit.sdk.rest.market.KLineResponse
import bybit.sdk.shared.AccountType
import kotlinx.coroutines.withContext

data class OpenOrder(
    val symbol: String,
    val triggerPrice: String,
    val side: String,
    val quantity: String,
    val orderId: String
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

    fun getByBitClient(): ByBitRestClient {
        val apiKey = "UV6R9A3gNuk9vl0vVQ"
        val apiSecret = "vRdpemzToMITR53ftZSM3ar7kSdx6NeodJTn"
        return ByBitRestClient(apiKey, apiSecret, true, httpClientProvider = okHttpClientProvider)
    }

    fun fetchOpenOrders(
        bybitClient: ByBitRestClient,
        recyclerView: RecyclerView
    ) {
        val params = OrdersOpenParams(category = Category.linear, settleCoin = "USDT")

        val callback = object : ByBitRestApiCallback<OrdersOpenResponse> {
            override fun onSuccess(result: OrdersOpenResponse) {
                val openOrders = result.result.list.map {
                    OpenOrder(
                        symbol = it.symbol,
                        triggerPrice = it.price,
                        side = it.side.toString(),
                        quantity = it.qty,
                        orderId = it.orderId
                    )
                }

                recyclerView.post {
                    val adapter = OpenOrdersAdapter(openOrders) { order ->
                        cancelOrder(bybitClient, order, recyclerView,
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
        }

        bybitClient.orderClient.ordersOpen(params, callback)
    }

    fun cancelOrder(
        bybitClient: ByBitRestClient,
        order: OpenOrder,
        recyclerView: RecyclerView,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val params = CancelOrderParams(
            category = Category.linear,
            symbol = order.symbol,
            orderId = order.orderId
        )

        val callback = object : ByBitRestApiCallback<CancelOrderResponse> {
            override fun onSuccess(result: CancelOrderResponse) {
                Log.d("CancelOrder", "Success: ${result.result.orderId}")
                onSuccess("Successfully cancelled order: ${order.orderId}")
                fetchOpenOrders(bybitClient, recyclerView)
            }

            override fun onError(error: Throwable) {
                Log.e("CancelOrder", "Error: ${error.message}")
                onError("Failed to cancel order: ${error.message}")
            }
        }

        bybitClient.orderClient.cancelOrder(params, callback)
    }

    fun fetchOpenPositions(
        bybitClient: ByBitRestClient,
        recyclerView: RecyclerView
    ) {
        val params = PositionInfoParams(category = Category.linear, settleCoin = "USDT")

        val callback = object : ByBitRestApiCallback<PositionInfoResponse> {
            override fun onSuccess(result: PositionInfoResponse) {
                // Map the result to a list of OpenPosition objects
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

                // Update the RecyclerView with the fetched open positions
                recyclerView.post {
                    // You should define an adapter for OpenPositions
                    val adapter = OpenPositionsAdapter(openPositions)
                    recyclerView.adapter = adapter
                }
            }

            override fun onError(error: Throwable) {
                Log.e("OpenPositions", "Error: ${error.message}")
            }
        }

        bybitClient.positionClient.getPositionInfo(params, callback)
    }


    fun startPeriodicUpdate(bybitClient: ByBitRestClient, recyclerView: RecyclerView) {
        val updateInterval = 5000L // 5 seconds interval

        // Handler to run the task periodically
        val handler = Handler(Looper.getMainLooper())

        val updateRunnable = object : Runnable {
            override fun run() {
                // Fetch the latest open positions
                fetchOpenPositions(bybitClient, recyclerView)

                // Post the Runnable to run after the specified interval
                handler.postDelayed(this, updateInterval)
            }
        }

        // Start the periodic update
        handler.post(updateRunnable)
    }

    fun stopPeriodicUpdate(handler: Handler) {
        // Remove all callbacks to stop the periodic updates
        handler.removeCallbacksAndMessages(null)
    }

    fun fetchKlines(
        bybitClient: ByBitRestClient,
        symbol: String,
        interval: String = "15",
        limit: Int = 100,
        onSuccess: (List<Candle>) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        Log.d("KlineFetch", "Fetching klines: symbol=$symbol, interval=$interval, limit=$limit")

        val params = KLineParams(
            category = Category.linear,
            symbol = symbol,
            interval = interval,
            limit = limit
        )

        val callback = object : ByBitRestApiCallback<KLineResponse> {
            override fun onSuccess(result: KLineResponse) {
                Log.d("KlineFetch", "API response success")

                if (result.result.list.isNullOrEmpty()) {
                    Log.w("KlineFetch", "Kline response is empty")
                }

                val candles = result.result.list.map { item ->
                    Log.d("KlineFetch", "Raw item: $item")
                    Candle(
                        time = item[0].toLong() / 1000,
                        open = item[1].toFloat(),
                        high = item[2].toFloat(),
                        low = item[3].toFloat(),
                        close = item[4].toFloat()
                    )
                }

                Log.d("KlineFetch", "Parsed ${candles.size} candles")
                onSuccess(candles)
            }

            override fun onError(error: Throwable) {
                Log.e("KlineFetch", "Error fetching klines: ${error.message}", error)
                onError(error)
            }
        }

        try {
            bybitClient.marketClient.getKline(params, callback)
        } catch (e: Exception) {
            Log.e("KlineFetch", "Exception while fetching klines", e)
            onError(e)
        }
    }

    object TradeUtils {
        fun fetchBalance(onResult: (String) -> Unit) {
            val apiKey = "UV6R9A3gNuk9vl0vVQ"
            val apiSecret = "vRdpemzToMITR53ftZSM3ar7kSdx6NeodJTn"

            val bybitClient = ByBitRestClient(apiKey, apiSecret, true, httpClientProvider = okHttpClientProvider)

            CoroutineScope(Dispatchers.IO).launch {
                val walletBalanceResponse = bybitClient.accountClient.getWalletBalanceBlocking(
                    WalletBalanceParams(AccountType.UNIFIED, listOf("BTC"))
                )
                val totalEquity = walletBalanceResponse.result?.list?.get(0)?.totalEquity
                val formattedBalance = totalEquity?.toDoubleOrNull()?.let {
                    String.format("%.2f", it)
                } ?: "Error"

                withContext(Dispatchers.Main) {
                    onResult(formattedBalance)
                }
            }
        }
    }
    fun fetchBalance(onResult: (String) -> Unit) {
        val apiKey = "UV6R9A3gNuk9vl0vVQ"
        val apiSecret = "vRdpemzToMITR53ftZSM3ar7kSdx6NeodJTn"

        val bybitClient = ByBitRestClient(apiKey, apiSecret, true, httpClientProvider = okHttpClientProvider)

        CoroutineScope(Dispatchers.IO).launch {
            val walletBalanceResponse = bybitClient.accountClient.getWalletBalanceBlocking(
                WalletBalanceParams(AccountType.UNIFIED, listOf("BTC"))
            )
            val totalEquity = walletBalanceResponse.result?.list?.get(0)?.totalEquity
            val formattedBalance = totalEquity?.toDoubleOrNull()?.let {
                String.format("%.2f", it)
            } ?: "Error"

            withContext(Dispatchers.Main) {
                onResult(formattedBalance)
            }
        }
    }

}
