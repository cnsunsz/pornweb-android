package com.pornweb.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val PwBg = Color(0xFF0B0D10)
val PwSurface = Color(0xFF14171C)
val PwSurfaceHigh = Color(0xFF1C2128)
val PwAccent = Color(0xFF22D3EE)
val PwAccentDim = Color(0xFF155E75)
val PwOnBg = Color(0xFFF1F5F9)
val PwMuted = Color(0xFF94A3B8)
val PwPlaceholder = Color(0xFF2A3038)

private val DarkColors = darkColorScheme(
    primary = PwAccent,
    onPrimary = Color(0xFF082F49),
    secondary = PwAccent,
    background = PwBg,
    onBackground = PwOnBg,
    surface = PwSurface,
    onSurface = PwOnBg,
    surfaceVariant = PwSurfaceHigh,
    onSurfaceVariant = PwMuted,
    outline = Color(0xFF334155),
    error = Color(0xFFF87171)
)

private val PwTypography = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp, color = PwOnBg),
    headlineMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp, color = PwOnBg),
    titleMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, color = PwOnBg),
    bodyMedium = TextStyle(fontSize = 14.sp, color = PwOnBg),
    bodySmall = TextStyle(fontSize = 12.sp, color = PwMuted),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp)
)

@Composable
fun PornWebTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = PwTypography,
        content = content
    )
}
