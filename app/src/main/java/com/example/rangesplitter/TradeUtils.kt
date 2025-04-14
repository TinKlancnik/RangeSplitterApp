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
import com.example.rangesplitter.OpenOrdersAdapter
import com.example.rangesplitter.R
import com.example.rangesplitter.SplitActivity.OpenOrder

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
}
