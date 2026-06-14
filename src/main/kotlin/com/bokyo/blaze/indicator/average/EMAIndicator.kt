package com.bokyo.blaze.indicator.average

import com.bokyo.blaze.BarSeries
import com.bokyo.blaze.indicator.BaseIndicator

/**
 * EMA with a pre-computed cache.
 *
 * The **last bar is never cached** — `get(size - 1)` is always recomputed from the
 * committed state at `size - 2`. This keeps `BarSeries.replaceLast()` correct (a forming
 * bar can be updated and re-read without stale values) and costs only one extra step.
 */
abstract class EMAIndicator(val period: Int, initialSize: Int) : BaseIndicator {
    protected abstract val data: FloatArray

    private var cache = FloatArray(initialSize)
    private var computedUntil = -1
    private val k = 2f / (period + 1)
    private val b = 1f - k

    private fun ensureCapacity(min: Int) {
        if (cache.size < min) cache = cache.copyOf(min)
    }

    /** Commits cache for indices up to (and including) [target]. Callers cap [target] at `size - 2`. */
    private fun commitUpTo(target: Int) {
        if (target < period || target <= computedUntil) return
        if (computedUntil < period) {
            cache[period] = data[period]
            computedUntil = period
        }
        for (i in computedUntil + 1..target) cache[i] = data[i] * k + cache[i - 1] * b
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
        if (index == period) return data[period]
        commitUpTo(index - 1)
        return data[index] * k + cache[index - 1] * b
    }

    internal class FixedEMA(
        override val data: FloatArray,
        period: Int,
    ) : EMAIndicator(period, data.size) {
        override val size get() = data.size

        init { computeAll() }
    }

    internal class DynamicEMA(
        private val series: BarSeries,
        period: Int,
    ) : EMAIndicator(period, series.size + BaseIndicator.GROW_SIZE) {
        override val data get() = series.closeData
        override val size get() = series.size

        init { computeAll() }
    }

    companion object {
        fun of(data: FloatArray, period: Int): EMAIndicator = FixedEMA(data, period)

        /** Backed by BarSeries. */
        fun of(series: BarSeries, period: Int): EMAIndicator = DynamicEMA(series, period)
    }
}
