package com.bokyo.blaze

import com.bokyo.blaze.indicator.BaseIndicator

class BarSeries private constructor(
    internal var openData: FloatArray,
    internal var highData: FloatArray,
    internal var lowData: FloatArray,
    internal var closeData: FloatArray,
    internal var volumeData: FloatArray,
    size: Int,
) {
    @JvmInline
    value class OpenIndicator internal constructor(val series: BarSeries) : BaseIndicator {
        override val size get() = series.size
        override fun get(index: Int): Float = series.openData[index]
    }

    @JvmInline
    value class HighIndicator internal constructor(val series: BarSeries) : BaseIndicator {
        override val size get() = series.size
        override fun get(index: Int): Float = series.highData[index]
    }

    @JvmInline
    value class LowIndicator internal constructor(val series: BarSeries) : BaseIndicator {
        override val size get() = series.size
        override fun get(index: Int): Float = series.lowData[index]
    }

    @JvmInline
    value class CloseIndicator internal constructor(val series: BarSeries) : BaseIndicator {
        override val size get() = series.size
        override fun get(index: Int): Float = series.closeData[index]
    }

    @JvmInline
    value class VolumeIndicator internal constructor(val series: BarSeries) : BaseIndicator {
        override val size get() = series.size
        override fun get(index: Int): Float = series.volumeData[index]
    }

    var size = size
        private set

    val open: OpenIndicator = OpenIndicator(this)
    val high: HighIndicator = HighIndicator(this)
    val low: LowIndicator = LowIndicator(this)
    val close: CloseIndicator = CloseIndicator(this)
    val volume: VolumeIndicator = VolumeIndicator(this)

    fun replaceLast(open: Float, high: Float, low: Float, close: Float, volume: Float) {
        require(size > 0) { "No bar to replace" }
        val i = size - 1
        openData[i] = open
        highData[i] = high
        lowData[i] = low
        closeData[i] = close
        volumeData[i] = volume
    }

    fun add(open: Float, high: Float, low: Float, close: Float, volume: Float) {
        ensureCapacity()
        openData[size] = open
        highData[size] = high
        lowData[size] = low
        closeData[size] = close
        volumeData[size] = volume
        size++
    }

    private fun ensureCapacity() {
        if (size == closeData.size) {
            val newSize = closeData.size + BaseIndicator.GROW_SIZE
            openData = openData.copyOf(newSize)
            highData = highData.copyOf(newSize)
            lowData = lowData.copyOf(newSize)
            closeData = closeData.copyOf(newSize)
            volumeData = volumeData.copyOf(newSize)
        }
    }

    companion object {
        fun of(
            open: FloatArray,
            high: FloatArray,
            low: FloatArray,
            close: FloatArray,
            volume: FloatArray,
        ): BarSeries {
            val n = open.size
            require(n == high.size && n == low.size && n == close.size && n == volume.size) {
                "All OHLCV arrays must have the same size"
            }
            return BarSeries(open, high, low, close, volume, n)
        }

        fun of(
            open: Iterable<Float>,
            high: Iterable<Float>,
            low: Iterable<Float>,
            close: Iterable<Float>,
            volume: Iterable<Float>,
        ): BarSeries {
            val o = open.toFloatArray()
            val h = high.toFloatArray()
            val l = low.toFloatArray()
            val c = close.toFloatArray()
            val v = volume.toFloatArray()
            require(o.size == h.size && h.size == l.size && l.size == c.size && c.size == v.size) {
                "All OHLCV iterables must have the same size"
            }
            return BarSeries(o, h, l, c, v, c.size)
        }

        fun <T> of(
            bars: Iterable<T>,
            open: (T) -> Float,
            high: (T) -> Float,
            low: (T) -> Float,
            close: (T) -> Float,
            volume: (T) -> Float,
        ): BarSeries {
            val n = bars.count()
            val o = FloatArray(n)
            val h = FloatArray(n)
            val l = FloatArray(n)
            val c = FloatArray(n)
            val v = FloatArray(n)
            bars.forEachIndexed { i, b ->
                o[i] = open(b)
                h[i] = high(b)
                l[i] = low(b)
                c[i] = close(b)
                v[i] = volume(b)
            }
            return BarSeries(o, h, l, c, v, n)
        }

        private fun Iterable<Float>.toFloatArray(): FloatArray {
            val arr = FloatArray(count())
            var i = 0
            for (x in this) arr[i++] = x
            return arr
        }
    }
}
