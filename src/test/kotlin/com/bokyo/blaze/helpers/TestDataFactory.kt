package com.bokyo.blaze.helpers

import org.ta4j.core.BaseBar
import org.ta4j.core.BaseBarSeriesBuilder
import org.ta4j.core.BarSeries
import org.ta4j.core.num.DecimalNum
import java.time.Duration
import java.time.Instant
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

data class OHLCVArrays(
    val open: FloatArray,
    val high: FloatArray,
    val low: FloatArray,
    val close: FloatArray,
    val volume: FloatArray,
)

object TestDataFactory {

    private const val DEFAULT_SEED = 42L

    /**
     * Generates a realistic close price array using random walk.
     * Starts at 100.0 and applies random percentage changes.
     */
    fun realisticCloseArray(size: Int, seed: Long = DEFAULT_SEED): FloatArray {
        val random = Random(seed)
        val prices = FloatArray(size)
        prices[0] = 100f
        for (i in 1 until size) {
            val change = (random.nextFloat() - 0.5f) * 0.04f // +-2% max change
            prices[i] = prices[i - 1] * (1f + change)
        }
        return prices
    }

    /**
     * Generates realistic OHLCV data from a close price random walk.
     * High/Low are derived from close with random spread.
     * Open is previous close with small gap.
     * Volume is randomized.
     */
    fun realisticOHLCVArrays(size: Int, seed: Long = DEFAULT_SEED): OHLCVArrays {
        val random = Random(seed)
        val close = realisticCloseArray(size, seed)
        val open = FloatArray(size)
        val high = FloatArray(size)
        val low = FloatArray(size)
        val volume = FloatArray(size)

        open[0] = close[0]
        for (i in 1 until size) {
            open[i] = close[i - 1] * (1f + (random.nextFloat() - 0.5f) * 0.005f)
        }

        for (i in 0 until size) {
            val spread = abs(close[i] * (random.nextFloat() * 0.02f))
            high[i] = max(open[i], close[i]) + spread
            low[i] = min(open[i], close[i]) - spread
            volume[i] = (random.nextFloat() * 1_000_000f + 100_000f)
        }

        return OHLCVArrays(open, high, low, close, volume)
    }

    /**
     * Builds a ta4j BarSeries from a close price array.
     * Other OHLC values are set to the close price, volume to 0.
     */
    fun buildTa4jSeries(closePrices: FloatArray): BarSeries {
        return BaseBarSeriesBuilder()
            .withBars(
                closePrices.mapIndexed { i, price ->
                    BaseBar(
                        Duration.ofDays(1),
                        Instant.EPOCH.plusSeconds(i.toLong() * 86400),
                        Instant.EPOCH.plusSeconds((i.toLong() + 1) * 86400),
                        DecimalNum.valueOf(price.toDouble()),
                        DecimalNum.valueOf(price.toDouble()),
                        DecimalNum.valueOf(price.toDouble()),
                        DecimalNum.valueOf(price.toDouble()),
                        DecimalNum.valueOf(0),
                        DecimalNum.valueOf(0),
                        0L,
                    )
                }
            )
            .build()
    }

    /**
     * Builds a ta4j BarSeries from OHLCV arrays.
     */
    fun buildTa4jOHLCVSeries(ohlcv: OHLCVArrays): BarSeries {
        val size = ohlcv.close.size
        return BaseBarSeriesBuilder()
            .withBars(
                (0 until size).map { i ->
                    BaseBar(
                        Duration.ofDays(1),
                        Instant.EPOCH.plusSeconds(i.toLong() * 86400),
                        Instant.EPOCH.plusSeconds((i.toLong() + 1) * 86400),
                        DecimalNum.valueOf(ohlcv.open[i].toDouble()),
                        DecimalNum.valueOf(ohlcv.high[i].toDouble()),
                        DecimalNum.valueOf(ohlcv.low[i].toDouble()),
                        DecimalNum.valueOf(ohlcv.close[i].toDouble()),
                        DecimalNum.valueOf(ohlcv.volume[i].toDouble()),
                        DecimalNum.valueOf(0),
                        0L,
                    )
                }
            )
            .build()
    }
}
