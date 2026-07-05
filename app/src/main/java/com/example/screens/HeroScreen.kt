package com.example.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun HeroScreen(onEnter: () -> Unit) {
    val cyan = Color(0xFF00D4FF)
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Canvas(modifier = Modifier.fillMaxSize().alpha(0.05f)) {
            val step = 36.dp.toPx()
            var gx = 0f
            while (gx <= size.width) {
                drawLine(cyan, Offset(gx, 0f), Offset(gx, size.height), strokeWidth = 1f)
                gx += step
            }
            var gy = 0f
            while (gy <= size.height) {
                drawLine(cyan, Offset(0f, gy), Offset(size.width, gy), strokeWidth = 1f)
                gy += step
            }
        }
        Canvas(modifier = Modifier.fillMaxWidth().height(220.dp).align(Alignment.BottomCenter)) {
            val midY = size.height * 0.5f
            val path1 = Path()
            for (i in 0..512) {
                val t = i.toFloat() / 512f
                val x = t * size.width
                val y = midY + (sin(t * 2 * PI * 3.0 + 0.5) * 0.3f * size.height * sin(t * PI)).toFloat()
                if (i == 0) path1.moveTo(x, y) else path1.lineTo(x, y)
            }
            drawPath(path1, color = Color(0x1500D4FF), style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round))
            drawPath(path1, color = Color(0x4000D4FF), style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round))
            drawPath(path1, color = cyan, style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round))
            val path2 = Path()
            for (i in 0..512) {
                val t = i.toFloat() / 512f
                val x = t * size.width
                val y = midY + (sin(t * 2 * PI * 7.0 + 1.2) * 0.14f * size.height * sin(t * PI)).toFloat()
                if (i == 0) path2.moveTo(x, y) else path2.lineTo(x, y)
            }
            drawPath(path2, color = Color(0x3000CC88), style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
            drawPath(path2, color = Color(0xFF00CC88), style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round))
        }
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(80.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(24.dp))
                    .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(44.dp)) {
                    val barHeights = listOf(0.4f, 0.75f, 1.0f, 0.6f, 0.85f)
                    val barW = size.width / (barHeights.size * 1.65f)
                    val gap = barW * 0.65f
                    val totalW = barHeights.size * barW + (barHeights.size - 1) * gap
                    val startX = (size.width - totalW) / 2f
                    barHeights.forEachIndexed { i, h ->
                        val bx = startX + i * (barW + gap)
                        val bh = size.height * h
                        val by = size.height - bh
                        drawRect(color = cyan, topLeft = Offset(bx, by), size = Size(barW, bh))
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
            Text(
                "OmniWave",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "v4.2",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 4.sp
            )
            Spacer(Modifier.height(20.dp))
            Text(
                "SCIENTIFIC GRADE RF & AUDIO ANALYZER",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(56.dp))
            Button(
                onClick = onEnter,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    "BEGIN ANALYSIS",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "Real-time FFT  ·  Multi-mode visualization  ·  AI analysis",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
