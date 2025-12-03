package com.example.rangesplitter
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment


class ChartFragment : Fragment(R.layout.fragment_chart) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val pairName: TextView = view.findViewById(R.id.pairName)
        val assetSubtitle = view.findViewById<TextView>(R.id.assetSubtitle)
        val currentPrice = view.findViewById<TextView>(R.id.currentPrice)
        val priceChange = view.findViewById<TextView>(R.id.priceChange)

        pairName.setOnClickListener {
            CoinSelectBottomSheet { coin ->

                // 1. Update pair
                pairName.text = coin.symbol

                // 2. Base asset (BTC from BTCUSDT)
                val base = coin.symbol.takeWhile { it.isLetter() }
                assetSubtitle.text = base

                // 3. Update price
                currentPrice.text = coin.priceText

                // 4. Update % change + color
                priceChange.text = coin.changeText

                val ctx = requireContext()
                val color = if (coin.changeValue >= 0) R.color.vibrant_green else R.color.red
                priceChange.setTextColor(ctx.getColor(color))

            }.show(parentFragmentManager, "coinSheet")
        }

    }
}

