package com.bokyo.blaze.indicator.momentum

import com.bokyo.blaze.BarSeries
import com.bokyo.blaze.indicator.BaseIndicator

/**
 * Wilder's RSI with a pre-computed cache.
 *
 * The **last bar is never cached** — `get(size - 1)` is recomputed from the committed
 * gain/loss averages at `size - 2`, so `BarSeries.replaceLast()` stays correct.
 */
abstract class RSIIndicator(val period: Int, initialSize: Int) : BaseIndicator {
    protected abstract val data: FloatArray

    private var cache = FloatArray(initialSize)
    private var computedUntil = -1
    private var avgGain = 0f
    private var avgLoss = 0f
    private val k = 1f / period
    private val b = 1f - k

    private fun ensureCapacity(min: Int) {
        if (cache.size < min) cache = cache.copyOf(min)
    }

    /** Commits cache + smoothing state up to (and including) [target]. Callers cap [target] at `size - 2`. */
    private fun commitUpTo(target: Int) {
        if (target < period || target <= computedUntil) return
        if (computedUntil < period) {
            seedAtPeriod()
            computedUntil = period
        }
        for (i in computedUntil + 1..target) step(i)
        computedUntil = target
    }

    /** Pre-commit everything except the last (tentative) bar. */
    protected fun computeAll() = commitUpTo(size - 2)

    override fun get(index: Int): Float {
        if (index !in 0..<size) throw IndexOutOfBoundsException("Index $index out of bounds for length $size")
        if (index < period) return Float.NaN
        ensureCapacity(index + 1 + BaseIndicator.GROW_SIZE)
        if (index < size - 1) {
            commitUpTo(index)
            return cache[index]
        }
        // Last bar: tentative, always recomputed (never cached).
        if (index == period) {
            val change = data[period] - data[period - 1]
            val g = if (change > 0f) change else 0f
            val l = if (change < 0f) -change else 0f
            return rsi(g, l)
        }
        commitUpTo(index - 1)
        val change = data[index] - data[index - 1]
        val g = if (change > 0f) change else 0f
        val l = if (change < 0f) -change else 0f
        return rsi(g * k + avgGain * b, l * k + avgLoss * b)
    }

    private fun seedAtPeriod() {
        val change = data[period] - data[period - 1]
        avgGain = if (change > 0f) change else 0f
        avgLoss = if (change < 0f) -change else 0f
        cache[period] = rsi(avgGain, avgLoss)
    }

    private fun step(i: Int) {
        val change = data[i] - data[i - 1]
        val g = if (change > 0f) change else 0f
        val l = if (change < 0f) -change else 0f
        avgGain = g * k + avgGain * b
        avgLoss = l * k + avgLoss * b
        cache[i] = rsi(avgGain, avgLoss)
    }

    private fun rsi(avgGain: Float, avgLoss: Float): Float =
        if (avgLoss == 0f) 100f else 100f - 100f / (1f + avgGain / avgLoss)

    internal class FixedRSI(
        override val data: FloatArray,
        period: Int,
    ) : RSIIndicator(period, data.size) {
        override val size = data.size

        init { computeAll() }
    }

    internal class DynamicRSI(
        private val series: BarSeries,
        period: Int,
    ) : RSIIndicator(period, series.size + BaseIndicator.GROW_SIZE) {
        override val data get() = series.closeData
        override val size get() = series.size

        init { computeAll() }
    }

    companion object {
        fun of(data: FloatArray, period: Int): RSIIndicator = FixedRSI(data, period)

        /** Backed by BarSeries. */
        fun of(series: BarSeries, period: Int): RSIIndicator = DynamicRSI(series, period)
    }
}
