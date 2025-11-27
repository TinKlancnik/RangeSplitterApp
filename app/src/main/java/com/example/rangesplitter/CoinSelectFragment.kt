package com.example.rangesplitter

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import bybit.sdk.rest.ByBitRestApiCallback
import bybit.sdk.rest.ByBitRestClient
import bybit.sdk.rest.market.InstrumentsInfoParams
import bybit.sdk.rest.market.InstrumentsInfoResponse
import bybit.sdk.rest.market.InstrumentsInfoResultItem
import bybit.sdk.rest.okHttpClientProvider
import bybit.sdk.shared.Category
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject

class CoinSelectFragment : Fragment(R.layout.fragment_coin_select) {

    // --- model ---

    data class Coin(
        val symbol: String,
        val iconRes: Int,
        var priceText: String = "--"
    )

    // --- RecyclerView adapter ---

    private class CoinAdapter(
        private val items: List<Coin>,
        private val onClick: (Coin) -> Unit
    ) : RecyclerView.Adapter<CoinAdapter.CoinViewHolder>() {

        inner class CoinViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val icon: ImageView = view.findViewById(R.id.imgCoinIcon)
            val name: TextView = view.findViewById(R.id.txtCoinName)
            val price: TextView = view.findViewById(R.id.txtCoinPrice)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CoinViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.layout_item_coin, parent, false)
            return CoinViewHolder(view)
        }

        override fun onBindViewHolder(holder: CoinViewHolder, position: Int) {
            val coin = items[position]
            holder.icon.setImageResource(coin.iconRes)
            holder.name.text = coin.symbol
            holder.price.text = coin.priceText
        }

        override fun getItemCount(): Int = items.size
    }

    // --- views & data ---

    private lateinit var searchInput: MaterialAutoCompleteTextView
    private lateinit var recycler: RecyclerView
    private lateinit var coinAdapter: CoinAdapter

    private val allCoins = mutableListOf<Coin>()      // full list from Bybit
    private val visibleCoins = mutableListOf<Coin>()  // filtered list for UI

    private var selectedSymbol: String = "BTCUSDT"

    private val updateInterval: Long = 500
    private val handler = Handler(Looper.getMainLooper())

    // --- lifecycle ---

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        searchInput = view.findViewById(R.id.coinList)
        recycler = view.findViewById(R.id.recyclerCoins)

        // RecyclerView setup
        coinAdapter = CoinAdapter(visibleCoins) { coin ->
            selectedSymbol = coin.symbol
            // optional: show selected symbol in search field
            searchInput.setText(coin.symbol, false)
        }

        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = coinAdapter

        // Filter list as user types
        searchInput.addTextChangedListener { text ->
            val query = text?.toString()?.trim()?.lowercase() ?: ""
            visibleCoins.clear()

            if (query.isEmpty()) {
                visibleCoins.addAll(allCoins)
            } else {
                visibleCoins.addAll(
                    allCoins.filter { it.symbol.lowercase().contains(query) }
                )
            }

            coinAdapter.notifyDataSetChanged()
        }

        // Load coin list from Bybit and start price updates
        fetchPerpetualCoins()
        startPriceUpdates()
    }

    // --- Bybit client ---

    private fun getByBitClient(): ByBitRestClient {
        val apiKey = "UV6R9A3gNuk9vl0vVQ"
        val apiSecret = "vRdpemzToMITR53ftZSM3ar7kSdx6NeodJTn"
        return ByBitRestClient(apiKey, apiSecret, true, httpClientProvider = okHttpClientProvider)
    }

    // --- icons ---

    private fun getIconForSymbol(symbol: String): Int {
        // Extend this when you add real icons
        return R.drawable.chart
    }

    // --- fetch coin list ---

    private fun fetchPerpetualCoins() {
        val bybitClient = getByBitClient()

        val params = InstrumentsInfoParams(
            category = Category.linear,
            symbol = null,
            limit = 1000
        )

        val callback = object : ByBitRestApiCallback<InstrumentsInfoResponse<InstrumentsInfoResultItem>> {
            override fun onSuccess(result: InstrumentsInfoResponse<InstrumentsInfoResultItem>) {
                allCoins.clear()
                visibleCoins.clear()

                result.result?.list?.forEach { instrument ->
                    val symbol = instrument.symbol
                    val coin = Coin(
                        symbol = symbol,
                        iconRes = getIconForSymbol(symbol),
                        priceText = "--"
                    )
                    allCoins.add(coin)
                }

                // show all by default
                visibleCoins.addAll(allCoins)

                activity?.runOnUiThread {
                    coinAdapter.notifyDataSetChanged()
                }
            }

            override fun onError(error: Throwable) {
                Log.e("SupportedInstrument", "Error fetching supported instruments: ${error.message}")
                activity?.runOnUiThread {
                    Toast.makeText(requireContext(), "Error loading coins", Toast.LENGTH_SHORT).show()
                }
            }
        }

        bybitClient.marketClient.getInstrumentsInfo(params, callback)
    }

    // --- periodic price updates ---

    private fun startPriceUpdates() {
        handler.post(object : Runnable {
            override fun run() {
                fetchCoinPrice(selectedSymbol)
                handler.postDelayed(this, updateInterval)
            }
        })
    }

    // --- price fetch ---

    private fun fetchCoinPrice(symbol: String) {
        val client = OkHttpClient()
        val url = "https://api-testnet.bybit.com/v5/market/tickers?category=inverse&symbol=$symbol"

        val request = Request.Builder()
            .url(url)
            .build()

        Thread {
            try {
                val response: Response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val jsonResponse = JSONObject(response.body?.string() ?: "")
                    val result = jsonResponse.getJSONObject("result")

                    if (result.length() > 0) {
                        val ticker = result.getJSONArray("list").getJSONObject(0)
                        val lastPrice = ticker.getString("lastPrice")

                        activity?.runOnUiThread {
                            // find coin in full list and update its price
                            val coin = allCoins.find { it.symbol == symbol }
                            if (coin != null) {
                                coin.priceText = lastPrice
                                coinAdapter.notifyDataSetChanged()
                            }
                        }

                        Log.d("CoinPrice", "Last price for $symbol: $lastPrice")
                    } else {
                        Log.d("CoinPrice", "No data available for $symbol.")
                    }
                } else {
                    Log.d("CoinPrice", "Failed to fetch coin price.")
                }
            } catch (e: Exception) {
                Log.d("CoinPrice", "Error: ${e.message}")
            }
        }.start()
    }
}
