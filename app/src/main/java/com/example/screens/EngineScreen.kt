package com.example.screens

import android.media.MediaRecorder
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.AudioAnalyzerViewModel
import com.example.ui.theme.getThemeColor

@Composable
fun EngineScreen(viewModel: AudioAnalyzerViewModel) {
    val context = LocalContext.current
    var sourceExpanded by remember { mutableStateOf(false) }
    var themeExpanded by remember { mutableStateOf(false) }

    val audioSources = listOf(
        "Microphone" to MediaRecorder.AudioSource.MIC,
        "Voice Communication" to MediaRecorder.AudioSource.VOICE_COMMUNICATION,
        "Unprocessed" to MediaRecorder.AudioSource.UNPROCESSED,
        "Camcorder" to MediaRecorder.AudioSource.CAMCORDER
    )
    var selectedSource by remember { mutableStateOf(audioSources[0]) }

    val themes = listOf("Default", "Ocean", "Fire", "Cyberpunk")
    val colorTheme by viewModel.colorTheme.collectAsState()
    val gain by viewModel.gain.collectAsState()
    val sensitivity by viewModel.sensitivity.collectAsState()
    val themeColor = getThemeColor(colorTheme)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = null,
                    tint = themeColor,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "DSP Engine",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
            }
            IconButton(onClick = {
                viewModel.updateGain(1.0f)
                viewModel.updateSensitivity(1.0f)
                viewModel.updateColorTheme(0)
            }) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Reset to defaults",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        SectionCard {
            SectionHeader("Audio Source", themeColor)
            Spacer(Modifier.height(8.dp))
            Box {
                OutlinedButton(
                    onClick = { sourceExpanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(selectedSource.first, color = MaterialTheme.colorScheme.onSurface)
                }
                DropdownMenu(expanded = sourceExpanded, onDismissRequest = { sourceExpanded = false }) {
                    audioSources.forEach { source ->
                        DropdownMenuItem(
                            text = { Text(source.first) },
                            onClick = {
                                selectedSource = source
                                sourceExpanded = false
                                viewModel.stopRecording()
                                viewModel.startRecording(context, source.second)
                            }
                        )
                    }
                }
            }
        }

        SectionCard {
            SectionHeader("Color Theme", themeColor)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { themeExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(themes[colorTheme], color = MaterialTheme.colorScheme.onSurface)
                    }
                    DropdownMenu(expanded = themeExpanded, onDismissRequest = { themeExpanded = false }) {
                        themes.forEachIndexed { index, themeName ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(getThemeColor(index))
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(themeName)
                                    }
                                },
                                onClick = {
                                    viewModel.updateColorTheme(index)
                                    themeExpanded = false
                                }
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(themeColor)
                        .border(2.dp, themeColor.copy(alpha = 0.4f), CircleShape)
                )
            }
        }

        SectionCard {
            SectionHeader("Gain & Sensitivity", themeColor)
            Spacer(Modifier.height(12.dp))

            Text(
                "Input Gain: ${String.format("%.1f", gain)}x",
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Slider(
                value = gain,
                onValueChange = { viewModel.updateGain(it) },
                valueRange = 0.1f..5.0f,
                colors = SliderDefaults.colors(
                    thumbColor = themeColor,
                    activeTrackColor = themeColor
                )
            )
            ValueBar(gain / 5.0f, themeColor)

            Spacer(Modifier.height(16.dp))

            Text(
                "Sensitivity: ${String.format("%.1f", sensitivity)}x",
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Slider(
                value = sensitivity,
                onValueChange = { viewModel.updateSensitivity(it) },
                valueRange = 0.1f..3.0f,
                colors = SliderDefaults.colors(
                    thumbColor = themeColor,
                    activeTrackColor = themeColor
                )
            )
            ValueBar(sensitivity / 3.0f, themeColor)
        }

        SectionCard {
            SectionHeader("Signal Pipeline", themeColor)
            Spacer(Modifier.height(8.dp))
            InfoRow("Sample Rate", "44,100 Hz")
            InfoRow("Buffer Size", "1024 samples")
            InfoRow("FFT Bins", "512 usable")
            InfoRow("Window", "Hann")
            InfoRow("Waterfall History", "60 frames")
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        themeColor.copy(alpha = 0.08f),
                        RoundedCornerShape(10.dp)
                    )
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = themeColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "dB Range: \u221280 dB \u2192 0 dB",
                        style = MaterialTheme.typography.bodyMedium,
                        color = themeColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
private fun SectionHeader(title: String, accentColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(accentColor)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            value,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ValueBar(fraction: Float, color: Color) {
    LinearProgressIndicator(
        progress = { fraction.coerceIn(0f, 1f) },
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp)),
        color = color,
        trackColor = color.copy(alpha = 0.12f),
        strokeCap = StrokeCap.Round
    )
}
