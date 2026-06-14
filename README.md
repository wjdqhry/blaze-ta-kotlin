# blaze-ta-kotlin

Blazing-fast Technical Analysis library for the JVM.

**9x–1587x** faster than traditional implementations, delivered by low-level tuning.

> ⚠️ **Disclaimer**: I'm not a quant expert, and mistakes in indicator formulas or edge cases are possible. Please use with caution. Issue reports are always welcome and appreciated 🙏

## Why?

Most JVM TA libraries (e.g. ta4j) use `BigDecimal`-wrapped objects with lazy recursive evaluation. This is flexible but slow. blaze-ta-kotlin takes a different approach:

- **Primitive `FloatArray` throughout** -- no boxing, no object allocation per bar
- **Inline value classes** -- zero-cost wrappers for indicator outputs (`macd.line`, `bb.upper`, ...)
- **SIMD tuning** -- vectorized calculations via [Java Vector API (Panama)](https://openjdk.org/jeps/489) where it pays off
- **Pre-computed cache** -- recursive indicators (EMA, RSI, MACD) compute once, O(1) lookup thereafter

## Benchmarks

blaze-ta-kotlin vs ta4j 0.22.3, measured with JMH on Apple Silicon (JDK 25).

Speedup vs ta4j (10,000 bars): **SMA 83x · EMA 146x · MACD 340x · RSI 484x · Bollinger 1587x**

```
Benchmark                               (dataSize)  (period)  Mode  Cnt      Score        Error  Units

SMABenchmark.blazeSma                          100        20  avgt   10      0.189  ±     0.001  us/op
SMABenchmark.ta4jSma                           100        20  avgt   10      8.374  ±     0.140  us/op
SMABenchmark.blazeSma                          100        50  avgt   10      0.691  ±     0.007  us/op
SMABenchmark.ta4jSma                           100        50  avgt   10      6.347  ±     0.567  us/op
SMABenchmark.blazeSma                        10000        20  avgt   10     23.087  ±     0.270  us/op
SMABenchmark.ta4jSma                         10000        20  avgt   10   1927.450  ±   139.709  us/op
SMABenchmark.blazeSma                        10000        50  avgt   10     68.337  ±     3.828  us/op
SMABenchmark.ta4jSma                         10000        50  avgt   10   1658.892  ±    73.230  us/op

MovingAverageBenchmark.blazeEma                100        20  avgt   10      0.146  ±     0.009  us/op
MovingAverageBenchmark.ta4jEma                 100        20  avgt   10     17.304  ±     1.034  us/op
MovingAverageBenchmark.blazeEma              10000        20  avgt   10     18.431  ±     0.920  us/op
MovingAverageBenchmark.ta4jEma               10000        20  avgt   10   2687.567  ±   181.410  us/op

MomentumBenchmark.blazeRsi                     100       N/A  avgt   10      0.130  ±     0.004  us/op
MomentumBenchmark.ta4jRsi                      100       N/A  avgt   10     54.615  ±     0.584  us/op
MomentumBenchmark.blazeRsi                   10000       N/A  avgt   10     16.336  ±     0.468  us/op
MomentumBenchmark.ta4jRsi                    10000       N/A  avgt   10   7906.073  ±   212.869  us/op

MomentumBenchmark.blazeMacd                    100       N/A  avgt   10      0.199  ±     0.006  us/op
MomentumBenchmark.ta4jMacd                     100       N/A  avgt   10     47.639  ±     0.524  us/op
MomentumBenchmark.blazeMacd                  10000       N/A  avgt   10     17.800  ±     0.324  us/op
MomentumBenchmark.ta4jMacd                   10000       N/A  avgt   10   6055.561  ±   749.170  us/op

BollingerBandsBenchmark.blazeBollinger         100        20  avgt   10      0.273  ±     0.025  us/op
BollingerBandsBenchmark.ta4jBollinger          100        20  avgt   10    292.723  ±     5.336  us/op
BollingerBandsBenchmark.blazeBollinger       10000        20  avgt   10     31.479  ±     0.649  us/op
BollingerBandsBenchmark.ta4jBollinger        10000        20  avgt   10  49956.875  ±  1283.826  us/op
```

## Quick Start

```kotlin
val prices: FloatArray = closePriceArray

// SMA -- SIMD vectorized windowed sum
val sma = SMAIndicator.of(prices, 20)
sma[99]  // SMA at index 99

// EMA -- scalar recursive with pre-computed cache
val ema = EMAIndicator.of(prices, 20)
ema[99]

// RSI -- single-pass gain/loss with Wilder's smoothing, pre-computed cache
val rsi = RSIIndicator.of(prices, 14)
rsi[99]

// MACD -- pre-computed line + signal + histogram
val macd = MACDIndicator.of(prices, 12, 26, 9)
macd.line[99]
macd.signal[99]
macd.histogram[99]

// Bollinger Bands -- sliding sum + sum-of-squares (double accumulators), pre-computed cache
val bb = BollingerBandsIndicator.of(prices, 20, multiplier = 2.0f)
bb.middle[99]; bb.upper[99]; bb.lower[99]
bb.width[99]; bb.percentB[99]
```

### Dynamic (streaming)

```kotlin
val series = BarSeries.of(open, high, low, close, volume)
val rsi = RSIIndicator.of(series, 14)

series.add(o, h, l, c, v)  // indicator auto-grows
rsi[series.size - 1]
```

## Requirements

- **JDK 25+** (Java Vector API)
- **Kotlin 2.3+**

```bash
# Run tests
./gradlew test

# Run benchmarks
./gradlew jmhJar
java --add-modules jdk.incubator.vector -jar build/libs/blaze-ta-kotlin-*-jmh.jar
```

## Implementation Status

- [x] SMA (Simple Moving Average)
- [x] EMA (Exponential Moving Average)
- [x] RSI (Relative Strength Index)
- [x] MACD
- [x] Bollinger Bands
- [ ] Stochastic Oscillator
- [ ] ATR (Average True Range)
- [ ] ADX (Average Directional Index)
- [ ] VWAP
- [ ] Parabolic SAR

## Known Limitations

This is an early release with deliberate trade-offs. Known issues and limitations are
tracked in the [GitHub Issues](../../issues).

## License

[MIT](LICENSE)
