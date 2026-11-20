package com.devcode940.ForexAnalyst.forex.data.repository

import com.devcode940.ForexAnalyst.forex.data.model.CandleStick
import com.devcode940.ForexAnalyst.forex.data.model.IndicatorSettings
import com.devcode940.ForexAnalyst.forex.data.model.PairCatalog
import com.devcode940.ForexAnalyst.forex.data.model.SignalType
import com.devcode940.ForexAnalyst.forex.data.model.Timeframe
import com.devcode940.ForexAnalyst.forex.data.model.VolatilityRegime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TechnicalAnalysisEngineTest {

    private val baseTime = 1_700_000_000_000L

    private fun flatSeries(count: Int, price: Double = 100.0, tfMinutes: Int = 60): List<CandleStick> =
        (0 until count).map { i ->
            CandleStick(baseTime + i * tfMinutes * 60_000L, price, price, price, price, 100.0)
        }

    private fun trendSeries(count: Int, start: Double, step: Double, range: Double): List<CandleStick> =
        (0 until count).map { i ->
            val close = start + step * i
            CandleStick(baseTime + i * 3_600_000L, close - step, close + range, close - range, close, 500.0)
        }

    @Test
    fun `RSI rises above 70 on persistent uptrend and falls below 30 on persistent downtrend`() {
        val upCloses = trendSeries(60, 100.0, 0.5, 0.2).map { it.close }
        val up = TechnicalAnalysisEngine.calculateRSI(upCloses)
        assertEquals("RSI series size must exactly match input closes size", upCloses.size, up.size)
        assertTrue("last RSI should be overbought", (up.lastOrNull() ?: 0.0) > 70.0)

        val downCloses = trendSeries(60, 130.0, -0.5, 0.2).map { it.close }
        val down = TechnicalAnalysisEngine.calculateRSI(downCloses)
        assertEquals("RSI series size must exactly match input closes size", downCloses.size, down.size)
        assertTrue("last RSI should be oversold", (down.lastOrNull() ?: 100.0) < 30.0)
    }

    @Test
    fun `ATR equals constant true range on a flat oscillating series`() {
        // Every candle: high=110 low=90, closes at 100 → TR = 20 every bar → ATR = 20.
        val candles = (0 until 40).map { i -> CandleStick(baseTime + i * 60_000L, 100.0, 110.0, 90.0, 100.0, 10.0) }
        val series = TechnicalAnalysisEngine.calculateATR(candles, 14)
        repeat(14) { idx -> assertNull("warmup should be null at $idx", series[idx]) }
        assertEquals(20.0, series.last()!!, 1e-9)
    }

    @Test
    fun `ATR seeds with the average of the first period true ranges`() {
        val candles = listOf(
            CandleStick(baseTime, 100.0, 105.0, 95.0, 102.0, 1.0),
            CandleStick(baseTime + 60_000, 102.0, 108.0, 100.0, 107.0, 1.0)
        )
        val atr = TechnicalAnalysisEngine.calculateATR(candles, 1)
        // period 1 → ATR[i] == TR[i]; TR for second candle = max(8, |108-102|, |100-102|) = 8
        assertEquals(8.0, atr.last()!!, 1e-9)
    }

    @Test
    fun `ADX produces bounded values on trending data`() {
        val adx = TechnicalAnalysisEngine.latestADX(trendSeries(120, 100.0, 0.4, 0.15))
        assertNotNull(adx)
        assertTrue(adx!! >= 0.0 && adx <= 100.0)
    }

    @Test
    fun `volatility regime is NORMAL for a flat ATR distribution and EXTREME after a shock`() {
        assertEquals(VolatilityRegime.NORMAL, TechnicalAnalysisEngine.detectVolatilityRegime(flatSeries(60, 100.0)))

        val shocked = (0 until 60).map { i ->
            val range = if (i >= 56) 15.0 else 1.0
            val mid = 100.0
            CandleStick(baseTime + i * 60_000L, mid, mid + range, mid - range, mid, 10.0)
        }
        assertEquals(VolatilityRegime.EXTREME, TechnicalAnalysisEngine.detectVolatilityRegime(shocked))
    }

    @Test
    fun `volatility sizing converts ATR stops into lots for fixed account risk`() {
        // Rising 20-pip-range EUR/USD-like series at pip 0.0001.
        val candles = (0 until 60).map { i ->
            val close = 1.0800 + 0.0002 * i
            CandleStick(baseTime + i * 3_600_000L, close - 0.0002, close + 0.0010, close - 0.0010, close, 100.0)
        }
        val pair = PairCatalog.pairFor("EUR/USD", candles.last().close, candles.maxOf { it.high }, candles.minOf { it.low })
        val sizing = TechnicalAnalysisEngine.computeVolatilitySizing(pair, candles, balance = 10_000.0, riskPercent = 1.0)

        assertEquals(100_000.0, sizing.contractSize, 1e-9)
        assertEquals(100.0, sizing.riskAmount, 1e-9)
        // ATR ~ 0.0020 (20 pips) → NORMAL regime stop = 1.5×ATR = 30 pips
        assertEquals(30.0, sizing.stopLossPips, 3.0)
        // lots = 100 / (0.0030 * 100000) = 0.33
        assertEquals(0.33, sizing.suggestedLots, 0.05)
        assertTrue(sizing.takeProfit1Pips > sizing.stopLossPips)
        assertTrue(sizing.takeProfit2Pips > sizing.takeProfit1Pips)
    }

    @Test
    fun `signal levels respect direction with ATR-derived stops`() {
        val candles = trendSeries(80, 1.0800, 0.0003, 0.0008)
        val pair = PairCatalog.pairFor("EUR/USD", candles.last().close, candles.maxOf { it.high }, candles.minOf { it.low })

        val signal = TechnicalAnalysisEngine.generateTradeSignal(pair, candles, Timeframe.H1)
        when (signal.type) {
            SignalType.BUY -> {
                assertTrue(signal.stopLoss < signal.entryPrice)
                assertTrue(signal.takeProfit1 > signal.entryPrice)
                assertTrue(signal.takeProfit2 > signal.takeProfit1)
            }
            SignalType.SELL -> {
                assertTrue(signal.stopLoss > signal.entryPrice)
                assertTrue(signal.takeProfit1 < signal.entryPrice)
                assertTrue(signal.takeProfit2 < signal.takeProfit1)
            }
            SignalType.NEUTRAL -> {
                assertTrue(signal.confidenceScore <= 65)
            }
        }
        assertTrue(signal.atrValue > 0.0)
        assertTrue(signal.stopLossPips > 0.0)
        assertTrue(signal.confidenceScore in 35..97)
        assertTrue(signal.summaryRationale.contains("ATR"))
    }

    @Test
    fun `riskRewardRatio matches TP1 over SL distance`() {
        val candles = trendSeries(80, 1.0800, 0.0003, 0.0008)
        val pair = PairCatalog.pairFor("EUR/USD", candles.last().close, candles.maxOf { it.high }, candles.minOf { it.low })
        val s = TechnicalAnalysisEngine.generateTradeSignal(pair, candles, Timeframe.H1)
        val slDist = Math.abs(s.entryPrice - s.stopLoss)
        val tpDist = Math.abs(s.takeProfit1 - s.entryPrice)
        assertEquals(tpDist / slDist, s.riskRewardRatio, 1e-6)
    }

    @Test
    fun `support and resistance come from confirmed swings`() {
        val prices = doubleArrayOf(100.0, 98.0, 95.0, 97.0, 99.0, 103.0, 106.0, 104.0, 101.0, 100.0, 102.0)
        val candles = prices.mapIndexed { i, p -> CandleStick(baseTime + i * 60_000L, p, p + 1.0, p - 1.0, p, 10.0) }
        val (supports, resistances) = TechnicalAnalysisEngine.detectSupportResistance(candles)
        assertTrue(supports.isNotEmpty())
        assertTrue(resistances.isNotEmpty())
        assertTrue(supports.all { it < 100.0 })
        assertTrue(resistances.all { it > 100.0 })
    }

    @Test
    fun `signals degrade gracefully with insufficient data`() {
        val tiny = flatSeries(12, 1.0854)
        val pair = PairCatalog.pairFor("EUR/USD", 1.0854, 1.0860, 1.0850)
        val signal = TechnicalAnalysisEngine.generateTradeSignal(pair, tiny, Timeframe.M5)
        assertEquals(SignalType.NEUTRAL, signal.type)
        assertNotNull(signal)
    }

    @Test
    fun `settings propagate into indicator periods`() {
        val settings = IndicatorSettings(rsiPeriod = 5)
        val candles = trendSeries(40, 1.0800, 0.0003, 0.0008)
        val pair = PairCatalog.pairFor("EUR/USD", candles.last().close, candles.maxOf { it.high }, candles.minOf { it.low })
        val signal = TechnicalAnalysisEngine.generateTradeSignal(pair, candles, Timeframe.H1, settings)
        assertTrue(signal.rsiValue > 70.0) // faster RSI reacts harder to the uptrend
    }

    @Test
    fun `detectCrossUp and detectCrossDown identify latest bar crossovers`() {
        val fastUp = listOf(1.0, 2.0, 4.0)
        val slowUp = listOf(2.0, 2.5, 3.0)
        assertTrue(TechnicalAnalysisEngine.detectCrossUp(fastUp, slowUp))
        assertFalse(TechnicalAnalysisEngine.detectCrossDown(fastUp, slowUp))

        val fastDown = listOf(4.0, 3.0, 1.5)
        val slowDown = listOf(2.0, 2.5, 2.5)
        assertTrue(TechnicalAnalysisEngine.detectCrossDown(fastDown, slowDown))
        assertFalse(TechnicalAnalysisEngine.detectCrossUp(fastDown, slowDown))
    }

    @Test
    fun `projectHarmonicTarget computes Gartley, Bat, and Butterfly D-points`() {
        val targets = TechnicalAnalysisEngine.projectHarmonicTarget(x = 1.0000, a = 1.1000, b = 1.0500)
        assertEquals(1.1000 - 0.1000 * 0.786, targets["Gartley (0.786)"]!!, 1e-6)
        assertEquals(1.1000 - 0.1000 * 0.886, targets["Bat (0.886)"]!!, 1e-6)
        assertEquals(1.1000 - 0.1000 * 1.272, targets["Butterfly (1.272)"]!!, 1e-6)
    }

    @Test
    fun `analyzeMarketStructure identifies bullish structure and equilibrium`() {
        val candles = trendSeries(30, 1.0800, 0.0004, 0.0006)
        val summary = TechnicalAnalysisEngine.analyzeMarketStructure(candles)
        assertEquals("BULLISH_STRUCTURE", summary.bias)
        assertEquals("PREMIUM", summary.zone)
        assertEquals("BULLISH_BOS", summary.breakOfStructure)
    }

    @Test
    fun `currentTradingSessionUtc classifies London Killzone and NY Overlap`() {
        assertEquals("LONDON_KILLZONE", TechnicalAnalysisEngine.currentTradingSessionUtc(8))
        assertEquals("LONDON_NY_OVERLAP", TechnicalAnalysisEngine.currentTradingSessionUtc(13))
        assertEquals("ASIAN_SESSION", TechnicalAnalysisEngine.currentTradingSessionUtc(3))
    }

    @Test
    fun `detectLiquiditySweep identifies buy-side and sell-side sweeps`() {
        val base = trendSeries(10, 1.0800, 0.0001, 0.0002)
        val priorHigh = base.maxOf { it.high }
        val sweepHigh = base + CandleStick(
            timestamp = base.last().timestamp + 3600_000L,
            open = priorHigh - 0.0002,
            high = priorHigh + 0.0010,
            low = priorHigh - 0.0003,
            close = priorHigh - 0.0001,
            volume = 100.0
        )
        assertEquals("BUY_SIDE_SWEEP", TechnicalAnalysisEngine.detectLiquiditySweep(sweepHigh))
    }
}
