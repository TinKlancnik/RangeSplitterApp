package com.example.rangesplitter.indicators

import com.example.rangesplitter.Candle
import com.tradingview.lightweightcharts.api.interfaces.SeriesApi
import com.tradingview.lightweightcharts.api.series.models.LineData
import com.tradingview.lightweightcharts.api.series.models.Time
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Bollinger Bands indicator.
 *
 * Uses closing prices to compute:
 *  - Middle band: Simple Moving Average (SMA)
 *  - Upper band:  SMA + multiplier * StdDev
 *  - Lower band:  SMA - multiplier * StdDev
 *
 * @param upperSeries   Line series for upper band
 * @param middleSeries  Line series for middle (basis) band
 * @param lowerSeries   Line series for lower band
 * @param period        Lookback period for SMA / StdDev (default 20)
 * @param multiplier    StdDev multiplier (default 2.0)
 */
class BB(
    private val upperSeries: SeriesApi,
    private val middleSeries: SeriesApi,
    private val lowerSeries: SeriesApi,
    private val period: Int = 20,
    private val multiplier: Double = 2.0
) {

    /**
     * Renders Bollinger Bands on the provided series.
     */
    fun render(candles: List<Candle>) {
        if (candles.isEmpty()) return

        val closes = candles.map { it.close.toDouble() }

        val size = closes.size
        val middle = MutableList(size) { 0.0 }
        val upper = MutableList(size) { 0.0 }
        val lower = MutableList(size) { 0.0 }

        for (i in closes.indices) {
            val start = max(0, i - period + 1)
            val window = closes.subList(start, i + 1)

            // SMA for middle band
            val mean = window.average()

            // Standard deviation for window
            val variance = window
                .map { (it - mean).pow(2.0) }
                .average()

            val stdDev = sqrt(variance)

            middle[i] = mean
            upper[i] = mean + multiplier * stdDev
            lower[i] = mean - multiplier * stdDev
        }

        // convert to LineData for the chart
        val middleData = candles.indices.map { i ->
            LineData(
                time = Time.Utc(candles[i].time),
                value = middle[i].toFloat()
            )
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

        // push to chart
        middleSeries.setData(middleData)
        upperSeries.setData(upperData)
        lowerSeries.setData(lowerData)
    }

    /**
     * Clears all three band series from the chart.
     */
    fun clear() {
        upperSeries.setData(emptyList())
        middleSeries.setData(emptyList())
        lowerSeries.setData(emptyList())
    }
}
