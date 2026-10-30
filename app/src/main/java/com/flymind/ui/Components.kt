package com.flymind.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flymind.brain.SimTrace
import com.flymind.ui.theme.Amber
import com.flymind.ui.theme.Fill
import com.flymind.ui.theme.Separator
import com.flymind.ui.theme.Surface
import com.flymind.ui.theme.SurfaceHigh
import com.flymind.ui.theme.Teal
import com.flymind.ui.theme.TextPrimary
import com.flymind.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.floor

/**
 * Grupo no estilo iOS: cabeçalho de seção cinza em cima e cartão branco
 * arredondado embaixo, sem borda — como as listas agrupadas dos Ajustes.
 */
@Composable
fun Panel(
    title: String? = null,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier.fillMaxWidth()) {
        if (title != null) {
            Text(
                title.uppercase(Locale("pt", "BR")),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.4.sp,
                color = TextSecondary,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 6.dp),
            )
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = BorderStroke(0.5.dp, Separator.copy(alpha = 0.6f)),
        ) {
            Column(Modifier.padding(16.dp)) {
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                    Spacer(Modifier.height(12.dp))
                }
                content()
            }
        }
    }
}

/** Controle segmentado iOS: pílula cinza com a opção ativa em cartão branco. */
@Composable
fun IosSegmented(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .background(Fill, RoundedCornerShape(9.dp))
            .padding(2.dp),
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (selected) Surface else Color.Transparent,
                        RoundedCornerShape(7.dp),
                    )
                    .then(
                        if (selected) {
                            Modifier.border(0.5.dp, Separator, RoundedCornerShape(7.dp))
                        } else {
                            Modifier
                        },
                    )
                    .clickable { onSelect(index) }
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (selected) TextPrimary else TextSecondary,
                )
            }
        }
    }
}

@Composable
fun MetricPill(label: String, value: String, color: Color = Teal) {
    Column(
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp),
    ) {
        Text(value, color = color, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        Text(label, color = TextSecondary, fontSize = 10.sp)
    }
}

@Composable
fun StatBlock(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            value,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            color = TextPrimary,
        )
        Text(
            label.uppercase(Locale("pt", "BR")),
            fontSize = 10.sp,
            letterSpacing = 0.4.sp,
            color = TextSecondary,
        )
    }
}

@Composable
fun StrengthBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    color: Color = Teal,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(6.dp)
            .background(Fill, RoundedCornerShape(3.dp)),
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(6.dp)
                .background(color, RoundedCornerShape(3.dp)),
        )
    }
}

@Composable
fun TraceChart(trace: SimTrace?, modifier: Modifier = Modifier) {
    val grid = Separator
    val line = Teal
    val reward = Amber
    Canvas(
        modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(SurfaceHigh, RoundedCornerShape(10.dp)),
    ) {
        val activity = trace?.activity ?: FloatArray(0)
        val da = trace?.dopamine ?: FloatArray(0)
        if (activity.isEmpty()) return@Canvas

        for (i in 1..3) {
            val y = size.height * i / 4f
            drawLine(grid, Offset(0f, y), Offset(size.width, y), 1f)
        }

        val maxActivity = activity.maxOrNull()?.coerceAtLeast(0.0001f) ?: 1f
        val activityPath = Path()
        activity.forEachIndexed { index, value ->
            val x = size.width * index / (activity.size - 1).coerceAtLeast(1)
            val y = size.height - (value / maxActivity) * size.height * 0.78f - 4f
            if (index == 0) activityPath.moveTo(x, y) else activityPath.lineTo(x, y)
        }
        drawPath(activityPath, line, style = Stroke(width = 2.2f, cap = StrokeCap.Round))

        if (da.isNotEmpty()) {
            val maxDa = da.maxOrNull()?.coerceAtLeast(0.001f) ?: 1f
            val daPath = Path()
            da.forEachIndexed { index, value ->
                val x = size.width * index / (da.size - 1).coerceAtLeast(1)
                val y = size.height - (value / maxDa) * size.height * 0.55f - 4f
                if (index == 0) daPath.moveTo(x, y) else daPath.lineTo(x, y)
            }
            drawPath(daPath, reward, style = Stroke(width = 1.7f, cap = StrokeCap.Round))
        }
    }
}

@Composable
fun SpikeRaster(trace: SimTrace?, modifier: Modifier = Modifier) {
    val spikeColor = Teal.copy(alpha = 0.55f)
    Canvas(
        modifier
            .fillMaxWidth()
            .height(110.dp)
            .background(SurfaceHigh, RoundedCornerShape(10.dp)),
    ) {
        if (trace == null || trace.rasterStep.isEmpty()) return@Canvas
        val maxStep = trace.rasterStep.maxOrNull()?.coerceAtLeast(1) ?: 1
        val maxNeuron = trace.rasterNeuron.maxOrNull()?.coerceAtLeast(1) ?: 1
        for (i in trace.rasterStep.indices) {
            val x = size.width * trace.rasterStep[i] / maxStep
            val y = size.height * trace.rasterNeuron[i] / maxNeuron
            drawCircle(spikeColor, radius = 1.1f, center = Offset(x, y))
        }
    }
}

@Composable
fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(8.dp)
                .background(color, RoundedCornerShape(4.dp)),
        )
        Spacer(Modifier.width(6.dp))
        Text(label, color = TextSecondary, fontSize = 11.sp)
    }
}

@Composable
fun HorizontalMetrics(items: List<Pair<String, String>>) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { (label, value) -> MetricPill(label, value) }
    }
}

fun formatTime(epochMillis: Long): String {
    if (epochMillis <= 0L) return "nunca"
    val format = SimpleDateFormat("dd/MM/yy HH:mm", Locale("pt", "BR"))
    return format.format(Date(epochMillis))
}

/** "1:04" para duração de arquivo de áudio. */
fun formatDuration(millis: Long): String {
    if (millis <= 0L) return "0:00"
    val total = floor(millis / 1000.0).toLong()
    val minutes = total / 60
    val seconds = total % 60
    return "%d:%02d".format(minutes, seconds)
}

fun formatSize(bytes: Long): String = when {
    bytes >= 1024L * 1024L -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
    bytes >= 1024L -> "%.0f KB".format(bytes / 1024.0)
    else -> "$bytes B"
}
