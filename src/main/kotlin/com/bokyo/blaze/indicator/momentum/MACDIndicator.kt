package com.bokyo.blaze.indicator.momentum

import com.bokyo.blaze.BarSeries
import com.bokyo.blaze.indicator.BaseIndicator

/**
 * MACD with pre-computed line / signal / histogram / emaShort / emaLong caches.
 *
 * The **last bar is never cached** — its five outputs are recomputed from the committed
 * state at `size - 2` on every read, keeping `BarSeries.replaceLast()` correct.
 */
abstract class MACDIndicator(
    val shortPeriod: Int,
    val longPeriod: Int,
    signalPeriod: Int,
    initialSize: Int,
) {
    @JvmInline
    value class LineIndicator internal constructor(val indicator: MACDIndicator) : BaseIndicator {
        override val size get() = indicator.size
        override fun get(index: Int): Float = indicator.value(index, LINE)
    }

    @JvmInline
    value class SignalIndicator internal constructor(val indicator: MACDIndicator) : BaseIndicator {
        override val size get() = indicator.size
        override fun get(index: Int): Float = indicator.value(index, SIGNAL)
    }

    @JvmInline
    value class HistogramIndicator internal constructor(val indicator: MACDIndicator) : BaseIndicator {
        override val size get() = indicator.size
        override fun get(index: Int): Float = indicator.value(index, HISTOGRAM)
    }

    @JvmInline
    value class EmaShortIndicator internal constructor(val indicator: MACDIndicator) : BaseIndicator {
        override val size get() = indicator.size
        override fun get(index: Int): Float = indicator.value(index, EMA_SHORT)
    }

    @JvmInline
    value class EmaLongIndicator internal constructor(val indicator: MACDIndicator) : BaseIndicator {
        override val size get() = indicator.size
        override fun get(index: Int): Float = indicator.value(index, EMA_LONG)
    }

    internal abstract val size: Int
    internal abstract val data: FloatArray

    private var lineCache = FloatArray(initialSize)
    private var signalCache = FloatArray(initialSize)
    private var histogramCache = FloatArray(initialSize)
    private var emaShortCache = FloatArray(initialSize)
    private var emaLongCache = FloatArray(initialSize)
    private var computedUntil = -1

    private val shortK = 2f / (shortPeriod + 1)
    private val longK = 2f / (longPeriod + 1)
    private val signalK = 2f / (signalPeriod + 1)
    private val shortB = 1f - shortK
    private val longB = 1f - longK
    private val signalB = 1f - signalK

    val line: LineIndicator = LineIndicator(this)
    val signal: SignalIndicator = SignalIndicator(this)
    val histogram: HistogramIndicator = HistogramIndicator(this)
    val emaShort: EmaShortIndicator = EmaShortIndicator(this)
    val emaLong: EmaLongIndicator = EmaLongIndicator(this)

    private fun ensureCapacity(min: Int) {
        if (lineCache.size < min) {
            lineCache = lineCache.copyOf(min)
            signalCache = signalCache.copyOf(min)
            histogramCache = histogramCache.copyOf(min)
            emaShortCache = emaShortCache.copyOf(min)
            emaLongCache = emaLongCache.copyOf(min)
        }
    }

    /** Commits all caches up to (and including) [target]. Callers cap [target] at `size - 2`. */
    private fun commitUpTo(target: Int) {
        if (target < longPeriod || target <= computedUntil) return
        val src = data
        if (computedUntil < longPeriod) {
            // emaShort warm-up over shortPeriod..longPeriod, then seed the long EMA at longPeriod.
            var emaShortVal = src[shortPeriod]
            emaShortCache[shortPeriod] = emaShortVal
            for (i in shortPeriod + 1 until longPeriod) {
                emaShortVal = src[i] * shortK + emaShortVal * shortB
                emaShortCache[i] = emaShortVal
            }
            emaShortVal = src[longPeriod] * shortK + emaShortVal * shortB
            val emaLongVal = src[longPeriod]
            emaShortCache[longPeriod] = emaShortVal
            emaLongCache[longPeriod] = emaLongVal
            lineCache[longPeriod] = emaShortVal - emaLongVal
            signalCache[longPeriod] = lineCache[longPeriod]
            histogramCache[longPeriod] = 0f
            computedUntil = longPeriod
        }
        for (i in computedUntil + 1..target) {
            val s = src[i] * shortK + emaShortCache[i - 1] * shortB
            val l = src[i] * longK + emaLongCache[i - 1] * longB
            emaShortCache[i] = s
            emaLongCache[i] = l
            val line = s - l
            lineCache[i] = line
            signalCache[i] = line * signalK + signalCache[i - 1] * signalB
            histogramCache[i] = line - signalCache[i]
        }
        computedUntil = target
    }

    /** Pre-commit everything except the last (tentative) bar. */
    protected fun computeAll() = commitUpTo(size - 2)

    /** emaShort at the seed index [longPeriod] (requires the warm-up walk). */
    private fun seedEmaShort(): Float {
        var e = data[shortPeriod]
        for (i in shortPeriod + 1..longPeriod) e = data[i] * shortK + e * shortB
        return e
    }

    private fun value(index: Int, component: Int): Float {
        if (index !in 0..<size) throw IndexOutOfBoundsException("Index $index out of bounds for length $size")
        if (index < longPeriod) return BaseIndicator.EMPTY
        ensureCapacity(index + 1 + BaseIndicator.GROW_SIZE)
        if (index < size - 1) {
            commitUpTo(index)
            return cached(index, component)
        }
        // Last bar: tentative, recomputed from committed state (never cached).
        return if (index == longPeriod) seedComponent(component) else tentativeComponent(index, component)
    }

    private fun cached(i: Int, component: Int): Float = when (component) {
        LINE -> lineCache[i]
        SIGNAL -> signalCache[i]
        HISTOGRAM -> histogramCache[i]
        EMA_SHORT -> emaShortCache[i]
        else -> emaLongCache[i]
    }

    /** Tentative outputs at the seed index [longPeriod]: signal == line, histogram == 0. */
    private fun seedComponent(component: Int): Float {
        val s = seedEmaShort()
        val l = data[longPeriod]
        return when (component) {
            LINE -> s - l
            SIGNAL -> s - l
            HISTOGRAM -> 0f
            EMA_SHORT -> s
            else -> l
        }
    }

    /** Tentative outputs for `index > longPeriod` from committed state at `index - 1`. */
    private fun tentativeComponent(index: Int, component: Int): Float {
        commitUpTo(index - 1)
        val s = data[index] * shortK + emaShortCache[index - 1] * shortB
        val l = data[index] * longK + emaLongCache[index - 1] * longB
        val line = s - l
        val sig = line * signalK + signalCache[index - 1] * signalB
        return when (component) {
            LINE -> line
            SIGNAL -> sig
            HISTOGRAM -> line - sig
            EMA_SHORT -> s
            else -> l
        }
    }

    internal class FixedMACD(
        override val data: FloatArray,
        shortPeriod: Int,
        longPeriod: Int,
        signalPeriod: Int,
    ) : MACDIndicator(shortPeriod, longPeriod, signalPeriod, data.size) {
        override val size get() = data.size

        init { computeAll() }
    }

    internal class DynamicMACD(
        private val series: BarSeries,
        shortPeriod: Int,
        longPeriod: Int,
        signalPeriod: Int,
    ) : MACDIndicator(shortPeriod, longPeriod, signalPeriod, series.size + BaseIndicator.GROW_SIZE) {
        override val data get() = series.closeData
        override val size get() = series.size

        init { computeAll() }
    }

    companion object {
        private const val LINE = 0
        private const val SIGNAL = 1
        private const val HISTOGRAM = 2
        private const val EMA_SHORT = 3
        private const val EMA_LONG = 4

        fun of(
            data: FloatArray,
            shortPeriod: Int = 12,
            longPeriod: Int = 26,
            signalPeriod: Int = 9,
        ): MACDIndicator = FixedMACD(data, shortPeriod, longPeriod, signalPeriod)

        /** Backed by BarSeries. */
        fun of(
            series: BarSeries,
            shortPeriod: Int = 12,
            longPeriod: Int = 26,
            signalPeriod: Int = 9,
        ): MACDIndicator = DynamicMACD(series, shortPeriod, longPeriod, signalPeriod)
    }
}
