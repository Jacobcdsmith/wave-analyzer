package com.example.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

data class ConnectionEntry(val address: String, val port: String)

@Composable
fun RemoteScreen() {
    var isConnected by remember { mutableStateOf(false) }
    var isConnecting by remember { mutableStateOf(false) }
    var ipAddress by remember { mutableStateOf("192.168.1.100") }
    var port by remember { mutableStateOf("5024") }
    var connectionError by remember { mutableStateOf<String?>(null) }
    var bytesReceived by remember { mutableLongStateOf(0L) }
    var connectionTimeMs by remember { mutableLongStateOf(0L) }
    var autoReconnect by remember { mutableStateOf(false) }
    var dataRate by remember { mutableLongStateOf(0L) }

    val scope = rememberCoroutineScope()
    var socket by remember { mutableStateOf<Socket?>(null) }
    var connectJob by remember { mutableStateOf<Job?>(null) }
    var readJob by remember { mutableStateOf<Job?>(null) }
    var timerJob by remember { mutableStateOf<Job?>(null) }
    var rateJob by remember { mutableStateOf<Job?>(null) }

    val recentConnections = remember { mutableStateListOf<ConnectionEntry>() }

    val glowAlpha by animateFloatAsState(
        targetValue = if (isConnected) 0.6f else 0f,
        animationSpec = tween(durationMillis = 800)
    )

    val errorGlowAlpha by animateFloatAsState(
        targetValue = if (connectionError != null) 0.5f else 0f,
        animationSpec = tween(durationMillis = 600)
    )

    fun addToRecent(ip: String, p: String) {
        val entry = ConnectionEntry(ip, p)
        recentConnections.removeAll { it.address == ip && it.port == p }
        recentConnections.add(0, entry)
        if (recentConnections.size > 3) {
            recentConnections.removeLast()
        }
    }

    fun disconnect() {
        readJob?.cancel()
        timerJob?.cancel()
        rateJob?.cancel()
        readJob = null
        timerJob = null
        rateJob = null
        try { socket?.close() } catch (_: Exception) {}
        socket = null
        isConnected = false
        isConnecting = false
        connectionError = null
        bytesReceived = 0L
        connectionTimeMs = 0L
        dataRate = 0L
    }

    fun connect() {
        connectionError = null
        isConnecting = true
        connectJob = scope.launch {
            val startTime = System.currentTimeMillis()
            try {
                val s = withContext(Dispatchers.IO) {
                    Socket().apply {
                        connect(InetSocketAddress(ipAddress, port.toInt()), 5000)
                    }
                }
                socket = s
                isConnected = true
                isConnecting = false
                connectionTimeMs = System.currentTimeMillis() - startTime
                addToRecent(ipAddress, port)

                readJob = scope.launch {
                    var lastBytes = 0L
                    var lastTime = System.currentTimeMillis()
                    withContext(Dispatchers.IO) {
                        try {
                            val input = s.getInputStream()
                            val buffer = ByteArray(1024)
                            while (isActive && !s.isClosed) {
                                val read = input.read(buffer)
                                if (read > 0) {
                                    bytesReceived += read
                                } else if (read == -1) {
                                    break
                                }
                                val now = System.currentTimeMillis()
                                if (now - lastTime >= 1000) {
                                    dataRate = bytesReceived - lastBytes
                                    lastBytes = bytesReceived
                                    lastTime = now
                                }
                            }
                        } catch (_: Exception) {}
                    }
                    if (autoReconnect && !s.isClosed) {
                        disconnect()
                        delay(2000)
                        connect()
                    } else if (!autoReconnect) {
                        disconnect()
                    }
                }
            } catch (e: Exception) {
                connectionError = e.message ?: "Connection failed"
                isConnected = false
                isConnecting = false
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            readJob?.cancel()
            timerJob?.cancel()
            rateJob?.cancel()
            connectJob?.cancel()
            try { socket?.close() } catch (_: Exception) {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Remote SDR Connection",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        ConnectionStatusCard(
            isConnected = isConnected,
            isConnecting = isConnecting,
            connectionError = connectionError,
            ipAddress = ipAddress,
            port = port,
            glowAlpha = glowAlpha,
            errorGlowAlpha = errorGlowAlpha
        )

        if (isConnected) {
            DataStreamCard(
                bytesReceived = bytesReceived,
                connectionTimeMs = connectionTimeMs,
                dataRate = dataRate
            )
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = ipAddress,
                    onValueChange = { ipAddress = it },
                    label = { Text("IP Address", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isConnected && !isConnecting,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    )
                )

                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it },
                    label = { Text("Port", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isConnected && !isConnecting,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = autoReconnect,
                            onCheckedChange = { autoReconnect = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary,
                                uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                checkmarkColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Auto-reconnect",
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (isConnecting) {
                        Text(
                            "Connecting...",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                Button(
                    onClick = {
                        if (isConnected || isConnecting) {
                            connectJob?.cancel()
                            readJob?.cancel()
                            timerJob?.cancel()
                            rateJob?.cancel()
                            disconnect()
                        } else {
                            connect()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when {
                            isConnecting -> MaterialTheme.colorScheme.surfaceVariant
                            isConnected -> Color(0xFFFF3D5A)
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )
                ) {
                    Text(
                        when {
                            isConnecting -> "Connecting..."
                            isConnected -> "Disconnect"
                            else -> "Connect"
                        }
                    )
                }

                connectionError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        if (recentConnections.isNotEmpty()) {
            RecentConnectionsCard(
                connections = recentConnections,
                isConnected = isConnected || isConnecting,
                onSelect = { entry ->
                    ipAddress = entry.address
                    port = entry.port
                }
            )
        }
    }
}

@Composable
private fun ConnectionStatusCard(
    isConnected: Boolean,
    isConnecting: Boolean,
    connectionError: String?,
    ipAddress: String,
    port: String,
    glowAlpha: Float,
    errorGlowAlpha: Float
) {
    val statusColor = when {
        connectionError != null -> Color(0xFFFF3D5A)
        isConnected -> Color(0xFF00E676)
        isConnecting -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val statusText = when {
        connectionError != null -> "ERROR"
        isConnected -> "CONNECTED"
        isConnecting -> "CONNECTING"
        else -> "DISCONNECTED"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isConnected) Modifier.shadow(8.dp, RoundedCornerShape(16.dp))
                else Modifier
            )
    ) {
        Box {
            if (isConnected) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF00E676).copy(alpha = glowAlpha * 0.15f),
                                    Color.Transparent
                                ),
                                radius = 300f
                            ),
                            RoundedCornerShape(16.dp)
                        )
                )
            }
            if (connectionError != null) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFF3D5A).copy(alpha = errorGlowAlpha * 0.15f),
                                    Color.Transparent
                                ),
                                radius = 300f
                            ),
                            RoundedCornerShape(16.dp)
                        )
                )
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatusIndicator(color = statusColor, active = isConnected || isConnecting)
                        Text(
                            statusText,
                            color = statusColor,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (isConnected) {
                        Text(
                            "$ipAddress:$port",
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                if (isConnected) {
                    Spacer(Modifier.height(12.dp))
                    DataStreamVisualization()
                }
            }
        }
    }
}

@Composable
private fun StatusIndicator(color: Color, active: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = Modifier
            .size(12.dp)
            .background(
                color.copy(alpha = if (active) pulseAlpha else 0.3f),
                CircleShape
            )
            .border(1.dp, color.copy(alpha = 0.5f), CircleShape)
    )
}

@Composable
private fun DataStreamVisualization() {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val amplitude by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "amplitude"
    )

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
    ) {
        val w = size.width
        val h = size.height
        val midY = h / 2f

        val path = Path().apply {
            moveTo(0f, midY)
            for (x in 0..w.toInt()) {
                val t = x / w
                val y = midY + kotlin.math.sin(t * 4 * Math.PI + phase) * midY * 0.7 * amplitude
                lineTo(x.toFloat(), y.toFloat())
            }
        }

        drawPath(
            path = path,
            color = Color(0xFF00E676).copy(alpha = 0.8f),
            style = Stroke(width = 2f)
        )

        val path2 = Path().apply {
            moveTo(0f, midY)
            for (x in 0..w.toInt()) {
                val t = x / w
                val y = midY + kotlin.math.sin(t * 6 * Math.PI + phase * 1.5) * midY * 0.4 * amplitude
                lineTo(x.toFloat(), y.toFloat())
            }
        }

        drawPath(
            path = path2,
            color = Color(0xFF00D4FF).copy(alpha = 0.4f),
            style = Stroke(width = 1.5f)
        )
    }
}

@Composable
private fun DataStreamCard(
    bytesReceived: Long,
    connectionTimeMs: Long,
    dataRate: Long
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "DATA STREAM",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )

            StatRow("Bytes Received", formatBytes(bytesReceived))
            StatRow("Data Rate", "${formatBytes(dataRate)}/s")
            StatRow("Connection Time", formatDuration(connectionTimeMs))
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            value,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun RecentConnectionsCard(
    connections: List<ConnectionEntry>,
    isConnected: Boolean,
    onSelect: (ConnectionEntry) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "RECENT CONNECTIONS",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )

            connections.forEach { entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            RoundedCornerShape(8.dp)
                        )
                        .then(
                            if (!isConnected) {
                                Modifier
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .background(Color.Transparent)
                            } else {
                                Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            }
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${entry.address}:${entry.port}",
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!isConnected) {
                        Text(
                            "USE",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                .then(
                                    Modifier.background(Color.Transparent)
                                )
                        )
                    }
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${"%.1f".format(bytes / 1024.0)} KB"
        bytes < 1024 * 1024 * 1024 -> "${"%.2f".format(bytes / (1024.0 * 1024.0))} MB"
        else -> "${"%.2f".format(bytes / (1024.0 * 1024.0 * 1024.0))} GB"
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m ${seconds}s"
        minutes > 0 -> "${minutes}m ${seconds}s"
        else -> "${seconds}s"
    }
}
