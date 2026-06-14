package com.bokyo.blaze.indicator

import com.bokyo.blaze.BarSeries
import com.bokyo.blaze.indicator.average.SMAIndicator
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.floats.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

class DynamicGrowTest : FunSpec({

    test("indicator grows with BarSeries") {
        val series = BarSeries.of(
            FloatArray(30) { 100f + it },
            FloatArray(30) { 105f + it },
            FloatArray(30) { 95f + it },
            FloatArray(30) { 102f + it },
            FloatArray(30) { 1000f },
        )
        val sma = SMAIndicator.of(series, 10)
        val before = sma[29]

        series.add(200f, 210f, 190f, 205f, 1000f)

        sma.size shouldBe 31
        sma[30] shouldBeGreaterThan before
    }
})
