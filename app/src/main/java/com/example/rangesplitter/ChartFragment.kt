package com.example.rangesplitter

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.rangesplitter.TradeUtils.fetchKlines
import com.example.rangesplitter.indicators.BB
import com.example.rangesplitter.indicators.DON
import com.example.rangesplitter.indicators.EMA
import com.example.rangesplitter.indicators.MA
import com.example.rangesplitter.indicators.Macro
import com.example.rangesplitter.indicators.RecentHL
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
import com.tradingview.lightweightcharts.api.options.models.applyLineSeriesOptions
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

    // Candle cache used by both REST and WebSocket
    private var lastCandles: List<Candle> = emptyList()

    private enum class ActiveIndicator { NONE, MACRO, EMA, MA, BB, DON, RECENT_HL }
    private var activeIndicator: ActiveIndicator = ActiveIndicator.NONE

    private var currentSymbol: String = "BTCUSDT"
    private var currentInterval: String = "60"

    private var isIndicatorPanelOpen: Boolean = false

    private val timeFrameMap = mapOf(
        R.id.btn1m to "1",
        R.id.btn5m to "5",
        R.id.btn15m to "15",
        R.id.btn1h to "60",
        R.id.btn4h to "240",
        R.id.btnD to "D",
    )

    // --- WebSocket helper (Streaming Implementation) ---
    private val klineWebSocket = BybitKlineWebSocket()

    // --- Header refs ---
    private lateinit var pairName: TextView
    private lateinit var currentPrice: TextView
    private lateinit var priceChange: TextView
    private lateinit var highValue: TextView
    private lateinit var lowValue: TextView
    private lateinit var volumeValue: TextView
    private lateinit var mcapValue: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pairName = view.findViewById(R.id.pairName)
        currentPrice = view.findViewById(R.id.currentPrice)
        priceChange = view.findViewById(R.id.priceChange)
        highValue = view.findViewById(R.id.highValue)
        lowValue = view.findViewById(R.id.lowValue)
        volumeValue = view.findViewById(R.id.volumeValue)
        mcapValue = view.findViewById(R.id.mcapValue)

        // indicator tiles (cards)
        val macroTile: View = view.findViewById(R.id.btnMacro)
        val emaTile: View = view.findViewById(R.id.btnEMA)
        val maTile: View? = view.findViewById(R.id.btnMA)
        val bbTile: View? = view.findViewById(R.id.btnBB)
        val donTile: View? = view.findViewById(R.id.btnDON)
        val recentHlTile: View? = view.findViewById(R.id.btnSUP) // HL

        // indicator grid + expand icon
        val indicatorsGrid: View = view.findViewById(R.id.indicatorsGrid)
        val expandIcon: ImageView = view.findViewById(R.id.indicatorsExpandIcon)

        indicatorsGrid.visibility = View.GONE
        isIndicatorPanelOpen = false
        expandIcon.setImageResource(android.R.drawable.arrow_up_float)

        expandIcon.setOnClickListener {
            isIndicatorPanelOpen = !isIndicatorPanelOpen
            indicatorsGrid.visibility = if (isIndicatorPanelOpen) View.VISIBLE else View.GONE
            expandIcon.setImageResource(
                if (isIndicatorPanelOpen) android.R.drawable.arrow_down_float
                else android.R.drawable.arrow_down_float
            )
        }

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

        // indicator tile clicks (toggle on/off)
        macroTile.setOnClickListener { toggleIndicator(ActiveIndicator.MACRO) }
        emaTile.setOnClickListener { toggleIndicator(ActiveIndicator.EMA) }
        maTile?.setOnClickListener { toggleIndicator(ActiveIndicator.MA) }
        bbTile?.setOnClickListener { toggleIndicator(ActiveIndicator.BB) }
        donTile?.setOnClickListener { toggleIndicator(ActiveIndicator.DON) }
        recentHlTile?.setOnClickListener { toggleIndicator(ActiveIndicator.RECENT_HL) }

        // COIN SELECT BOTTOM SHEET (only on click)
        pairName.setOnClickListener {
            CoinSelectBottomSheet { coin ->
                onCoinSelected(coin)
            }.show(parentFragmentManager, "coinSheet")
        }

        // ✅ Load BTC header + BTC chart immediately on enter
        loadDefaultBTC()
    }

    private fun toggleIndicator(type: ActiveIndicator) {
        activeIndicator = if (activeIndicator == type) ActiveIndicator.NONE else type
        updateIndicators()
    }

    // --- Reused "click handler" logic ---
    private fun onCoinSelected(coin: Coin) {
        pairName.text = coin.symbol
        currentPrice.text = coin.priceText

        priceChange.text = coin.changeText
        val ctx = requireContext()
        val color = if (coin.changeValue >= 0) R.color.vibrant_green else R.color.vibrant_red
        priceChange.setTextColor(ctx.getColor(color))

        highValue.text = String.format("$%,.2f", coin.high24h)
        lowValue.text = String.format("$%,.2f", coin.low24h)

        volumeValue.text = TradeUtils.formatCompactNumber(coin.turnover24h)

        mcapValue.text = String.format("%.4f%%", coin.fundingRate * 100)
        val colorF = if (coin.fundingRate >= 0) R.color.vibrant_green else R.color.vibrant_red
        mcapValue.setTextColor(ctx.getColor(colorF))

        currentSymbol = coin.symbol
        loadChartData()
    }

    // ✅ Fetch BTC once and apply same UI logic as a click
    private fun loadDefaultBTC() {
        // Optional: show something immediately while loading
        pairName.text = "BTCUSDT"

        TradeUtils.fetchTopCoinsWithPrices(
            sortMode = SortMode.VOLUME,
            limit = 50,
            onSuccess = { coins ->
                if (!isAdded) return@fetchTopCoinsWithPrices

                val btc = coins.firstOrNull { it.symbol.equals("BTCUSDT", true) }
                if (btc != null) {
                    activity?.runOnUiThread { onCoinSelected(btc) }
                } else {
                    // fallback: at least load candles
                    activity?.runOnUiThread {
                        currentSymbol = "BTCUSDT"
                        loadChartData()
                    }
                }
            },
            onError = {
                // fallback: at least load candles
                if (!isAdded) return@fetchTopCoinsWithPrices
                activity?.runOnUiThread {
                    currentSymbol = "BTCUSDT"
                    loadChartData()
                }
            }
        )
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

                            emaLine.applyLineSeriesOptions {
                                color = Color.parseColor("#FF8C00").toIntColor()
                            }
                        }

                        // Macro indicator
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
                            maLine.applyLineSeriesOptions {
                                color = Color.parseColor("#F4D13D").toIntColor()
                            }

                            maIndicator = MA(
                                maSeries = maLine,
                                period = 20,
                                type = MA.Type.SMA
                            )
                        }

                        // Bollinger Bands
                        chartView.api.addLineSeries { upper ->
                            bbUpperSeries = upper
                            upper.applyLineSeriesOptions {
                                color = Color.parseColor("#FF4B4B").toIntColor()
                            }

                            chartView.api.addLineSeries { middle ->
                                bbMiddleSeries = middle
                                middle.applyLineSeriesOptions {
                                    color = Color.parseColor("#CCCCCC").toIntColor()
                                }

                                chartView.api.addLineSeries { lower ->
                                    bbLowerSeries = lower
                                    lower.applyLineSeriesOptions {
                                        color = Color.parseColor("#44FF44").toIntColor()
                                    }

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

                            highSeries.applyLineSeriesOptions {
                                color = Color.parseColor("#FFFFFF").toIntColor()
                            }

                            chartView.api.addLineSeries { lowSeries ->
                                recentLowSeries = lowSeries

                                lowSeries.applyLineSeriesOptions {
                                    color = Color.parseColor("#FFFFFF").toIntColor()
                                }

                                recentHLIndicator = RecentHL(
                                    highLineSeries = highSeries,
                                    lowLineSeries = lowSeries,
                                    lookback = 20
                                )
                            }
                        }

                        // NOTE: we do NOT call loadChartData() here anymore,
                        // because loadDefaultBTC() in onViewCreated triggers it.
                    }
                }
            }
        }
    }

    // ----------------- Data loading (history) -----------------

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
                    updateIndicators()

                    // ✅ Avoid multiple subscriptions
                    klineWebSocket.disconnect()

                    // --- Streaming Implementation (subscribe) ---
                    klineWebSocket.subscribe(
                        symbol = symbol,
                        interval = interval,
                        onBar = { bar -> handleRealtimeBar(bar) },
                        onError = { e ->
                            Log.e("ChartFragment", "WS error: ${e.message}", e)
                        }
                    )
                }
            },
            onError = {
                Log.e("ChartFragment", "Failed to load candle data: ${it.message}", it)
            }
        )
    }

    // ----------------- Streaming handler (lightweight-charts style) -----------------

    private fun handleRealtimeBar(bar: KlineBar) {
        if (lastCandles.isEmpty()) {
            Log.w("RT_BAR", "No candles yet, ignoring realtime bar")
            return
        }

        val current = lastCandles
        val last = current.last()

        val updatedList: List<Candle> = when {
            bar.time == last.time -> {
                current.dropLast(1) + Candle(
                    time = bar.time,
                    open = bar.open,
                    high = bar.high,
                    low = bar.low,
                    close = bar.close
                )
            }
            bar.time > last.time -> {
                current + Candle(
                    time = bar.time,
                    open = bar.open,
                    high = bar.high,
                    low = bar.low,
                    close = bar.close
                )
            }
            else -> return
        }

        lastCandles = updatedList

        val listForChart = updatedList.map {
            CandlestickData(
                time = Time.Utc(it.time), // it.time is SECONDS
                open = it.open,
                high = it.high,
                low = it.low,
                close = it.close
            )
        }

        activity?.runOnUiThread {
            candlestickSeries?.setData(listForChart)
            updateIndicators()
        }
    }

    // ----------------- Indicator logic -----------------

    private fun updateIndicators() {
        if (lastCandles.isEmpty()) return

        clearAllIndicators()

        when (activeIndicator) {
            ActiveIndicator.NONE -> Unit
            ActiveIndicator.MACRO -> macroIndicator?.render(lastCandles, true)
            ActiveIndicator.EMA -> ema12Indicator?.render(lastCandles)
            ActiveIndicator.MA -> maIndicator?.render(lastCandles)
            ActiveIndicator.BB -> bbIndicator?.render(lastCandles)
            ActiveIndicator.DON -> donIndicator?.render(lastCandles)
            ActiveIndicator.RECENT_HL -> recentHLIndicator?.render(lastCandles)
        }
    }

    private fun clearAllIndicators() {
        fastEmaSeries?.setData(emptyList())
        slowEmaSeries?.setData(emptyList())
        biasEmaSeries?.setData(emptyList())
        candlestickSeries?.setMarkers(emptyList())

        ema12Series?.setData(emptyList())
        maSeries?.setData(emptyList())

        bbIndicator?.clear()
        donIndicator?.clear()
        recentHLIndicator?.clear()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        klineWebSocket.disconnect()
    }
}