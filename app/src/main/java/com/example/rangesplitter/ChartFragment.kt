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
        val highValue = view.findViewById<TextView>(R.id.highValue)
        val lowValue = view.findViewById<TextView>(R.id.lowValue)
        val volumeValue = view.findViewById<TextView>(R.id.volumeValue)
        val mcapValue = view.findViewById<TextView>(R.id.mcapValue)


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
                val color = if (coin.changeValue >= 0) R.color.vibrant_green else R.color.vibrant_red
                priceChange.setTextColor(ctx.getColor(color))


                // 5. Update 24H HIGH
                highValue.text = String.format("$%,.2f", coin.high24h)

                // 6. Update 24H LOW
                lowValue.text = String.format("$%,.2f", coin.low24h)

                // 7. Update VOLUME (use turnover24h because Bybit’s base-volume is misleading)
                val volumeFormatted = TradeUtils.formatCompactNumber(coin.turnover24h)
                volumeValue.text = volumeFormatted

                // 8. Funding rate instead of market cap
                mcapValue.text = String.format("%.4f%%", coin.fundingRate * 100)

                val ctxF = requireContext()
                val colorF = if (coin.fundingRate >= 0) R.color.vibrant_green else R.color.vibrant_red
                mcapValue.setTextColor(ctxF.getColor(colorF))



            }.show(parentFragmentManager, "coinSheet")
        }

    }
}

