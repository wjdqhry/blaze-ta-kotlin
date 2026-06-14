package com.bokyo.blaze.indicator.average

import com.bokyo.blaze.BarSeries
import com.bokyo.blaze.indicator.BaseIndicator
import com.bokyo.blaze.indicator.CachedIndicator
import com.bokyo.blaze.indicator.simdSum

abstract class SMAIndicator(val period: Int) : BaseIndicator {
    protected abstract val data: FloatArray

    override fun get(index: Int): Float {
        if (index !in 0..<size) throw IndexOutOfBoundsException("Index $index out of bounds for length $size")
        if (index < period - 1) return Float.NaN
        return data.simdSum(index - period + 1, index) / period.toFloat()
    }

    internal class FixedSMA(
        override val data: FloatArray,
        period: Int,
    ) : SMAIndicator(period) {
        override val size get() = data.size
    }

    internal class DynamicSMA(
        private val series: BarSeries,
        period: Int,
    ) : SMAIndicator(period) {
        override val data get() = series.closeData
        override val size get() = series.size
    }

    companion object {
        fun of(data: FloatArray, period: Int, cached: Boolean = true): BaseIndicator =
            FixedSMA(data, period).let { if (cached) CachedIndicator(it) else it }

        /** Backed by BarSeries. */
        fun of(series: BarSeries, period: Int, cached: Boolean = true): BaseIndicator =
            DynamicSMA(series, period).let { if (cached) CachedIndicator(it) else it }
    }
}
