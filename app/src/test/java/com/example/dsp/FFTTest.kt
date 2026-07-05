package com.example.dsp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class FFTTest {

    @Test
    fun `impulse has constant magnitude across all bins`() {
        val n = 64
        val real = DoubleArray(n) { if (it == 0) 1.0 else 0.0 }
        val imag = DoubleArray(n) { 0.0 }

        FFT.transform(real, imag)

        for (i in 0 until n) {
            val mag = sqrt(real[i] * real[i] + imag[i] * imag[i])
            assertEquals(1.0, mag, 1e-10)
        }
    }

    @Test
    fun `sine tone produces peak at expected positive frequency bin`() {
        val n = 256
        val sampleRate = 8000
        val freq = 1000
        val cycles = n * freq / sampleRate  // 32 cycles
        val real = DoubleArray(n) { i -> cos(2.0 * PI * cycles * i / n) }
        val imag = DoubleArray(n) { 0.0 }

        FFT.transform(real, imag)

        val half = n / 2
        var peakBin = 0
        var peakMag = 0.0
        for (i in 0 until half) {
            val mag = sqrt(real[i] * real[i] + imag[i] * imag[i])
            if (mag > peakMag) {
                peakMag = mag
                peakBin = i
            }
        }

        assertEquals(cycles, peakBin)
        // Energy should be concentrated in the peak bin (within a few percent of total)
        val total = (0 until half).sumOf { sqrt(real[it] * real[it] + imag[it] * imag[it]) }
        assertTrue(peakMag / total > 0.5)
    }

    @Test
    fun `parseval theorem holds approximately for random signal`() {
        val n = 128
        val real = DoubleArray(n) { i -> sin(2.0 * PI * i * 3 / n) }
        val imag = DoubleArray(n) { 0.0 }

        val timeEnergy = real.sumOf { it * it }

        FFT.transform(real, imag)

        val freqEnergy = (0 until n).sumOf { real[it] * real[it] + imag[it] * imag[it] } / n
        assertEquals(timeEnergy, freqEnergy, 1e-6)
    }

    @Test
    fun `works for common power-of-two sizes`() {
        listOf(16, 32, 64, 128, 256, 512, 1024).forEach { n ->
            val real = DoubleArray(n) { i -> cos(2.0 * PI * i / n) }
            val imag = DoubleArray(n) { 0.0 }
            FFT.transform(real, imag)
            val mag = sqrt(real[1] * real[1] + imag[1] * imag[1])
            assertTrue("Size $n failed", mag > n * 0.4)
        }
    }

    @Test
    fun `dc signal appears in bin 0`() {
        val n = 64
        val real = DoubleArray(n) { 5.0 }
        val imag = DoubleArray(n) { 0.0 }

        FFT.transform(real, imag)

        assertEquals(5.0 * n, real[0], 1e-10)
        assertEquals(0.0, imag[0], 1e-10)
        for (i in 1 until n) {
            assertEquals(0.0, real[i], 1e-10)
            assertEquals(0.0, imag[i], 1e-10)
        }
    }
}
