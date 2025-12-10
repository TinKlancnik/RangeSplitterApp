package com.example.rangesplitter

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.rangesplitter.TradeUtils.fetchKlines
import com.example.rangesplitter.indicators.Macro
import com.example.rangesplitter.indicators.EMA
import com.example.rangesplitter.indicators.MA
import com.example.rangesplitter.indicators.BB
import com.example.rangesplitter.indicators.DON
import com.example.rangesplitter.indicators.RecentHL   // ⬅ use RecentHL instead of SUP
import com.google.android.material.chip.ChipGroup
import com.tradingview.lightweightcharts.api.chart.models.color.surface.SolidColor
import com.tradingview.lightweightcharts.api.chart.models.color.toIntColor
import com.tradingview.lightweightcharts.api.interfaces.SeriesApi
import com.tradingview.lightweightcharts.api.options.models.ChartOptions
import com.tradingview.lightweightcharts.api.options.models.GridLineOptions
import com.tradingview.lightweightcharts.api.options.models.GridOptions
import com.tradingview.lightweightcharts.api.options.models.HandleScaleOptions
import com.tradingview.lightweightcharts.api.options.models.HandleScrollOptions
import com.tradingview.lightweightcharts.api.options.models.LayoutOptions
import com.tradingview.lightweightcharts.api.options.models.LocalizationOptions
import com.tradingview.lightweightcharts.api.options.models.PriceScaleOptions
import com.tradingview.lightweightcharts.api.options.models.TimeScaleOptions
import com.tradingview.lightweightcharts.api.series.models.CandlestickData
import com.tradingview.lightweightcharts.api.series.models.Time
import com.tradingview.lightweightcharts.runtime.plugins.DateTimeFormat
import com.tradingview.lightweightcharts.runtime.plugins.PriceFormatter
import com.tradingview.lightweightcharts.runtime.plugins.TimeFormatter
import com.tradingview.lightweightcharts.view.ChartsView

class ChartFragment : Fragment(R.layout.fragment_chart) {

    // --- Chart / series refs ---
    private var candlestickSeries: SeriesApi? = null

    // Macro EMAs
    private var fastEmaSeries: SeriesApi? = null
    private var slowEmaSeries: SeriesApi? = null
    private var biasEmaSeries: SeriesApi? = null

    // EMA 12
    private var ema12Series: SeriesApi? = null

    // MA (e.g. SMA 20)
    private var maSeries: SeriesApi? = null

    // Bollinger Bands
    private var bbUpperSeries: SeriesApi? = null
    private var bbMiddleSeries: SeriesApi? = null
    private var bbLowerSeries: SeriesApi? = null

    // Donchian Channels
    private var donUpperSeries: SeriesApi? = null
    private var donMiddleSeries: SeriesApi? = null
    private var donLowerSeries: SeriesApi? = null

    // Recent High / Low lines
    private var recentHighSeries: SeriesApi? = null
    private var recentLowSeries: SeriesApi? = null

    // --- Indicators ---
    private var macroIndicator: Macro? = null
    private var ema12Indicator: EMA? = null
    private var maIndicator: MA? = null
    private var bbIndicator: BB? = null
    private var donIndicator: DON? = null
    private var recentHLIndicator: RecentHL? = null

    private var lastCandles: List<Candle> = emptyList()

    private enum class ActiveIndicator { NONE, MACRO, EMA, MA, BB, DON, RECENT_HL }

    // ⬅ default: NONE → no indicator on load
    private var activeIndicator: ActiveIndicator = ActiveIndicator.NONE

    private var currentSymbol: String = "BTCUSDT"
    private var currentInterval: String = "60"

    private val timeFrameMap = mapOf(
        R.id.btn1m  to "1",
        R.id.btn5m  to "5",
        R.id.btn15m to "15",
        R.id.btn1h  to "60",
        R.id.btn4h  to "240",
        R.id.btnD   to "D",
    )

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

        // indicator tiles (cards)
        val macroTile: View = view.findViewById(R.id.btnMacro)
        val emaTile: View = view.findViewById(R.id.btnEMA)

        // optional extra tiles
        val maTile: View? = view.findViewById(R.id.btnMA)
        val bbTile: View? = view.findViewById(R.id.btnBB)
        val donTile: View? = view.findViewById(R.id.btnDON)
        val recentHlTile: View? = view.findViewById(R.id.btnSUP) // reuse SUP button for Recent HL

        val chartView = view.findViewById<ChartsView>(R.id.charts_view)
        setupChart(chartView)

        // timeframe chips
        val timeframeGroup = view.findViewById<ChipGroup>(R.id.timeframeGroup)
        timeframeGroup.check(R.id.btn1h)
        currentInterval = "60"

        timeframeGroup.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == View.NO_ID) return@setOnCheckedChangeListener
            val newInterval = timeFrameMap[checkedId] ?: return@setOnCheckedChangeListener
            if (newInterval == currentInterval) return@setOnCheckedChangeListener

            currentInterval = newInterval
            loadChartData()
        }

        // indicator tile clicks
        macroTile.setOnClickListener {
            activeIndicator = ActiveIndicator.MACRO
            Log.d("ChartFragment", "Macro tile clicked")
            updateIndicators()
        }

        emaTile.setOnClickListener {
            activeIndicator = ActiveIndicator.EMA
            Log.d("ChartFragment", "EMA tile clicked")
            updateIndicators()
        }

        maTile?.setOnClickListener {
            activeIndicator = ActiveIndicator.MA
            Log.d("ChartFragment", "MA tile clicked")
            updateIndicators()
        }

        bbTile?.setOnClickListener {
            activeIndicator = ActiveIndicator.BB
            Log.d("ChartFragment", "BB tile clicked")
            updateIndicators()
        }

        donTile?.setOnClickListener {
            activeIndicator = ActiveIndicator.DON
            Log.d("ChartFragment", "DON tile clicked")
            updateIndicators()
        }

        // Recent High/Low button
        recentHlTile?.setOnClickListener {
            activeIndicator = ActiveIndicator.RECENT_HL
            Log.d("ChartFragment", "RecentHL tile clicked")
            updateIndicators()
        }

        // ---------- COIN SELECT BOTTOM SHEET ----------
        pairName.setOnClickListener {
            CoinSelectBottomSheet { coin ->

                pairName.text = coin.symbol

                val base = coin.symbol.takeWhile { it.isLetter() }
                assetSubtitle.text = base

                currentPrice.text = coin.priceText

                priceChange.text = coin.changeText
                val ctx = requireContext()
                val color =
                    if (coin.changeValue >= 0) R.color.vibrant_green else R.color.vibrant_red
                priceChange.setTextColor(ctx.getColor(color))

                highValue.text = String.format("$%,.2f", coin.high24h)
                lowValue.text = String.format("$%,.2f", coin.low24h)

                val volumeFormatted = TradeUtils.formatCompactNumber(coin.turnover24h)
                volumeValue.text = volumeFormatted

                mcapValue.text = String.format("%.4f%%", coin.fundingRate * 100)
                val ctxF = requireContext()
                val colorF =
                    if (coin.fundingRate >= 0) R.color.vibrant_green else R.color.vibrant_red
                mcapValue.setTextColor(ctxF.getColor(colorF))

                currentSymbol = coin.symbol
                loadChartData()
            }.show(parentFragmentManager, "coinSheet")
        }
    }

    // ----------------- Chart setup -----------------

    private fun setupChart(chartView: ChartsView) {
        val layoutOptions = LayoutOptions().apply {
            background = SolidColor(Color.TRANSPARENT)
            textColor = Color.parseColor("#EAEDED").toIntColor()
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

                priceScale = PriceScaleOptions().apply {
                    borderColor = Color.TRANSPARENT.toIntColor()
                }

                timeScale = TimeScaleOptions().apply {
                    borderColor = Color.TRANSPARENT.toIntColor()
                    rightBarStaysOnScroll = true
                }

                handleScroll = HandleScrollOptions(true)
                handleScale = HandleScaleOptions(true, true)

                grid = GridOptions().apply {
                    vertLines = GridLineOptions().apply {
                        color = Color.parseColor("#4D00C9A7").toIntColor()
                    }
                    horzLines = GridLineOptions().apply {
                        color = Color.parseColor("#4D00C9A7").toIntColor()
                    }
                }
            }
        )

        // main candle series + indicator series
        chartView.api.addCandlestickSeries { candleSeries ->
            candlestickSeries = candleSeries

            // NEST all EMA series + indicators so they exist before we use them
            chartView.api.addLineSeries { fast ->
                fastEmaSeries = fast

                chartView.api.addLineSeries { slow ->
                    slowEmaSeries = slow

                    chartView.api.addLineSeries { bias ->
                        biasEmaSeries = bias

                        // EMA 12
                        chartView.api.addLineSeries { emaLine ->
                            ema12Series = emaLine
                            ema12Indicator = EMA(emaLine, 12)
                        }

                        // Macro indicator – we now have candleSeries, fast, slow, bias
                        macroIndicator = Macro(
                            candlestickSeries = candleSeries,
                            fastEmaSeries = fast,
                            slowEmaSeries = slow,
                            biasEmaSeries = bias,
                            useStochRsiFilter = false
                        )

                        // MA (e.g. SMA 20)
                        chartView.api.addLineSeries { maLine ->
                            maSeries = maLine
                            maIndicator = MA(
                                maSeries = maLine,
                                period = 20,
                                type = MA.Type.SMA
                            )
                        }

                        // Bollinger Bands
                        chartView.api.addLineSeries { upper ->
                            bbUpperSeries = upper
                            chartView.api.addLineSeries { middle ->
                                bbMiddleSeries = middle
                                chartView.api.addLineSeries { lower ->
                                    bbLowerSeries = lower
                                    bbIndicator = BB(
                                        upperSeries = upper,
                                        middleSeries = middle,
                                        lowerSeries = lower,
                                        period = 20,
                                        multiplier = 2.0
                                    )
                                }
                            }
                        }

                        // Donchian Channels
                        chartView.api.addLineSeries { upper ->
                            donUpperSeries = upper
                            chartView.api.addLineSeries { middle ->
                                donMiddleSeries = middle
                                chartView.api.addLineSeries { lower ->
                                    donLowerSeries = lower
                                    donIndicator = DON(
                                        upperSeries = upper,
                                        middleSeries = middle,
                                        lowerSeries = lower,
                                        period = 20
                                    )
                                }
                            }
                        }

                        // Recent High / Low lines
                        chartView.api.addLineSeries { highSeries ->
                            recentHighSeries = highSeries
                            chartView.api.addLineSeries { lowSeries ->
                                recentLowSeries = lowSeries

                                recentHLIndicator = RecentHL(
                                    highLineSeries = highSeries,
                                    lowLineSeries = lowSeries,
                                    lookback = 20
                                )
                            }
                        }

                        // initial load AFTER everything is wired
                        loadChartData()
                    }
                }
            }
        }
    }

    // ----------------- Data loading -----------------

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
                    Log.d("ChartFragment", "Loaded ${lastCandles.size} candles")
                    // ⬅ Do NOT force an indicator: activeIndicator may be NONE
                    updateIndicators()
                }
            },
            onError = {
                Log.e("ChartFragment", "Failed to load candle data: ${it.message}", it)
            }
        )
    }

    // ----------------- Indicator switching -----------------

    private fun updateIndicators() {
        if (lastCandles.isEmpty()) {
            Log.w("ChartFragment", "updateIndicators: no candles yet")
            return
        }

        clearAllIndicators()

        when (activeIndicator) {
            ActiveIndicator.NONE -> {
                // show only candles; nothing to render
                Log.d("ChartFragment", "No indicator active")
            }
            ActiveIndicator.MACRO -> {
                Log.d("ChartFragment", "Applying MACRO indicator")
                macroIndicator?.render(lastCandles, true)
            }
            ActiveIndicator.EMA -> {
                Log.d("ChartFragment", "Applying EMA 12 indicator")
                ema12Indicator?.render(lastCandles)
            }
            ActiveIndicator.MA -> {
                Log.d("ChartFragment", "Applying MA indicator")
                maIndicator?.render(lastCandles)
            }
            ActiveIndicator.BB -> {
                Log.d("ChartFragment", "Applying Bollinger Bands indicator")
                bbIndicator?.render(lastCandles)
            }
            ActiveIndicator.DON -> {
                Log.d("ChartFragment", "Applying Donchian Channels indicator")
                donIndicator?.render(lastCandles)
            }
            ActiveIndicator.RECENT_HL -> {
                Log.d("ChartFragment", "Applying Recent High/Low indicator")
                recentHLIndicator?.render(lastCandles)
            }
        }
    }

    /**
     * Clears all overlay indicators (lines + markers) from chart.
     * Candles remain.
     */
    private fun clearAllIndicators() {
        // Macro EMAs + markers
        fastEmaSeries?.setData(emptyList())
        slowEmaSeries?.setData(emptyList())
        biasEmaSeries?.setData(emptyList())
        candlestickSeries?.setMarkers(emptyList())

        // EMA 12
        ema12Series?.setData(emptyList())

        // MA
        maSeries?.setData(emptyList())

        // Bands / channels
        bbIndicator?.clear()
        donIndicator?.clear()

        // Recent HL
        recentHLIndicator?.clear()
    }
}
