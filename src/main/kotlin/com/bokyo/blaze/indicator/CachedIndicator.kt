package com.bokyo.blaze.indicator

/**
 * Memoizes [inner] up to the second-to-last bar. The **last bar is never cached** —
 * it is always delegated fresh to [inner], so `BarSeries.replaceLast()` stays correct
 * even through a cached indicator.
 */
class CachedIndicator internal constructor(
    private val inner: BaseIndicator,
) : BaseIndicator {
    override val size get() = inner.size

    private var cache = FloatArray(inner.size + BaseIndicator.GROW_SIZE)
    private var computedUntil = -1

    override fun get(index: Int): Float {
        if (index !in 0..<size) throw IndexOutOfBoundsException("Index $index out of bounds for length $size")
        // Last bar is tentative — never memoized.
        if (index >= size - 1) return inner[index]
        if (index > computedUntil) {
            if (cache.size <= index) cache = cache.copyOf(index + 1 + BaseIndicator.GROW_SIZE)
            for (i in computedUntil + 1..index) cache[i] = inner[i]
            computedUntil = index
        }
        return cache[index]
    }
}
