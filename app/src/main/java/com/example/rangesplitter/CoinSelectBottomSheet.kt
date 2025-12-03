package com.example.rangesplitter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class CoinSelectBottomSheet(
    private val onCoinSelected: (Coin) -> Unit
) : BottomSheetDialogFragment() {

    private lateinit var recycler: RecyclerView
    private val coins = mutableListOf<Coin>()
    private lateinit var adapter: CoinAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.bottom_sheet_coin_select, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recycler = view.findViewById(R.id.recyclerCoins)
        recycler.layoutManager = LinearLayoutManager(requireContext())

        adapter = CoinAdapter(coins) { coin ->
            onCoinSelected(coin)
            dismiss()
        }
        recycler.adapter = adapter

        loadCoins()
    }

    private fun loadCoins() {
        TradeUtils.fetchTopCoinsWithPrices(
            sortMode = SortMode.VOLUME,
            limit = 50,
            onSuccess = { list ->
                coins.clear()
                coins.addAll(list)
                adapter.notifyDataSetChanged()
            },
            onError = { msg ->
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Reuse same CoinAdapter as CoinSelectFragment
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
            val base = coin.symbol.takeWhile { it.isLetter() }

            holder.symbol.text = base
            holder.name.text = coin.symbol
            holder.price.text = coin.priceText
            holder.change.text = coin.changeText

            val ctx = holder.itemView.context
            val color = if (coin.changeValue >= 0) R.color.vibrant_green else R.color.red
            holder.change.setTextColor(ctx.getColor(color))

            holder.itemView.setOnClickListener { onClick(coin) }
        }

        override fun getItemCount() = items.size
    }
}
