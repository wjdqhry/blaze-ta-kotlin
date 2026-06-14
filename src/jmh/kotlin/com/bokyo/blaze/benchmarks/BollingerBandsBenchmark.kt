package com.bokyo.blaze.benchmarks

import com.bokyo.blaze.benchmarks.data.BenchmarkDataFactory
import com.bokyo.blaze.indicator.volatility.BollingerBandsIndicator
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole
import org.ta4j.core.indicators.averages.SMAIndicator
import org.ta4j.core.indicators.bollinger.BollingerBandsMiddleIndicator
import org.ta4j.core.indicators.bollinger.BollingerBandsUpperIndicator
import org.ta4j.core.indicators.bollinger.BollingerBandsLowerIndicator
import org.ta4j.core.indicators.statistics.StandardDeviationIndicator
import java.util.concurrent.TimeUnit

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2, jvmArgs = ["--add-modules", "jdk.incubator.vector"])
open class BollingerBandsBenchmark : BenchmarkDataFactory() {

    @Param("20")
    var period: Int = 0

    @Benchmark
    fun blazeBollinger(bh: Blackhole) {
        val bb = BollingerBandsIndicator.of(closePrices, period)
        for (i in period - 1 until dataSize) {
            bh.consume(bb.middle[i])
            bh.consume(bb.upper[i])
            bh.consume(bb.lower[i])
        }
    }

    @Benchmark
    fun ta4jBollinger(bh: Blackhole) {
        val sma = SMAIndicator(ta4jClose, period)
        val stdDev = StandardDeviationIndicator(ta4jClose, period)
        val middle = BollingerBandsMiddleIndicator(sma)
        val upper = BollingerBandsUpperIndicator(middle, stdDev)
        val lower = BollingerBandsLowerIndicator(middle, stdDev)
        for (i in period - 1 until dataSize) {
            bh.consume(middle.getValue(i))
            bh.consume(upper.getValue(i))
            bh.consume(lower.getValue(i))
        }
    }
}
