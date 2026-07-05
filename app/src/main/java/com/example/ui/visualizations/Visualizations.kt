package com.example.ui.visualizations

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.example.ui.theme.getThemeColor
import com.example.ui.theme.getThemeHueOffset
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun WaveformCanvas(waveform: FloatArray, colorTheme: Int = 0) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val midY = height / 2f

        if (waveform.isEmpty()) return@Canvas

        val path = Path()
        val step = width / waveform.size.toFloat()

        path.moveTo(0f, midY)
        for (i in waveform.indices) {
            val x = i * step
            val y = midY - (waveform[i] * height * 0.4f)
            path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = getThemeColor(colorTheme),
            style = Stroke(width = 2f)
        )
    }
}

@Composable
fun SpectrumCanvas(
    spectrum: FloatArray,
    peakSpectrum: FloatArray = FloatArray(0),
    colorTheme: Int = 0,
    zoomStartBin: Int = 0,
    zoomBinCount: Int = spectrum.size,
    sampleRate: Int = 44100,
    bufferSize: Int = 1024
) {
    val themeColor = getThemeColor(colorTheme)
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        if (spectrum.isEmpty()) return@Canvas

        val start = zoomStartBin.coerceIn(0, spectrum.size)
        val end = (start + zoomBinCount).coerceAtMost(spectrum.size)
        val displayBins = end - start
        if (displayBins <= 0) return@Canvas

        val binWidth = width / displayBins.toFloat()
        val nyquist = sampleRate / 2f
        val binResolution = nyquist / (bufferSize / 2f)
        val gridColor = Color(0xFF1A3050)
        val labelColor = Color.White.copy(alpha = 0.35f)

        // Horizontal dB grid lines (-80, -60, -40, -20, 0 dB)
        val dbTicks = listOf(0f, 0.25f, 0.5f, 0.75f, 1f)
        dbTicks.forEach { fraction ->
            val y = height * (1 - fraction)
            drawLine(gridColor, start = Offset(0f, y), end = Offset(width, y), strokeWidth = 1f)
        }

        // Vertical frequency grid lines (roughly 5 ticks across the zoom window)
        val tickCount = 5
        for (i in 0..tickCount) {
            val bin = start + (i * displayBins / tickCount)
            val x = i * width / tickCount
            drawLine(gridColor, start = Offset(x, 0f), end = Offset(x, height), strokeWidth = 1f)
        }

        val path = Path()
        path.moveTo(0f, height)

        val peakPath = Path()

        for (i in 0 until displayBins) {
            val binIndex = start + i
            val x = i * binWidth
            val h = (spectrum[binIndex] * height).coerceIn(0f, height)
            val y = height - h
            path.lineTo(x, y)

            if (peakSpectrum.size > binIndex) {
                val ph = (peakSpectrum[binIndex] * height).coerceIn(0f, height)
                val py = height - ph
                if (i == 0) peakPath.moveTo(x, py) else peakPath.lineTo(x, py)
            }
        }
        path.lineTo(width, height)
        path.close()

        drawPath(path = path, color = themeColor.copy(alpha = 0.25f))
        drawPath(path = path, color = themeColor.copy(alpha = 0.7f), style = Stroke(width = 2f))
        if (peakSpectrum.isNotEmpty()) {
            drawPath(path = peakPath, color = Color.White.copy(alpha = 0.5f), style = Stroke(width = 1.5f))
        }
    }
}

@Composable
fun SDRWaterfallCanvas(
    waterfall: List<FloatArray>,
    colorTheme: Int = 0,
    zoomStartBin: Int = 0,
    zoomBinCount: Int = waterfall.firstOrNull()?.size ?: 0
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        if (waterfall.isEmpty()) return@Canvas

        val w = size.width
        val h = size.height

        val numRows = waterfall.size
        val maxRows = 60
        val rowH = h / maxRows

        val binCount = waterfall.first().size
        val start = zoomStartBin.coerceIn(0, binCount)
        val end = (start + zoomBinCount).coerceAtMost(binCount)
        val displayBins = end - start
        if (displayBins <= 0) return@Canvas

        val stepX = maxOf(1, displayBins / 120)
        val binW = (w / displayBins) * stepX
        val baseHue = getThemeHueOffset(colorTheme)
        val gridColor = Color(0xFF1A3050)

        // Frequency grid lines
        val tickCount = 5
        for (i in 0..tickCount) {
            val x = i * w / tickCount
            drawLine(gridColor, start = Offset(x, 0f), end = Offset(x, h), strokeWidth = 1f)
        }

        for (r in 0 until numRows) {
            val spectrum = waterfall[numRows - 1 - r]
            val y = r * rowH

            for (i in 0 until displayBins step stepX) {
                val binIndex = start + i
                val x = i * (w / displayBins)
                val intensity = spectrum[binIndex].coerceIn(0f, 1f)

                val hue = (baseHue - intensity * 240f) % 360f
                val color = Color.hsv(hue, 1f, intensity.coerceAtLeast(0.2f))

                drawRect(
                    color = color,
                    topLeft = Offset(x, y),
                    size = Size(binW + 1f, rowH + 1f)
                )
            }
        }
    }
}

@Composable
fun IQConstellationCanvas(waveform: FloatArray, colorTheme: Int = 0) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = min(cx, cy) * 0.9f

        drawLine(Color(0xFF49454F), start = Offset(0f, cy), end = Offset(size.width, cy))
        drawLine(Color(0xFF49454F), start = Offset(cx, 0f), end = Offset(cx, size.height))
        drawCircle(Color(0xFF49454F), radius = radius, style = Stroke(1f))
        drawCircle(Color(0xFF49454F), radius = radius * 0.5f, style = Stroke(1f))

        if (waveform.size > 1) {
            val points = mutableListOf<Offset>()
            val delay = 5
            for (i in delay until waveform.size step 2) {
                val iData = waveform[i]
                val qData = waveform[i - delay]
                points.add(Offset(cx + iData * radius, cy - qData * radius))
            }
            if (points.isNotEmpty()) {
                drawPoints(
                    points = points,
                    pointMode = PointMode.Points,
                    color = getThemeColor(colorTheme),
                    strokeWidth = 4f,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
fun Waterfall3DCanvas(
    waterfall: List<FloatArray>,
    colorTheme: Int = 0,
    zoomStartBin: Int = 0,
    zoomBinCount: Int = waterfall.firstOrNull()?.size ?: 0
) {
    var rotX by remember { mutableFloatStateOf(0.5f) }
    var rotY by remember { mutableFloatStateOf(-0.3f) }

    Canvas(modifier = Modifier
        .fillMaxSize()
        .pointerInput(Unit) {
            detectDragGestures { change, dragAmount ->
                change.consume()
                rotY -= dragAmount.x * 0.01f
                rotX -= dragAmount.y * 0.01f
                rotX = rotX.coerceIn(-1.5f, 1.5f)
            }
        }) {

        if (waterfall.isEmpty()) return@Canvas

        val cx = size.width / 2f
        val cy = size.height / 2f
        val scale = min(size.width, size.height) * 0.7f

        val historySize = waterfall.size
        val binCount = waterfall.first().size

        val start = zoomStartBin.coerceIn(0, binCount)
        val end = (start + zoomBinCount).coerceAtMost(binCount)
        val displayBins = end - start
        if (displayBins <= 0) return@Canvas

        val stepZ = 1
        val stepX = maxOf(1, displayBins / 100)
        val baseHue = getThemeHueOffset(colorTheme)

        for (h in 0 until historySize step stepZ) {
            val spectrum = waterfall[h]
            val zNorm = (h.toFloat() / historySize) - 0.5f
            val trueZ = zNorm

            val path = Path()
            var first = true

            for (i in 0 until displayBins step stepX) {
                val binIndex = start + i
                val xNorm = (i.toFloat() / displayBins) - 0.5f
                val yNorm = spectrum[binIndex] * 0.6f

                val x1 = xNorm * cos(rotY) - trueZ * sin(rotY)
                val z1 = xNorm * sin(rotY) + trueZ * cos(rotY)

                val y2 = yNorm * cos(rotX) - z1 * sin(rotX)

                val screenX = cx + x1 * scale
                val screenY = cy - y2 * scale + (scale * 0.2f)

                if (first) {
                    path.moveTo(screenX, screenY)
                    first = false
                } else {
                    path.lineTo(screenX, screenY)
                }
            }

            val intensity = (h.toFloat() / historySize)
            val hue = (baseHue - (intensity * 60f) + 360f) % 360f

            val baseColor = Color.hsv(hue, 0.8f, 0.4f + 0.6f * intensity)

            drawPath(
                path = path,
                color = baseColor,
                style = Stroke(width = 3f)
            )
        }
    }
}

@Composable
fun RadarSpectrumCanvas(
    spectrum: FloatArray,
    colorTheme: Int = 0,
    zoomStartBin: Int = 0,
    zoomBinCount: Int = spectrum.size
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val maxRadius = min(cx, cy) * 0.9f

        val start = zoomStartBin.coerceIn(0, spectrum.size)
        val end = (start + zoomBinCount).coerceAtMost(spectrum.size)
        val numBins = (end - start).coerceAtMost(256)

        for (i in 1..4) {
            val r = maxRadius * (i / 4f)
            drawCircle(Color(0xFF49454F).copy(alpha = 0.5f), radius = r, style = Stroke(1f))
        }

        if (numBins == 0) return@Canvas

        val path = Path()
        val path2 = Path()
        var first = true
        for (i in 0 until numBins) {
            val binIndex = start + i
            val angle = (i.toFloat() / numBins) * 2f * PI.toFloat() - PI.toFloat() / 2f
            val amplitude = spectrum[binIndex].coerceIn(0f, 1f)
            val r = maxRadius * (0.1f + amplitude * 0.9f)
            val x = cx + r * cos(angle)
            val y = cy + r * sin(angle)

            val angle2 = (-i.toFloat() / numBins) * 2f * PI.toFloat() - PI.toFloat() / 2f
            val x2 = cx + r * cos(angle2)
            val y2 = cy + r * sin(angle2)

            if (first) {
                path.moveTo(x, y)
                path2.moveTo(x2, y2)
                first = false
            } else {
                path.lineTo(x, y)
                path2.lineTo(x2, y2)
            }
        }

        path.close()
        path2.close()

        drawPath(path = path, color = getThemeColor(colorTheme, 0.4f))
        drawPath(path = path, color = getThemeColor(colorTheme), style = Stroke(width = 3f))

        drawPath(path = path2, color = getThemeColor(colorTheme, 0.4f))
        drawPath(path = path2, color = getThemeColor(colorTheme), style = Stroke(width = 3f))
    }
}

@Composable
fun PhaseSpaceCanvas(waveform: FloatArray, colorTheme: Int = 0) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = min(cx, cy) * 0.9f

        drawLine(Color(0xFF49454F), start = Offset(0f, cy), end = Offset(size.width, cy))
        drawLine(Color(0xFF49454F), start = Offset(cx, 0f), end = Offset(cx, size.height))
        drawCircle(Color(0xFF49454F), radius = radius, style = Stroke(1f))

        if (waveform.size > 2) {
            val points = mutableListOf<Offset>()
            val delay = 12
            for (i in delay until waveform.size) {
                val xData = waveform[i]
                val yData = waveform[i - delay]
                points.add(Offset(cx + xData * radius, cy - yData * radius))
            }
            if (points.isNotEmpty()) {
                drawPoints(
                    points = points,
                    pointMode = PointMode.Polygon,
                    color = getThemeColor(colorTheme, 0.6f),
                    strokeWidth = 2f,
                    cap = StrokeCap.Round
                )

                drawCircle(Color.White, radius = 6f, center = points.last())
            }
        }
    }
}
