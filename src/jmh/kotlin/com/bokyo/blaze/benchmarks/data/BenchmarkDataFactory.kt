package com.bokyo.blaze.benchmarks.data

import org.ta4j.core.BaseBar
import org.ta4j.core.BaseBarSeriesBuilder
import org.ta4j.core.BarSeries
import org.ta4j.core.indicators.helpers.ClosePriceIndicator
import org.ta4j.core.indicators.helpers.HighPriceIndicator
import org.ta4j.core.indicators.helpers.LowPriceIndicator
import org.ta4j.core.indicators.helpers.VolumeIndicator
import org.ta4j.core.num.DecimalNum
import org.openjdk.jmh.annotations.*
import java.time.Duration
import java.time.Instant
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

@State(Scope.Benchmark)
open class BenchmarkDataFactory {

    @Param("100", "10000")
    var dataSize: Int = 0

    lateinit var closePrices: FloatArray
    lateinit var openPrices: FloatArray
    lateinit var highPrices: FloatArray
    lateinit var lowPrices: FloatArray
    lateinit var volumes: FloatArray

    lateinit var ta4jSeries: BarSeries
    lateinit var ta4jClose: ClosePriceIndicator
    lateinit var ta4jHigh: HighPriceIndicator
    lateinit var ta4jLow: LowPriceIndicator
    lateinit var ta4jVolume: VolumeIndicator

    @Setup(Level.Trial)
    fun setup() {
        val random = Random(42)

        // Generate close prices (random walk)
        closePrices = FloatArray(dataSize)
        closePrices[0] = 100f
        for (i in 1 until dataSize) {
            val change = (random.nextFloat() - 0.5f) * 0.04f
            closePrices[i] = closePrices[i - 1] * (1f + change)
        }

        // Generate OHLCV from close
        openPrices = FloatArray(dataSize)
        highPrices = FloatArray(dataSize)
        lowPrices = FloatArray(dataSize)
        volumes = FloatArray(dataSize)

        openPrices[0] = closePrices[0]
        for (i in 1 until dataSize) {
            openPrices[i] = closePrices[i - 1] * (1f + (random.nextFloat() - 0.5f) * 0.005f)
        }
        for (i in 0 until dataSize) {
            val spread = abs(closePrices[i] * (random.nextFloat() * 0.02f))
            highPrices[i] = max(openPrices[i], closePrices[i]) + spread
            lowPrices[i] = min(openPrices[i], closePrices[i]) - spread
            volumes[i] = random.nextFloat() * 1_000_000f + 100_000f
        }

        ta4jSeries = BaseBarSeriesBuilder()
            .withBars(
                (0 until dataSize).map { i ->
                    BaseBar(
                        Duration.ofDays(1),
                        Instant.EPOCH.plusSeconds(i.toLong() * 86400),
                        Instant.EPOCH.plusSeconds((i.toLong() + 1) * 86400),
                        DecimalNum.valueOf(openPrices[i].toDouble()),
                        DecimalNum.valueOf(highPrices[i].toDouble()),
                        DecimalNum.valueOf(lowPrices[i].toDouble()),
                        DecimalNum.valueOf(closePrices[i].toDouble()),
                        DecimalNum.valueOf(volumes[i].toDouble()),
                        DecimalNum.valueOf(0),
                        0L,
                    )
                }
            )
            .build()

        ta4jClose = ClosePriceIndicator(ta4jSeries)
        ta4jHigh = HighPriceIndicator(ta4jSeries)
        ta4jLow = LowPriceIndicator(ta4jSeries)
        ta4jVolume = VolumeIndicator(ta4jSeries)
    }
}
