package com.example.dsp

import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

enum class ToneWaveform {
    SINE, SQUARE, SAWTOOTH, NOISE
}

/**
 * Generates synthetic audio buffers for self-testing the analyzer without a microphone.
 */
class ToneGenerator(
    val sampleRate: Int = 44100
) {
    private var phase = 0.0

    /**
     * Generate a buffer of 16-bit PCM samples.
     *
     * @param bufferSize number of samples to generate
     * @param waveform waveform shape
     * @param frequencyHz tone frequency
     * @param amplitude amplitude 0..1 (clamped)
     */
    fun generate(
        bufferSize: Int,
        waveform: ToneWaveform,
        frequencyHz: Float,
        amplitude: Float
    ): ShortArray {
        val amp = amplitude.coerceIn(0f, 1f)
        val samples = ShortArray(bufferSize)
        val phaseIncrement = 2.0 * PI * frequencyHz / sampleRate

        for (i in 0 until bufferSize) {
            val value = when (waveform) {
                ToneWaveform.SINE -> sin(phase)
                ToneWaveform.SQUARE -> if (sin(phase) >= 0) 1.0 else -1.0
                ToneWaveform.SAWTOOTH -> 2.0 * ((phase / (2.0 * PI)) - (phase / (2.0 * PI)).toInt()) - 1.0
                ToneWaveform.NOISE -> Random.nextDouble(-1.0, 1.0)
            }
            samples[i] = (value * amp * Short.MAX_VALUE).roundToInt().toShort()
            phase += phaseIncrement
            if (phase >= 2.0 * PI) phase -= 2.0 * PI
        }

        return samples
    }

    fun reset() {
        phase = 0.0
    }
}
