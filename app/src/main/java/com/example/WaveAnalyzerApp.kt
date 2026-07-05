package com.example

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.screens.CaptureScreen
import com.example.screens.EngineScreen
import com.example.screens.HeroScreen
import com.example.screens.MonitorScreen
import com.example.screens.RemoteScreen

@Composable
fun WaveAnalyzerApp() {
    var showHero by remember { mutableStateOf(true) }
    if (showHero) {
        HeroScreen(onEnter = { showHero = false })
        return
    }

    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    var currentTab by remember { mutableIntStateOf(0) }
    val navItems = listOf("Monitor" to Icons.Default.Home, "Capture" to Icons.Default.DateRange, "Engine" to Icons.Default.Build, "Remote" to Icons.Default.Share)
    val viewModel: AudioAnalyzerViewModel = viewModel()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline)
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                navItems.forEachIndexed { index, (title, icon) ->
                    val isSelected = index == currentTab
                    val color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    Column(
                        modifier = Modifier
                            .clickable { currentTab = index }
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(imageVector = icon, contentDescription = title, tint = color)
                        Text(title, style = MaterialTheme.typography.labelSmall, color = color)
                    }
                }
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            if (!hasPermission) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF332600))
                        .border(1.dp, Color(0xFF665200))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Microphone permission is required for live audio capture. Tone generator works without it.",
                        color = Color.Yellow,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(12.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Grant", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Box(Modifier.weight(1f)) {
                when (currentTab) {
                    0 -> MonitorScreen(viewModel, onNavigateToEngine = { currentTab = 2 }, hasRecordPermission = hasPermission)
                    1 -> CaptureScreen(viewModel)
                    2 -> EngineScreen(viewModel, hasRecordPermission = hasPermission, onRequestPermission = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) })
                    3 -> RemoteScreen()
                }
            }
        }
    }
}
