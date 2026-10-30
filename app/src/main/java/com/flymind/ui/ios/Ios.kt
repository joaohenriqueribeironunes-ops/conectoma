package com.flymind.ui.ios

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flymind.ui.theme.Fill
import com.flymind.ui.theme.FillStrong
import com.flymind.ui.theme.IosTypeface
import com.flymind.ui.theme.Green
import com.flymind.ui.theme.Outline
import com.flymind.ui.theme.Raised
import com.flymind.ui.theme.Rose
import com.flymind.ui.theme.Separator
import com.flymind.ui.theme.Surface
import com.flymind.ui.theme.Teal
import com.flymind.ui.theme.TextPrimary
import com.flymind.ui.theme.TextSecondary
import com.flymind.ui.theme.TextTertiary

/**
 * Primitivas no estilo do iOS: botões que escurecem ao toque (sem ripple),
 * campos de formulário com preenchimento cinza, switch e slider de sistema,
 * linhas de lista com ícone em azulejo e chevron, e alerta centralizado.
 */

private val FieldShape = RoundedCornerShape(10.dp)
private val ButtonShape = RoundedCornerShape(12.dp)

@Composable
private fun PressableBox(
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier,
    shape: RoundedCornerShape,
    background: Color,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val alpha by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.72f else 1f,
        label = "press",
    )
    Box(
        modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .clip(shape)
            .background(background)
            .alpha(if (enabled) alpha else 0.4f)
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/** Botão principal do iOS: azul cheio, 50 dp, cantos de 12 dp, texto branco. */
@Composable
fun IosButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = Teal,
    icon: ImageVector? = null,
) {
    PressableBox(onClick = onClick, enabled = enabled, modifier = modifier, shape = ButtonShape, background = tint) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text,
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.2).sp,
            )
        }
    }
}

/** Botão tonalizado: fundo azul a 13% e texto azul, como as ações secundárias. */
@Composable
fun IosTintedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = Teal,
    icon: ImageVector? = null,
) {
    PressableBox(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = ButtonShape,
        background = tint.copy(alpha = 0.13f),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text, color = tint, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** Botão cinza: fundo cinza de preenchimento e texto azul. */
@Composable
fun IosGrayButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = Teal,
    icon: ImageVector? = null,
) {
    PressableBox(onClick = onClick, enabled = enabled, modifier = modifier, shape = ButtonShape, background = Fill) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text, color = tint, fontSize = 17.sp, fontWeight = FontWeight.Medium)
        }
    }
}

/** Ação de texto, sem fundo, como "Cancelar" nas telas do iOS. */
@Composable
fun IosPlainButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = Teal,
    destructive: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier
            .heightIn(min = 44.dp)
            .alpha(if (enabled) (if (pressed) 0.55f else 1f) else 0.35f)
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = if (destructive) Rose else tint,
            fontSize = 17.sp,
            fontWeight = FontWeight.Normal,
        )
    }
}

/** Campo de formulário do iOS: rótulo de 13 dp em cima e caixa cinza de 10 dp. */
@Composable
fun IosTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    Column(modifier.fillMaxWidth()) {
        if (label != null) {
            Text(
                label,
                fontSize = 13.sp,
                color = TextSecondary,
                modifier = Modifier.padding(start = 2.dp, bottom = 6.dp),
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = singleLine,
            minLines = minLines,
            keyboardOptions = keyboardOptions,
            textStyle = TextStyle(
                fontFamily = IosTypeface,
                fontSize = 17.sp,
                lineHeight = 23.sp,
                color = TextPrimary,
                letterSpacing = (-0.2).sp,
            ),
            cursorBrush = SolidColor(Teal),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(FieldShape)
                        .background(if (enabled) Fill else Fill.copy(alpha = 0.6f))
                        .padding(horizontal = 14.dp, vertical = 13.dp),
                ) {
                    if (value.isEmpty() && placeholder != null) {
                        Text(
                            placeholder,
                            fontSize = 17.sp,
                            color = TextTertiary,
                            letterSpacing = (-0.2).sp,
                        )
                    }
                    inner()
                }
            },
        )
    }
}

/** Campo de busca do iOS: lupa à esquerda, fundo cinza e botão de limpar. */
@Composable
fun IosSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Buscar",
) {
    Row(
        modifier
            .fillMaxWidth()
            .height(38.dp)
            .clip(FieldShape)
            .background(Fill)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.Search,
            contentDescription = null,
            tint = TextTertiary,
            modifier = Modifier.size(17.dp),
        )
        Spacer(Modifier.width(6.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(placeholder, fontSize = 17.sp, color = TextTertiary)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(
                    fontFamily = IosTypeface,
                    fontSize = 17.sp,
                    color = TextPrimary,
                    letterSpacing = (-0.2).sp,
                ),
                cursorBrush = SolidColor(Teal),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (value.isNotEmpty()) {
            Icon(
                Icons.Rounded.Cancel,
                contentDescription = "Limpar busca",
                tint = TextTertiary,
                modifier = Modifier
                    .size(17.dp)
                    .clickable { onValueChange("") },
            )
        }
    }
}

/** Switch do sistema iOS: trilho verde quando ligado. */
@Composable
fun IosSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        modifier = modifier,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = Green,
            checkedBorderColor = Color.Transparent,
            checkedIconColor = Color.Transparent,
            uncheckedThumbColor = Color.White,
            uncheckedTrackColor = FillStrong,
            uncheckedBorderColor = Color.Transparent,
            uncheckedIconColor = Color.Transparent,
            disabledCheckedThumbColor = Color.White,
            disabledCheckedTrackColor = Green.copy(alpha = 0.45f),
            disabledUncheckedThumbColor = Color.White,
            disabledUncheckedTrackColor = FillStrong.copy(alpha = 0.6f),
        ),
    )
}

/** Slider do iOS: trilho fino, ativo azul e botão branco. */
@Composable
fun IosSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    enabled: Boolean = true,
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        steps = steps,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        colors = SliderDefaults.colors(
            thumbColor = Color.White,
            activeTrackColor = Teal,
            inactiveTrackColor = FillStrong,
            activeTickColor = Color.Transparent,
            inactiveTickColor = Color.Transparent,
            disabledThumbColor = Color.White,
            disabledActiveTrackColor = Teal.copy(alpha = 0.4f),
            disabledInactiveTrackColor = FillStrong,
        ),
    )
}

/** Barra de progresso do iOS: 4 dp, arredondada, azul. */
@Composable
fun IosProgress(
    progress: Float?,
    modifier: Modifier = Modifier,
    tint: Color = Teal,
) {
    val shape = RoundedCornerShape(2.dp)
    if (progress == null) {
        LinearProgressIndicator(
            modifier = modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(shape),
            color = tint,
            trackColor = FillStrong,
        )
        return
    }
    Box(
        modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(shape)
            .background(FillStrong),
    ) {
        Box(
            Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(4.dp)
                .clip(shape)
                .background(tint),
        )
    }
}

/** Azulejo de ícone das listas do iOS: quadrado de 29 dp com o tom a 15%. */
@Composable
fun IosIconTile(
    icon: ImageVector,
    tint: Color,
    size: Int = 29,
) {
    Box(
        Modifier
            .size(size.dp)
            .clip(RoundedCornerShape((size * 0.24f).dp))
            .background(tint.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size((size * 0.6f).dp))
    }
}

/** Linha de lista: ícone, título, subtítulo, valor, chevron e separador fino. */
@Composable
fun IosRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    tint: Color = Teal,
    value: String? = null,
    onClick: (() -> Unit)? = null,
    showChevron: Boolean = false,
    divider: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val background by animateColorAsState(
        targetValue = if (pressed && onClick != null) Fill else Color.Transparent,
        label = "row",
    )
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(background)
                .then(
                    if (onClick != null) {
                        Modifier.clickable(
                            interactionSource = interaction,
                            indication = null,
                            onClick = onClick,
                        )
                    } else {
                        Modifier
                    },
                )
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                IosIconTile(icon, tint)
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    fontSize = 17.sp,
                    color = TextPrimary,
                    letterSpacing = (-0.2).sp,
                )
                if (subtitle != null) {
                    Text(subtitle, fontSize = 13.sp, color = TextSecondary)
                }
            }
            if (value != null) {
                Text(value, fontSize = 17.sp, color = TextSecondary)
            }
            if (trailing != null) {
                Spacer(Modifier.width(8.dp))
                trailing()
            }
            if (showChevron) {
                Spacer(Modifier.width(6.dp))
                Icon(
                    Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = Outline,
                    modifier = Modifier.size(17.dp),
                )
            }
        }
        if (divider) {
            IosDivider(startIndent = if (icon != null) 55.dp else 14.dp)
        }
    }
}

/** Separador de 0,5 dp, recuado como nas listas do iOS. */
@Composable
fun IosDivider(startIndent: androidx.compose.ui.unit.Dp = 0.dp) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = startIndent)
            .height(0.5.dp)
            .background(Separator),
    )
}

/** Alça de arraste dos sheets do iOS. */
@Composable
fun IosGrabber(modifier: Modifier = Modifier) {
    Box(
        modifier
            .padding(top = 6.dp, bottom = 10.dp)
            .width(38.dp)
            .height(5.dp)
            .clip(CircleShape)
            .background(FillStrong),
    )
}

/** Alerta centralizado do iOS: título de 17 dp, texto de 13 dp e ações azuis. */
@Composable
fun IosAlert(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissLabel: String? = "Cancelar",
    destructive: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(14.dp),
        containerColor = Raised,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Text(
                title,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        text = {
            Text(
                text,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = TextSecondary,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (dismissLabel != null) {
                    IosPlainButton(dismissLabel, onDismiss)
                    Spacer(Modifier.width(4.dp))
                }
                IosPlainButton(
                    confirmLabel,
                    onConfirm,
                    tint = if (destructive) Rose else Teal,
                )
            }
        },
    )
}

/** Cartão branco das listas do iOS, sem borda e com cantos de 10 dp. */
@Composable
fun IosCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Surface),
    ) {
        Column { content() }
    }
}

/** Cabeçalho de seção: caixa-alta de 13 dp, cinza, com 16 dp de recuo. */
@Composable
fun IosSectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(java.util.Locale("pt", "BR")),
        fontSize = 13.sp,
        color = TextSecondary,
        modifier = modifier.padding(start = 16.dp, end = 16.dp, bottom = 7.dp),
    )
}

/** Rodapé de seção: texto de 13 dp em cinza, abaixo do cartão. */
@Composable
fun IosSectionFooter(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        color = TextSecondary,
        modifier = modifier.padding(start = 16.dp, end = 16.dp, top = 7.dp),
    )
}

/** Título interno de cartão, para telas que não usam linhas de lista. */
@Composable
fun IosCardTitle(title: String, subtitle: String? = null) {
    Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
        Text(
            title,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            letterSpacing = (-0.2).sp,
        )
        if (subtitle != null) {
            Text(subtitle, fontSize = 13.sp, color = TextSecondary, lineHeight = 17.sp)
        }
    }
}
