package com.example.rangesplitter.indicators

import com.example.rangesplitter.Candle
import com.tradingview.lightweightcharts.api.interfaces.SeriesApi
import com.tradingview.lightweightcharts.api.series.models.*
import com.tradingview.lightweightcharts.api.chart.models.color.toIntColor
import android.graphics.Color

/**
 * Recent Highs & Lows Indicator
 *
 * Detects recent swing highs & swing lows using a simple pivot method.
 * Draws:
 *  • horizontal line at swing high
 *  • horizontal line at swing low
 */
class RecentHL(
    private val highLineSeries: SeriesApi,
    private val lowLineSeries: SeriesApi,
    private val lookback: Int = 20   // number of candles to scan for recent HL
) {

    fun render(candles: List<Candle>) {
        if (candles.size < lookback) return

        val recent = candles.takeLast(lookback)

        // Find highest & lowest prices in the lookback window
        val highCandle = recent.maxByOrNull { it.high }!!
        val lowCandle  = recent.minByOrNull { it.low }!!

        val highLevel = highCandle.high.toFloat()
        val lowLevel = lowCandle.low.toFloat()

        // Line spans the full visible time window
        val times = candles.map { Time.Utc(it.time) }

        val highLine = times.map {
            LineData(time = it, value = highLevel)
        }
        val lowLine = times.map {
            LineData(time = it, value = lowLevel)
        }

        highLineSeries.setData(highLine)
        lowLineSeries.setData(lowLine)
    }

    fun clear() {
        highLineSeries.setData(emptyList())
        lowLineSeries.setData(emptyList())
    }
}
