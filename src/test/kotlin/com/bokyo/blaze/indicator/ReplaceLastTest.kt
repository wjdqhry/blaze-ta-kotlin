package com.bokyo.blaze.indicator

import com.bokyo.blaze.BarSeries
import com.bokyo.blaze.helpers.TestDataFactory
import com.bokyo.blaze.indicator.average.EMAIndicator
import com.bokyo.blaze.indicator.average.SMAIndicator
import com.bokyo.blaze.indicator.momentum.MACDIndicator
import com.bokyo.blaze.indicator.momentum.RSIIndicator
import com.bokyo.blaze.indicator.volatility.BollingerBandsIndicator
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe

/**
 * The last bar is tentative: `replaceLast` must be reflected by a subsequent read, and the
 * recomputed value must match a fresh indicator built on the replaced data.
 */
class ReplaceLastTest : FunSpec({

    fun seriesOf(close: FloatArray): BarSeries =
        BarSeries.of(close, close, close, close, FloatArray(close.size) { 1f })

    val base = TestDataFactory.realisticCloseArray(60)
    val tol = 0.01f

    test("SMA reflects replaceLast on the last bar") {
        val series = seriesOf(base.copyOf())
        val sma = SMAIndicator.of(series, 10) // cached = true by default
        val last = series.size - 1

        series.replaceLast(0f, 0f, 0f, 999f, 1f)

        val expectedClose = base.copyOf().also { it[last] = 999f }
        val expected = SMAIndicator.of(expectedClose, 10, cached = false)
        sma[last] shouldBe (expected[last] plusOrMinus tol)
    }

    test("EMA reflects replaceLast on the last bar") {
        val series = seriesOf(base.copyOf())
        val ema = EMAIndicator.of(series, 10)
        val last = series.size - 1

        series.replaceLast(0f, 0f, 0f, 999f, 1f)

        val expected = EMAIndicator.of(base.copyOf().also { it[last] = 999f }, 10)
        ema[last] shouldBe (expected[last] plusOrMinus tol)
    }

    test("RSI reflects replaceLast on the last bar") {
        val series = seriesOf(base.copyOf())
        val rsi = RSIIndicator.of(series, 14)
        val last = series.size - 1

        series.replaceLast(0f, 0f, 0f, 999f, 1f)

        val expected = RSIIndicator.of(base.copyOf().also { it[last] = 999f }, 14)
        rsi[last] shouldBe (expected[last] plusOrMinus 0.1f)
    }

    test("MACD reflects replaceLast on the last bar") {
        val series = seriesOf(base.copyOf())
        val macd = MACDIndicator.of(series, 12, 26, 9)
        val last = series.size - 1

        series.replaceLast(0f, 0f, 0f, 999f, 1f)

        val expected = MACDIndicator.of(base.copyOf().also { it[last] = 999f }, 12, 26, 9)
        macd.line[last] shouldBe (expected.line[last] plusOrMinus 0.1f)
        macd.signal[last] shouldBe (expected.signal[last] plusOrMinus 0.1f)
        macd.histogram[last] shouldBe (expected.histogram[last] plusOrMinus 0.1f)
    }

    test("Bollinger reflects replaceLast on the last bar") {
        val series = seriesOf(base.copyOf())
        val bb = BollingerBandsIndicator.of(series, 20, 2.0f)
        val last = series.size - 1

        series.replaceLast(0f, 0f, 0f, 999f, 1f)

        val expected = BollingerBandsIndicator.of(base.copyOf().also { it[last] = 999f }, 20, 2.0f)
        bb.middle[last] shouldBe (expected.middle[last] plusOrMinus tol)
        bb.upper[last] shouldBe (expected.upper[last] plusOrMinus tol)
        bb.lower[last] shouldBe (expected.lower[last] plusOrMinus tol)
    }

    test("add then read confirms previously-tentative bar and grows correctly") {
        val series = seriesOf(base.copyOf())
        val ema = EMAIndicator.of(series, 10)
        val firstLast = series.size - 1
        val vBefore = ema[firstLast]

        series.add(0f, 0f, 0f, 123f, 1f)

        // previously-last bar is now confirmed and unchanged
        ema[firstLast] shouldBe (vBefore plusOrMinus tol)
        // new last bar computes against a fresh reference
        val expected = EMAIndicator.of(base.copyOf() + 123f, 10)
        ema[series.size - 1] shouldBe (expected[series.size - 1] plusOrMinus tol)
    }
})
