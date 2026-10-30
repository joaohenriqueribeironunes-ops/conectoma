package com.flymind.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Paleta no vocabulário visual do iOS: fundo de listas agrupadas, cartões
 * brancos, azul de sistema como acento, laranja/vermelho de status e
 * separadores cinza. O app é claro como as telas Ajustes do iOS.
 */
val Bg = Color(0xFFF2F2F7)
val Surface = Color(0xFFFFFFFF)
val SurfaceHigh = Color(0xFFF7F7FA)
val Teal = Color(0xFF007AFF)
val TealSoft = Color(0xFFD6E9FF)
val TealDeep = Color(0xFFE9F1FF)
val Amber = Color(0xFFFF9500)
val AmberDeep = Color(0xFFFFF4E5)
val Rose = Color(0xFFFF3B30)
val TextPrimary = Color(0xFF000000)
val TextSecondary = Color(0xFF6E6E73)
val Outline = Color(0xFFC6C6C8)
val Separator = Color(0xFFE5E5EA)
val Fill = Color(0xFFEFEFF4)

private val IosColors = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    primaryContainer = TealDeep,
    onPrimaryContainer = Color(0xFF0040DD),
    secondary = Amber,
    onSecondary = Color.White,
    secondaryContainer = AmberDeep,
    onSecondaryContainer = Color(0xFFB26A00),
    tertiary = Rose,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDBD8),
    onTertiaryContainer = Color(0xFF8F1208),
    background = Bg,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = Fill,
    onSurfaceVariant = TextSecondary,
    outline = Outline,
    outlineVariant = Separator,
    error = Rose,
    onError = Color.White,
)

private val IosShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(14.dp),
)

@Composable
fun FlyMindTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = IosColors,
        shapes = IosShapes,
        typography = Typography(),
        content = content,
    )
}
