package com.example.rangesplitter

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import bybit.sdk.rest.ByBitRestApiCallback
import bybit.sdk.rest.ByBitRestClient
import bybit.sdk.rest.account.WalletBalanceParams
import bybit.sdk.rest.market.InstrumentsInfoParams
import bybit.sdk.rest.market.InstrumentsInfoResponse
import bybit.sdk.rest.market.InstrumentsInfoResultItem
import bybit.sdk.rest.okHttpClientProvider
import bybit.sdk.rest.order.PlaceOrderParams
import bybit.sdk.rest.order.PlaceOrderResponse
import bybit.sdk.rest.order.ordersOpen
import bybit.sdk.rest.position.PositionInfoParams
import bybit.sdk.rest.position.PositionInfoResponse
import bybit.sdk.shared.AccountType
import bybit.sdk.shared.Category
import bybit.sdk.shared.OrderType
import bybit.sdk.shared.Side
import bybit.sdk.shared.TimeInForce
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import bybit.sdk.rest.order.OrdersOpenParams
import bybit.sdk.rest.order.OrdersOpenResponse

class SplitActivity : AppCompatActivity() {

    private lateinit var coinAdapter: ArrayAdapter<String>
    private val coinList = mutableListOf<String>()
    private var selectedSymbol: String = "BTCUSDT"  // Default symbol
    private val updateInterval: Long = 500  // Update every 3 seconds
    private val handler = android.os.Handler()
    private lateinit var coinPriceTextView: TextView

    @SuppressLint("SuspiciousIndentation")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportActionBar?.hide()
        window.navigationBarColor = resources.getColor(android.R.color.black)

        val coinSpinner = findViewById<Spinner>(R.id.coinList)
        coinAdapter = ArrayAdapter(this, R.layout.spinner_item, coinList)
        coinAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        coinSpinner.adapter = coinAdapter

        // Set up the spinner listener to update the selected symbol
        coinSpinner.setOnItemSelectedListener(object :
            android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: android.widget.AdapterView<*>,
                view: View,
                position: Int,
                id: Long
            ) {
                selectedSymbol = coinList[position]
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {
                selectedSymbol = "BTCUSDT"  // Default fallback
            }
        })

        val spinner = findViewById<Spinner>(R.id.spinnerValues)
        val items = arrayOf(3, 5)
        val adapter = ArrayAdapter(this, R.layout.spinner_item, items)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinner.adapter = adapter
        spinner.setSelection(0)

        val rangeTopEditText = findViewById<EditText>(R.id.editTextRangeTop)
        val rangeLowEditText = findViewById<EditText>(R.id.editTextRangeLow)
        val amountEditText = findViewById<EditText>(R.id.editTextAmount)
        val buyButton = findViewById<Button>(R.id.buttonBuy)
        coinPriceTextView = findViewById<TextView>(R.id.coinPrice)

        buyButton.setOnClickListener {
            fetchPerpetualCoins()

            val rangeTop = rangeTopEditText.text.toString().toFloatOrNull() ?: 0f
            val rangeLow = rangeLowEditText.text.toString().toFloatOrNull() ?: 0f
            val numberOfValues = spinner.selectedItem as Int
            val results = calculateNumbers(rangeLow, rangeTop, numberOfValues)
            for (result in results) {
                placeATrade(result.toString(), amountEditText.text.toString())
            }
            closekey(it)
        }
        fetchPerpetualCoins()
        //fetchOpenTrades()
        startPriceUpdates()
        fetchOpenOrders()
    }

    private fun closekey(view: View) {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun calculateNumbers(low: Float, top: Float, count: Int): List<Float> {
        val step = (top - low) / (count - 1)
        return List(count) { low + it * step }
    }

    private fun getByBitClient(): ByBitRestClient {
        val apiKey = "UV6R9A3gNuk9vl0vVQ"
        val apiSecret = "vRdpemzToMITR53ftZSM3ar7kSdx6NeodJTn"
        return ByBitRestClient(apiKey, apiSecret, true, httpClientProvider = okHttpClientProvider)
    }

    private fun placeATrade(price: String,amount: String) {
        val bybitClient = getByBitClient()

        val tradeParams = PlaceOrderParams(
            category = Category.linear,
            symbol = selectedSymbol,
            side = Side.Buy,
            orderType = OrderType.Limit,
            price = price,
            qty = amount,
            timeInForce = TimeInForce.GTC,
            reduceOnly = false
        )

        val callback = object : ByBitRestApiCallback<PlaceOrderResponse> {
            override fun onSuccess(result: PlaceOrderResponse) {
                Log.d("Trade", "Trade placed successfully: ${result}")
                showTradeResultDialog("Success", "Trade placed successfully for $selectedSymbol at $price.")
            }

            override fun onError(error: Throwable) {
                Log.e("Trade", "Error encountered: ${error.message}")
                showTradeResultDialog("Error", "Failed to place trade: ${error.message}")
            }
        }

        bybitClient.orderClient.placeOrder(tradeParams, callback)
    }

    private fun showTradeResultDialog(title: String, message: String) {
        runOnUiThread {
            val dialogBuilder = androidx.appcompat.app.AlertDialog.Builder(this)
            dialogBuilder.setTitle(title)
            dialogBuilder.setMessage(message)
            dialogBuilder.setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            dialogBuilder.show()
        }
    }

    private fun fetchPerpetualCoins() {
        val bybitClient = getByBitClient()

        val params = InstrumentsInfoParams(
            category = Category.linear,
            symbol = null,
            limit = 500
        )

        val callback = object : ByBitRestApiCallback<InstrumentsInfoResponse<InstrumentsInfoResultItem>> {
            override fun onSuccess(result: InstrumentsInfoResponse<InstrumentsInfoResultItem>) {
                coinList.clear()
                result.result?.list?.forEach { instrument ->
                    coinList.add(instrument.symbol)
                }
                coinAdapter.notifyDataSetChanged()
            }

            override fun onError(error: Throwable) {
                Log.e("SupportedInstrument", "Error fetching supported instruments: ${error.message}")
            }
        }

        bybitClient.marketClient.getInstrumentsInfo(params, callback)
    }

    private fun startPriceUpdates() {
        handler.post(object : Runnable {
            override fun run() {
                fetchCoinPrice(selectedSymbol)
                handler.postDelayed(this, updateInterval)
            }
        })
    }

    private fun fetchOpenTrades() {
        val bybitClient = getByBitClient() // Ensure this returns a valid ByBitPositionClient

        val params = PositionInfoParams(
            category = Category.linear,
            symbol = null,
            settleCoin = "USDT"
        )

       val callback = object : ByBitRestApiCallback<PositionInfoResponse> {
           override fun onSuccess(result: PositionInfoResponse) {
               Log.d("OpenTrades", "Full response: ${result.result.list}")
               result.result.list.forEach { position ->
                   Log.d("OpenTrades", "Symbol: ${position.symbol}, Size: ${position.size}, PnL: ${position.positionValue}")
               }
           }
           override fun onError(error: Throwable) {
               Log.e("OpenTrades", "Error fetching supported instruments: ${error.message}")
           }
       }
        bybitClient.positionClient.getPositionInfo(params, callback)
    }

    private fun fetchOpenOrders() {
        val bybitClient = getByBitClient()

        val params = OrdersOpenParams(
            category = Category.linear,
            settleCoin = "USDT"

        )

        // Log the full URL before sending the request
        val urlBuilder = StringBuilder("https://api-testnet.bybit.com/v5/order/realtime?")
        urlBuilder.append("category=${params.category}")
        params.settleCoin?.let { urlBuilder.append("&settleCoin=$it") }
        // Don't append symbol if it's null
        params.symbol?.let { urlBuilder.append("&symbol=$it") }

        Log.d("OpenOrders", "Request URL: $urlBuilder")

        val callback = object : ByBitRestApiCallback<OrdersOpenResponse> {
            override fun onSuccess(result: OrdersOpenResponse) {
                Log.d("OpenOrders", "Full response: ${result.result.list}")
                result.result.list.forEach { position ->
                    Log.d("OpenOrders", "Symbol: ${position.price}, Size: ${position.qty}, PnL: ${position.orderStatus}")
                }
            }
            override fun onError(error: Throwable) {
                Log.e("OpenOrders", "Error fetching supported instruments: ${error.message}")
            }
        }

        bybitClient.orderClient.ordersOpen(params, callback)
    }


    fun fetchCoinPrice(symbol: String) {
        val client = OkHttpClient()

        // Define the URL and the parameters for the request
        val url = "https://api-testnet.bybit.com/v5/market/tickers?category=inverse&symbol=$symbol"

        // Build the GET request
        val request = Request.Builder()
            .url(url)
            .build()

        // Execute the request on a background thread to avoid blocking the main UI thread
        Thread {
            try {
                val response: Response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    // Parse the response JSON
                    val jsonResponse = JSONObject(response.body?.string() ?: "")
                    val result = jsonResponse.getJSONObject("result")

                    // Extract information from the response
                    if (result.length() > 0) {
                        val ticker = result.getJSONArray("list").getJSONObject(0)
                        val lastPrice = ticker.getString("lastPrice")

                        // Update UI on the main thread
                        runOnUiThread {
                            coinPriceTextView.text = "Price: $lastPrice"
                        }

                        // Log the result (for debugging)
                        Log.d("CoinPrice", "Last price for $symbol: $lastPrice")
                    } else {
                        Log.d("CoinPrice", "No data available for $symbol.")
                    }
                } else {
                    Log.d("CoinPrice", "Request failed with status code: ${response.code}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start() // Start the background thread
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
