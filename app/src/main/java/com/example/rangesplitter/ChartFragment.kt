package com.example.rangesplitter

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.fragment.app.Fragment
import com.example.rangesplitter.TradeUtils.fetchKlines
import com.example.rangesplitter.indicators.Macro
import com.tradingview.lightweightcharts.api.chart.models.color.toIntColor
import com.tradingview.lightweightcharts.api.interfaces.SeriesApi
import com.tradingview.lightweightcharts.api.options.models.HandleScaleOptions
import com.tradingview.lightweightcharts.api.options.models.HandleScrollOptions
import com.tradingview.lightweightcharts.api.options.models.LayoutOptions
import com.tradingview.lightweightcharts.api.options.models.LocalizationOptions
import com.tradingview.lightweightcharts.api.series.models.CandlestickData
import com.tradingview.lightweightcharts.api.series.models.Time
import com.tradingview.lightweightcharts.runtime.plugins.DateTimeFormat
import com.tradingview.lightweightcharts.runtime.plugins.PriceFormatter
import com.tradingview.lightweightcharts.runtime.plugins.TimeFormatter
import com.tradingview.lightweightcharts.view.ChartsView

class ChartFragment : Fragment() {

    // keep EVERYTHING inside the fragment
    private var candlestickSeries: SeriesApi? = null
    private var fastEmaSeries: SeriesApi? = null
    private var slowEmaSeries: SeriesApi? = null
    private var biasEmaSeries: SeriesApi? = null

    private var xoIndicator: Macro? = null
    private var lastCandles: List<Candle> = emptyList()

    private val timeFrameMap = mapOf(
        "1H" to "60",
        "2H" to "120",
        "4H" to "240",
        "6H" to "360",
        "12H" to "720",
        "1D" to "D",
        "1W" to "W",
        "1M" to "M"
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_chart, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val chartView = view.findViewById<ChartsView>(R.id.charts_view)
        val timeFrameSpinner = view.findViewById<Spinner>(R.id.time_frame_spinner)
        if (chartView == null) { Log.e("ChartFragment", "ChartView is null"); return }

        // options
        val layoutOptions = LayoutOptions().apply { textColor = Color.BLACK.toIntColor() }
        val localizationOptions = LocalizationOptions().apply {
            locale = "en-US"
            priceFormatter = PriceFormatter("{price:#2}$")
            timeFormatter = TimeFormatter("en-US", DateTimeFormat.DATE_TIME)
        }
        chartView.api.applyOptions(
            com.tradingview.lightweightcharts.api.options.models.ChartOptions().apply {
                layout = layoutOptions
                localization = localizationOptions
                timeScale = com.tradingview.lightweightcharts.api.options.models.TimeScaleOptions().apply {
                    fixLeftEdge = false
                    lockVisibleTimeRangeOnResize = false
                    rightBarStaysOnScroll = true
                }
                handleScroll = HandleScrollOptions(true)
                handleScale = HandleScaleOptions(true, true)
            }
        )

        // Create all series in a CHAIN, then init Macro, then set up UI/fetch
        chartView.api.addCandlestickSeries { cs ->
            candlestickSeries = cs

            chartView.api.addLineSeries { fast ->
                fastEmaSeries = fast

                chartView.api.addLineSeries { slow ->
                    slowEmaSeries = slow

                    chartView.api.addLineSeries { bias ->
                        biasEmaSeries = bias

                        // ✅ now safe to construct Macro
                        xoIndicator = Macro(
                            candlestickSeries = cs,
                            fastEmaSeries = fast,
                            slowEmaSeries = slow,
                            biasEmaSeries = bias,
                            useStochRsiFilter = false
                        )

                        // spinner
                        val labels = timeFrameMap.keys.toList()
                        timeFrameSpinner.adapter = ArrayAdapter(
                            requireContext(),
                            android.R.layout.simple_spinner_item,
                            labels
                        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

                        timeFrameSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                                val tf = timeFrameMap[labels[position]] ?: "60"
                                fetchCandles(tf)
                            }
                            override fun onNothingSelected(parent: AdapterView<*>) {}
                        }

                        // initial load
                        fetchCandles("60")
                    }
                }
            }
        }
    }

    private fun fetchCandles(timeFrame: String) {
        fetchKlines(
            symbol = "BTCUSDT",
            interval = timeFrame,
            limit = 1000,
            onSuccess = { candles ->
                val sorted = candles.sortedBy { it.time } // keep raw list for indicators
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
                    lastCandles = sorted                     // ✅ set before render-
                    candlestickSeries?.setData(data)         // draw candles first
                    xoIndicator?.render(lastCandles, true)   // then overlay indicator
                }
            },
            onError = { Log.e("ChartFragment", "Failed to load candle data: ${it.message}", it) }
        )
    }
}
