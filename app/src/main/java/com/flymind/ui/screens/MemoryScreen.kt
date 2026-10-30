package com.flymind.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flymind.data.EventKind
import com.flymind.data.Fact
import com.flymind.data.LearningEvent
import com.flymind.ui.FlyUiState
import com.flymind.ui.IosSegmented
import com.flymind.ui.StrengthBar
import com.flymind.ui.formatTime
import com.flymind.ui.ios.IosAlert
import com.flymind.ui.ios.IosDivider
import com.flymind.ui.ios.IosIconTile
import com.flymind.ui.ios.IosSearchField
import com.flymind.ui.ios.IosSectionHeader
import com.flymind.ui.theme.Amber
import com.flymind.ui.theme.Fill
import com.flymind.ui.theme.Rose
import com.flymind.ui.theme.Surface
import com.flymind.ui.theme.Teal
import com.flymind.ui.theme.TextPrimary
import com.flymind.ui.theme.TextSecondary
import com.flymind.ui.theme.TextTertiary

@Composable
fun MemoryScreen(
    state: FlyUiState,
    onReplay: (Fact) -> Unit,
    onDelete: (Fact) -> Unit,
    onClearAll: () -> Unit,
    onExport: () -> Unit,
    onSpeak: (String) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var showDiary by rememberSaveable { mutableStateOf(false) }
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<Fact?>(null) }

    val filtered = state.facts.filter { fact ->
        query.isBlank() ||
            fact.topic.contains(query, true) ||
            fact.question.contains(query, true) ||
            fact.answer.contains(query, true)
    }
    val grouped = filtered.groupBy { it.topic }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 12.dp, top = 4.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IosSearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = "Buscar memória",
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(10.dp))
            RoundAction(
                icon = Icons.Rounded.FileUpload,
                description = "Exportar memórias",
                enabled = state.facts.isNotEmpty(),
                onClick = onExport,
            )
            Spacer(Modifier.width(8.dp))
            RoundAction(
                icon = Icons.Rounded.DeleteSweep,
                description = "Apagar tudo",
                enabled = state.facts.isNotEmpty(),
                tint = Rose,
                onClick = { confirmClear = true },
            )
        }

        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            IosSegmented(
                options = listOf(
                    "Memórias (${state.facts.size})",
                    "Diário (${state.events.size})",
                ),
                selectedIndex = if (showDiary) 1 else 0,
                onSelect = { showDiary = it == 1 },
            )
        }

        if (!showDiary) {
            if (filtered.isEmpty()) {
                Box(Modifier.weight(1f)) {
                    EmptyMemory(hasFacts = state.facts.isNotEmpty())
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(22.dp),
                ) {
                    grouped.forEach { (topic, facts) ->
                        item(key = "header-$topic") {
                            Column {
                                IosSectionHeader(topic)
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Surface),
                                ) {
                                    facts.forEachIndexed { index, fact ->
                                        FactRow(
                                            fact = fact,
                                            onSpeak = { onSpeak(fact.answer) },
                                            onReplay = { onReplay(fact) },
                                            onDelete = { confirmDelete = fact },
                                        )
                                        if (index != facts.lastIndex) {
                                            IosDivider(startIndent = 14.dp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            if (state.events.isEmpty()) {
                Box(Modifier.weight(1f)) { EmptyDiary() }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(22.dp),
                ) {
                    item {
                        IosSectionHeader("Diário de aprendizado")
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Surface),
                        ) {
                            state.events.forEachIndexed { index, event ->
                                EventRow(event)
                                if (index != state.events.lastIndex) {
                                    IosDivider(startIndent = 55.dp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirmClear) {
        IosAlert(
            title = "Apagar todas as memórias?",
            text = "Os fatos, o diário e a plasticidade sináptica serão apagados. " +
                "Essa ação não pode ser desfeita.",
            confirmLabel = "Apagar",
            destructive = true,
            onConfirm = {
                confirmClear = false
                onClearAll()
            },
            onDismiss = { confirmClear = false },
        )
    }

    confirmDelete?.let { fact ->
        IosAlert(
            title = "Esquecer esta memória?",
            text = fact.question,
            confirmLabel = "Esquecer",
            destructive = true,
            onConfirm = {
                confirmDelete = null
                onDelete(fact)
            },
            onDismiss = { confirmDelete = null },
        )
    }
}

@Composable
private fun RoundAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    enabled: Boolean,
    tint: androidx.compose.ui.graphics.Color = Teal,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(Fill)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = description,
            tint = if (enabled) tint else TextTertiary.copy(alpha = 0.5f),
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun FactRow(
    fact: Fact,
    onSpeak: () -> Unit,
    onReplay: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "força ${(fact.strength * 100).toInt()}%",
                fontSize = 12.sp,
                color = Teal,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.weight(1f))
            Text(formatTime(fact.createdAt), fontSize = 12.sp, color = TextTertiary)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            fact.question,
            fontSize = 17.sp,
            lineHeight = 22.sp,
            letterSpacing = (-0.2).sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            fact.answer,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            color = TextSecondary,
        )
        Spacer(Modifier.height(10.dp))
        StrengthBar(fact.strength)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "estabilidade ${(fact.stability * 100).toInt()}% · ${fact.trials} ensaios · " +
                    "${fact.daPulses} pulsos",
                fontSize = 12.sp,
                color = TextSecondary,
            )
            Spacer(Modifier.weight(1f))
            RowIcon(Icons.Rounded.VolumeUp, "Ouvir", Teal, onSpeak)
            Spacer(Modifier.width(6.dp))
            RowIcon(Icons.Rounded.Refresh, "Reconsolidar", Amber, onReplay)
            Spacer(Modifier.width(6.dp))
            RowIcon(Icons.Rounded.Delete, "Esquecer", Rose, onDelete)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "consultada ${fact.recallCount}x",
            fontSize = 12.sp,
            color = TextTertiary,
        )
    }
}

@Composable
private fun RowIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    tint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.12f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun EventRow(event: LearningEvent) {
    val (icon, tint, label) = when (event.kind) {
        EventKind.LEARN -> Triple(Icons.Rounded.MenuBook, Teal, "aprendeu")
        EventKind.RECALL -> Triple(Icons.Rounded.Psychology, Amber, "lembrou")
        EventKind.MISS -> Triple(Icons.Rounded.Search, Rose, "não sabia")
        EventKind.REPLAY -> Triple(Icons.Rounded.Refresh, Amber, "reconsolidou")
        EventKind.FORGET -> Triple(Icons.Rounded.Delete, Rose, "esqueceu")
        else -> Triple(Icons.Rounded.Psychology, TextSecondary, "evento")
    }
    Row(
        Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.Top,
    ) {
        IosIconTile(icon, tint)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, fontSize = 13.sp, color = tint, fontWeight = FontWeight.SemiBold)
                if (event.topic.isNotBlank()) {
                    Text(" · ${event.topic}", fontSize = 13.sp, color = TextSecondary)
                }
                Spacer(Modifier.weight(1f))
                Text(formatTime(event.createdAt), fontSize = 12.sp, color = TextTertiary)
            }
            Spacer(Modifier.height(2.dp))
            Text(event.title, fontSize = 15.sp, maxLines = 2)
            Text(event.detail, fontSize = 13.sp, color = TextSecondary, maxLines = 2)
            if (event.spikes > 0) {
                Text(
                    "${event.spikes} disparos · ${event.daPulses} pulsos de dopamina",
                    fontSize = 12.sp,
                    color = TextTertiary,
                )
            }
        }
    }
}

@Composable
private fun EmptyMemory(hasFacts: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Rounded.MenuBook, null, Modifier.size(42.dp), tint = Teal)
        Spacer(Modifier.height(14.dp))
        Text(
            if (hasFacts) "Nenhuma memória encontrada" else "A base está vazia",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            if (hasFacts) "Tente outro termo de busca."
            else "Cada fato ensinado vira uma memória com engrama, força e histórico de consultas.",
            fontSize = 15.sp,
            lineHeight = 20.sp,
            color = TextSecondary,
        )
    }
}

@Composable
private fun EmptyDiary() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Rounded.Psychology, null, Modifier.size(42.dp), tint = Amber)
        Spacer(Modifier.height(14.dp))
        Text("O diário ainda está vazio", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(
            "Cada sessão de ensino, consulta, acerto e esquecimento fica registrada aqui.",
            fontSize = 15.sp,
            lineHeight = 20.sp,
            color = TextSecondary,
        )
    }
}
