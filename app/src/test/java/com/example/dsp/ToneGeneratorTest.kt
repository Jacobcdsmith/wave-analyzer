package com.example.dsp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class ToneGeneratorTest {

    @Test
    fun `sine wave averages to zero`() {
        val gen = ToneGenerator(sampleRate = 8000)
        val samples = gen.generate(256, ToneWaveform.SINE, 1000f, 1.0f)

        val avg = samples.average()
        assertEquals(0.0, avg, 500.0) // loose tolerance due to single cycle
    }

    @Test
    fun `silence when amplitude is zero`() {
        val gen = ToneGenerator(sampleRate = 8000)
        val samples = gen.generate(256, ToneWaveform.SINE, 1000f, 0.0f)

        assertTrue(samples.all { it == 0.toShort() })
    }

    @Test
    fun `square wave only contains max negative or positive values`() {
        val gen = ToneGenerator(sampleRate = 8000)
        val samples = gen.generate(256, ToneWaveform.SQUARE, 1000f, 1.0f)

        val maxAmp = Short.MAX_VALUE.toInt()
        assertTrue(samples.all { it == maxAmp.toShort() || it == (-maxAmp).toShort() })
    }

    @Test
    fun `sawtooth spans both positive and negative`() {
        val gen = ToneGenerator(sampleRate = 8000)
        val samples = gen.generate(256, ToneWaveform.SAWTOOTH, 1000f, 1.0f)

        assertTrue(samples.any { it > 0 })
        assertTrue(samples.any { it < 0 })
    }

    @Test
    fun `noise is non-zero and varies`() {
        val gen = ToneGenerator(sampleRate = 8000)
        val samples = gen.generate(256, ToneWaveform.NOISE, 1000f, 1.0f)

        val distinct = samples.distinct().size
        assertTrue(distinct > 100)
    }

    @Test
    fun `amplitude scales peak value`() {
        val gen = ToneGenerator(sampleRate = 8000)
        val full = gen.generate(256, ToneWaveform.SQUARE, 1000f, 1.0f)
        val half = gen.generate(256, ToneWaveform.SQUARE, 1000f, 0.5f)

        val fullPeak = full.maxOf { abs(it.toInt()) }
        val halfPeak = half.maxOf { abs(it.toInt()) }
        val expectedHalf = fullPeak / 2
        assertTrue("expected ~$expectedHalf, got $halfPeak", kotlin.math.abs(expectedHalf - halfPeak) <= 2)
    }
}
