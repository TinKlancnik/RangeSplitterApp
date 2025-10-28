package com.example.rangesplitter.indicators

import android.util.Log
import com.example.rangesplitter.Candle
import com.tradingview.lightweightcharts.api.interfaces.SeriesApi
import com.tradingview.lightweightcharts.api.series.enums.SeriesMarkerPosition
import com.tradingview.lightweightcharts.api.series.enums.SeriesMarkerShape
import com.tradingview.lightweightcharts.api.series.models.LineData
import com.tradingview.lightweightcharts.api.series.models.SeriesMarker
import com.tradingview.lightweightcharts.api.series.models.Time
import com.tradingview.lightweightcharts.api.chart.models.color.toIntColor
import kotlin.math.max
import kotlin.math.min

class Macro(
    private val candlestickSeries: SeriesApi,
    private val fastEmaSeries: SeriesApi,
    private val slowEmaSeries: SeriesApi,
    private val biasEmaSeries: SeriesApi,

    // ---- Stoch RSI filter (logic-only; nothing is drawn) ----
    private val useStochRsiFilter: Boolean = true,
    private val rsiLen: Int = 14,
    private val stochLen: Int = 14,
    private val kSmoothing: Int = 3,
    private val dSmoothing: Int = 3,
    private val midBand: Double = 50.0
) {

    fun render(candles: List<Candle>, showBothEmas: Boolean = true) {
        if (candles.isEmpty()) return
        try {
            val times = candles.map { it.time }
            val closes = candles.map { it.close.toDouble() }

            // --- EMAs ---
            val fast = ema(closes, 12)
            val slow = ema(closes, 25)
            val bias = ema(closes, 25)

            fun line(points: List<Double>) =
                points.indices.map { i -> LineData(Time.Utc(times[i]), points[i].toFloat()) }

            // --- Determine color regime ---
            val emaColor = fast.zip(slow).map { (f, s) ->
                when {
                    f > s -> 0xFF43A047.toInt().toIntColor()   // Green
                    f < s -> 0xFFD32F2F.toInt().toIntColor()   // Red
                    else -> 0xFFFF530D.toInt().toIntColor()    // Neutral orange
                }
            }

            // --- Helper to create colored line segments ---
            fun coloredLine(points: List<Double>): List<LineData> {
                val data = mutableListOf<LineData>()
                for (i in points.indices) {
                    data += LineData(
                        time = Time.Utc(times[i]),
                        value = points[i].toFloat(),
                        color = emaColor[i] // 🔹 color each segment
                    )
                }
                return data
            }

            // --- Apply colored data to series ---
            fastEmaSeries.setData(if (showBothEmas) coloredLine(fast) else emptyList())
            slowEmaSeries.setData(if (showBothEmas) coloredLine(slow) else emptyList())
            biasEmaSeries.setData(if (!showBothEmas) coloredLine(bias) else emptyList())


            // --- Stochastic RSI (logic only) ---
            val (k, d) = if (useStochRsiFilter) stochRsi(closes) else Pair(emptyList(), emptyList())

            // --- Signals (EMA cross, gated by Stoch RSI if enabled) ---
            val markers = generateMarkers(times, fast, slow, k, d)
            candlestickSeries.setMarkers(markers)

        } catch (e: Exception) {
            Log.e("TraderXOMacro", "Error rendering indicator: ${e.message}", e)
        }
    }

    // ================== helpers ==================

    private fun ema(x: List<Double>, period: Int): List<Double> {
        if (x.isEmpty()) return emptyList()
        val k = 2.0 / (period + 1)
        val out = ArrayList<Double>(x.size)
        var e = x.first()
        out.add(e)
        for (i in 1 until x.size) {
            e = x[i] * k + e * (1 - k)
            out.add(e)
        }
        return out
    }

    private fun sma(x: List<Double>, period: Int): List<Double> {
        if (period <= 0 || x.isEmpty()) return emptyList()
        val out = MutableList(x.size) { Double.NaN }
        var sum = 0.0
        for (i in x.indices) {
            sum += x[i]
            if (i >= period) sum -= x[i - period]
            if (i >= period - 1) out[i] = sum / period
        }
        return out
    }

    private fun rsi(closes: List<Double>, period: Int): List<Double> {
        if (closes.size < period + 1) return List(closes.size) { Double.NaN }
        val out = MutableList(closes.size) { Double.NaN }
        var gain = 0.0
        var loss = 0.0
        // seed
        for (i in 1..period) {
            val ch = closes[i] - closes[i - 1]
            if (ch >= 0) gain += ch else loss -= ch
        }
        var avgGain = gain / period
        var avgLoss = loss / period
        out[period] = rsToRsi(avgGain, avgLoss)

        for (i in period + 1 until closes.size) {
            val ch = closes[i] - closes[i - 1]
            val g = if (ch > 0) ch else 0.0
            val l = if (ch < 0) -ch else 0.0
            avgGain = (avgGain * (period - 1) + g) / period
            avgLoss = (avgLoss * (period - 1) + l) / period
            out[i] = rsToRsi(avgGain, avgLoss)
        }
        return out
    }

    private fun rsToRsi(avgGain: Double, avgLoss: Double): Double {
        return if (avgLoss == 0.0) 100.0 else {
            val rs = avgGain / avgLoss
            100.0 - (100.0 / (1.0 + rs))
        }
    }

    /**
     * Stochastic RSI:
     *  rsi1 = RSI(close, rsiLen)
     *  stoch = 100 * (rsi1 - min(rsi1, stochLen)) / (max(rsi1, stochLen) - min(...))
     *  K = SMA(stoch, kSmoothing)
     *  D = SMA(K, dSmoothing)
     */
    private fun stochRsi(closes: List<Double>): Pair<List<Double>, List<Double>> {
        val rsi1 = rsi(closes, rsiLen)
        if (rsi1.isEmpty()) return Pair(emptyList(), emptyList())
        val stoch = MutableList(rsi1.size) { Double.NaN }

        var minDeque = ArrayDeque<Int>()
        var maxDeque = ArrayDeque<Int>()

        fun pushMin(i: Int) {
            while (minDeque.isNotEmpty() && rsi1[minDeque.last()] >= rsi1[i]) minDeque.removeLast()
            minDeque.addLast(i)
        }
        fun pushMax(i: Int) {
            while (maxDeque.isNotEmpty() && rsi1[maxDeque.last()] <= rsi1[i]) maxDeque.removeLast()
            maxDeque.addLast(i)
        }
        fun popOutOfWindow(i: Int) {
            val start = i - stochLen + 1
            if (minDeque.isNotEmpty() && minDeque.first() < start) minDeque.removeFirst()
            if (maxDeque.isNotEmpty() && maxDeque.first() < start) maxDeque.removeFirst()
        }

        for (i in rsi1.indices) {
            if (rsi1[i].isNaN()) continue
            pushMin(i); pushMax(i)
            if (i >= stochLen - 1) {
                popOutOfWindow(i)
                val minVal = rsi1[minDeque.first()]
                val maxVal = rsi1[maxDeque.first()]
                val denom = max(1e-9, maxVal - minVal)
                stoch[i] = 100.0 * (rsi1[i] - minVal) / denom
            }
        }

        val kRaw = stoch
        val k = sma(kRaw, kSmoothing)
        val d = sma(k, dSmoothing)
        return Pair(k, d)
    }

    private fun crossover(aPrev: Double, aNow: Double, bPrev: Double, bNow: Double): Boolean {
        return !aPrev.isNaN() && !aNow.isNaN() && !bPrev.isNaN() && !bNow.isNaN() &&
                aPrev < bPrev && aNow > bNow
    }

    private fun crossunder(aPrev: Double, aNow: Double, bPrev: Double, bNow: Double): Boolean {
        return !aPrev.isNaN() && !aNow.isNaN() && !bPrev.isNaN() && !bNow.isNaN() &&
                aPrev > bPrev && aNow < bNow
    }

    private fun generateMarkers(
        times: List<Long>,
        fast: List<Double>,
        slow: List<Double>,
        k: List<Double>,
        d: List<Double>
    ): List<SeriesMarker> {
        val markers = mutableListOf<SeriesMarker>()
        val useFilter = useStochRsiFilter && k.isNotEmpty() && d.isNotEmpty()

        for (i in 1 until minOf(fast.size, slow.size, times.size)) {
            val prevFast = fast[i - 1]
            val prevSlow = slow[i - 1]
            val currFast = fast[i]
            val currSlow = slow[i]

            val emaBullCross = prevFast < prevSlow && currFast > currSlow
            val emaBearCross = prevFast > prevSlow && currFast < currSlow

            var allowBull = true
            var allowBear = true

            if (useFilter) {
                val kPrev = if (i - 1 < k.size) k[i - 1] else Double.NaN
                val dPrev = if (i - 1 < d.size) d[i - 1] else Double.NaN
                val kNow = if (i < k.size) k[i] else Double.NaN
                val dNow = if (i < d.size) d[i] else Double.NaN

                val stochBull = crossover(kPrev, kNow, dPrev, dNow) && (kNow < midBand || dNow < midBand)
                val stochBear = crossunder(kPrev, kNow, dPrev, dNow) && (kNow > midBand || dNow > midBand)

                allowBull = stochBull
                allowBear = stochBear
            }

            // Bullish crossover + (optional) Stoch RSI confirmation
            if (emaBullCross && allowBull) {
                markers += SeriesMarker(
                    time = Time.Utc(times[i]),
                    position = SeriesMarkerPosition.BELOW_BAR,
                    shape = SeriesMarkerShape.ARROW_UP,
                    color = 0xFF43A047.toInt().toIntColor(),
                    text = "Buy"
                )
            }
            // Bearish crossover + (optional) Stoch RSI confirmation
            if (emaBearCross && allowBear) {
                markers += SeriesMarker(
                    time = Time.Utc(times[i]),
                    position = SeriesMarkerPosition.ABOVE_BAR,
                    shape = SeriesMarkerShape.ARROW_DOWN,
                    color = 0xFFD32F2F.toInt().toIntColor(),
                    text = "Sell"
                )
            }
        }
        return markers
    }
}
