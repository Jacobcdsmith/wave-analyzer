package com.example.dsp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.cos

class SpectrumProcessorTest {

    @Test
    fun `process returns waveform matching input scale`() {
        val processor = SpectrumProcessor(sampleRate = 8000, bufferSize = 256)
        val samples = ShortArray(256) { 1000 }

        val frame = processor.process(samples, gain = 1.0f, sensitivity = 1.0f)

        assertEquals(256, frame.waveform.size)
        assertTrue(frame.waveform[0] > 0f)
    }

    @Test
    fun `dominant frequency matches injected sine tone`() {
        val sampleRate = 8000
        val bufferSize = 256
        val freq = 1000
        val processor = SpectrumProcessor(sampleRate = sampleRate, bufferSize = bufferSize)
        val samples = ShortArray(bufferSize) { i ->
            (cos(2.0 * PI * freq * i / sampleRate) * 10000).toInt().toShort()
        }

        val frame = processor.process(samples)

        assertEquals(freq.toFloat(), frame.dominantFreq, 50f)
    }

    @Test
    fun `waterfall history does not exceed configured size`() {
        val processor = SpectrumProcessor(sampleRate = 8000, bufferSize = 64, historySize = 5)
        val samples = ShortArray(64) { i -> (i * 100).toShort() }

        repeat(10) { processor.process(samples) }

        assertEquals(5, processor.process(samples).waterfall.size)
    }

    @Test
    fun `peak spectrum decays between silent frames`() {
        val processor = SpectrumProcessor(sampleRate = 8000, bufferSize = 64)
        val loud = ShortArray(64) { i -> (cos(2.0 * PI * i / 64) * 16000).toInt().toShort() }
        val silent = ShortArray(64) { 0 }

        val loudFrame = processor.process(loud)
        val peakAfterLoud = loudFrame.peakSpectrum.max()

        repeat(20) { processor.process(silent) }
        val peakAfterSilence = processor.process(silent).peakSpectrum.max()

        assertTrue(peakAfterSilence < peakAfterLoud)
    }

    @Test
    fun `reset clears peak and waterfall state`() {
        val processor = SpectrumProcessor(sampleRate = 8000, bufferSize = 64, historySize = 5)
        val samples = ShortArray(64) { i -> (cos(2.0 * PI * i / 64) * 16000).toInt().toShort() }

        processor.process(samples)
        processor.reset()
        val frame = processor.process(ShortArray(64) { 0 })

        assertEquals(1, frame.waterfall.size)
        assertEquals(0f, frame.peakSpectrum.max(), 0f)
    }
}
