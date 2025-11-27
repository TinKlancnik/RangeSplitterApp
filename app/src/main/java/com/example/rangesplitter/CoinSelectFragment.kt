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
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject

class CoinSelectFragment : Fragment(R.layout.fragment_coin_select) {

    // --- model ---

    data class Coin(
        val symbol: String,
        var priceText: String = "--"
    )

    private enum class SortMode { VOLUME, CHANGE, VOLATILITY, ALPHA }

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
                .inflate(R.layout.layout_item_coin, parent, false)   // or R.layout.item_coin_row if that's your file name
            return CoinViewHolder(view)
        }

        override fun onBindViewHolder(holder: CoinViewHolder, position: Int) {
            val coin = items[position]

            // Extract base asset, e.g. "BTCUSDT" -> "btc", "1000PEPEUSDT" -> "pepe"
            val lettersOnly = coin.symbol.takeWhile { it.isLetter() }
            val baseAsset = if (lettersOnly.isNotEmpty()) {
                lettersOnly.lowercase()
            } else {
                coin.symbol.lowercase()
            }

            val iconUrl = "https://cryptoicons.org/api/icon/$baseAsset/64"

            Glide.with(holder.itemView)
                .load(iconUrl)
                .placeholder(R.drawable.chart)
                .error(R.drawable.chart)
                .into(holder.icon)

            holder.name.text = coin.symbol
            holder.price.text = coin.priceText
        }

        override fun getItemCount(): Int = items.size
    }

    // --- views & data ---

    private lateinit var searchInput: MaterialAutoCompleteTextView
    private lateinit var recycler: RecyclerView
    private lateinit var filterGroup: MaterialButtonToggleGroup
    private lateinit var coinAdapter: CoinAdapter

    private val allCoins = mutableListOf<Coin>()      // full list from Bybit (max 50)
    private val visibleCoins = mutableListOf<Coin>()  // filtered list for UI

    private var selectedSymbol: String = "BTCUSDT"
    private var sortMode: SortMode = SortMode.VOLUME

    private val updateInterval: Long = 3_000          // 3s
    private val handler = Handler(Looper.getMainLooper())

    // --- lifecycle ---

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        searchInput = view.findViewById(R.id.coinList)
        recycler = view.findViewById(R.id.recyclerCoins)
        filterGroup = view.findViewById(R.id.filterGroup)

        // RecyclerView setup
        coinAdapter = CoinAdapter(visibleCoins) { coin ->
            selectedSymbol = coin.symbol
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

        // Button group: change sort mode
        filterGroup.check(R.id.btnVolume) // default selection

        filterGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener

            sortMode = when (checkedId) {
                R.id.btnVolume -> SortMode.VOLUME
                R.id.btnChange -> SortMode.CHANGE
                R.id.btnVolatility -> SortMode.VOLATILITY
                R.id.btnAlphabetical -> SortMode.ALPHA
                else -> SortMode.VOLUME
            }

            // Re-fetch and re-sort coins with new mode
            fetchTopCoinsWithPrices(limit = 50)
        }

        // Initial load + periodic refresh
        fetchTopCoinsWithPrices(limit = 50)
        startPriceUpdates()
    }

    // --- fetch 50 coins + their prices, sorted by current sortMode ---

    private fun fetchTopCoinsWithPrices(limit: Int = 50) {
        val client = OkHttpClient()
        val url = "https://api-testnet.bybit.com/v5/market/tickers?category=linear"

        val request = Request.Builder()
            .url(url)
            .build()

        Thread {
            try {
                val response: Response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val jsonResponse = JSONObject(response.body?.string() ?: "")
                    val result = jsonResponse.getJSONObject("result")
                    val list = result.getJSONArray("list")

                    data class RawCoin(
                        val symbol: String,
                        val lastPrice: String,
                        val turnover24h: Double,
                        val volume24h: Double,
                        val change24h: Double,
                        val volatility: Double
                    )

                    val rawCoins = mutableListOf<RawCoin>()

                    for (i in 0 until list.length()) {
                        val ticker = list.getJSONObject(i)
                        val symbol = ticker.getString("symbol")
                        val lastPrice = ticker.getString("lastPrice")

                        val turnoverStr = ticker.optString("turnover24h", "0")
                        val volumeStr = ticker.optString("volume24h", "0")
                        val changeStr = ticker.optString("price24hPcnt", "0")
                        val highStr = ticker.optString("highPrice24h", "0")
                        val lowStr = ticker.optString("lowPrice24h", "0")

                        val turnover = turnoverStr.toDoubleOrNull() ?: 0.0
                        val volume = volumeStr.toDoubleOrNull() ?: 0.0
                        val change = changeStr.toDoubleOrNull() ?: 0.0
                        val high = highStr.toDoubleOrNull() ?: 0.0
                        val low = lowStr.toDoubleOrNull() ?: 0.0
                        val volatility = high - low

                        rawCoins.add(
                            RawCoin(
                                symbol = symbol,
                                lastPrice = lastPrice,
                                turnover24h = turnover,
                                volume24h = volume,
                                change24h = change,
                                volatility = volatility
                            )
                        )
                    }

                    // Filter to USDT perps only (optional - comment out if you want everything)
                    val filtered = rawCoins.filter { it.symbol.endsWith("USDT") }

                    val sorted = when (sortMode) {
                        SortMode.VOLUME -> filtered.sortedByDescending { it.turnover24h }
                        SortMode.CHANGE -> filtered.sortedByDescending { it.change24h }
                        SortMode.VOLATILITY -> filtered.sortedByDescending { it.volatility }
                        SortMode.ALPHA -> filtered.sortedBy { it.symbol }
                    }

                    val top = sorted.take(limit)

                    val newCoins = top.map { raw ->
                        Coin(
                            symbol = raw.symbol,
                            priceText = raw.lastPrice
                        )
                    }

                    activity?.runOnUiThread {
                        allCoins.clear()
                        visibleCoins.clear()

                        allCoins.addAll(newCoins)
                        visibleCoins.addAll(newCoins)

                        coinAdapter.notifyDataSetChanged()
                    }

                    Log.d("CoinList", "Loaded ${newCoins.size} coins, mode=$sortMode")
                } else {
                    Log.d("CoinList", "Failed to fetch tickers: ${response.code}")
                    activity?.runOnUiThread {
                        Toast.makeText(requireContext(), "Failed to fetch coins", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.d("CoinList", "Error: ${e.message}")
                activity?.runOnUiThread {
                    Toast.makeText(requireContext(), "Error loading coins", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun startPriceUpdates() {
        handler.post(object : Runnable {
            override fun run() {
                fetchTopCoinsWithPrices(limit = 50)
                handler.postDelayed(this, updateInterval)
            }
        })
    }
}
