package com.example.rangesplitter.indicators

import com.example.rangesplitter.Candle
import com.tradingview.lightweightcharts.api.interfaces.SeriesApi
import com.tradingview.lightweightcharts.api.series.models.LineData
import com.tradingview.lightweightcharts.api.series.models.Time

class EMA(
    private val emaSeries: SeriesApi,   // The series you will draw the EMA line on
    private val period: Int = 12         // Default = EMA 12
) {

    fun render(candles: List<Candle>) {
        if (candles.isEmpty()) return

        // prepare close prices
        val closes = candles.map { it.close.toDouble() }

        // calculate EMA values
        val emaValues = calculateEMA(closes, period)

        // convert EMA values into Lightweight-Charts LineData
        val emaData = candles.indices.map { i ->
            LineData(
                time = Time.Utc(candles[i].time),
                value = emaValues[i].toFloat()
            )
        }

        // push to chart
        emaSeries.setData(emaData)
    }

    /**
     * Standard EMA calculation:
     * EMA[i] = price[i] * k + EMA[i-1] * (1 - k)
     * where k = 2 / (period + 1)
     */
    private fun calculateEMA(prices: List<Double>, period: Int): List<Double> {
        val ema = MutableList(prices.size) { 0.0 }

        if (prices.isEmpty()) return ema

        val k = 2.0 / (period + 1)
        ema[0] = prices[0]   // seed with first close

        for (i in 1 until prices.size) {
            ema[i] = prices[i] * k + ema[i - 1] * (1 - k)
        }

        return ema
    }
}
