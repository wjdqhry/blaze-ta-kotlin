package com.bokyo.blaze.indicator

import jdk.incubator.vector.FloatVector
import jdk.incubator.vector.VectorOperators
import jdk.incubator.vector.VectorSpecies

val SPECIES: VectorSpecies<Float> = FloatVector.SPECIES_PREFERRED

internal fun FloatArray.simdSum(start: Int, end: Int): Float {
    var i = start
    val laneSize = SPECIES.length()
    var acc = FloatVector.zero(SPECIES)

    while (i + laneSize <= end + 1) {
        acc = acc.add(FloatVector.fromArray(SPECIES, this, i))
        i += laneSize
    }

    if (i <= end) {
        val mask = SPECIES.indexInRange(0, end - i + 1)
        acc = acc.add(FloatVector.fromArray(SPECIES, this, i, mask))
    }

    return acc.reduceLanes(VectorOperators.ADD)
}

internal fun FloatArray.simdMin(start: Int, end: Int): Float {
    var i = start
    val laneSize = SPECIES.length()
    var acc = FloatVector.broadcast(SPECIES, Float.MAX_VALUE)

    while (i + laneSize <= end + 1) {
        acc = acc.min(FloatVector.fromArray(SPECIES, this, i))
        i += laneSize
    }

    if (i <= end) {
        val mask = SPECIES.indexInRange(0, end - i + 1)
        val maxVec = FloatVector.broadcast(SPECIES, Float.MAX_VALUE)
        val tail = FloatVector.fromArray(SPECIES, this, i, mask).blend(maxVec, mask.not())
        acc = acc.min(tail)
    }
    return acc.reduceLanes(VectorOperators.MIN)
}

internal fun FloatArray.simdMax(start: Int, end: Int): Float {
    var i = start
    val laneSize = SPECIES.length()
    var acc = FloatVector.broadcast(SPECIES, -Float.MAX_VALUE)

    while (i + laneSize <= end + 1) {
        acc = acc.max(FloatVector.fromArray(SPECIES, this, i))
        i += laneSize
    }

    if (i <= end) {
        val mask = SPECIES.indexInRange(0, end - i + 1)
        val minVec = FloatVector.broadcast(SPECIES, -Float.MAX_VALUE)
        val tail = FloatVector.fromArray(SPECIES, this, i, mask).blend(minVec, mask.not())
        acc = acc.max(tail)
    }
    return acc.reduceLanes(VectorOperators.MAX)
}

internal fun FloatArray.simdSumAbs(start: Int, end: Int): Float {
    var i = start
    val laneSize = SPECIES.length()
    var acc = FloatVector.zero(SPECIES)

    while (i + laneSize <= end + 1) {
        acc = acc.add(FloatVector.fromArray(SPECIES, this, i).abs())
        i += laneSize
    }

    if (i <= end) {
        val mask = SPECIES.indexInRange(0, end - i + 1)
        acc = acc.add(FloatVector.fromArray(SPECIES, this, i, mask).abs())
    }

    return acc.reduceLanes(VectorOperators.ADD)
}

internal fun FloatArray.simdSumPositive(start: Int, end: Int): Float {
    var i = start
    val laneSize = SPECIES.length()
    val zero = FloatVector.zero(SPECIES)
    var acc = zero

    while (i + laneSize <= end + 1) {
        val v = FloatVector.fromArray(SPECIES, this, i).max(zero)
        acc = acc.add(v)
        i += laneSize
    }

    if (i <= end) {
        val mask = SPECIES.indexInRange(0, end - i + 1)
        val v = FloatVector.fromArray(SPECIES, this, i, mask).max(zero)
        acc = acc.add(v)
    }

    return acc.reduceLanes(VectorOperators.ADD)
}

internal fun FloatArray.simdSumNegative(start: Int, end: Int): Float {
    var i = start
    val laneSize = SPECIES.length()
    val zero = FloatVector.zero(SPECIES)
    var acc = zero

    while (i + laneSize <= end + 1) {
        val v = FloatVector.fromArray(SPECIES, this, i).min(zero)
        acc = acc.add(v)
        i += laneSize
    }

    if (i <= end) {
        val mask = SPECIES.indexInRange(0, end - i + 1)
        val v = FloatVector.fromArray(SPECIES, this, i, mask).min(zero)
        acc = acc.add(v)
    }

    return acc.reduceLanes(VectorOperators.ADD)
}

internal fun FloatArray.simdSumOfSquare(start: Int, end: Int, mean: Float): Float {
    var i = start
    val laneSize = SPECIES.length()
    var acc = FloatVector.zero(SPECIES)
    val meanVec = FloatVector.broadcast(SPECIES, mean)

    while (i + laneSize <= end + 1) {
        val v = FloatVector.fromArray(SPECIES, this, i).sub(meanVec)
        acc = acc.add(v.mul(v))
        i += laneSize
    }

    if (i <= end) {
        val mask = SPECIES.indexInRange(0, end - i + 1)
        val v = FloatVector.fromArray(SPECIES, this, i, mask).sub(meanVec)
        acc = acc.add(v.mul(v))
    }

    return acc.reduceLanes(VectorOperators.ADD)
}

internal fun FloatArray.simdWeightedSum(
    start: Int, end: Int,
    weights: FloatArray
): Float {
    var i = start
    var wi = 0
    val laneSize = SPECIES.length()
    var acc = FloatVector.zero(SPECIES)

    while (i + laneSize <= end + 1) {
        val v = FloatVector.fromArray(SPECIES, this, i)
        val w = FloatVector.fromArray(SPECIES, weights, wi)
        acc = acc.add(v.mul(w))
        i += laneSize
        wi += laneSize
    }

    if (i <= end) {
        val mask = SPECIES.indexInRange(0, end - i + 1)
        val v = FloatVector.fromArray(SPECIES, this, i, mask)
        val w = FloatVector.fromArray(SPECIES, weights, wi, mask)
        acc = acc.add(v.mul(w))
    }

    return acc.reduceLanes(VectorOperators.ADD)
}

internal fun simdGainLoss(src: FloatArray, size: Int, gains: FloatArray, losses: FloatArray) {
    val laneSize = SPECIES.length()
    val zero = FloatVector.zero(SPECIES)
    var i = 1

    while (i + laneSize <= size) {
        val current = FloatVector.fromArray(SPECIES, src, i)
        val prev = FloatVector.fromArray(SPECIES, src, i - 1)
        val change = current.sub(prev)
        change.max(zero).intoArray(gains, i)
        change.neg().max(zero).intoArray(losses, i)
        i += laneSize
    }

    if (i < size) {
        val mask = SPECIES.indexInRange(0, size - i)
        val current = FloatVector.fromArray(SPECIES, src, i, mask)
        val prev = FloatVector.fromArray(SPECIES, src, i - 1, mask)
        val change = current.sub(prev)
        change.max(zero).intoArray(gains, i, mask)
        change.neg().max(zero).intoArray(losses, i, mask)
    }
}