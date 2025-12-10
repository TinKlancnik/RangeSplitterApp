package com.example.rangesplitter.indicators

import com.example.rangesplitter.Candle
import com.tradingview.lightweightcharts.api.interfaces.SeriesApi
import com.tradingview.lightweightcharts.api.series.models.LineData
import com.tradingview.lightweightcharts.api.series.models.Time
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Supertrend (SUP) indicator.
 *
 * Uses:
 *  - ATR (Average True Range)
 *  - Multiplier (e.g. 3.0)
 *
 * Plots:
 *  - upSeries: Supertrend when trend is bullish
 *  - downSeries: Supertrend when trend is bearish
 *
 * Typical defaults:
 *  period = 10, multiplier = 3.0
 */
class SUP(
    private val upSeries: SeriesApi,
    private val downSeries: SeriesApi,
    private val period: Int = 10,
    private val multiplier: Double = 3.0
) {

    /**
     * Render Supertrend onto the chart.
     */
    fun render(candles: List<Candle>) {
        if (candles.size < 2) return

        val size = candles.size

        // --- 1. Compute True Range (TR) & ATR ---
        val tr = MutableList(size) { 0.0 }
        val atr = MutableList(size) { 0.0 }

        for (i in candles.indices) {
            if (i == 0) {
                tr[i] = candles[i].high.toDouble() - candles[i].low.toDouble()
            } else {
                val high = candles[i].high.toDouble()
                val low = candles[i].low.toDouble()
                val prevClose = candles[i - 1].close.toDouble()

                val tr1 = high - low
                val tr2 = abs(high - prevClose)
                val tr3 = abs(low - prevClose)

                tr[i] = max(tr1, max(tr2, tr3))
            }
        }

        // Wilder's ATR
        atr[0] = tr[0]
        for (i in 1 until size) {
            atr[i] = (atr[i - 1] * (period - 1) + tr[i]) / period
        }

        // --- 2. Basic upper / lower bands ---
        val basicUpperBand = MutableList(size) { 0.0 }
        val basicLowerBand = MutableList(size) { 0.0 }

        for (i in candles.indices) {
            val midPrice = (candles[i].high.toDouble() + candles[i].low.toDouble()) / 2.0
            basicUpperBand[i] = midPrice + multiplier * atr[i]
            basicLowerBand[i] = midPrice - multiplier * atr[i]
        }

        // --- 3. Final upper / lower bands + trend logic ---
        val finalUpperBand = MutableList(size) { 0.0 }
        val finalLowerBand = MutableList(size) { 0.0 }

        // start from index 0
        finalUpperBand[0] = basicUpperBand[0]
        finalLowerBand[0] = basicLowerBand[0]

        val supertrend = MutableList(size) { 0.0 }
        val trendUp = MutableList(size) { true }  // true = uptrend, false = downtrend

        supertrend[0] = finalLowerBand[0]
        trendUp[0] = true

        for (i in 1 until size) {
            // final upper band
            finalUpperBand[i] =
                if (basicUpperBand[i] < finalUpperBand[i - 1] || candles[i - 1].close > finalUpperBand[i - 1]) {
                    basicUpperBand[i]
                } else {
                    finalUpperBand[i - 1]
                }

            // final lower band
            finalLowerBand[i] =
                if (basicLowerBand[i] > finalLowerBand[i - 1] || candles[i - 1].close < finalLowerBand[i - 1]) {
                    basicLowerBand[i]
                } else {
                    finalLowerBand[i - 1]
                }

            // trend decision
            if (supertrend[i - 1] == finalUpperBand[i - 1]) {
                // previously downtrend
                if (candles[i].close > finalUpperBand[i]) {
                    // switch to uptrend
                    supertrend[i] = finalLowerBand[i]
                    trendUp[i] = true
                } else {
                    // stay in downtrend
                    supertrend[i] = finalUpperBand[i]
                    trendUp[i] = false
                }
            } else {
                // previously uptrend
                if (candles[i].close < finalLowerBand[i]) {
                    // switch to downtrend
                    supertrend[i] = finalUpperBand[i]
                    trendUp[i] = false
                } else {
                    // stay in uptrend
                    supertrend[i] = finalLowerBand[i]
                    trendUp[i] = true
                }
            }
        }

        // --- 4. Build separate up/down series data ---
        val upData = mutableListOf<LineData>()
        val downData = mutableListOf<LineData>()

        for (i in candles.indices) {
            val time = Time.Utc(candles[i].time)

            if (trendUp[i]) {
                // uptrend: value on upSeries, NaN (gap) on downSeries
                upData.add(LineData(time = time, value = supertrend[i].toFloat()))
                downData.add(LineData(time = time, value = Float.NaN))
            } else {
                // downtrend: value on downSeries, NaN (gap) on upSeries
                upData.add(LineData(time = time, value = Float.NaN))
                downData.add(LineData(time = time, value = supertrend[i].toFloat()))
            }
        }

        // --- 5. Push to chart ---
        upSeries.setData(upData)
        downSeries.setData(downData)
    }

    /**
     * Clears both up/down Supertrend series from the chart.
     */
    fun clear() {
        upSeries.setData(emptyList())
        downSeries.setData(emptyList())
    }
}
