package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AIService
import com.example.dsp.SpectrumProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AudioAnalyzerViewModel : ViewModel() {
    val sampleRate = 44100
    val bufferSize = 1024
    private val waterfallHistorySize = 60

    private val _waveform = MutableStateFlow(FloatArray(0))
    val waveform: StateFlow<FloatArray> = _waveform.asStateFlow()

    private val _waterfall = MutableStateFlow(emptyList<FloatArray>())
    val waterfall: StateFlow<List<FloatArray>> = _waterfall.asStateFlow()

    private val _peakSpectrum = MutableStateFlow(FloatArray(0))
    val peakSpectrum: StateFlow<FloatArray> = _peakSpectrum.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _gain = MutableStateFlow(1.0f)
    val gain: StateFlow<Float> = _gain.asStateFlow()

    private val _sensitivity = MutableStateFlow(1.0f)
    val sensitivity: StateFlow<Float> = _sensitivity.asStateFlow()

    private val _colorTheme = MutableStateFlow(0)
    val colorTheme: StateFlow<Int> = _colorTheme.asStateFlow()

    private val _aiAnalysis = MutableStateFlow<String?>("Ready for analysis.")
    val aiAnalysis: StateFlow<String?> = _aiAnalysis.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _peakDb = MutableStateFlow(-80f)
    val peakDb: StateFlow<Float> = _peakDb.asStateFlow()

    private val _dominantFreq = MutableStateFlow(0f)
    val dominantFreq: StateFlow<Float> = _dominantFreq.asStateFlow()

    private val _throughputPercent = MutableStateFlow(0)
    val throughputPercent: StateFlow<Int> = _throughputPercent.asStateFlow()

    private val _rmsLevel = MutableStateFlow(0f)
    val rmsLevel: StateFlow<Float> = _rmsLevel.asStateFlow()

    private val _frozenSpectrum = MutableStateFlow<FloatArray?>(null)
    val frozenSpectrum: StateFlow<FloatArray?> = _frozenSpectrum.asStateFlow()

    private val _zoomStartBin = MutableStateFlow(0)
    val zoomStartBin: StateFlow<Int> = _zoomStartBin.asStateFlow()

    private val _zoomBinCount = MutableStateFlow(bufferSize / 2)
    val zoomBinCount: StateFlow<Int> = _zoomBinCount.asStateFlow()

    fun updateGain(g: Float) { _gain.value = g }
    fun updateSensitivity(s: Float) { _sensitivity.value = s }
    fun updateColorTheme(t: Int) { _colorTheme.value = t }

    fun freezeSpectrum() {
        _frozenSpectrum.value = _peakSpectrum.value.clone()
    }

    fun clearFreeze() {
        _frozenSpectrum.value = null
    }

    fun clearAnalysis() {
        _aiAnalysis.value = "Ready for analysis."
    }

    fun resetZoom() {
        _zoomStartBin.value = 0
        _zoomBinCount.value = bufferSize / 2
    }

    fun applyZoom(zoomCenterBin: Float, zoomFactor: Float) {
        val totalBins = bufferSize / 2
        val minBins = 16
        val newBinCount = (_zoomBinCount.value / zoomFactor).toInt().coerceIn(minBins, totalBins)
        val windowStart = _zoomStartBin.value.toFloat()
        val windowEnd = windowStart + _zoomBinCount.value
        val center = zoomCenterBin.coerceIn(windowStart, windowEnd)
        val fraction = if (_zoomBinCount.value > 0) (center - windowStart) / _zoomBinCount.value else 0.5f
        var newStart = (center - fraction * newBinCount).toInt()
        newStart = newStart.coerceIn(0, totalBins - newBinCount)
        _zoomStartBin.value = newStart
        _zoomBinCount.value = newBinCount
    }

    fun applyPan(deltaBins: Float) {
        val totalBins = bufferSize / 2
        var newStart = (_zoomStartBin.value - deltaBins).toInt()
        newStart = newStart.coerceIn(0, totalBins - _zoomBinCount.value)
        _zoomStartBin.value = newStart
    }

    fun analyzeSpectrum() {
        if (_isAnalyzing.value) return
        val spectrum = _frozenSpectrum.value ?: _peakSpectrum.value
        if (spectrum.isEmpty()) return
        _isAnalyzing.value = true
        _aiAnalysis.value = null

        viewModelScope.launch {
            val peaks = spectrum.take(20).toList()
            val result = AIService.analyzeSpectrum(peaks)
            _aiAnalysis.value = result.getOrElse { "Analysis failed: ${it.message}" }
            _isAnalyzing.value = false
        }
    }

    private var audioRecord: AudioRecord? = null
    private val spectrumProcessor = SpectrumProcessor(sampleRate, bufferSize, waterfallHistorySize)

    fun startRecording(context: Context, audioSource: Int = MediaRecorder.AudioSource.MIC) {
        if (_isRecording.value) return

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val minBufSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        val actualBufSize = maxOf(minBufSize, bufferSize * 2)

        try {
            audioRecord = AudioRecord(
                audioSource,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                actualBufSize
            )

            audioRecord?.startRecording()
            _isRecording.value = true

            viewModelScope.launch(Dispatchers.IO) {
                val audioBuffer = ShortArray(bufferSize)
                var frameCount = 0L
                var processedFrames = 0L
                val startTime = System.nanoTime()

                while (isActive && _isRecording.value) {
                    val readResult = audioRecord?.read(audioBuffer, 0, bufferSize) ?: 0
                    if (readResult > 0) {
                        val frame = spectrumProcessor.process(
                            samples = audioBuffer,
                            gain = _gain.value,
                            sensitivity = _sensitivity.value
                        )

                        _waveform.value = frame.waveform
                        _rmsLevel.value = frame.rms
                        _peakDb.value = frame.peakDb
                        _dominantFreq.value = frame.dominantFreq
                        _waterfall.value = frame.waterfall
                        _peakSpectrum.value = frame.peakSpectrum

                        processedFrames++
                    } else {
                        delay(10)
                    }
                    frameCount++
                    val elapsed = (System.nanoTime() - startTime) / 1e9
                    if (elapsed > 0.5) {
                        val expectedFrames = (elapsed * sampleRate / bufferSize).toLong()
                        val pct = if (expectedFrames > 0) ((processedFrames.toDouble() / expectedFrames) * 100).toInt().coerceIn(0, 100) else 0
                        _throughputPercent.value = pct
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _isRecording.value = false
        }
    }

    fun stopRecording() {
        _isRecording.value = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }

    override fun onCleared() {
        super.onCleared()
        stopRecording()
    }
}
