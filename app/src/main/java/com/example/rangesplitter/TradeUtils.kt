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
                    val adapter = OpenPositionsAdapter(openPositions) { position ->
                        // Handle cancel action for the open position
                        cancelPosition(bybitClient, position, recyclerView,
                            { Log.d("CancelPosition", it) },
                            { Log.e("CancelPosition", it) }
                        )
                    }
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



    fun cancelPosition(
        bybitClient: ByBitRestClient,
        position: OpenPosition,
        recyclerView: RecyclerView,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        // Assuming you have a function to close or reduce positions
        val params = CancelOrderParams( // Replace this with correct position cancellation logic
            category = Category.linear,
            symbol = position.symbol,
            orderId = "position-close" // This is a placeholder; actual API call may differ
        )

        val callback = object : ByBitRestApiCallback<CancelOrderResponse> {
            override fun onSuccess(result: CancelOrderResponse) {
                Log.d("CancelPosition", "Successfully closed position: ${position.symbol}")
                onSuccess("Successfully closed position: ${position.symbol}")
                fetchOpenPositions(bybitClient, recyclerView) // Refresh the open positions
            }

            override fun onError(error: Throwable) {
                Log.e("CancelPosition", "Error: ${error.message}")
                onError("Failed to close position: ${error.message}")
            }
        }

        bybitClient.orderClient.cancelOrder(params, callback) // Replace with correct position-related API if needed
    }

}
