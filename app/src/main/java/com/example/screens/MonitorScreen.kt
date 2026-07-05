package com.example.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.AudioAnalyzerViewModel
import com.example.VisualMode
import com.example.ui.theme.getThemeColor
import com.example.ui.visualizations.AudioOrbCanvas
import com.example.ui.visualizations.DNAHelixCanvas
import com.example.ui.visualizations.ElectricArcCanvas
import com.example.ui.visualizations.FireworksCanvas
import com.example.ui.visualizations.FountainCanvas
import com.example.ui.visualizations.GuitarStringsCanvas
import com.example.ui.visualizations.HexagonGridCanvas
import com.example.ui.visualizations.IQConstellationCanvas
import com.example.ui.visualizations.LiquidBlobCanvas
import com.example.ui.visualizations.LissajousCanvas
import com.example.ui.visualizations.MandalaCanvas
import com.example.ui.visualizations.MatrixRainCanvas
import com.example.ui.visualizations.NeonRingsCanvas
import com.example.ui.visualizations.ParticleSpectrumCanvas
import com.example.ui.visualizations.PulseHeartCanvas
import com.example.ui.visualizations.RadarSpectrumCanvas
import com.example.ui.visualizations.SDRWaterfallCanvas
import com.example.ui.visualizations.SoundRingsCanvas
import com.example.ui.visualizations.SpectrumCanvas
import com.example.ui.visualizations.PhaseSpaceCanvas
import com.example.ui.visualizations.StarfieldWarpCanvas
import com.example.ui.visualizations.VinylGrooveCanvas
import com.example.ui.visualizations.Waterfall3DCanvas
import com.example.ui.visualizations.WaveformCanvas
import kotlin.math.abs
import kotlin.random.Random

@Composable
fun MonitorScreen(
    viewModel: AudioAnalyzerViewModel,
    onNavigateToEngine: () -> Unit = {},
    hasRecordPermission: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isRecording by viewModel.isRecording.collectAsState()
    var currentMode by remember { mutableStateOf(VisualMode.WATERFALL_3D) }
    val colorTheme by viewModel.colorTheme.collectAsState()
    val peakDb by viewModel.peakDb.collectAsState()
    val dominantFreq by viewModel.dominantFreq.collectAsState()
    val throughput by viewModel.throughputPercent.collectAsState()
    val rms by viewModel.rmsLevel.collectAsState()
    val zoomStartBin by viewModel.zoomStartBin.collectAsState()
    val zoomBinCount by viewModel.zoomBinCount.collectAsState()
    val audioInputMode by viewModel.audioInputMode.collectAsState()

    val supportsZoomPan = currentMode in setOf(
        VisualMode.SPECTRUM,
        VisualMode.SDR_WATERFALL,
        VisualMode.RADAR_SPECTRUM
    )
    val isZoomed = zoomStartBin != 0 || zoomBinCount != viewModel.bufferSize / 2

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val themeColor = getThemeColor(colorTheme)
    val dbDisplay = if (peakDb > -100f) String.format("%.1f", peakDb) else "---"
    val freqDisplay = when {
        dominantFreq < 20f -> "---"
        dominantFreq >= 1000f -> String.format("%.1fk", dominantFreq / 1000f)
        else -> String.format("%.0f", dominantFreq)
    }
    val binResolution = viewModel.sampleRate.toFloat() / viewModel.bufferSize
    val zoomStartFreq = zoomStartBin * binResolution
    val zoomEndFreq = (zoomStartBin + zoomBinCount) * binResolution
    fun formatFreq(freq: Float): String = when {
        freq >= 1000f -> String.format("%.1fk", freq / 1000f)
        else -> String.format("%.0f", freq)
    }
    val zoomRangeDisplay = "${formatFreq(zoomStartFreq)}-${formatFreq(zoomEndFreq)} Hz"
    val rmsDb = if (rms > 0f) (20 * kotlin.math.log10(rms.toDouble())).toFloat() else -100f
    val meterLevel = ((rmsDb + 80f) / 80f).coerceIn(0f, 1f)

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(24.dp)) {
                        drawRect(color = Color(0xFF00121E), topLeft = Offset(4.dp.toPx(), 12.dp.toPx()), size = androidx.compose.ui.geometry.Size(4.dp.toPx(), 8.dp.toPx()))
                        drawRect(color = Color(0xFF00121E), topLeft = Offset(10.dp.toPx(), 6.dp.toPx()), size = androidx.compose.ui.geometry.Size(4.dp.toPx(), 14.dp.toPx()))
                        drawRect(color = Color(0xFF00121E), topLeft = Offset(16.dp.toPx(), 16.dp.toPx()), size = androidx.compose.ui.geometry.Size(4.dp.toPx(), 4.dp.toPx()))
                    }
                }
                Column {
                    Text("OmniWave v4.2", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
                    Text("SCIENTIFIC GRADE ANALYZER", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
            IconButton(onClick = onNavigateToEngine) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 0.dp)
                .background(Color.Black, shape = RoundedCornerShape(24.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .then(
                    if (supportsZoomPan) {
                        Modifier.pointerInput(zoomStartBin, zoomBinCount) {
                            detectTransformGestures { centroid, pan, zoom, _ ->
                                if (zoom != 1f) {
                                    val centerBin = zoomStartBin + (centroid.x / size.width) * zoomBinCount
                                    viewModel.applyZoom(centerBin, zoom)
                                }
                                if (pan.x != 0f) {
                                    val deltaBins = -(pan.x / size.width) * zoomBinCount
                                    viewModel.applyPan(deltaBins)
                                }
                            }
                        }
                    } else Modifier
                )
        ) {
            Canvas(modifier = Modifier.fillMaxSize().alpha(0.2f)) {
                val step = 40.dp.toPx()
                for (x in 0..size.width.toInt() step step.toInt()) {
                    drawLine(color = Color(0xFF1A3050), start = Offset(x.toFloat(), 0f), end = Offset(x.toFloat(), size.height))
                }
                for (y in 0..size.height.toInt() step step.toInt()) {
                    drawLine(color = Color(0xFF1A3050), start = Offset(0f, y.toFloat()), end = Offset(size.width, y.toFloat()))
                }
            }

            when (currentMode) {
                VisualMode.WAVEFORM -> {
                    val waveform by viewModel.waveform.collectAsState()
                    WaveformCanvas(waveform, colorTheme)
                }
                VisualMode.SPECTRUM -> {
                    val waterfall by viewModel.waterfall.collectAsState()
                    val peakSpectrum by viewModel.peakSpectrum.collectAsState()
                    val spectrum = waterfall.lastOrNull() ?: FloatArray(0)
                    SpectrumCanvas(
                        spectrum,
                        peakSpectrum,
                        colorTheme,
                        zoomStartBin = zoomStartBin,
                        zoomBinCount = zoomBinCount,
                        sampleRate = viewModel.sampleRate,
                        bufferSize = viewModel.bufferSize
                    )
                }
                VisualMode.WATERFALL_3D -> {
                    val waterfall by viewModel.waterfall.collectAsState()
                    Waterfall3DCanvas(waterfall, colorTheme, zoomStartBin, zoomBinCount)
                }
                VisualMode.SDR_WATERFALL -> {
                    val waterfall by viewModel.waterfall.collectAsState()
                    SDRWaterfallCanvas(waterfall, colorTheme, zoomStartBin, zoomBinCount)
                }
                VisualMode.IQ_PLOT -> {
                    val waveform by viewModel.waveform.collectAsState()
                    IQConstellationCanvas(waveform, colorTheme)
                }
                VisualMode.RADAR_SPECTRUM -> {
                    val waterfall by viewModel.waterfall.collectAsState()
                    val spectrum = waterfall.lastOrNull() ?: FloatArray(0)
                    RadarSpectrumCanvas(spectrum, colorTheme, zoomStartBin, zoomBinCount)
                }
                VisualMode.PHASE_SPACE -> {
                    val waveform by viewModel.waveform.collectAsState()
                    PhaseSpaceCanvas(waveform, colorTheme)
                }
                VisualMode.LISSAJOUS -> {
                    val waveform by viewModel.waveform.collectAsState()
                    LissajousCanvas(waveform, colorTheme)
                }
                VisualMode.PARTICLE_SPECTRUM -> {
                    val waterfall by viewModel.waterfall.collectAsState()
                    val spectrum = waterfall.lastOrNull() ?: FloatArray(0)
                    ParticleSpectrumCanvas(spectrum, colorTheme, zoomStartBin, zoomBinCount)
                }
                VisualMode.NEON_RINGS -> {
                    val waterfall by viewModel.waterfall.collectAsState()
                    val spectrum = waterfall.lastOrNull() ?: FloatArray(0)
                    NeonRingsCanvas(spectrum, colorTheme)
                }
                VisualMode.MATRIX_RAIN -> {
                    val waterfall by viewModel.waterfall.collectAsState()
                    val spectrum = waterfall.lastOrNull() ?: FloatArray(0)
                    MatrixRainCanvas(spectrum, colorTheme, zoomStartBin, zoomBinCount)
                }
                VisualMode.STARFIELD_WARP -> {
                    val waveform by viewModel.waveform.collectAsState()
                    StarfieldWarpCanvas(waveform, colorTheme)
                }
                VisualMode.VINYL_GROOVE -> {
                    val waveform by viewModel.waveform.collectAsState()
                    VinylGrooveCanvas(waveform, colorTheme)
                }
                VisualMode.LIQUID_BLOB -> {
                    val waveform by viewModel.waveform.collectAsState()
                    LiquidBlobCanvas(waveform, colorTheme)
                }
                VisualMode.SOUND_RINGS -> {
                    val waveform by viewModel.waveform.collectAsState()
                    SoundRingsCanvas(waveform, colorTheme)
                }
                VisualMode.FIREWORKS -> {
                    val waterfall by viewModel.waterfall.collectAsState()
                    val spectrum = waterfall.lastOrNull() ?: FloatArray(0)
                    FireworksCanvas(spectrum, colorTheme, zoomStartBin, zoomBinCount)
                }
                VisualMode.HEXAGON_GRID -> {
                    val waterfall by viewModel.waterfall.collectAsState()
                    val spectrum = waterfall.lastOrNull() ?: FloatArray(0)
                    HexagonGridCanvas(spectrum, colorTheme, zoomStartBin, zoomBinCount)
                }
                VisualMode.MANDALA -> {
                    val waterfall by viewModel.waterfall.collectAsState()
                    val spectrum = waterfall.lastOrNull() ?: FloatArray(0)
                    MandalaCanvas(spectrum, colorTheme)
                }
                VisualMode.PULSE_HEART -> {
                    val waveform by viewModel.waveform.collectAsState()
                    PulseHeartCanvas(waveform, colorTheme)
                }
                VisualMode.DNA_HELIX -> {
                    val waveform by viewModel.waveform.collectAsState()
                    DNAHelixCanvas(waveform, colorTheme)
                }
                VisualMode.ELECTRIC_ARC -> {
                    val waterfall by viewModel.waterfall.collectAsState()
                    val spectrum = waterfall.lastOrNull() ?: FloatArray(0)
                    ElectricArcCanvas(spectrum, colorTheme, zoomStartBin, zoomBinCount)
                }
                VisualMode.FOUNTAIN -> {
                    val waterfall by viewModel.waterfall.collectAsState()
                    val spectrum = waterfall.lastOrNull() ?: FloatArray(0)
                    FountainCanvas(spectrum, colorTheme, zoomStartBin, zoomBinCount)
                }
                VisualMode.GUITAR_STRINGS -> {
                    val waveform by viewModel.waveform.collectAsState()
                    GuitarStringsCanvas(waveform, colorTheme)
                }
                VisualMode.AUDIO_ORB -> {
                    val waveform by viewModel.waveform.collectAsState()
                    AudioOrbCanvas(waveform, colorTheme)
                }
            }

            Column(modifier = Modifier.align(Alignment.TopStart).padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.7f), shape = RoundedCornerShape(6.dp))
                        .border(1.dp, themeColor.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .scale(if (isRecording) pulseScale else 1f)
                            .alpha(if (isRecording) pulseAlpha else 0.3f)
                            .background(if (isRecording) Color(0xFF00E676) else Color.Gray, shape = CircleShape)
                    )
                    Text(
                        if (isRecording) "LIVE" else "PAUSED",
                        color = if (isRecording) Color(0xFF00E676) else Color.Gray,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text("44.1kHz", color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.7f), shape = RoundedCornerShape(6.dp))
                            .border(1.dp, themeColor.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("PEAK: $freqDisplay Hz", color = themeColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.7f), shape = RoundedCornerShape(6.dp))
                            .border(1.dp, themeColor.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            if (isZoomed) zoomRangeDisplay else "1024-pt FFT",
                            color = if (isZoomed) themeColor else Color.White.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isZoomed) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
                if (isZoomed) {
                    Box(
                        modifier = Modifier
                            .background(themeColor.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp))
                            .border(1.dp, themeColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .clickable { viewModel.resetZoom() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "RESET ZOOM",
                            color = themeColor,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                if (audioInputMode == AudioAnalyzerViewModel.AudioInputMode.MICROPHONE && !hasRecordPermission) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF332600), shape = RoundedCornerShape(6.dp))
                            .border(1.dp, Color(0xFF665200), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "MIC PERMISSION NEEDED",
                            color = Color.Yellow,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Column(modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)) {
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height(120.dp)
                        .background(Color.Black.copy(alpha = 0.7f), shape = RoundedCornerShape(6.dp))
                        .border(1.dp, themeColor.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                ) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(3.dp)) {
                        val barH = size.height * meterLevel
                        val brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFFFF1744), Color(0xFFFFEA00), Color(0xFF00E676)),
                            startY = 0f,
                            endY = size.height
                        )
                        drawRect(
                            brush = brush,
                            topLeft = Offset(0f, size.height - barH),
                            size = androidx.compose.ui.geometry.Size(size.width, barH)
                        )
                        for (i in 1..4) {
                            val y = size.height * (i / 5f)
                            drawLine(Color.White.copy(alpha = 0.2f), start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 1f)
                        }
                    }
                }
                Text("dB", color = Color.White.copy(alpha = 0.4f), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 2.dp))
            }

            Column(modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp), horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        dbDisplay,
                        color = when {
                            peakDb > -6f -> Color(0xFFFF1744)
                            peakDb > -20f -> Color(0xFFFFEA00)
                            else -> themeColor
                        },
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Light
                    )
                    Text("dB", color = themeColor, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 4.dp, start = 4.dp))
                }
                Text("PEAK AMPLITUDE", color = Color.White.copy(alpha = 0.4f), style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-24).dp)
                .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .padding(top = 24.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("VISUALIZATION MODES", style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp), color = MaterialTheme.colorScheme.onBackground)
                Text("${currentMode.title}", style = MaterialTheme.typography.labelSmall, color = themeColor, fontWeight = FontWeight.Bold)
            }

            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                VisualMode.entries.forEach { mode ->
                    val isSelected = currentMode == mode
                    val bgColor = if (isSelected) themeColor else Color(0xFF49454F)
                    val contentColor = if (isSelected) Color.Black else MaterialTheme.colorScheme.onBackground

                    Box(
                        modifier = Modifier
                            .width(100.dp)
                            .background(bgColor, shape = RoundedCornerShape(16.dp))
                            .clickable { currentMode = mode }
                            .then(
                                if (isSelected) Modifier.border(2.dp, themeColor, RoundedCornerShape(16.dp))
                                else Modifier.border(1.dp, Color(0xFF49454F), RoundedCornerShape(16.dp))
                            )
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Canvas(modifier = Modifier.size(20.dp)) {
                                when (mode) {
                                    VisualMode.WAVEFORM, VisualMode.GUITAR_STRINGS -> {
                                        val p = androidx.compose.ui.graphics.Path()
                                        p.moveTo(0f, size.height / 2)
                                        for (i in 0..20) {
                                            val x = i * size.width / 20f
                                            val y = size.height / 2 + kotlin.math.sin(i * 0.8f) * size.height * 0.35f
                                            p.lineTo(x, y)
                                        }
                                        drawPath(p, color = contentColor, style = Stroke(2f))
                                    }
                                    VisualMode.SPECTRUM, VisualMode.RADAR_SPECTRUM, VisualMode.PARTICLE_SPECTRUM, VisualMode.FOUNTAIN -> {
                                        val bars = listOf(0.3f, 0.7f, 1f, 0.5f, 0.8f)
                                        val bw = size.width / (bars.size * 2f)
                                        bars.forEachIndexed { i, h ->
                                            drawRect(color = contentColor, topLeft = Offset(i * bw * 2, size.height * (1 - h)), size = androidx.compose.ui.geometry.Size(bw, size.height * h))
                                        }
                                    }
                                    VisualMode.WATERFALL_3D, VisualMode.SDR_WATERFALL -> {
                                        for (r in 0..3) {
                                            drawLine(contentColor, Offset(0f, r * size.height / 4), Offset(size.width, r * size.height / 4), strokeWidth = 1.5f)
                                        }
                                    }
                                    VisualMode.IQ_PLOT, VisualMode.PHASE_SPACE, VisualMode.LISSAJOUS -> {
                                        drawCircle(color = contentColor, radius = size.width * 0.35f, style = Stroke(1.5f))
                                        drawCircle(color = contentColor, radius = 3f)
                                    }
                                    VisualMode.NEON_RINGS, VisualMode.SOUND_RINGS, VisualMode.MANDALA, VisualMode.AUDIO_ORB -> {
                                        drawCircle(contentColor, radius = size.width * 0.35f, style = Stroke(1.5f))
                                        drawCircle(contentColor, radius = size.width * 0.2f, style = Stroke(1.5f))
                                    }
                                    VisualMode.MATRIX_RAIN, VisualMode.HEXAGON_GRID -> {
                                        for (r in 0..2) {
                                            for (c in 0..2) {
                                                drawRect(contentColor, topLeft = Offset(c * size.width / 3f, r * size.height / 3f), size = androidx.compose.ui.geometry.Size(size.width / 4f, size.height / 4f))
                                            }
                                        }
                                    }
                                    VisualMode.STARFIELD_WARP -> {
                                        repeat(8) {
                                            drawCircle(contentColor, radius = 1.5f, center = Offset(Random.nextFloat() * size.width, Random.nextFloat() * size.height))
                                        }
                                    }
                                    VisualMode.VINYL_GROOVE -> {
                                        drawCircle(contentColor, radius = size.width * 0.4f, style = Stroke(1.5f))
                                        drawCircle(contentColor, radius = size.width * 0.15f)
                                    }
                                    VisualMode.LIQUID_BLOB -> {
                                        drawCircle(contentColor, radius = size.width * 0.35f)
                                    }
                                    VisualMode.FIREWORKS -> {
                                        repeat(5) {
                                            drawCircle(contentColor, radius = 2f, center = Offset(Random.nextFloat() * size.width, Random.nextFloat() * size.height * 0.5f))
                                        }
                                    }
                                    VisualMode.PULSE_HEART -> {
                                        val p = androidx.compose.ui.graphics.Path()
                                        p.moveTo(size.width / 2, size.height * 0.3f)
                                        p.cubicTo(size.width * 0.1f, 0f, 0f, size.height * 0.6f, size.width / 2, size.height * 0.9f)
                                        p.cubicTo(size.width, size.height * 0.6f, size.width * 0.9f, 0f, size.width / 2, size.height * 0.3f)
                                        p.close()
                                        drawPath(p, contentColor, style = Stroke(2f))
                                    }
                                    VisualMode.DNA_HELIX -> {
                                        drawLine(contentColor, Offset(0f, size.height * 0.2f), Offset(size.width, size.height * 0.8f), strokeWidth = 2f)
                                        drawLine(contentColor, Offset(size.width, size.height * 0.2f), Offset(0f, size.height * 0.8f), strokeWidth = 2f)
                                    }
                                    VisualMode.ELECTRIC_ARC -> {
                                        val p = androidx.compose.ui.graphics.Path()
                                        p.moveTo(0f, size.height * 0.7f)
                                        p.lineTo(size.width * 0.3f, size.height * 0.3f)
                                        p.lineTo(size.width * 0.6f, size.height * 0.6f)
                                        p.lineTo(size.width, size.height * 0.2f)
                                        drawPath(p, contentColor, style = Stroke(2f))
                                    }
                                }
                            }
                            Text(mode.title.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = contentColor, maxLines = 1)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2B2930), shape = CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            if (isRecording) Color(0xFFFF1744) else MaterialTheme.colorScheme.secondary,
                            shape = CircleShape
                        )
                        .clickable { viewModel.toggleRecording(context) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play/Stop",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("THROUGHPUT", style = MaterialTheme.typography.labelSmall, color = themeColor)
                        Text("$throughput%", style = MaterialTheme.typography.labelSmall, color = themeColor, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(6.dp).background(Color(0xFF49454F), shape = CircleShape)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(throughput / 100f)
                                .fillMaxHeight()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            themeColor.copy(alpha = 0.6f),
                                            themeColor
                                        )
                                    ),
                                    shape = CircleShape
                                )
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .scale(if (isRecording) pulseScale else 1f)
                            .alpha(if (isRecording) pulseAlpha else 0.3f)
                            .background(if (isRecording) Color.Red else Color.Gray, shape = CircleShape)
                    )
                }
            }
        }
    }
}
