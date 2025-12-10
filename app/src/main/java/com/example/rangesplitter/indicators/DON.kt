package com.example.rangesplitter.indicators

import com.example.rangesplitter.Candle
import com.tradingview.lightweightcharts.api.interfaces.SeriesApi
import com.tradingview.lightweightcharts.api.series.models.LineData
import com.tradingview.lightweightcharts.api.series.models.Time
import kotlin.math.max

/**
 * Donchian Channels indicator.
 *
 * Uses high/low prices over a rolling window:
 *  - Upper band: highest high over [period]
 *  - Lower band: lowest low over [period]
 *  - Middle band: (upper + lower) / 2
 *
 * @param upperSeries   Line series for upper band
 * @param middleSeries  Line series for middle band (mid-channel)
 * @param lowerSeries   Line series for lower band
 * @param period        Lookback period (default 20)
 */
class DON(
    private val upperSeries: SeriesApi,
    private val middleSeries: SeriesApi,
    private val lowerSeries: SeriesApi,
    private val period: Int = 20
) {

    /**
     * Renders Donchian Channels on the provided candles.
     */
    fun render(candles: List<Candle>) {
        if (candles.isEmpty()) return

        val size = candles.size
        val upper = MutableList(size) { 0.0 }
        val lower = MutableList(size) { 0.0 }
        val middle = MutableList(size) { 0.0 }

        for (i in candles.indices) {
            val start = max(0, i - period + 1)
            val window = candles.subList(start, i + 1)

            val highestHigh = window.maxOf { it.high.toDouble() }
            val lowestLow = window.minOf { it.low.toDouble() }

            upper[i] = highestHigh
            lower[i] = lowestLow
            middle[i] = (highestHigh + lowestLow) / 2.0
        }

        val upperData = candles.indices.map { i ->
            LineData(
                time = Time.Utc(candles[i].time),
                value = upper[i].toFloat()
            )
        }

        val lowerData = candles.indices.map { i ->
            LineData(
                time = Time.Utc(candles[i].time),
                value = lower[i].toFloat()
            )
        }

        val middleData = candles.indices.map { i ->
            LineData(
                time = Time.Utc(candles[i].time),
                value = middle[i].toFloat()
            )
        }

        upperSeries.setData(upperData)
        lowerSeries.setData(lowerData)
        middleSeries.setData(middleData)
    }

    /**
     * Clears all Donchian bands from the chart.
     */
    fun clear() {
        upperSeries.setData(emptyList())
        middleSeries.setData(emptyList())
        lowerSeries.setData(emptyList())
    }
}
