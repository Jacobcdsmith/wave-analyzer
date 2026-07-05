package com.example.ui.theme

import androidx.compose.ui.graphics.Color

fun getThemeColor(theme: Int, alpha: Float = 1f): Color {
    return when(theme) {
        0 -> Color(0xFF00E5FF).copy(alpha = alpha)
        1 -> Color(0xFF00B0FF).copy(alpha = alpha)
        2 -> Color(0xFFFF3D00).copy(alpha = alpha)
        3 -> Color(0xFFE040FB).copy(alpha = alpha)
        else -> Color.Cyan.copy(alpha = alpha)
    }
}

fun getThemeHueOffset(theme: Int): Float {
    return when(theme) {
        0 -> 240f
        1 -> 180f
        2 -> 60f
        3 -> 300f
        else -> 240f
    }
}
