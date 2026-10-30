package com.flymind.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import com.flymind.ui.theme.Outline
import com.flymind.ui.theme.Surface
import com.flymind.ui.theme.Teal
import com.flymind.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun Panel(
    title: String? = null,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        border = BorderStroke(1.dp, Outline.copy(alpha = 0.55f)),
    ) {
        Column(Modifier.padding(16.dp)) {
            if (title != null) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (subtitle != null) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
                Spacer(Modifier.height(12.dp))
            }
            content()
        }
    }
}

@Composable
fun MetricPill(label: String, value: String, color: Color = Teal) {
    Column(
        modifier = Modifier
            .background(color.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp),
    ) {
        Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text(label, color = TextSecondary, fontSize = 10.sp)
    }
}

@Composable
fun StatBlock(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(value, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
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
            .background(Outline.copy(alpha = 0.5f), RoundedCornerShape(3.dp)),
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
    Canvas(
        modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(Color(0xFF0A1714), RoundedCornerShape(12.dp)),
    ) {
        val activity = trace?.activity ?: FloatArray(0)
        val da = trace?.dopamine ?: FloatArray(0)
        if (activity.isEmpty()) return@Canvas

        val grid = Color.White.copy(alpha = 0.06f)
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
        drawPath(activityPath, Teal, style = Stroke(width = 2.2f, cap = StrokeCap.Round))

        if (da.isNotEmpty()) {
            val maxDa = da.maxOrNull()?.coerceAtLeast(0.001f) ?: 1f
            val daPath = Path()
            da.forEachIndexed { index, value ->
                val x = size.width * index / (da.size - 1).coerceAtLeast(1)
                val y = size.height - (value / maxDa) * size.height * 0.55f - 4f
                if (index == 0) daPath.moveTo(x, y) else daPath.lineTo(x, y)
            }
            drawPath(daPath, Amber, style = Stroke(width = 1.7f, cap = StrokeCap.Round))
        }
    }
}

@Composable
fun SpikeRaster(trace: SimTrace?, modifier: Modifier = Modifier) {
    Canvas(
        modifier
            .fillMaxWidth()
            .height(110.dp)
            .background(Color(0xFF0A1714), RoundedCornerShape(12.dp)),
    ) {
        if (trace == null || trace.rasterStep.isEmpty()) return@Canvas
        val maxStep = trace.rasterStep.maxOrNull()?.coerceAtLeast(1) ?: 1
        val maxNeuron = trace.rasterNeuron.maxOrNull()?.coerceAtLeast(1) ?: 1
        val color = Teal.copy(alpha = 0.65f)
        for (i in trace.rasterStep.indices) {
            val x = size.width * trace.rasterStep[i] / maxStep
            val y = size.height * trace.rasterNeuron[i] / maxNeuron
            drawCircle(color, radius = 1.1f, center = Offset(x, y))
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
