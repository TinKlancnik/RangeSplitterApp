package com.example.rangesplitter

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.rangesplitter.TradeUtils.fetchKlines
import com.example.rangesplitter.indicators.Macro
import com.tradingview.lightweightcharts.api.chart.models.color.surface.SurfaceColor
import com.tradingview.lightweightcharts.api.chart.models.color.toIntColor
import com.tradingview.lightweightcharts.api.interfaces.SeriesApi
import com.tradingview.lightweightcharts.api.options.models.ChartOptions
import com.tradingview.lightweightcharts.api.options.models.HandleScaleOptions
import com.tradingview.lightweightcharts.api.options.models.HandleScrollOptions
import com.tradingview.lightweightcharts.api.options.models.LayoutOptions
import com.tradingview.lightweightcharts.api.options.models.LocalizationOptions
import com.tradingview.lightweightcharts.api.options.models.TimeScaleOptions
import com.tradingview.lightweightcharts.api.series.models.CandlestickData
import com.tradingview.lightweightcharts.api.series.models.LineData
import com.tradingview.lightweightcharts.api.series.models.Time
import com.tradingview.lightweightcharts.runtime.plugins.DateTimeFormat
import com.tradingview.lightweightcharts.runtime.plugins.PriceFormatter
import com.tradingview.lightweightcharts.runtime.plugins.TimeFormatter
import com.tradingview.lightweightcharts.view.ChartsView
import com.tradingview.lightweightcharts.api.chart.models.color.surface.SolidColor

class ChartFragment : Fragment(R.layout.fragment_chart) {

    // --- Chart / indicator stuff ---
    private var candlestickSeries: SeriesApi? = null
    private var fastEmaSeries: SeriesApi? = null
    private var slowEmaSeries: SeriesApi? = null
    private var biasEmaSeries: SeriesApi? = null

    private var xoIndicator: Macro? = null
    private var lastCandles: List<Candle> = emptyList()

    private var isMacroVisible = true
    private var indicatorToggleBtn: ImageButton? = null

    private var currentSymbol: String = "BTCUSDT"
    private var currentInterval: String = "60"   // 1H by default

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

        val chartView = view.findViewById<ChartsView>(R.id.charts_view)

        // ---------- CHART SETUP ----------
        setupChart(chartView)

        // Load default symbol once series are created
        // (series are created inside setupChart -> add*Series chain)
        // we call loadChart() at the end of that chain

        // ---------- COIN SELECT BOTTOM SHEET ----------
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
                val color =
                    if (coin.changeValue >= 0) R.color.vibrant_green else R.color.vibrant_red
                priceChange.setTextColor(ctx.getColor(color))

                // 5. 24H HIGH
                highValue.text = String.format("$%,.2f", coin.high24h)

                // 6. 24H LOW
                lowValue.text = String.format("$%,.2f", coin.low24h)

                // 7. VOLUME (use turnover24h)
                val volumeFormatted = TradeUtils.formatCompactNumber(coin.turnover24h)
                volumeValue.text = volumeFormatted

                // 8. Funding rate instead of market cap
                mcapValue.text = String.format("%.4f%%", coin.fundingRate * 100)

                val ctxF = requireContext()
                val colorF =
                    if (coin.fundingRate >= 0) R.color.vibrant_green else R.color.vibrant_red
                mcapValue.setTextColor(ctxF.getColor(colorF))

                // 9. Update chart to selected symbol
                currentSymbol = coin.symbol
                loadChartData()
            }.show(parentFragmentManager, "coinSheet")
        }
    }

    // ----------------- Chart helpers -----------------

    private fun setupChart(chartView: ChartsView) {

        val layoutOptions = LayoutOptions().apply {
            background = SolidColor(Color.parseColor("#0D0D0D"))
            textColor = Color.WHITE.toIntColor()
        }
        val localizationOptions = LocalizationOptions().apply {
            locale = "en-US"
            priceFormatter = PriceFormatter("{price:#2}$")
            timeFormatter = TimeFormatter("en-US", DateTimeFormat.DATE_TIME)
        }

        chartView.api.applyOptions(
            ChartOptions().apply {
                layout = layoutOptions
                localization = localizationOptions

                // PRICE SCALE (remove white border)
                priceScale = com.tradingview.lightweightcharts.api.options.models.PriceScaleOptions().apply {
                    borderColor = Color.TRANSPARENT.toIntColor()
                }

                // TIME SCALE (remove white border)
                timeScale = TimeScaleOptions().apply {
                    borderColor = Color.TRANSPARENT.toIntColor()
                    rightBarStaysOnScroll = true
                }

                handleScroll = HandleScrollOptions(true)
                handleScale = HandleScaleOptions(true, true)
            }
        )
    }



    private fun loadChartData() {
        val symbol = currentSymbol
        val interval = currentInterval

        fetchKlines(
            symbol = symbol,
            interval = interval,
            limit = 1000,
            onSuccess = { candles ->
                val sorted = candles.sortedBy { it.time }
                val data = sorted.map {
                    CandlestickData(
                        time = Time.Utc(it.time),
                        open = it.open,
                        high = it.high,
                        low = it.low,
                        close = it.close
                    )
                }

                activity?.runOnUiThread {
                    lastCandles = sorted
                    candlestickSeries?.setData(data)

                }
            },
            onError = {
                Log.e("ChartFragment", "Failed to load candle data: ${it.message}", it)
            }
        )
    }
}
