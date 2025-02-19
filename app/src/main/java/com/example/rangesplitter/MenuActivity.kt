package com.example.rangesplitter

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import bybit.sdk.rest.ByBitRestClient
import bybit.sdk.rest.account.WalletBalanceParams
import bybit.sdk.shared.AccountType
import bybit.sdk.rest.okHttpClientProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MenuActivity : AppCompatActivity() {

    private lateinit var balanceTextView: TextView

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        balanceTextView = findViewById(R.id.balanceTextView)
        val mainButton = findViewById<Button>(R.id.splitButton)

        mainButton.setOnClickListener {
            val intent = Intent(this, SplitActivity::class.java)
            startActivity(intent)
        }

        // Fetch the balance when the activity is created
        fetchAndDisplayBalance()
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
                    balanceTextView.text = "Balance: $walletBalanceResponse BTC"
                } else {
                    balanceTextView.text = "Error fetching balance"
                }
            } catch (e: Exception) {
                balanceTextView.text = "Error: ${e.message}"
            }
        }
    }
}
