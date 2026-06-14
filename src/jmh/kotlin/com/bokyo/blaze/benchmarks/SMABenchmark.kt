package com.bokyo.blaze.benchmarks

import com.bokyo.blaze.indicator.average.SMAIndicator as SMAIIndicator
import com.bokyo.blaze.benchmarks.data.BenchmarkDataFactory
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole
import org.ta4j.core.indicators.averages.SMAIndicator
import java.util.concurrent.TimeUnit

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2, jvmArgs = ["--add-modules", "jdk.incubator.vector"])
open class SMABenchmark : BenchmarkDataFactory() {

    @Param("20", "50")
    var period: Int = 0

    @Benchmark
    fun blazeSma(bh: Blackhole) {
        // cached = false to measure the raw vectorized windowed-sum kernel
        val sma = SMAIIndicator.of(closePrices, period, cached = false)
        for (i in period - 1 until dataSize) {
            bh.consume(sma[i])
        }
    }

    @Benchmark
    fun ta4jSma(bh: Blackhole) {
        val sma = SMAIndicator(ta4jClose, period)
        for (i in period - 1 until dataSize) {
            bh.consume(sma.getValue(i))
        }
    }
}
