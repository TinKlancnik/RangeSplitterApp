package com.example.rangesplitter.indicators

import android.util.Log
import com.example.rangesplitter.Candle
import com.tradingview.lightweightcharts.api.interfaces.SeriesApi
import com.tradingview.lightweightcharts.api.series.models.*
import com.tradingview.lightweightcharts.api.series.enums.*
import com.tradingview.lightweightcharts.api.chart.models.color.toIntColor

class Macro (
    private val candlestickSeries: SeriesApi,
    private val fastEmaSeries: SeriesApi,
    private val slowEmaSeries: SeriesApi,
    private val biasEmaSeries: SeriesApi
    ) {

    fun render(candles: List<Candle>, showBothEmas: Boolean = true) {
        if (candles.isEmpty()) return
        try {
            val times = candles.map { it.time }
            val closes = candles.map { it.close.toDouble() }

            val fast = ema(closes, 12)
            val slow = ema(closes, 25)
            val bias = ema(closes, 25)

            val isBuyRegime = fast.zip(slow).map { (f, s) -> f > s }
            val isSellRegime = fast.zip(slow).map { (f, s) -> f < s }

            fun line(points: List<Double>) =
                points.indices.map { i ->
                    LineData(Time.Utc(times[i]), points[i].toFloat())
                }

            fastEmaSeries.setData(if (showBothEmas) line(fast) else emptyList())
            slowEmaSeries.setData(if (showBothEmas) line(slow) else emptyList())
            biasEmaSeries.setData(if (!showBothEmas) line(bias) else emptyList())

            // --- Signals ---
            val markers = generateMarkers(times, fast, slow)
            candlestickSeries.setMarkers(markers)

        } catch (e: Exception) {
            Log.e("TraderXOMacro", "Error rendering indicator: ${e.message}", e)
        }
    }

    private fun ema(x: List<Double>, period: Int): List<Double> {
        if (x.isEmpty()) return emptyList()
        val k = 2.0 / (period + 1)
        val out = ArrayList<Double>(x.size)
        var ema = x.first()
        out.add(ema)
        for (i in 1 until x.size) {
            ema = x[i] * k + ema * (1 - k)
            out.add(ema)
        }
        return out
    }

    private fun generateMarkers(times: List<Long>, fast: List<Double>, slow: List<Double>): List<SeriesMarker> {
        val markers = mutableListOf<SeriesMarker>()
        for (i in 1 until fast.size) {
            val prevFast = fast[i - 1]
            val prevSlow = slow[i - 1]
            val currFast = fast[i]
            val currSlow = slow[i]

            // Bullish crossover
            if (prevFast < prevSlow && currFast > currSlow) {
                markers += SeriesMarker(
                    time = Time.Utc(times[i]),
                    position = SeriesMarkerPosition.BELOW_BAR,
                    shape = SeriesMarkerShape.ARROW_UP,
                    color = 0xFF43A047.toInt().toIntColor(),
                    text = "Bull"
                )
            }
            // Bearish crossover
            else if (prevFast > prevSlow && currFast < currSlow) {
                markers += SeriesMarker(
                    time = Time.Utc(times[i]),
                    position = SeriesMarkerPosition.ABOVE_BAR,
                    shape = SeriesMarkerShape.ARROW_DOWN,
                    color = 0xFF43A047.toInt().toIntColor(),
                    text = "Bear"
                )
            }
        }
        return markers
    }
}




