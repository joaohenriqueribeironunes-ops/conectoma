package com.flymind.ui.theme

import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flymind.R

/*
 * Tokens no vocabulário do iOS (Human Interface Guidelines), nas duas
 * aparências: clara e escura. As cores de acento têm variantes próprias no
 * escuro (systemBlue #0A84FF em vez de #007AFF), como no iOS.
 */

@Immutable
data class IosPalette(
    val blue: Color,
    val orange: Color,
    val green: Color,
    val red: Color,
    val purple: Color,
    val indigo: Color,
    val cyan: Color,
    val mint: Color,
    val blueSoft: Color,
    val blueDeep: Color,
    val orangeDeep: Color,
    val groupedBackground: Color,
    val secondaryGroupedBackground: Color,
    val raisedBackground: Color,
    val fill: Color,
    val fillStrong: Color,
    val label: Color,
    val secondaryLabel: Color,
    val tertiaryLabel: Color,
    val separator: Color,
    val separatorOpaque: Color,
    val outline: Color,
)

val LightPalette = IosPalette(
    blue = Color(0xFF007AFF),
    orange = Color(0xFFFF9500),
    green = Color(0xFF34C759),
    red = Color(0xFFFF3B30),
    purple = Color(0xFFAF52DE),
    indigo = Color(0xFF5856D6),
    cyan = Color(0xFF32ADE6),
    mint = Color(0xFF00C7BE),
    blueSoft = Color(0xFFD6E9FF),
    blueDeep = Color(0xE6EAF2FF),
    orangeDeep = Color(0xFFFFF4E5),
    groupedBackground = Color(0xFFF2F2F7),
    secondaryGroupedBackground = Color(0xFFFFFFFF),
    raisedBackground = Color(0xFFF7F7F8),
    fill = Color(0xFFEFEFF4),
    fillStrong = Color(0xFFE5E5EA),
    label = Color(0xFF000000),
    secondaryLabel = Color(0xFF6E6E73),
    tertiaryLabel = Color(0xFF8E8E93),
    separator = Color(0x4D3C3C43),
    separatorOpaque = Color(0xFFC6C6C8),
    outline = Color(0xFFC7C7CC),
)

/** Equivalente escuro: preto puro no fundo agrupado e #1C1C1E nos cartões. */
val DarkPalette = IosPalette(
    blue = Color(0xFF0A84FF),
    orange = Color(0xFFFF9F0A),
    green = Color(0xFF30D158),
    red = Color(0xFFFF453A),
    purple = Color(0xFFBF5AF2),
    indigo = Color(0xFF5E5CE6),
    cyan = Color(0xFF64D2FF),
    mint = Color(0xFF63E6E2),
    blueSoft = Color(0xFF0A2540),
    blueDeep = Color(0x33264A80),
    orangeDeep = Color(0x33FF9F0A),
    groupedBackground = Color(0xFF000000),
    secondaryGroupedBackground = Color(0xFF1C1C1E),
    raisedBackground = Color(0xFF2C2C2E),
    fill = Color(0x2E787880),
    fillStrong = Color(0x3D787880),
    label = Color(0xFFFFFFFF),
    secondaryLabel = Color(0xFF98989F),
    tertiaryLabel = Color(0xFF8E8E93),
    separator = Color(0x99545458),
    separatorOpaque = Color(0xFF38383A),
    outline = Color(0xFF48484A),
)

val LocalIosPalette = staticCompositionLocalOf { LightPalette }

// Tokens usados pelas telas: leem a paleta atual, então acompanham o tema.
val Bg: Color
    @Composable @ReadOnlyComposable get() = LocalIosPalette.current.groupedBackground

val Surface: Color
    @Composable @ReadOnlyComposable get() = LocalIosPalette.current.secondaryGroupedBackground

val SurfaceHigh: Color
    @Composable @ReadOnlyComposable get() = LocalIosPalette.current.raisedBackground

val Raised: Color
    @Composable @ReadOnlyComposable get() = LocalIosPalette.current.raisedBackground

val Fill: Color
    @Composable @ReadOnlyComposable get() = LocalIosPalette.current.fill

val FillStrong: Color
    @Composable @ReadOnlyComposable get() = LocalIosPalette.current.fillStrong

val TextPrimary: Color
    @Composable @ReadOnlyComposable get() = LocalIosPalette.current.label

val TextSecondary: Color
    @Composable @ReadOnlyComposable get() = LocalIosPalette.current.secondaryLabel

val TextTertiary: Color
    @Composable @ReadOnlyComposable get() = LocalIosPalette.current.tertiaryLabel

val Outline: Color
    @Composable @ReadOnlyComposable get() = LocalIosPalette.current.outline

val Separator: Color
    @Composable @ReadOnlyComposable get() = LocalIosPalette.current.separator

val SeparatorOpaque: Color
    @Composable @ReadOnlyComposable get() = LocalIosPalette.current.separatorOpaque

val Teal: Color
    @Composable @ReadOnlyComposable get() = LocalIosPalette.current.blue

val Amber: Color
    @Composable @ReadOnlyComposable get() = LocalIosPalette.current.orange

val Rose: Color
    @Composable @ReadOnlyComposable get() = LocalIosPalette.current.red

val Green: Color
    @Composable @ReadOnlyComposable get() = LocalIosPalette.current.green

val Purple: Color
    @Composable @ReadOnlyComposable get() = LocalIosPalette.current.purple

val Indigo: Color
    @Composable @ReadOnlyComposable get() = LocalIosPalette.current.indigo

val Cyan: Color
    @Composable @ReadOnlyComposable get() = LocalIosPalette.current.cyan

val Mint: Color
    @Composable @ReadOnlyComposable get() = LocalIosPalette.current.mint

val TealSoft: Color
    @Composable @ReadOnlyComposable get() = LocalIosPalette.current.blueSoft

val TealDeep: Color
    @Composable @ReadOnlyComposable get() = LocalIosPalette.current.blueDeep

val AmberDeep: Color
    @Composable @ReadOnlyComposable get() = LocalIosPalette.current.orangeDeep

private fun lightScheme(p: IosPalette) = lightColorScheme(
    primary = p.blue,
    onPrimary = Color.White,
    primaryContainer = p.blueSoft,
    onPrimaryContainer = Color(0xFF0040DD),
    secondary = p.orange,
    onSecondary = Color.White,
    secondaryContainer = p.orangeDeep,
    onSecondaryContainer = Color(0xFFB26A00),
    tertiary = p.red,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDBD8),
    onTertiaryContainer = Color(0xFF8F1208),
    background = p.groupedBackground,
    onBackground = p.label,
    surface = p.secondaryGroupedBackground,
    onSurface = p.label,
    surfaceVariant = p.fill,
    onSurfaceVariant = p.secondaryLabel,
    surfaceContainerHighest = p.secondaryGroupedBackground,
    outline = p.outline,
    outlineVariant = p.separator,
    error = p.red,
    onError = Color.White,
)

private fun darkScheme(p: IosPalette) = darkColorScheme(
    primary = p.blue,
    onPrimary = Color.White,
    primaryContainer = p.blueSoft,
    onPrimaryContainer = Color(0xFF9CC4FF),
    secondary = p.orange,
    onSecondary = Color.Black,
    secondaryContainer = p.orangeDeep,
    onSecondaryContainer = Color(0xFFFFD399),
    tertiary = p.red,
    onTertiary = Color.White,
    tertiaryContainer = Color(0x4DFF453A),
    onTertiaryContainer = Color(0xFFFFB3AC),
    background = p.groupedBackground,
    onBackground = p.label,
    surface = p.secondaryGroupedBackground,
    onSurface = p.label,
    surfaceVariant = p.fill,
    onSurfaceVariant = p.secondaryLabel,
    surfaceContainerHighest = p.secondaryGroupedBackground,
    outline = p.outline,
    outlineVariant = p.separator,
    error = p.red,
    onError = Color.Black,
)

private val IosShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(14.dp),
)

/**
 * Inter: a substituta livre mais próxima do SF Pro, que é licenciado só para
 * plataformas da Apple. Fica embarcada no APK, sem depender de fonte do sistema.
 */
val IosTypeface = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

/**
 * Escala tipográfica do iOS: largeTitle 34, title2 22, headline 17, body 17,
 * footnote 13, caption 11. Sem o espaçamento largo do Material e com entrelinha
 * fechada, como o SF Pro.
 */
private fun style(
    size: Int,
    lineHeight: Int,
    weight: FontWeight,
    tracking: Double = 0.0,
) = TextStyle(
    fontFamily = IosTypeface,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    fontWeight = weight,
    letterSpacing = tracking.sp,
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.None,
    ),
)

private val IosTypography = Typography(
    displayLarge = style(34, 41, FontWeight.Bold, -0.4),
    displayMedium = style(28, 34, FontWeight.Bold, -0.3),
    displaySmall = style(22, 28, FontWeight.Bold, -0.2),
    headlineLarge = style(22, 28, FontWeight.SemiBold, -0.2),
    headlineMedium = style(20, 25, FontWeight.SemiBold, -0.1),
    headlineSmall = style(17, 22, FontWeight.SemiBold),
    titleLarge = style(17, 22, FontWeight.SemiBold),
    titleMedium = style(16, 21, FontWeight.SemiBold),
    titleSmall = style(15, 20, FontWeight.Medium),
    bodyLarge = style(17, 23, FontWeight.Normal, -0.2),
    bodyMedium = style(16, 21, FontWeight.Normal, -0.2),
    bodySmall = style(14, 19, FontWeight.Normal, -0.1),
    labelLarge = style(17, 22, FontWeight.SemiBold),
    labelMedium = style(13, 18, FontWeight.Medium),
    labelSmall = style(11, 13, FontWeight.Medium, 0.1),
)

/** Sem o ripple radial do Android: toque com resposta discreta, como no iOS. */
private object NoRipple : IndicationNodeFactory {
    override fun create(
        interactionSource: androidx.compose.foundation.interaction.InteractionSource,
    ): DelegatableNode = object : Modifier.Node() {}

    override fun equals(other: Any?): Boolean = other === this

    override fun hashCode(): Int = "no-ripple".hashCode()
}

@Composable
fun FlyMindTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val palette = if (darkTheme) DarkPalette else LightPalette
    CompositionLocalProvider(
        LocalIosPalette provides palette,
        androidx.compose.foundation.LocalIndication provides NoRipple,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) darkScheme(palette) else lightScheme(palette),
            shapes = IosShapes,
            typography = IosTypography,
            content = content,
        )
    }
}

/** Espaçamento padrão das listas agrupadas. */
val ScreenPadding = 16.dp
val GroupSpacing = 22.dp
