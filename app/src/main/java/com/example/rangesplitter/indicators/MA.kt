package com.example.rangesplitter.indicators

import com.example.rangesplitter.Candle
import com.tradingview.lightweightcharts.api.interfaces.SeriesApi
import com.tradingview.lightweightcharts.api.series.models.LineData
import com.tradingview.lightweightcharts.api.series.models.Time

/**
 * General-purpose Moving Average indicator supporting:
 *  - SMA (Simple Moving Average)
 *  - EMA (Exponential Moving Average)
 *
 * @param maSeries  The line series where the MA will be drawn
 * @param period    Number of candles used in the MA calculation
 * @param type      Type of MA: "SMA" or "EMA"
 */
class MA(
    private val maSeries: SeriesApi,
    private val period: Int = 20,
    private val type: Type = Type.SMA
) {

    enum class Type {
        SMA, EMA
    }

    /** Draw MA on chart */
    fun render(candles: List<Candle>) {
        if (candles.isEmpty()) return

        val closes = candles.map { it.close.toDouble() }

        val maValues = when (type) {
            Type.SMA -> calculateSMA(closes, period)
            Type.EMA -> calculateEMA(closes, period)
        }

        val maData = candles.indices.map { i ->
            LineData(
                time = Time.Utc(candles[i].time),
                value = maValues[i].toFloat()
            )
        }

        maSeries.setData(maData)
    }

    /** Simple Moving Average */
    private fun calculateSMA(prices: List<Double>, period: Int): List<Double> {
        val result = MutableList(prices.size) { 0.0 }
        var sum = 0.0

        for (i in prices.indices) {
            sum += prices[i]

            if (i >= period) {
                sum -= prices[i - period]
            }

            result[i] = if (i >= period - 1) sum / period else prices[i]
        }
        return result
    }

    /** Exponential Moving Average */
    private fun calculateEMA(prices: List<Double>, period: Int): List<Double> {
        if (prices.isEmpty()) return emptyList()
        val result = MutableList(prices.size) { 0.0 }

        val k = 2.0 / (period + 1)
        result[0] = prices[0]

        for (i in 1 until prices.size) {
            result[i] = prices[i] * k + result[i - 1] * (1 - k)
        }

        return result
    }
}
