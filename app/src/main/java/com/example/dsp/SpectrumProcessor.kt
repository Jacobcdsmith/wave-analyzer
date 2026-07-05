package com.example.dsp

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Pure, testable DSP pipeline: window -> FFT -> magnitude -> dB -> peak-hold -> waterfall.
 */
class SpectrumProcessor(
    val sampleRate: Int = 44100,
    val bufferSize: Int = 1024,
    val historySize: Int = 60,
    private val decay: Float = 0.99f,
    private val floorDb: Float = -80f
) {
    private val real = DoubleArray(bufferSize)
    private val imag = DoubleArray(bufferSize)
    private val window = DoubleArray(bufferSize) { i ->
        0.54 - 0.46 * cos(2.0 * PI * i / (bufferSize - 1))
    }

    private val halfSize = bufferSize / 2
    private var peakSpectrum = FloatArray(halfSize) { 0f }
    private val waterfall = ArrayDeque<FloatArray>(historySize)

    data class Frame(
        val waveform: FloatArray,
        val spectrum: FloatArray,
        val peakSpectrum: FloatArray,
        val dominantFreq: Float,
        val peakDb: Float,
        val rms: Float,
        val waterfall: List<FloatArray>
    )

    /**
     * Process one audio buffer of 16-bit PCM samples.
     *
     * @param samples ShortArray of interleaved mono PCM samples.
     * @param gain Gain multiplier applied before windowing.
     * @param sensitivity dB scaling factor (0..n).
     */
    fun process(samples: ShortArray, gain: Float = 1.0f, sensitivity: Float = 1.0f): Frame {
        val readResult = samples.size.coerceAtMost(bufferSize)
        val waveform = FloatArray(readResult)
        var sumSq = 0.0
        var peakMag = 0.0
        var peakBin = 0

        for (i in 0 until readResult) {
            val norm = (samples[i] / 32768.0) * gain
            waveform[i] = norm.toFloat()
            real[i] = norm * window[i]
            imag[i] = 0.0
            sumSq += norm * norm
        }

        for (i in readResult until bufferSize) {
            real[i] = 0.0
            imag[i] = 0.0
        }

        FFT.transform(real, imag)

        val spectrum = FloatArray(halfSize)
        val newPeak = peakSpectrum.clone()
        for (i in 0 until halfSize) {
            val mag = sqrt(real[i] * real[i] + imag[i] * imag[i])
            val db = ((20 * log10(mag.coerceAtLeast(1e-4)) - floorDb) / -floorDb * sensitivity).toFloat()
            spectrum[i] = db.coerceIn(0f, 1f)
            newPeak[i] = max(newPeak[i] * decay, spectrum[i])
            if (mag > peakMag) {
                peakMag = mag
                peakBin = i
            }
        }
        peakSpectrum = newPeak

        val rawDb = (20 * log10(peakMag.coerceAtLeast(1e-4))).toFloat()
        val dominantFreq = peakBin * (sampleRate.toFloat() / bufferSize)
        val rms = sqrt(sumSq / readResult).toFloat()

        waterfall.addLast(spectrum)
        if (waterfall.size > historySize) waterfall.removeFirst()

        return Frame(
            waveform = waveform,
            spectrum = spectrum,
            peakSpectrum = peakSpectrum.clone(),
            dominantFreq = dominantFreq,
            peakDb = rawDb,
            rms = rms,
            waterfall = waterfall.toList()
        )
    }

    fun reset() {
        peakSpectrum = FloatArray(halfSize) { 0f }
        waterfall.clear()
    }
}
