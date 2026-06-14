package com.bokyo.blaze.indicator

interface BaseIndicator {

    val size: Int

    /**
     * Returns the indicator value at [index].
     *
     * - Returns [Float.NaN] when there is not enough prior data to compute a value
     *   (e.g. index < period for EMA, index < period - 1 for SMA).
     * - Throws [IndexOutOfBoundsException] only when [index] is outside `0..<size`.
     */
    operator fun get(index: Int): Float

    companion object {
        const val EMPTY = Float.NaN
        internal const val GROW_SIZE = 100
        internal fun FloatArray.grow(): FloatArray = this.copyOf(this.size + GROW_SIZE)
    }
}
