package com.example.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.AudioAnalyzerViewModel
import com.example.ui.theme.getThemeColor
import com.example.ui.visualizations.SpectrumCanvas

@Composable
fun CaptureScreen(viewModel: AudioAnalyzerViewModel) {
    val aiAnalysis by viewModel.aiAnalysis.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val peakSpectrum by viewModel.peakSpectrum.collectAsState()
    val frozenSpectrum by viewModel.frozenSpectrum.collectAsState()
    val colorTheme by viewModel.colorTheme.collectAsState()
    val peakDb by viewModel.peakDb.collectAsState()
    val dominantFreq by viewModel.dominantFreq.collectAsState()
    val rmsLevel by viewModel.rmsLevel.collectAsState()

    val accent = getThemeColor(colorTheme)
    val accentDim = getThemeColor(colorTheme, alpha = 0.15f)
    val accentMid = getThemeColor(colorTheme, alpha = 0.4f)
    val isFrozen = frozenSpectrum != null
    val displaySpectrum = frozenSpectrum ?: peakSpectrum

    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            "Spectral Capture & Analysis",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = accent
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .background(Color.Black, shape = RoundedCornerShape(16.dp))
                .border(
                    1.dp,
                    accent.copy(alpha = 0.5f * glowPulse),
                    RoundedCornerShape(16.dp)
                )
                .clip(RoundedCornerShape(16.dp))
                .drawBehind {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(accent.copy(alpha = 0.06f), Color.Transparent),
                            startY = 0f,
                            endY = size.height * 0.4f
                        )
                    )
                }
        ) {
            SpectrumCanvas(spectrum = displaySpectrum)

            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (isFrozen) "FROZEN" else "LIVE",
                    color = if (isFrozen) accent else Color(0xFF80CBC4),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        FrequencyRuler(accent = accent)

        StatsRow(
            peakDb = peakDb,
            dominantFreq = dominantFreq,
            rmsLevel = rmsLevel,
            accent = accent,
            accentDim = accentDim
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActionButton(
                label = "Freeze",
                icon = Icons.Default.Star,
                onClick = { viewModel.freezeSpectrum() },
                enabled = !isFrozen,
                accent = accent,
                accentDim = accentDim,
                modifier = Modifier.weight(1f)
            )
            ActionButton(
                label = "Clear",
                icon = Icons.Default.Refresh,
                onClick = { viewModel.clearFreeze() },
                enabled = isFrozen,
                accent = accent,
                accentDim = accentDim,
                modifier = Modifier.weight(1f)
            )
            ActionButton(
                label = "Reset AI",
                icon = Icons.Default.Delete,
                onClick = { viewModel.clearAnalysis() },
                enabled = aiAnalysis != null,
                accent = accent,
                accentDim = accentDim,
                modifier = Modifier.weight(1f)
            )
        }

        Button(
            onClick = { viewModel.analyzeSpectrum() },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            enabled = !isAnalyzing,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = accent,
                contentColor = Color.Black,
                disabledContainerColor = accentDim,
                disabledContentColor = accent.copy(alpha = 0.5f)
            )
        ) {
            if (isAnalyzing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.Black,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(10.dp))
                Text("Analyzing Spectrum...", fontWeight = FontWeight.SemiBold)
            } else {
                Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    if (isFrozen) "Analyze Frozen Spectrum" else "Run AI Intelligence Analysis",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        aiAnalysis?.let {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(accentDim, MaterialTheme.colorScheme.surface)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .border(1.dp, accent.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(accent, CircleShape)
                        )
                        Text(
                            "AI Analysis Report",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = accent
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f),
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun FrequencyRuler(accent: Color) {
    val labels = listOf("0", "5k", "10k", "15k", "20k")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        labels.forEach { label ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(6.dp)
                        .background(accent.copy(alpha = 0.5f))
                )
                Text(
                    text = label,
                    color = accent.copy(alpha = 0.6f),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun StatsRow(
    peakDb: Float,
    dominantFreq: Float,
    rmsLevel: Float,
    accent: Color,
    accentDim: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(accentDim, RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatItem(
            label = "PEAK",
            value = "${peakDb.toInt()} dB",
            accent = accent,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(32.dp)
                .background(accent.copy(alpha = 0.2f))
        )
        StatItem(
            label = "FREQ",
            value = formatFreq(dominantFreq),
            accent = accent,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(32.dp)
                .background(accent.copy(alpha = 0.2f))
        )
        StatItem(
            label = "RMS",
            value = "${(rmsLevel * 100).toInt()}%",
            accent = accent,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatItem(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            color = accent.copy(alpha = 0.6f),
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.5.sp
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = accent,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    enabled: Boolean,
    accent: Color,
    accentDim: Color,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = accentDim,
            contentColor = accent,
            disabledContainerColor = MaterialTheme.colorScheme.surface,
            disabledContentColor = accent.copy(alpha = 0.3f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (enabled) accent.copy(alpha = 0.4f) else accent.copy(alpha = 0.1f)
        )
    ) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
    }
}

private fun formatFreq(hz: Float): String {
    return when {
        hz >= 1000f -> "${"%.1f".format(hz / 1000f)} kHz"
        else -> "${hz.toInt()} Hz"
    }
}
