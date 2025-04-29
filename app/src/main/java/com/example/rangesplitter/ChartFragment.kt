package com.example.rangesplitter

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.tradingview.lightweightcharts.api.chart.models.color.toIntColor
import com.tradingview.lightweightcharts.api.interfaces.SeriesApi
import com.tradingview.lightweightcharts.api.options.models.LayoutOptions
import com.tradingview.lightweightcharts.api.options.models.LocalizationOptions
import com.tradingview.lightweightcharts.api.series.models.CandlestickData
import com.tradingview.lightweightcharts.api.series.models.Time
import com.tradingview.lightweightcharts.runtime.plugins.DateTimeFormat
import com.tradingview.lightweightcharts.runtime.plugins.PriceFormatter
import com.tradingview.lightweightcharts.runtime.plugins.TimeFormatter
import com.tradingview.lightweightcharts.view.ChartsView
import com.example.rangesplitter.TradeUtils.fetchKlines
import com.tradingview.lightweightcharts.api.options.models.HandleScaleOptions
import com.tradingview.lightweightcharts.api.options.models.HandleScrollOptions

class ChartFragment : Fragment() {

    private var candlestickSeries: SeriesApi? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_chart, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val chartView = view.findViewById<ChartsView>(R.id.charts_view)
        if (chartView == null) {
            Log.e("ChartFragment", "ChartView is null")
            return
        }

        // Chart styling
        val layoutOptions = LayoutOptions().apply {
            textColor = Color.BLACK.toIntColor()
        }

        val localizationOptions = LocalizationOptions().apply {
            locale = "en-US"
            priceFormatter = PriceFormatter("{price:#2}$")
            timeFormatter = TimeFormatter("en-US", DateTimeFormat.DATE_TIME)
        }

// Instead of trying to modify chartView.api.timeScale inside applyOptions, do this:
        val options = com.tradingview.lightweightcharts.api.options.models.ChartOptions().apply {
            layout = layoutOptions
            localization = localizationOptions
            timeScale = com.tradingview.lightweightcharts.api.options.models.TimeScaleOptions().apply {
                fixLeftEdge = false
                lockVisibleTimeRangeOnResize = false
                rightBarStaysOnScroll = true
            }
            handleScroll = HandleScrollOptions(true) // ✅ Correct way
            handleScale = HandleScaleOptions(true, true)
        }

// Then apply the whole options object
        chartView.api.applyOptions(options)



        // Add candlestick series and fetch data
        chartView.api.addCandlestickSeries { series ->
            candlestickSeries = series


            fetchKlines(
                symbol = "BTCUSDT",
                interval = "15",
                limit = 1000,
                onSuccess = { candles ->
                    val data = candles.map {
                        CandlestickData(
                            time = Time.Utc(it.time),
                            open = it.open,
                            high = it.high,
                            low = it.low,
                            close = it.close
                        )
                    }.sortedBy { (it.time as Time.Utc).timestamp }

                    Log.d("ChartFragment", "Candles ready: ${data.size}")
                    data.forEachIndexed { i, c ->
                        Log.d("ChartFragment", "Candle $i -> time: ${c.time}, open: ${c.open}, close: ${c.close}")
                    }

                    activity?.runOnUiThread {
                        candlestickSeries?.setData(data)
                    }
                },
                onError = {
                    Log.e("ChartFragment", "Failed to load candle data: ${it.message}", it)
                }
            )
        }
    }
}
