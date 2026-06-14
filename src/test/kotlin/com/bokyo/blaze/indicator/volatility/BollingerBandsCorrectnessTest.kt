package com.bokyo.blaze.indicator.volatility

import com.bokyo.blaze.helpers.TestDataFactory
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import org.ta4j.core.indicators.averages.SMAIndicator as Ta4jSMAIndicator
import org.ta4j.core.indicators.bollinger.BollingerBandsMiddleIndicator as Ta4jBBMiddle
import org.ta4j.core.indicators.bollinger.BollingerBandsUpperIndicator as Ta4jBBUpper
import org.ta4j.core.indicators.bollinger.BollingerBandsLowerIndicator as Ta4jBBLower
import org.ta4j.core.indicators.statistics.StandardDeviationIndicator as Ta4jStdDev
import org.ta4j.core.indicators.helpers.ClosePriceIndicator

class BollingerBandsCorrectnessTest : FunSpec({

    val closePrices = TestDataFactory.realisticCloseArray(50)
    val ta4jSeries = TestDataFactory.buildTa4jSeries(closePrices)
    val ta4jClose = ClosePriceIndicator(ta4jSeries)

    val period = 20
    val multiplier = 2.0f
    val tolerance = 0.01f

    val bb = BollingerBandsIndicator.of(closePrices, period, multiplier)

    val ta4jSma = Ta4jSMAIndicator(ta4jClose, period)
    val ta4jStdDev = Ta4jStdDev(ta4jClose, period)
    val ta4jMiddle = Ta4jBBMiddle(ta4jSma)
    val ta4jUpper = Ta4jBBUpper(ta4jMiddle, ta4jStdDev)
    val ta4jLower = Ta4jBBLower(ta4jMiddle, ta4jStdDev)

    test("Bollinger Bands correctness vs ta4j") {
        for (i in period - 1 until closePrices.size) {
            val m = ta4jMiddle.getValue(i).floatValue()
            val u = ta4jUpper.getValue(i).floatValue()
            val l = ta4jLower.getValue(i).floatValue()

            bb.middle[i] shouldBe (m plusOrMinus tolerance)
            bb.upper[i] shouldBe (u plusOrMinus tolerance)
            bb.lower[i] shouldBe (l plusOrMinus tolerance)
            bb.width[i] shouldBe ((u - l) / m plusOrMinus tolerance)
            bb.percentB[i] shouldBe ((closePrices[i] - l) / (u - l) plusOrMinus tolerance)
        }
    }

    test("Bollinger Bands returns NaN for warm-up and throws for out-of-bounds") {
        // warm-up → NaN
        bb.middle[period - 2].isNaN() shouldBe true
        bb.upper[period - 2].isNaN() shouldBe true
        bb.lower[period - 2].isNaN() shouldBe true
        bb.width[period - 2].isNaN() shouldBe true
        bb.percentB[period - 2].isNaN() shouldBe true

        // out-of-bounds → IOOBE
        org.junit.jupiter.api.assertThrows<IndexOutOfBoundsException> { bb.middle[-1] }
        org.junit.jupiter.api.assertThrows<IndexOutOfBoundsException> { bb.middle[closePrices.size] }
    }
})
