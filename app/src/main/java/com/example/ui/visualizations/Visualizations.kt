package com.example.ui.visualizations

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.example.ui.theme.getThemeColor
import com.example.ui.theme.getThemeHueOffset
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

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

// MARK: - Bonus visualizations

@Composable
fun LissajousCanvas(waveform: FloatArray, colorTheme: Int = 0) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = min(cx, cy) * 0.85f

        drawCircle(Color(0xFF49454F).copy(alpha = 0.5f), radius = radius, style = Stroke(1f))
        drawLine(Color(0xFF49454F).copy(alpha = 0.3f), start = Offset(cx - radius, cy), end = Offset(cx + radius, cy))
        drawLine(Color(0xFF49454F).copy(alpha = 0.3f), start = Offset(cx, cy - radius), end = Offset(cx, cy + radius))

        if (waveform.size > 10) {
            val points = mutableListOf<Offset>()
            val delay = 8
            for (i in delay until waveform.size step 2) {
                val x = waveform[i]
                val y = waveform[i - delay]
                points.add(Offset(cx + x * radius * 0.9f, cy - y * radius * 0.9f))
            }
            if (points.size > 1) {
                val path = Path()
                path.moveTo(points.first().x, points.first().y)
                points.drop(1).forEach { path.lineTo(it.x, it.y) }
                drawPath(path, color = getThemeColor(colorTheme, 0.3f), style = Stroke(width = 3f))
                drawPoints(points, PointMode.Points, getThemeColor(colorTheme), strokeWidth = 3f, cap = StrokeCap.Round)
            }
        }
    }
}

@Composable
fun ParticleSpectrumCanvas(
    spectrum: FloatArray,
    colorTheme: Int = 0,
    zoomStartBin: Int = 0,
    zoomBinCount: Int = spectrum.size
) {
    val themeColor = getThemeColor(colorTheme)
    val particles = remember { mutableStateListOf<Particle>() }
    var tick by remember { mutableIntStateOf(0) }

    LaunchedEffect(spectrum.contentToString(), tick) {
        delay(16)
        tick++
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        if (spectrum.isEmpty()) return@Canvas
        val start = zoomStartBin.coerceIn(0, spectrum.size)
        val end = (start + zoomBinCount).coerceAtMost(spectrum.size)
        val bins = end - start
        if (bins <= 0) return@Canvas
        val barW = width / bins

        // Spawn particles on strong bars
        if (tick % 3 == 0) {
            repeat(bins) { i ->
                val amp = spectrum[start + i]
                if (amp > 0.6f && Random.nextFloat() < amp) {
                    repeat((amp * 3).toInt()) {
                        particles.add(
                            Particle(
                                x = i * barW + Random.nextFloat() * barW,
                                y = height,
                                vx = Random.nextFloat() * 4f - 2f,
                                vy = -Random.nextFloat() * 8f * amp - 2f,
                                life = 1f,
                                decay = 0.02f + Random.nextFloat() * 0.03f
                            )
                        )
                    }
                }
            }
        }

        // Draw bars
        for (i in 0 until bins) {
            val amp = spectrum[start + i]
            val h = amp * height
            val x = i * barW
            drawRect(
                color = themeColor.copy(alpha = 0.25f),
                topLeft = Offset(x, height - h),
                size = Size(barW - 1f, h)
            )
        }

        // Update & draw particles
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.x += p.vx
            p.y += p.vy
            p.vy += 0.15f // gravity
            p.life -= p.decay
            if (p.life <= 0f) {
                iterator.remove()
            } else {
                drawCircle(
                    color = themeColor.copy(alpha = p.life),
                    radius = 3f * p.life,
                    center = Offset(p.x, p.y)
                )
            }
        }
    }
}

private data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var life: Float,
    var decay: Float
)

@Composable
fun NeonRingsCanvas(spectrum: FloatArray, colorTheme: Int = 0) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val maxR = min(cx, cy) * 0.9f
        val themeColor = getThemeColor(colorTheme)

        if (spectrum.isEmpty()) return@Canvas

        val rings = 8
        for (i in 0 until rings) {
            val bin = (i * spectrum.size / rings).coerceIn(0, spectrum.size - 1)
            val amp = spectrum[bin]
            val baseR = maxR * (i + 1) / rings
            val r = baseR * (0.5f + amp * 0.7f)
            val stroke = 2f + amp * 6f
            drawCircle(
                color = themeColor.copy(alpha = 0.1f + amp * 0.5f),
                radius = r,
                style = Stroke(width = stroke)
            )
        }
    }
}

@Composable
fun MatrixRainCanvas(
    spectrum: FloatArray,
    colorTheme: Int = 0,
    zoomStartBin: Int = 0,
    zoomBinCount: Int = spectrum.size
) {
    val textMeasurer = rememberTextMeasurer()
    val chars = remember { "0123456789ABCDEFΩπ∑√λθψξ".toList() }
    val drops = remember { mutableStateListOf<MatrixDrop>() }
    var tick by remember { mutableIntStateOf(0) }

    LaunchedEffect(spectrum.contentToString(), tick) {
        delay(30)
        tick++
    }

    val themeColor = getThemeColor(colorTheme)
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val cols = 16
        val colW = width / cols

        if (spectrum.isEmpty()) return@Canvas
        val start = zoomStartBin.coerceIn(0, spectrum.size)
        val end = (start + zoomBinCount).coerceAtMost(spectrum.size)
        val bins = end - start

        // Spawn/update drops based on spectrum energy
        if (tick % 2 == 0) {
            repeat(cols) { col ->
                val binIndex = start + (col * bins / cols).coerceIn(0, bins - 1)
                val energy = spectrum[binIndex]
                if (energy > 0.4f && Random.nextFloat() < energy * 0.3f) {
                    drops.add(
                        MatrixDrop(
                            x = col * colW + colW / 2,
                            y = 0f,
                            speed = 4f + energy * 12f,
                            char = chars.random(),
                            life = 1f
                        )
                    )
                }
            }
        }

        val iterator = drops.iterator()
        while (iterator.hasNext()) {
            val drop = iterator.next()
            drop.y += drop.speed
            if (drop.y > height) iterator.remove()
            else {
                drawText(
                    textMeasurer = textMeasurer,
                    text = drop.char.toString(),
                    topLeft = Offset(drop.x - 8f, drop.y),
                    style = TextStyle(
                        color = themeColor.copy(alpha = 0.4f + drop.life * 0.6f),
                        fontSize = 14.sp
                    )
                )
            }
        }
    }
}

private data class MatrixDrop(
    var x: Float,
    var y: Float,
    var speed: Float,
    val char: Char,
    var life: Float
)

@Composable
fun StarfieldWarpCanvas(waveform: FloatArray, colorTheme: Int = 0) {
    val stars = remember { List(200) { StarfieldStar() } }
    var tick by remember { mutableIntStateOf(0) }

    LaunchedEffect(waveform.contentToString(), tick) {
        delay(16)
        tick++
    }

    val themeColor = getThemeColor(colorTheme)
    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val speedBase = if (waveform.isEmpty()) 0.02f else (waveform.map { abs(it) }.average().toFloat() * 0.15f + 0.02f).coerceIn(0.02f, 0.35f)

        stars.forEach { star ->
            star.z -= speedBase * star.speed
            if (star.z <= 0f) {
                star.x = Random.nextFloat() * 2f - 1f
                star.y = Random.nextFloat() * 2f - 1f
                star.z = 1f
            }
            val sx = cx + (star.x / star.z) * cx
            val sy = cy + (star.y / star.z) * cy
            val radius = (1f - star.z) * 5f + 1f
            val alpha = (1f - star.z).coerceIn(0f, 1f)
            if (sx in 0f..size.width && sy in 0f..size.height) {
                drawCircle(themeColor.copy(alpha = alpha), radius = radius, center = Offset(sx, sy))
            }
        }
    }
}

private data class StarfieldStar(
    var x: Float = Random.nextFloat() * 2f - 1f,
    var y: Float = Random.nextFloat() * 2f - 1f,
    var z: Float = Random.nextFloat(),
    val speed: Float = 0.5f + Random.nextFloat()
)

@Composable
fun VinylGrooveCanvas(waveform: FloatArray, colorTheme: Int = 0) {
    var rotation by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(waveform.contentToString()) {
        rotation += 5f
    }
    val themeColor = getThemeColor(colorTheme)
    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = min(cx, cy) * 0.85f

        rotate(rotation, pivot = Offset(cx, cy)) {
            drawCircle(Color.Black, radius = radius)
            drawCircle(themeColor.copy(alpha = 0.6f), radius = radius, style = Stroke(width = 2f))
            drawCircle(themeColor.copy(alpha = 0.4f), radius = radius * 0.35f)
            drawCircle(Color.Black, radius = radius * 0.12f)

            // Groove waveform
            if (waveform.isNotEmpty()) {
                val path = Path()
                for (i in 0..360 step 2) {
                    val angle = i * PI.toFloat() / 180f
                    val sampleIndex = (i * waveform.size / 360).coerceIn(0, waveform.size - 1)
                    val amp = waveform[sampleIndex] * radius * 0.25f
                    val r = radius * 0.6f + amp
                    val x = cx + cos(angle) * r
                    val y = cy + sin(angle) * r
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                drawPath(path, color = themeColor.copy(alpha = 0.5f), style = Stroke(width = 2f))
            }
        }
    }
}

@Composable
fun LiquidBlobCanvas(waveform: FloatArray, colorTheme: Int = 0) {
    val themeColor = getThemeColor(colorTheme)
    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val baseRadius = min(cx, cy) * 0.5f

        if (waveform.isEmpty()) {
            drawCircle(themeColor.copy(alpha = 0.3f), radius = baseRadius)
            return@Canvas
        }

        val path = Path()
        val points = 120
        for (i in 0..points) {
            val angle = i * 2f * PI.toFloat() / points
            val sampleIndex = (i * waveform.size / points).coerceIn(0, waveform.size - 1)
            val distortion = 1f + waveform[sampleIndex] * 0.5f
            val r = baseRadius * distortion.coerceIn(0.3f, 1.8f)
            val x = cx + cos(angle) * r
            val y = cy + sin(angle) * r
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        drawPath(path, color = themeColor.copy(alpha = 0.25f))
        drawPath(path, color = themeColor.copy(alpha = 0.8f), style = Stroke(width = 3f))
    }
}

@Composable
fun SoundRingsCanvas(waveform: FloatArray, colorTheme: Int = 0) {
    val rings = remember { mutableStateListOf<SoundRing>() }
    var tick by remember { mutableIntStateOf(0) }

    LaunchedEffect(waveform.contentToString(), tick) {
        delay(40)
        tick++
    }

    val themeColor = getThemeColor(colorTheme)
    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f

        val rms = if (waveform.isEmpty()) 0f else waveform.map { it * it }.average().toFloat().let { sqrt(it) }
        if (tick % 5 == 0 && rms > 0.15f) {
            rings.add(SoundRing(cx, cy, 10f, rms * 4f))
        }

        val iterator = rings.iterator()
        while (iterator.hasNext()) {
            val ring = iterator.next()
            ring.radius += ring.speed
            ring.alpha -= 0.015f
            if (ring.alpha <= 0f) iterator.remove()
            else {
                drawCircle(
                    color = themeColor.copy(alpha = ring.alpha),
                    radius = ring.radius,
                    style = Stroke(width = 3f)
                )
            }
        }
    }
}

private data class SoundRing(
    val cx: Float,
    val cy: Float,
    var radius: Float,
    val speed: Float,
    var alpha: Float = 1f
)

@Composable
fun FireworksCanvas(
    spectrum: FloatArray,
    colorTheme: Int = 0,
    zoomStartBin: Int = 0,
    zoomBinCount: Int = spectrum.size
) {
    val fireworks = remember { mutableStateListOf<Firework>() }
    var tick by remember { mutableIntStateOf(0) }

    LaunchedEffect(spectrum.contentToString(), tick) {
        delay(16)
        tick++
    }

    val themeColor = getThemeColor(colorTheme)
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        if (spectrum.isEmpty()) return@Canvas
        val start = zoomStartBin.coerceIn(0, spectrum.size)
        val end = (start + zoomBinCount).coerceAtMost(spectrum.size)
        val bins = end - start

        if (tick % 10 == 0) {
            repeat(bins) { i ->
                val amp = spectrum[start + i]
                if (amp > 0.75f && Random.nextFloat() < amp * 0.2f) {
                    val x = i * width / bins + width / (bins * 2f)
                    fireworks.add(Firework(x, height, themeColor))
                }
            }
        }

        val it = fireworks.iterator()
        while (it.hasNext()) {
            val fw = it.next()
            fw.y -= fw.vy
            fw.vy -= 0.2f
            fw.alpha -= 0.01f
            if (fw.vy <= 0f || fw.alpha <= 0f) {
                // Explode particles
                repeat(12) { _ ->
                    fw.particles.add(
                        FireworkParticle(
                            x = fw.x,
                            y = fw.y,
                            vx = Random.nextFloat() * 10f - 5f,
                            vy = Random.nextFloat() * 10f - 5f,
                            color = fw.color,
                            life = 1f
                        )
                    )
                }
                it.remove()
            } else {
                drawCircle(fw.color.copy(alpha = fw.alpha), radius = 4f, center = Offset(fw.x, fw.y))
            }
        }

        val pit = fwParticlesIterator(fireworks)
        while (pit.hasNext()) {
            val p = pit.next()
            p.x += p.vx
            p.y += p.vy
            p.vy += 0.15f
            p.life -= 0.02f
            if (p.life <= 0f) pit.remove()
            else drawCircle(p.color.copy(alpha = p.life), radius = 3f * p.life, center = Offset(p.x, p.y))
        }
    }
}

private fun fwParticlesIterator(fireworks: MutableList<Firework>): MutableIterator<FireworkParticle> {
    return fireworks.flatMap { it.particles }.toMutableList().iterator()
}

private data class Firework(
    var x: Float,
    var y: Float,
    val color: Color,
    var vy: Float = 12f,
    var alpha: Float = 1f,
    val particles: MutableList<FireworkParticle> = mutableStateListOf()
)

private data class FireworkParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val color: Color,
    var life: Float
)

@Composable
fun HexagonGridCanvas(
    spectrum: FloatArray,
    colorTheme: Int = 0,
    zoomStartBin: Int = 0,
    zoomBinCount: Int = spectrum.size
) {
    val baseHue = getThemeHueOffset(colorTheme)
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        if (spectrum.isEmpty()) return@Canvas
        val start = zoomStartBin.coerceIn(0, spectrum.size)
        val end = (start + zoomBinCount).coerceAtMost(spectrum.size)
        val bins = end - start

        val hexRadius = 24f
        val hexWidth = hexRadius * 2f
        val hexHeight = sqrt(3f) * hexRadius
        val cols = (width / (hexWidth * 0.75f)).toInt() + 2
        val rows = (height / hexHeight).toInt() + 2

        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val binIndex = start + ((col + row * cols) % max(bins, 1))
                val amp = if (binIndex < end) spectrum[binIndex] else 0f
                val cx = col * hexWidth * 0.75f
                val cy = row * hexHeight + (if (col % 2 == 1) hexHeight / 2 else 0f)
                val hue = (baseHue + amp * 120f) % 360f
                val color = Color.hsv(hue, 0.8f, 0.2f + amp * 0.8f)
                drawHexagon(cx, cy, hexRadius * (0.6f + amp * 0.5f), color)
            }
        }
    }
}

private fun DrawScope.drawHexagon(cx: Float, cy: Float, radius: Float, color: Color) {
    val path = Path()
    for (i in 0..6) {
        val angle = i * PI.toFloat() / 3f - PI.toFloat() / 2f
        val x = cx + cos(angle) * radius
        val y = cy + sin(angle) * radius
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color = color)
}

@Composable
fun MandalaCanvas(spectrum: FloatArray, colorTheme: Int = 0) {
    val themeColor = getThemeColor(colorTheme)
    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val maxR = min(cx, cy) * 0.9f

        if (spectrum.isEmpty()) return@Canvas

        val petals = 12
        repeat(petals) { p ->
            val angle = p * 2f * PI.toFloat() / petals
            rotate(angle * 180f / PI.toFloat(), pivot = Offset(cx, cy)) {
                val path = Path()
                val points = 40
                for (i in 0..points) {
                    val t = i / points.toFloat()
                    val bin = (t * spectrum.size).toInt().coerceIn(0, spectrum.size - 1)
                    val amp = spectrum[bin]
                    val r = maxR * t * (0.2f + amp)
                    val x = cx + r
                    val y = cy + sin(t * PI.toFloat() * 4f) * r * 0.3f
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path, color = themeColor.copy(alpha = 0.15f))
                drawPath(path, color = themeColor.copy(alpha = 0.6f), style = Stroke(width = 1.5f))
            }
        }
    }
}

@Composable
fun PulseHeartCanvas(waveform: FloatArray, colorTheme: Int = 0) {
    val themeColor = getThemeColor(colorTheme)
    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val scale = min(cx, cy) * 0.006f
        val rms = if (waveform.isEmpty()) 0f else waveform.map { it * it }.average().toFloat().let { sqrt(it) }
        val pulse = 1f + rms * 0.6f

        withTransform({
            translate(cx, cy)
            scale(scale * pulse, scale * pulse)
        }) {
            val path = Path()
            for (t in 0..100) {
                val angle = t / 100f * 2f * PI.toFloat()
                val x = 16 * sin(angle).pow(3)
                val y = -(13 * cos(angle) - 5 * cos(2 * angle) - 2 * cos(3 * angle) - cos(4 * angle))
                if (t == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawPath(path, color = themeColor.copy(alpha = 0.3f))
            drawPath(path, color = themeColor.copy(alpha = 0.9f), style = Stroke(width = 2f))
        }
    }
}

@Composable
fun DNAHelixCanvas(waveform: FloatArray, colorTheme: Int = 0) {
    val themeColor = getThemeColor(colorTheme)
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val midX = width / 2f
        val amplitude = min(width, height) * 0.25f

        if (waveform.isEmpty()) return@Canvas

        val strands = 2
        val points = 80
        val radius = 6f
        for (s in 0 until strands) {
            val pointsList = mutableListOf<Offset>()
            for (i in 0 until points) {
                val t = i / points.toFloat()
                val y = height * 0.1f + t * height * 0.8f
                val sampleIndex = (i * waveform.size / points).coerceIn(0, waveform.size - 1)
                val phase = t * 4f * PI.toFloat() + s * PI.toFloat() + waveform[sampleIndex] * 2f
                val x = midX + sin(phase) * amplitude
                pointsList.add(Offset(x, y))
            }
            drawPoints(pointsList, PointMode.Polygon, themeColor.copy(alpha = 0.6f), strokeWidth = 3f, cap = StrokeCap.Round)
            pointsList.forEach { drawCircle(themeColor, radius = radius, center = it) }
        }

        // Connecting rungs
        for (i in 0 until points step 3) {
            val t = i / points.toFloat()
            val y = height * 0.1f + t * height * 0.8f
            val sampleIndex = (i * waveform.size / points).coerceIn(0, waveform.size - 1)
            val phase1 = t * 4f * PI.toFloat() + waveform[sampleIndex] * 2f
            val phase2 = phase1 + PI.toFloat()
            val x1 = midX + sin(phase1) * amplitude
            val x2 = midX + sin(phase2) * amplitude
            drawLine(themeColor.copy(alpha = 0.3f), Offset(x1, y), Offset(x2, y), strokeWidth = 1f)
        }
    }
}

@Composable
fun ElectricArcCanvas(
    spectrum: FloatArray,
    colorTheme: Int = 0,
    zoomStartBin: Int = 0,
    zoomBinCount: Int = spectrum.size
) {
    val themeColor = getThemeColor(colorTheme)
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        if (spectrum.isEmpty()) return@Canvas
        val start = zoomStartBin.coerceIn(0, spectrum.size)
        val end = (start + zoomBinCount).coerceAtMost(spectrum.size)
        val bins = end - start
        if (bins <= 0) return@Canvas

        val peaks = mutableListOf<Pair<Float, Float>>()
        for (i in 0 until bins) {
            val amp = spectrum[start + i]
            if (amp > 0.6f) {
                peaks.add(i * width / bins to height * (1 - amp))
            }
        }

        if (peaks.size > 1) {
            for (i in 0 until peaks.size - 1) {
                val p1 = peaks[i]
                val p2 = peaks[i + 1]
                val path = Path()
                path.moveTo(p1.first, p1.second)
                val midX = (p1.first + p2.first) / 2f
                val midY = (p1.second + p2.second) / 2f
                val jitter = Random.nextFloat() * 40f - 20f
                path.lineTo(midX + jitter, midY + jitter)
                path.lineTo(p2.first, p2.second)
                drawPath(path, color = themeColor.copy(alpha = 0.7f), style = Stroke(width = 2f))
            }
        }
    }
}

@Composable
fun FountainCanvas(
    spectrum: FloatArray,
    colorTheme: Int = 0,
    zoomStartBin: Int = 0,
    zoomBinCount: Int = spectrum.size
) {
    val particles = remember { mutableStateListOf<FountainParticle>() }
    var tick by remember { mutableIntStateOf(0) }

    LaunchedEffect(spectrum.contentToString(), tick) {
        delay(12)
        tick++
    }

    val themeColor = getThemeColor(colorTheme)
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        if (spectrum.isEmpty()) return@Canvas
        val start = zoomStartBin.coerceIn(0, spectrum.size)
        val end = (start + zoomBinCount).coerceAtMost(spectrum.size)
        val bins = end - start
        if (bins <= 0) return@Canvas
        val barW = width / bins

        for (i in 0 until bins) {
            val amp = spectrum[start + i]
            if (amp > 0.3f && Random.nextFloat() < amp * 0.5f) {
                repeat((amp * 3).toInt()) {
                    particles.add(
                        FountainParticle(
                            x = i * barW + barW / 2,
                            y = height - amp * height,
                            vx = Random.nextFloat() * 6f - 3f,
                            vy = -Random.nextFloat() * 12f * amp - 4f,
                            color = themeColor.copy(alpha = 0.6f + Random.nextFloat() * 0.4f),
                            life = 1f
                        )
                    )
                }
            }
        }

        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.x += p.vx
            p.y += p.vy
            p.vy += 0.25f
            p.life -= 0.015f
            if (p.life <= 0f || p.y > height) iterator.remove()
            else {
                drawCircle(p.color.copy(alpha = p.life), radius = 4f * p.life, center = Offset(p.x, p.y))
            }
        }
    }
}

private data class FountainParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val color: Color,
    var life: Float
)

@Composable
fun GuitarStringsCanvas(waveform: FloatArray, colorTheme: Int = 0) {
    val themeColor = getThemeColor(colorTheme)
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val strings = 6
        val stringY = { index: Int -> height * (0.15f + index * 0.14f) }

        for (s in 0 until strings) {
            val path = Path()
            val baseY = stringY(s)
            path.moveTo(0f, baseY)
            if (waveform.isNotEmpty()) {
                for (x in 0..width.toInt() step 8) {
                    val sampleIndex = (x * waveform.size / width).toInt().coerceIn(0, waveform.size - 1)
                    val amp = waveform[sampleIndex] * height * 0.08f * (1f - s * 0.1f)
                    val y = baseY + sin(x * 0.05f + s) * amp
                    path.lineTo(x.toFloat(), y)
                }
            } else {
                path.lineTo(width, baseY)
            }
            drawPath(path, color = themeColor.copy(alpha = 0.7f - s * 0.08f), style = Stroke(width = 2f))
        }
    }
}

@Composable
fun AudioOrbCanvas(waveform: FloatArray, colorTheme: Int = 0) {
    val themeColor = getThemeColor(colorTheme)
    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val baseRadius = min(cx, cy) * 0.35f

        if (waveform.isEmpty()) {
            drawCircle(themeColor.copy(alpha = 0.3f), radius = baseRadius)
            return@Canvas
        }

        val rings = 24
        for (r in 0 until rings) {
            val sampleIndex = (r * waveform.size / rings).coerceIn(0, waveform.size - 1)
            val amp = waveform[sampleIndex]
            val radius = baseRadius + r * 8f + amp * 60f
            val alpha = (1f - r / rings.toFloat()) * 0.5f
            drawCircle(themeColor.copy(alpha = alpha), radius = radius, style = Stroke(width = 2f))
        }

        // Center glow
        val rms = waveform.map { it * it }.average().toFloat().let { sqrt(it) }
        drawCircle(themeColor.copy(alpha = 0.2f + rms * 0.5f), radius = baseRadius * (0.5f + rms))
    }
}
