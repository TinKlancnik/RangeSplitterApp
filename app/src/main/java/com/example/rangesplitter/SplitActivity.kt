package com.example.rangesplitter

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
import bybit.sdk.rest.market.InstrumentsInfoParams
import bybit.sdk.rest.market.InstrumentsInfoResponse
import bybit.sdk.rest.market.InstrumentsInfoResultItem
import bybit.sdk.rest.okHttpClientProvider
import bybit.sdk.rest.order.PlaceOrderParams
import bybit.sdk.rest.order.PlaceOrderResponse
import bybit.sdk.shared.Category
import bybit.sdk.shared.OrderType
import bybit.sdk.shared.Side
import bybit.sdk.shared.TimeInForce
import com.unciv.utils.Concurrency.runBlocking

class SplitActivity : AppCompatActivity() {

    private lateinit var coinAdapter: ArrayAdapter<String>  // Define your coin adapter
    private val coinList = mutableListOf<String>()  // List to store the coin symbols

    override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_main)

        // Hide the action bar
        supportActionBar?.hide()

        // Make the navigation bar black
        window.navigationBarColor = resources.getColor(android.R.color.black)

        // Initialize the Spinner for coins
        val coinSpinner = findViewById<Spinner>(R.id.coinList)  // Reference to your Spinner

        // Set up the coin adapter for displaying coins
        coinAdapter = ArrayAdapter(this, R.layout.spinner_item, coinList)
        coinAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        coinSpinner.adapter = coinAdapter

        val spinner = findViewById<Spinner>(R.id.spinnerValues)
        val items = arrayOf(3, 5)
        val adapter = ArrayAdapter(this, R.layout.spinner_item, items)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinner.adapter = adapter
        spinner.setSelection(0)

        data class Coin(val name: String)

        val rangeTopEditText = findViewById<EditText>(R.id.editTextRangeTop)
        val rangeLowEditText = findViewById<EditText>(R.id.editTextRangeLow)
        //val calculateButton = findViewById<Button>(R.id.calculateButton)
        val buyButton = findViewById<Button>(R.id.buttonBuy)

            buyButton.setOnClickListener {
                fetchPerpetualCoins()
                val rangeTop = rangeTopEditText.text.toString().toFloatOrNull() ?: 0f
                val rangeLow = rangeLowEditText.text.toString().toFloatOrNull() ?: 0f
                val numberOfValues = spinner.selectedItem as Int
                val results = calculateNumbers(rangeLow, rangeTop, numberOfValues)
                for (result in results) {
                    placeATrade(result.toString())
                }
                closekey(it)
            }
        fetchPerpetualCoins()
    }
    private fun closekey(view: View){
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun calculateNumbers(low: Float, top: Float, count: Int): List<Float> {
        val step = (top - low) / (count - 1)
        return List(count) { low + it * step }
    }

    private fun placeATrade(price: String) {
        val apiKey = "UV6R9A3gNuk9vl0vVQ"
        val apiSecret = "vRdpemzToMITR53ftZSM3ar7kSdx6NeodJTn"

        // Initialize ByBitRestClient
        val bybitClient = ByBitRestClient(apiKey, apiSecret, true, httpClientProvider = okHttpClientProvider)

        // Define trade parameters
        val tradeParams = PlaceOrderParams(
            category = Category.linear,  // Enum for Perpetual Futures
            symbol = "BTCUSDT",
            side = Side.Buy,            // Enum for Buy
            orderType = OrderType.Limit, // Enum for Market Order
            price = price,
            qty = "0.1",                // Position size
            timeInForce = TimeInForce.GTC, // Enum for Good-Till-Cancelled
            reduceOnly = false          // New position (not reducing)
        )

        // Create the callback to handle the response
        val callback = object : ByBitRestApiCallback<PlaceOrderResponse> {
            override fun onSuccess(result: PlaceOrderResponse) {
                Log.d("Trade", "Trade placed successfully: ${result}")
            }
            override fun onError(error: Throwable) {
                Log.e("Trade", "Error encountered: ${error.message}")
            }
        }

        // Send order request with callback
        bybitClient.orderClient.placeOrder(tradeParams, callback)
    }

    private fun fetchPerpetualCoins() {
        val apiKey = "UV6R9A3gNuk9vl0vVQ"
        val apiSecret = "vRdpemzToMITR53ftZSM3ar7kSdx6NeodJTn"

        // Initialize ByBitRestClient
        val bybitClient = ByBitRestClient(apiKey, apiSecret, true, httpClientProvider = okHttpClientProvider)

        // Define parameters for fetching instruments info
        val params = InstrumentsInfoParams(
            category = Category.linear, // Category for perpetual contracts
            symbol = null,  // Fetch all instruments
            limit = 100      // Limit the number of instruments
        )

        // Create the callback to handle the response
        val callback = object : ByBitRestApiCallback<InstrumentsInfoResponse<InstrumentsInfoResultItem>> {
            override fun onSuccess(result: InstrumentsInfoResponse<InstrumentsInfoResultItem>) {
                // Clear the existing coin list and add new coins from the response
                coinList.clear()
                result.result?.list?.forEach { instrument ->
                    coinList.add(instrument.symbol)  // Add the symbol to the coin list
                }

                // Notify the adapter that the data has changed
                coinAdapter.notifyDataSetChanged()
            }

            override fun onError(error: Throwable) {
                Log.e("SupportedInstrument", "Error fetching supported instruments: ${error.message}")
            }

        }

        // Make the asynchronous API call with the callback
        bybitClient.marketClient.getInstrumentsInfo(params, callback)
    }

}
