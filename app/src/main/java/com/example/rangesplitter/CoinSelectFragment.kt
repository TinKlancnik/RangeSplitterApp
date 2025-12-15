package com.example.rangesplitter

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.example.rangesplitter.ws.BybitLinearTickerWebSocket
import com.example.rangesplitter.ws.BybitLinearTickerWebSocket.TickerListener
import com.example.rangesplitter.ws.BybitLinearTickerWebSocket.TickerUpdate

class CoinSelectFragment : Fragment(R.layout.fragment_coin_select), TickerListener {

    // ---------------- ADAPTER ----------------

    private class CoinAdapter(
        private val items: List<Coin>,
        private val tickerMap: Map<String, TickerUpdate>,
        private val onClick: (Coin) -> Unit
    ) : RecyclerView.Adapter<CoinAdapter.CoinViewHolder>() {

        inner class CoinViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val symbol: TextView = view.findViewById(R.id.txtCoinSymbol)
            val name: TextView = view.findViewById(R.id.txtCoinName)
            val price: TextView = view.findViewById(R.id.txtCoinPrice)
            val change: TextView = view.findViewById(R.id.txtCoinChange)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CoinViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.layout_item_coin, parent, false)
            return CoinViewHolder(view)
        }

        override fun onBindViewHolder(holder: CoinViewHolder, position: Int) {
            val coin = items[position]

            val base = coin.symbol.takeWhile { it.isLetter() }
                .ifEmpty { coin.symbol }

            holder.symbol.text = base
            holder.name.text = coin.symbol.removeSuffix("USDT")

            val wsSymbol = normalizeSymbolForWs(coin.symbol)
            val ticker = tickerMap[wsSymbol]

            val livePrice = when {
                ticker?.markPrice != null  -> ticker.markPrice
                ticker?.indexPrice != null -> ticker.indexPrice
                ticker?.lastPrice != null  -> ticker.lastPrice
                else                       -> null
            }

            val priceText = livePrice?.let { String.format("%.4f", it) } ?: coin.priceText
            holder.price.text = priceText

            val changeValue = when {
                ticker?.price24hPcnt != null -> ticker.price24hPcnt * 100.0   // Bybit gives fraction, e.g. 0.0123
                else -> coin.changeValue
            }

            val changeText = String.format("%.2f%%", changeValue)
            holder.change.text = changeText

            val ctx = holder.itemView.context
            val colorRes = if (changeValue >= 0.0) {
                R.color.vibrant_green
            } else {
                R.color.vibrant_red
            }
            holder.change.setTextColor(ctx.getColor(colorRes))

            holder.itemView.setOnClickListener { onClick(coin) }
        }

        override fun getItemCount(): Int = items.size

        private fun normalizeSymbolForWs(symbol: String): String {
            // If your Coin symbols are like "BTCUSDT.P", strip suffix for WebSocket:
            return symbol.removeSuffix(".P")
        }
    }

    // ---------------- FIELDS ----------------

    private lateinit var searchInput: MaterialAutoCompleteTextView
    private lateinit var recycler: RecyclerView
    private lateinit var filterGroup: ChipGroup
    private lateinit var coinAdapter: CoinAdapter

    private val allCoins = mutableListOf<Coin>()
    private val visibleCoins = mutableListOf<Coin>()

    private var selectedSymbol: String = "BTCUSDT"
    private var sortMode: SortMode = SortMode.VOLUME

    // WebSocket ticker data cache (key: wsSymbol, e.g. "BTCUSDT")
    private val tickerMap = mutableMapOf<String, TickerUpdate>()

    // ---------------- LIFECYCLE ----------------

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        searchInput = view.findViewById(R.id.coinList)
        recycler = view.findViewById(R.id.recyclerCoins)
        filterGroup = view.findViewById(R.id.filterGroup)

        // RecyclerView setup
        coinAdapter = CoinAdapter(
            visibleCoins,
            tickerMap
        ) { coin ->
            selectedSymbol = coin.symbol
            openSplitFragment(coin.symbol)
        }

        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = coinAdapter

        // 🔧 Disable change animations to remove flicker
        (recycler.itemAnimator as? SimpleItemAnimator)?.apply {
            supportsChangeAnimations = false
            changeDuration = 0L
        }

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

        // ChipGroup: change sort mode
        filterGroup.check(R.id.chipVolume)   // default selected chip

        filterGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            val checkedId = checkedIds.firstOrNull() ?: return@setOnCheckedStateChangeListener

            sortMode = when (checkedId) {
                R.id.chipVolume -> SortMode.VOLUME
                R.id.chipChange -> SortMode.CHANGE
                R.id.chipVolatility -> SortMode.VOLATILITY
                R.id.chipAlphabetical -> SortMode.ALPHA
                else -> SortMode.VOLUME
            }

            // Re-fetch list when sort changes (REST used only occasionally)
            fetchTopCoinsWithPrices(limit = 50)
        }

        // Start listening to WebSocket updates
        BybitLinearTickerWebSocket.addListener(this)

        // Initial load of top coins via REST (one-time snapshot)
        fetchTopCoinsWithPrices(limit = 50)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Stop listening to WebSocket updates from this fragment
        BybitLinearTickerWebSocket.removeListener(this)

        // Unsubscribe from all current symbols (optional but cleaner)
        unsubscribeForCurrentCoins()
    }

    // ---------------- NAVIGATION ----------------

    private fun openSplitFragment(symbol: String) {
        (requireActivity() as MainActivity).openSplitForSymbol(symbol)
    }

    // ---------------- REST → INITIAL LIST ----------------

    private fun fetchTopCoinsWithPrices(limit: Int = 50) {
        // Before replacing the list, unsubscribe from old ones
        unsubscribeForCurrentCoins()

        TradeUtils.fetchTopCoinsWithPrices(
            sortMode = sortMode,
            limit = limit,
            onSuccess = onSuccess@{ coins ->
                // Fragment might already be detached when the network call finishes
                if (!isAdded) return@onSuccess

                allCoins.clear()
                visibleCoins.clear()

                allCoins.addAll(coins)
                visibleCoins.addAll(coins)

                // Subscribe WebSocket for new list of coins
                subscribeForCurrentCoins()

                coinAdapter.notifyDataSetChanged()
            },
            onError = onError@{ msg ->
                // Fragment might already be detached here too
                if (!isAdded) return@onError

                // Use safe context access instead of requireContext()
                context?.let {
                    Toast.makeText(it, msg, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }


    private fun subscribeForCurrentCoins() {
        for (coin in allCoins) {
            val wsSymbol = normalizeSymbolForWs(coin.symbol)
            BybitLinearTickerWebSocket.subscribe(wsSymbol)
        }
    }

    private fun unsubscribeForCurrentCoins() {
        for (coin in allCoins) {
            val wsSymbol = normalizeSymbolForWs(coin.symbol)
            BybitLinearTickerWebSocket.unsubscribe(wsSymbol)
        }
    }

    private fun normalizeSymbolForWs(symbol: String): String {
        // If your `Coin.symbol` is "BTCUSDT.P", map it to "BTCUSDT" for Bybit WS topic
        return symbol.removeSuffix(".P")
    }

    // ---------------- WEBSOCKET LISTENER ----------------

    override fun onTicker(update: TickerUpdate) {
        // Save latest merged update for this symbol (symbol is already WS symbol, e.g. "BTCUSDT")
        tickerMap[update.symbol] = update

        // Find visible index for this symbol so we can refresh only that row
        val index = visibleCoins.indexOfFirst {
            normalizeSymbolForWs(it.symbol) == update.symbol
        }

        if (index != -1) {
            activity?.runOnUiThread {
                coinAdapter.notifyItemChanged(index)
            }
        }
    }
}
