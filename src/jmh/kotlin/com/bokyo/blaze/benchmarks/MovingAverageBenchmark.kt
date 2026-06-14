package com.bokyo.blaze.benchmarks

import com.bokyo.blaze.benchmarks.data.BenchmarkDataFactory
import com.bokyo.blaze.indicator.average.EMAIndicator as SimdEMAIndicator
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole
import org.ta4j.core.indicators.averages.*
import java.util.concurrent.TimeUnit

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2, jvmArgs = ["--add-modules", "jdk.incubator.vector"])
open class MovingAverageBenchmark : BenchmarkDataFactory() {

    @Param("20")
    var period: Int = 0

    @Benchmark
    fun ta4jEma(bh: Blackhole) {
        val ema = EMAIndicator(ta4jClose, period)
        for (i in period - 1 until dataSize) {
            bh.consume(ema.getValue(i))
        }
    }

    @Benchmark
    fun blazeEma(bh: Blackhole) {
        val ema = SimdEMAIndicator.of(closePrices, period)
        for (i in period until dataSize) {
            bh.consume(ema[i])
        }
    }
}
