package com.example.rangesplitter

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.example.rangesplitter.Coin
import com.example.rangesplitter.SortMode
import com.example.rangesplitter.TradeUtils
import android.widget.Toast


class CoinSelectFragment : Fragment(R.layout.fragment_coin_select) {

    private class CoinAdapter(
        private val items: List<Coin>,
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

            // Base asset from pair, e.g. "BTCUSDT" -> "BTC"
            val base = coin.symbol.takeWhile { it.isLetter() }

            holder.symbol.text = base
            holder.name.text = coin.symbol
            holder.price.text = coin.priceText
            holder.change.text = coin.changeText

            // Color by sign
            val ctx = holder.itemView.context
            val colorRes = if (coin.changeValue >= 0.0) {
                R.color.vibrant_green
            } else {
                R.color.vibrant_red   // your red color
            }
            holder.change.setTextColor(ctx.getColor(colorRes))

            holder.itemView.setOnClickListener { onClick(coin) }
        }

        override fun getItemCount(): Int = items.size
    }

    // --- views & data ---

    private lateinit var searchInput: MaterialAutoCompleteTextView
    private lateinit var recycler: RecyclerView
    private lateinit var filterGroup: ChipGroup
    private lateinit var coinAdapter: CoinAdapter

    private val allCoins = mutableListOf<Coin>()
    private val visibleCoins = mutableListOf<Coin>()

    private var selectedSymbol: String = "BTCUSDT"
    private var sortMode: SortMode = SortMode.VOLUME

    private val updateInterval: Long = 5_000
    private val handler = Handler(Looper.getMainLooper())

    // --- lifecycle ---

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        searchInput = view.findViewById(R.id.coinList)
        recycler = view.findViewById(R.id.recyclerCoins)
        filterGroup = view.findViewById(R.id.filterGroup)   // ChipGroup in XML

        // RecyclerView setup
        coinAdapter = CoinAdapter(visibleCoins) { coin ->
            selectedSymbol = coin.symbol
            openSplitFragment(coin.symbol)
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

            fetchTopCoinsWithPrices(limit = 50)
        }

        fetchTopCoinsWithPrices(limit = 50)
        startPriceUpdates()
    }

    private fun openSplitFragment(symbol: String) {
        (requireActivity() as MainActivity).openSplitForSymbol(symbol)
    }

    private fun startPriceUpdates() {
        handler.post(object : Runnable {
            override fun run() {
                fetchTopCoinsWithPrices(limit = 50)
                handler.postDelayed(this, updateInterval)
            }
        })
    }
    private fun fetchTopCoinsWithPrices(limit: Int = 50) {
        TradeUtils.fetchTopCoinsWithPrices(
            sortMode = sortMode,
            limit = limit,
            onSuccess = { coins ->
                allCoins.clear()
                visibleCoins.clear()

                allCoins.addAll(coins)
                visibleCoins.addAll(coins)

                coinAdapter.notifyDataSetChanged()
            },
            onError = { msg ->
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            }
        )
    }
    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null)
    }
}
