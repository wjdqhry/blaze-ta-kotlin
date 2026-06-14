package com.bokyo.blaze.indicator.momentum

import com.bokyo.blaze.helpers.TestDataFactory
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import org.ta4j.core.indicators.helpers.ClosePriceIndicator
import org.ta4j.core.indicators.RSIIndicator as Ta4jRSIIndicator
import org.ta4j.core.indicators.MACDIndicator as Ta4jMACDIndicator
import org.ta4j.core.indicators.averages.EMAIndicator as Ta4jEMAIndicator

class MomentumCorrectnessTest : FunSpec({

    val closePrices = TestDataFactory.realisticCloseArray(50)
    val ta4jSeries = TestDataFactory.buildTa4jSeries(closePrices)
    val ta4jClose = ClosePriceIndicator(ta4jSeries)

    val tolerance = 0.1f // recursive indicators need wider tolerance

    // --- Tier 1 ---

    test("RSI correctness vs ta4j") {
        val period = 14
        val simdRsi = RSIIndicator.of(closePrices, period)
        val ta4jRsi = Ta4jRSIIndicator(ta4jClose, period)
        for (i in period until closePrices.size) {
            simdRsi[i] shouldBe (ta4jRsi.getValue(i).floatValue() plusOrMinus tolerance)
        }
    }

    test("MACD Line correctness vs ta4j") {
        val macd = MACDIndicator.of(closePrices, 12, 26, 9)
        val ta4jMacd = Ta4jMACDIndicator(ta4jClose, 12, 26)
        for (i in 26 until closePrices.size) {
            macd.line[i] shouldBe (ta4jMacd.getValue(i).floatValue() plusOrMinus tolerance)
        }
    }

    test("MACD Signal correctness vs ta4j") {
        val macd = MACDIndicator.of(closePrices, 12, 26, 9)
        val ta4jMacd = Ta4jMACDIndicator(ta4jClose, 12, 26)
        val ta4jSignal = Ta4jEMAIndicator(ta4jMacd, 9)
        val signalStart = 26 + 9
        for (i in signalStart until closePrices.size) {
            macd.signal[i] shouldBe (ta4jSignal.getValue(i).floatValue() plusOrMinus tolerance)
        }
    }

})
