package com.bokyo.blaze.indicator.average

import com.bokyo.blaze.helpers.TestDataFactory
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import org.ta4j.core.indicators.averages.SMAIndicator as Ta4jSMAIndicator
import org.ta4j.core.indicators.averages.EMAIndicator as Ta4jEMAIndicator
import org.ta4j.core.indicators.helpers.ClosePriceIndicator

class MovingAverageCorrectnessTest : FunSpec({

    val closePrices = TestDataFactory.realisticCloseArray(20)
    val ta4jSeries = TestDataFactory.buildTa4jSeries(closePrices)
    val ta4jClose = ClosePriceIndicator(ta4jSeries)

    val tolerance = 0.01f

    // --- Tier 1 ---

    test("SMA correctness vs ta4j") {
        val period = 10
        val simdSma = SMAIndicator.of(closePrices, period)
        val ta4jSma = Ta4jSMAIndicator(ta4jClose, period)

        for (i in period - 1 until closePrices.size) {
            simdSma[i] shouldBe (ta4jSma.getValue(i).floatValue() plusOrMinus tolerance)
        }
    }

    test("EMA correctness vs ta4j") {
        val period = 10
        val simdEma = EMAIndicator.of(closePrices, period)
        val ta4jEma = Ta4jEMAIndicator(ta4jClose, period)
        for (i in period until closePrices.size) {
            simdEma[i] shouldBe (ta4jEma.getValue(i).floatValue() plusOrMinus tolerance)
        }
    }

})
