package com.example.rangesplitter

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
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

    // Map of displayed time frames to corresponding numeric values
    private val timeFrameMap = mapOf(
        "1H" to "60",    // 1 hour -> "60" minutes
        "2H" to "120",   // 2 hours -> "120" minutes
        "4H" to "240",   // 4 hours -> "240" minutes
        "6H" to "360",   // 6 hours -> "360" minutes
        "12H" to "720",  // 12 hours -> "720" minutes
        "1D" to "D",     // 1 day -> "D"
        "1W" to "W",     // 1 week -> "W"
        "1M" to "M"      // 1 month -> "M"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_chart, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val chartView = view.findViewById<ChartsView>(R.id.charts_view)
        val timeFrameSpinner = view.findViewById<Spinner>(R.id.time_frame_spinner)

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

        // Apply chart options
        chartView.api.applyOptions(options)

        // Add candlestick series and fetch data
        chartView.api.addCandlestickSeries { series ->
            candlestickSeries = series

            // Set up the spinner with the time frame labels
            val timeFrameLabels = timeFrameMap.keys.toList()
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, timeFrameLabels)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            timeFrameSpinner.adapter = adapter

            // Fetch initial candle data for the default time frame (e.g., 1H)
            fetchCandles("60") // "60" for 1 hour

            // Set a listener for the spinner
            timeFrameSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    val selectedTimeFrameLabel = timeFrameLabels[position]
                    val selectedTimeFrame = timeFrameMap[selectedTimeFrameLabel] ?: "60" // Default to "60" if something goes wrong
                    // Call loadCandles with the selected time frame
                    fetchCandles(selectedTimeFrame)
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    // Optionally handle when nothing is selected
                }
            }
        }
    }

    // Use this function to fetch the candles with the selected time frame
    private fun fetchCandles(timeFrame: String) {
        fetchKlines(
            symbol = "BTCUSDT",
            interval = timeFrame,
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


