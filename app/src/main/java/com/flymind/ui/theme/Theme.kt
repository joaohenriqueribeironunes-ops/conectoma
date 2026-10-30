package com.flymind.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Bg = Color(0xFF07110F)
val Surface = Color(0xFF0D1B18)
val SurfaceHigh = Color(0xFF14251F)
val Teal = Color(0xFF4FD1B5)
val TealSoft = Color(0xFF9CF3DF)
val TealDeep = Color(0xFF0B3B32)
val Amber = Color(0xFFFFD166)
val AmberDeep = Color(0xFF3A2A08)
val Rose = Color(0xFFFF8FA3)
val TextPrimary = Color(0xFFE7F5F0)
val TextSecondary = Color(0xFF9CB8AF)
val Outline = Color(0xFF2C4740)

private val FlyColors = darkColorScheme(
    primary = Teal,
    onPrimary = Color(0xFF00201A),
    primaryContainer = TealDeep,
    onPrimaryContainer = TealSoft,
    secondary = Amber,
    onSecondary = Color(0xFF261A00),
    secondaryContainer = AmberDeep,
    onSecondaryContainer = Amber,
    tertiary = Rose,
    onTertiary = Color(0xFF33000C),
    background = Bg,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceHigh,
    onSurfaceVariant = TextSecondary,
    outline = Outline,
    outlineVariant = Color(0xFF1C312B),
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF2A0000),
)

@Composable
fun FlyMindTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FlyColors,
        typography = Typography(),
        content = content,
    )
}
