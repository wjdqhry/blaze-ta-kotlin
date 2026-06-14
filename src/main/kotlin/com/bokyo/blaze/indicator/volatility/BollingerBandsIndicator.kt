package com.bokyo.blaze.indicator.volatility

import com.bokyo.blaze.BarSeries
import com.bokyo.blaze.indicator.BaseIndicator
import kotlin.math.sqrt

/**
 * Bollinger Bands via sliding sum / sum-of-squares (double accumulators).
 *
 * The **last bar is never cached** — its outputs are recomputed with a fresh window scan
 * on every read, keeping `BarSeries.replaceLast()` correct. The sliding accumulators are
 * only advanced for committed (non-last) bars.
 */
abstract class BollingerBandsIndicator(
    val period: Int = 20,
    val multiplier: Float = 2.0f,
    initialSize: Int,
) {
    @JvmInline
    value class UpperIndicator internal constructor(val indicator: BollingerBandsIndicator) : BaseIndicator {
        override val size get() = indicator.size
        override fun get(index: Int): Float = indicator.value(index, UPPER)
    }

    @JvmInline
    value class MiddleIndicator internal constructor(val indicator: BollingerBandsIndicator) : BaseIndicator {
        override val size get() = indicator.size
        override fun get(index: Int): Float = indicator.value(index, MIDDLE)
    }

    @JvmInline
    value class LowerIndicator internal constructor(val indicator: BollingerBandsIndicator) : BaseIndicator {
        override val size get() = indicator.size
        override fun get(index: Int): Float = indicator.value(index, LOWER)
    }

    @JvmInline
    value class WidthIndicator internal constructor(val indicator: BollingerBandsIndicator) : BaseIndicator {
        override val size get() = indicator.size
        override fun get(index: Int): Float = indicator.value(index, WIDTH)
    }

    @JvmInline
    value class PercentBIndicator internal constructor(val indicator: BollingerBandsIndicator) : BaseIndicator {
        override val size get() = indicator.size
        override fun get(index: Int): Float = indicator.value(index, PERCENT_B)
    }

    internal abstract val size: Int
    internal abstract val data: FloatArray

    private var middleCache = FloatArray(initialSize)
    private var upperCache = FloatArray(initialSize)
    private var lowerCache = FloatArray(initialSize)
    private var widthCache = FloatArray(initialSize)
    private var percentBCache = FloatArray(initialSize)
    private var computedUntil = -1

    private var sumD: Double = 0.0
    private var sumSqD: Double = 0.0

    // Scratch for the tentative (last) bar — recomputed on every read, never persisted.
    private var tMiddle = 0f
    private var tUpper = 0f
    private var tLower = 0f
    private var tWidth = 0f
    private var tPercentB = 0f

    val upper: UpperIndicator = UpperIndicator(this)
    val middle: MiddleIndicator = MiddleIndicator(this)
    val lower: LowerIndicator = LowerIndicator(this)
    val width: WidthIndicator = WidthIndicator(this)
    val percentB: PercentBIndicator = PercentBIndicator(this)

    private fun ensureCapacity(min: Int) {
        if (middleCache.size < min) {
            middleCache = middleCache.copyOf(min)
            upperCache = upperCache.copyOf(min)
            lowerCache = lowerCache.copyOf(min)
            widthCache = widthCache.copyOf(min)
            percentBCache = percentBCache.copyOf(min)
        }
    }

    /** Pre-commit everything except the last (tentative) bar. */
    internal fun computeAll() = commitUpTo(size - 2)

    private fun value(index: Int, component: Int): Float {
        if (index !in 0..<size) throw IndexOutOfBoundsException("Index $index out of bounds for length $size")
        if (index < period - 1) return BaseIndicator.EMPTY
        ensureCapacity(index + 1 + BaseIndicator.GROW_SIZE)
        if (index < size - 1) {
            commitUpTo(index)
            return when (component) {
                MIDDLE -> middleCache[index]
                UPPER -> upperCache[index]
                LOWER -> lowerCache[index]
                WIDTH -> widthCache[index]
                else -> percentBCache[index]
            }
        }
        // Last bar: tentative, recomputed with a fresh window scan (never cached).
        computeTentative(index)
        return when (component) {
            MIDDLE -> tMiddle
            UPPER -> tUpper
            LOWER -> tLower
            WIDTH -> tWidth
            else -> tPercentB
        }
    }

    /** Commits caches + sliding accumulators up to (and including) [target]. Callers cap [target] at `size - 2`. */
    private fun commitUpTo(target: Int) {
        val firstValid = period - 1
        var i = maxOf(computedUntil + 1, firstValid)
        if (i > target) return

        if (i == firstValid) {
            baselineWindow(firstValid)
            writeOutputs(firstValid)
            i++
        }

        while (i <= target) {
            val xIn = data[i].toDouble()
            val xOut = data[i - period].toDouble()
            sumD += xIn - xOut
            sumSqD += xIn * xIn - xOut * xOut

            if (i % REBASELINE_EVERY == 0) baselineWindow(i)

            writeOutputs(i)
            i++
        }
        computedUntil = target
    }

    private fun baselineWindow(i: Int) {
        val start = i - period + 1
        var s = 0.0
        var sq = 0.0
        for (j in start..i) {
            val x = data[j].toDouble()
            s += x
            sq += x * x
        }
        sumD = s
        sumSqD = sq
    }

    private fun writeOutputs(i: Int) {
        val mean = sumD / period
        val variance = (sumSqD / period - mean * mean).coerceAtLeast(0.0)
        val meanF = mean.toFloat()
        val stddevF = sqrt(variance).toFloat()
        val upper = meanF + multiplier * stddevF
        val lower = meanF - multiplier * stddevF
        val band = upper - lower

        middleCache[i] = meanF
        upperCache[i] = upper
        lowerCache[i] = lower
        widthCache[i] = if (meanF != 0f) band / meanF else 0f
        percentBCache[i] = if (band != 0f) (data[i] - lower) / band else 0f
    }

    /** Fresh full-window computation for the tentative last bar, into the scratch fields. */
    private fun computeTentative(i: Int) {
        val start = i - period + 1
        var s = 0.0
        var sq = 0.0
        for (j in start..i) {
            val x = data[j].toDouble()
            s += x
            sq += x * x
        }
        val mean = s / period
        val variance = (sq / period - mean * mean).coerceAtLeast(0.0)
        val meanF = mean.toFloat()
        val stddevF = sqrt(variance).toFloat()
        val upper = meanF + multiplier * stddevF
        val lower = meanF - multiplier * stddevF
        val band = upper - lower

        tMiddle = meanF
        tUpper = upper
        tLower = lower
        tWidth = if (meanF != 0f) band / meanF else 0f
        tPercentB = if (band != 0f) (data[i] - lower) / band else 0f
    }

    internal class FixedBollingerBands(
        override val data: FloatArray,
        period: Int,
        multiplier: Float,
    ) : BollingerBandsIndicator(period, multiplier, data.size) {
        override val size get() = data.size

        init { computeAll() }
    }

    internal class DynamicBollingerBands(
        private val series: BarSeries,
        period: Int,
        multiplier: Float,
    ) : BollingerBandsIndicator(period, multiplier, series.size + BaseIndicator.GROW_SIZE) {
        override val data get() = series.closeData
        override val size get() = series.size

        init { computeAll() }
    }

    companion object {
        private const val REBASELINE_EVERY = 65_536

        private const val MIDDLE = 0
        private const val UPPER = 1
        private const val LOWER = 2
        private const val WIDTH = 3
        private const val PERCENT_B = 4

        fun of(
            data: FloatArray,
            period: Int = 20,
            multiplier: Float = 2.0f,
        ): BollingerBandsIndicator = FixedBollingerBands(data, period, multiplier)

        /** Backed by BarSeries. */
        fun of(
            series: BarSeries,
            period: Int = 20,
            multiplier: Float = 2.0f,
        ): BollingerBandsIndicator = DynamicBollingerBands(series, period, multiplier)
    }
}
