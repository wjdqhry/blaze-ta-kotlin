package com.bokyo.blaze.benchmarks

import com.bokyo.blaze.benchmarks.data.BenchmarkDataFactory
import com.bokyo.blaze.indicator.momentum.MACDIndicator
import com.bokyo.blaze.indicator.momentum.RSIIndicator
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole
import org.ta4j.core.indicators.*
import java.util.concurrent.TimeUnit
import org.ta4j.core.indicators.RSIIndicator as Ta4jRSIIndicator
import org.ta4j.core.indicators.MACDIndicator as Ta4jMACDIndicator

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2, jvmArgs = ["--add-modules", "jdk.incubator.vector"])
open class MomentumBenchmark : BenchmarkDataFactory() {

    @Benchmark
    fun ta4jRsi(bh: Blackhole) {
        val rsi = Ta4jRSIIndicator(ta4jClose, 14)
        for (i in 14 until dataSize) {
            bh.consume(rsi.getValue(i))
        }
    }

    @Benchmark
    fun blazeRsi(bh: Blackhole) {
        val rsi = RSIIndicator.of(closePrices, 14)
        for (i in 14 until dataSize) {
            bh.consume(rsi[i])
        }
    }

    @Benchmark
    fun ta4jMacd(bh: Blackhole) {
        val macd = Ta4jMACDIndicator(ta4jClose, 12, 26)
        for (i in 26 until dataSize) {
            bh.consume(macd.getValue(i))
        }
    }

    @Benchmark
    fun blazeMacd(bh: Blackhole) {
        val macd = MACDIndicator.of(closePrices, 12, 26, 9)
        for (i in 26 until dataSize) {
            bh.consume(macd.line[i])
        }
    }
}
