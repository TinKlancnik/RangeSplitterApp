package com.example.rangesplitter

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
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
import com.example.rangesplitter.UI.ViewPagerAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
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

        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)

        val adapter = ViewPagerAdapter(this)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Orders"
                1 -> "Positions"
                else -> ""
            }
        }.attach()

        // Fetch the balance when the activity is created
        fetchAndDisplayBalance()
    }

    private fun fetchAndDisplayBalance() {
        val apiKey = "UV6R9A3gNuk9vl0vVQ"
        val apiSecret = "vRdpemzToMITR53ftZSM3ar7kSdx6NeodJTn"

        val bybitClient = ByBitRestClient(apiKey, apiSecret, true,  httpClientProvider = okHttpClientProvider)

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
