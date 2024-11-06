package com.example.rangesplitter

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.rangesplitter.api.BybitRepository

class MenuActivity : AppCompatActivity() {

    private lateinit var balanceTextView: TextView
    private val repository = BybitRepository()

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        balanceTextView = findViewById(R.id.balanceTextView)
        val mainButton = findViewById<Button>(R.id.splitButton)

        // Fetch the balance when the activity is created
        fetchAndDisplayBalance()

        mainButton.setOnClickListener {
            val intent = Intent(this, SplitActivity::class.java)
            startActivity(intent)
        }
    }

    private fun fetchAndDisplayBalance() {
        // Use coroutines to fetch the balance in the background
        CoroutineScope(Dispatchers.IO).launch {
            val apiKey = "0YegwCRINljpDctbNr"
            val secretKey = "Luh0b2FYpywY50b1ppqpLV50R4LX8QIswXJ6"

            // Call the function to get the wallet balance
            val balance = repository.getWalletBalance(apiKey, secretKey)

            // Update the UI on the main thread
            runOnUiThread {
                if (balance != null) {
                    balanceTextView.text = "Balance: $$balance USDT"
                } else {
                    balanceTextView.text = "Error fetching balance"
                }
            }
        }
    }
}
