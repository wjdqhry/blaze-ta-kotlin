package com.bokyo.blaze.indicator

import com.bokyo.blaze.indicator.average.SMAIndicator
import io.kotest.core.spec.style.FunSpec
import org.ta4j.core.BaseBar
import org.ta4j.core.BaseBarSeriesBuilder
import org.ta4j.core.indicators.averages.SMAIndicator as Ta4jSMAIndicator
import org.ta4j.core.indicators.helpers.ClosePriceIndicator
import org.ta4j.core.num.DecimalNum
import java.time.Duration
import java.time.Instant

class IndicatorTest: FunSpec({

    val testSMA = FloatArray(100) { it.toFloat() }

    test("test SMA for simdta") {
        val startTime = Instant.now()

        repeat(100000) {
            val sma = SMAIndicator.of(testSMA, 20)
        }
        println("simdta time: ${Duration.between(startTime, Instant.now())}")
    }

    test("test SMA for normal ta4j") {
        val series = BaseBarSeriesBuilder()
            .withBars(
                testSMA.map {
                    BaseBar(
                        Duration.ZERO,
                        Instant.EPOCH,
                        Instant.EPOCH,
                        DecimalNum.valueOf(0),
                        DecimalNum.valueOf(0),
                        DecimalNum.valueOf(0),
                        DecimalNum.valueOf(it),
                        DecimalNum.valueOf(0),
                        DecimalNum.valueOf(0),
                        0L,
                    )
                }
            )
            .build()

        val startTime = Instant.now()

        repeat(100000) {
            val sma = Ta4jSMAIndicator(ClosePriceIndicator(series), 20)
        }
        println("ta4j time: ${Duration.between(startTime, Instant.now())}")
    }
})
