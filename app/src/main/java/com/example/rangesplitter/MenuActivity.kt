package com.example.rangesplitter

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import bybit.sdk.rest.ByBitRestApiCallback
import bybit.sdk.rest.ByBitRestClient
import bybit.sdk.rest.account.WalletBalanceParams
import bybit.sdk.shared.AccountType
import bybit.sdk.rest.okHttpClientProvider
import bybit.sdk.rest.order.PlaceOrderParams
import bybit.sdk.rest.order.PlaceOrderResponse
import bybit.sdk.shared.Category
import bybit.sdk.shared.OrderType
import bybit.sdk.shared.Side
import bybit.sdk.shared.TimeInForce
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.*
import kotlinx.coroutines.*

class MenuActivity : AppCompatActivity() {

    private lateinit var balanceTextView: TextView

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        balanceTextView = findViewById(R.id.balanceTextView)
        val mainButton = findViewById<Button>(R.id.splitButton)
        val tradeButton = findViewById<Button>(R.id.trade)

        mainButton.setOnClickListener {
            val intent = Intent(this, SplitActivity::class.java)
            startActivity(intent)
        }
        tradeButton.setOnClickListener {
            placeATrade()
        }

        // Fetch the balance when the activity is created
        fetchAndDisplayBalance()
    }
    private fun placeATrade() {
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
            price = "30000",
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


    private fun fetchAndDisplayBalance() {
        val apiKey = "UV6R9A3gNuk9vl0vVQ"
        val apiSecret = "vRdpemzToMITR53ftZSM3ar7kSdx6NeodJTn"

        // Initialize ByBitRestClient
        val bybitClient = ByBitRestClient(apiKey, apiSecret, true,  httpClientProvider = okHttpClientProvider)

        // Use runBlocking to call the blocking function
        runBlocking {
            try {
                val walletBalanceResponse = bybitClient.accountClient.getWalletBalanceBlocking(
                    WalletBalanceParams(AccountType.UNIFIED, listOf("BTC"))
                )
                // Display the balance
                if (walletBalanceResponse != null) {
                    val totalEquity = walletBalanceResponse.result?.list?.get(0)?.totalEquity
                    balanceTextView.text = "Total Balance: ${totalEquity} USDT"
                } else {
                    balanceTextView.text = "Error fetching balance"
                }
            } catch (e: Exception) {
                balanceTextView.text = "Error: ${e.message}"
            }
        }
    }
}
