package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary              = textAccent,           // #00D4FF electric cyan
    onPrimary            = accentButtonText,     // #00121E very dark
    primaryContainer     = accentDark,           // #003552
    onPrimaryContainer   = textPrimary,          // #DFF4FF
    secondary            = Color(0xFF00CC88),    // neon mint
    onSecondary          = Color(0xFF002018),
    secondaryContainer   = Color(0xFF003828),
    onSecondaryContainer = Color(0xFFB0FFE0),
    background           = bgPrimary,            // #060A0F
    onBackground         = textPrimary,          // #DFF4FF
    surface              = bgSurface,            // #0D1521
    onSurface            = textPrimary,          // #DFF4FF
    surfaceVariant       = Color(0xFF152840),
    onSurfaceVariant     = textSecondary,        // #527D96
    outline              = borderDark,           // #1A3050
    outlineVariant       = Color(0xFF0D1E33),
    error                = Color(0xFFFF3D5A),
    onError              = Color(0xFF2D0010),
)

@Composable
fun MyApplicationTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColorScheme, typography = Typography, content = content)
}
