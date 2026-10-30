package com.flymind.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flymind.data.EventKind
import com.flymind.data.Fact
import com.flymind.data.LearningEvent
import com.flymind.ui.FlyUiState
import com.flymind.ui.IosSegmented
import com.flymind.ui.Panel
import com.flymind.ui.StrengthBar
import com.flymind.ui.formatTime
import com.flymind.ui.theme.Amber
import com.flymind.ui.theme.Outline
import com.flymind.ui.theme.Separator
import com.flymind.ui.theme.Surface
import com.flymind.ui.theme.Rose
import com.flymind.ui.theme.Teal
import com.flymind.ui.theme.TextSecondary

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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Buscar memória") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
            )
            IconButton(onClick = onExport, enabled = state.facts.isNotEmpty()) {
                Icon(Icons.Rounded.FileUpload, contentDescription = "Exportar memórias", tint = Teal)
            }
            IconButton(onClick = { confirmClear = true }, enabled = state.facts.isNotEmpty()) {
                Icon(Icons.Rounded.DeleteSweep, contentDescription = "Apagar tudo", tint = Rose)
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
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
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    grouped.forEach { (topic, facts) ->
                        item(key = "header-$topic") {
                            Text(
                                topic.uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Amber,
                                modifier = Modifier.padding(top = 6.dp, start = 4.dp),
                            )
                        }
                        items(facts, key = { it.id }) { fact ->
                            FactCard(
                                fact = fact,
                                onSpeak = { onSpeak(fact.answer) },
                                onReplay = { onReplay(fact) },
                                onDelete = { confirmDelete = fact },
                            )
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
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.events, key = { it.id }) { event -> EventCard(event) }
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            shape = RoundedCornerShape(14.dp),
            onDismissRequest = { confirmClear = false },
            title = { Text("Apagar todas as memórias?") },
            text = {
                Text(
                    "Os fatos, o diário e a plasticidade sináptica serão apagados. " +
                        "Essa ação não pode ser desfeita.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    onClearAll()
                }) { Text("Apagar", color = Rose) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("Cancelar") }
            },
        )
    }

    confirmDelete?.let { fact ->
        AlertDialog(
            shape = RoundedCornerShape(14.dp),
            onDismissRequest = { confirmDelete = null },
            title = { Text("Esquecer esta memória?") },
            text = { Text(fact.question) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = null
                    onDelete(fact)
                }) { Text("Esquecer", color = Rose) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = null }) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun FactCard(
    fact: Fact,
    onSpeak: () -> Unit,
    onReplay: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        border = BorderStroke(0.5.dp, Separator.copy(alpha = 0.6f)),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    fact.topic,
                    fontSize = 10.sp,
                    color = Amber,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "força ${(fact.strength * 100).toInt()}%",
                    fontSize = 10.sp,
                    color = Teal,
                )
            }
            Spacer(Modifier.height(5.dp))
            Text(
                fact.question,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(5.dp))
            Text(
                fact.answer,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 4,
            )
            Spacer(Modifier.height(10.dp))
            StrengthBar(fact.strength)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "estabilidade ${(fact.stability * 100).toInt()}% · ${fact.trials} ensaios · " +
                        "${fact.daPulses} pulsos",
                    fontSize = 10.sp,
                    color = TextSecondary,
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onSpeak, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Rounded.VolumeUp, "Ouvir", Modifier.size(17.dp), tint = Teal)
                }
                IconButton(onClick = onReplay, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Rounded.Refresh, "Reconsolidar", Modifier.size(17.dp), tint = Amber)
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Rounded.Delete, "Esquecer", Modifier.size(17.dp), tint = Rose)
                }
            }
            Text(
                "consultada ${fact.recallCount}x · ${formatTime(fact.lastRecalledAt)}",
                fontSize = 9.sp,
                color = Outline,
            )
        }
    }
}

@Composable
private fun EventCard(event: LearningEvent) {
    val (icon, color, label) = when (event.kind) {
        EventKind.LEARN -> Triple(Icons.Rounded.MenuBook, Teal, "aprendeu")
        EventKind.RECALL -> Triple(Icons.Rounded.Psychology, Amber, "lembrou")
        EventKind.MISS -> Triple(Icons.Rounded.Search, Rose, "não sabia")
        EventKind.REPLAY -> Triple(Icons.Rounded.Refresh, Amber, "reconsolidou")
        EventKind.LINK -> Triple(Icons.Rounded.Hub, Teal, "conectou")
        EventKind.FORGET -> Triple(Icons.Rounded.Delete, Rose, "esqueceu")
        else -> Triple(Icons.Rounded.Psychology, TextSecondary, "evento")
    }
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        border = BorderStroke(0.5.dp, Separator.copy(alpha = 0.6f)),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(label, fontSize = 10.sp, color = color, fontWeight = FontWeight.Bold)
                    if (event.topic.isNotBlank()) {
                        Text(
                            " · ${event.topic}",
                            fontSize = 10.sp,
                            color = TextSecondary,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Text(formatTime(event.createdAt), fontSize = 9.sp, color = TextSecondary)
                }
                Spacer(Modifier.height(3.dp))
                Text(event.title, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                Text(
                    event.detail,
                    fontSize = 10.sp,
                    color = TextSecondary,
                    maxLines = 2,
                )
                if (event.spikes > 0) {
                    Text(
                        "${event.spikes} disparos · ${event.daPulses} pulsos de dopamina",
                        fontSize = 9.sp,
                        color = Outline,
                    )
                }
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
        Icon(Icons.Rounded.MenuBook, null, Modifier.size(48.dp), tint = Teal)
        Spacer(Modifier.height(12.dp))
        Text(
            if (hasFacts) "Nenhuma memória encontrada" else "A base está vazia",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            if (hasFacts) "Tente outro termo de busca."
            else "Cada fato ensinado vira uma memória com engrama, força e histórico de consultas.",
            fontSize = 12.sp,
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
        Icon(Icons.Rounded.Psychology, null, Modifier.size(48.dp), tint = Amber)
        Spacer(Modifier.height(12.dp))
        Text("O diário ainda está vazio", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            "Cada sessão de ensino, consulta, acerto e esquecimento fica registrada aqui.",
            fontSize = 12.sp,
            color = TextSecondary,
        )
    }
}
