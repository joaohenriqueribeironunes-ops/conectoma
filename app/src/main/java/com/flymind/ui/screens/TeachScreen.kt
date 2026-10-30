package com.flymind.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flymind.brain.TrainingOutcome
import com.flymind.ui.FlyUiState
import com.flymind.ui.IosSegmented
import com.flymind.ui.LegendDot
import com.flymind.ui.MetricPill
import com.flymind.ui.Panel
import com.flymind.ui.StrengthBar
import com.flymind.ui.TraceChart
import com.flymind.ui.theme.Amber
import com.flymind.ui.theme.Rose
import com.flymind.ui.theme.Teal
import com.flymind.ui.theme.TextSecondary

@Composable
fun TeachScreen(
    state: FlyUiState,
    onTeach: (String, String, String, Int) -> Unit,
    onTeachBatch: (String, String, Int) -> Unit,
    onPreviewUrl: (String) -> Unit,
    onTeachUrl: (String, String, Int) -> Unit,
) {
    var topic by rememberSaveable { mutableStateOf("Biologia") }
    var question by rememberSaveable { mutableStateOf("") }
    var answer by rememberSaveable { mutableStateOf("") }
    var notebook by rememberSaveable { mutableStateOf("") }
    var trials by rememberSaveable { mutableStateOf(6f) }
    var batchMode by rememberSaveable { mutableStateOf(false) }
    var url by rememberSaveable { mutableStateOf("") }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            EnvironmentPanel(state)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IosSegmented(
                    options = listOf("Fato único", "Caderno"),
                    selectedIndex = if (batchMode) 1 else 0,
                    onSelect = { batchMode = it == 1 },
                )
            }
        }

        if (!batchMode) {
            item {
                Panel(
                    title = "O que a mosca deve aprender",
                    subtitle = "Um fato por vez. A pergunta vira o padrão sensorial; a resposta vira a memória.",
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = topic,
                            onValueChange = { topic = it },
                            label = { Text("Tema") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = question,
                            onValueChange = { question = it },
                            label = { Text("Pergunta ou conceito") },
                            placeholder = { Text("Ex.: O que é fotossíntese?") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = answer,
                            onValueChange = { answer = it },
                            label = { Text("Resposta que ela deve dar") },
                            placeholder = {
                                Text("Ex.: É o processo em que plantas usam luz, água e CO2 para produzir glicose e oxigênio.")
                            },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            "Tentativas de treino: ${trials.toInt()}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Slider(
                            value = trials,
                            onValueChange = { trials = it },
                            valueRange = 3f..12f,
                            steps = 8,
                        )
                        Button(
                            onClick = { onTeach(topic, question, answer, trials.toInt()) },
                            enabled = !state.teaching && question.isNotBlank() && answer.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                        ) {
                            Icon(Icons.Rounded.Bolt, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (state.teaching) "Ensinando..." else "Ensinar à mosca")
                        }
                    }
                }
            }
        } else {
            item {
                Panel(
                    title = "Caderno de ensino",
                    subtitle = "Cole tudo o que ela deve aprender. Uma linha por memória.",
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = topic,
                            onValueChange = { topic = it },
                            label = { Text("Tema do caderno") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = notebook,
                            onValueChange = { notebook = it },
                            label = { Text("pergunta = resposta") },
                            placeholder = {
                                Text(
                                    "O que é fotossíntese? = Processo que converte luz em energia química.\n" +
                                        "Quem foi Santos Dumont? = Pioneiro da aviação, voou com o 14-Bis em 1906.\n" +
                                        "O que é entropia? = Medida da desordem de um sistema.",
                                )
                            },
                            minLines = 6,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            "Aceita os separadores  =  |  =>  e ignora linhas com #.",
                            fontSize = 11.sp,
                            color = TextSecondary,
                        )
                        Text(
                            "Tentativas por fato: ${trials.toInt()}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Slider(
                            value = trials,
                            onValueChange = { trials = it },
                            valueRange = 3f..12f,
                            steps = 8,
                        )
                        Button(
                            onClick = { onTeachBatch(topic, notebook, trials.toInt()) },
                            enabled = !state.teaching && notebook.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                        ) {
                            Icon(Icons.Rounded.MenuBook, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (state.teaching) "Aprendendo caderno..." else "Aprender caderno inteiro")
                        }
                        if (state.batchLabel != null) {
                            Text(state.batchLabel, color = Amber, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        item {
            Panel(
                title = "Aprender de um site",
                subtitle = "Cole a URL de uma página e a mosca aprende o conteúdo dela.",
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("URL da página") },
                        placeholder = { Text("https://...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { onPreviewUrl(url) },
                            enabled = !state.teaching && !state.webLoading,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(if (state.webLoading) "Buscando..." else "Buscar página", fontSize = 14.sp)
                        }
                        Button(
                            onClick = { onTeachUrl(url, topic, trials.toInt()) },
                            enabled = !state.teaching && state.webPreview != null,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Aprender site", fontSize = 14.sp)
                        }
                    }
                    if (state.webLoading) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            color = Teal,
                            trackColor = Teal.copy(alpha = 0.15f),
                        )
                    }
                    if (state.webError != null) {
                        Text(state.webError, fontSize = 12.sp, color = Rose)
                    }
                    state.webPreview?.let { page ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                page.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                            )
                            Text(
                                "${page.sections.size} trechos · ${page.chars} caracteres · ${page.host}",
                                fontSize = 11.sp,
                                color = TextSecondary,
                            )
                            page.sections.take(2).forEach { section ->
                                Text(
                                    "• ${section.title.ifEmpty { section.body.take(60) }}",
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                    Text(
                        "A busca usa a internet só para baixar esta página — nada é enviado. " +
                            "O app não percorre o site inteiro: aprende o conteúdo da URL colada " +
                            "e usa o tema acima para agrupar os trechos.",
                        fontSize = 11.sp,
                        color = TextSecondary,
                    )
                }
            }
        }

        if (state.lastTraining != null && !state.teaching) {
            item { TrainResultPanel(state.lastTraining) }
        }

        item {
            Panel(
                title = "Como o aprendizado acontece",
                subtitle = "Circuito real, plasticidade simulada",
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(
                        "O app embarca 6.909 neurônios e 44.893 sinapses do FlyWire FAFB v783 " +
                            "(Drosophila melanogaster), incluindo 3.000 células de Kenyon, 331 neurônios " +
                            "dopaminérgicos e as vias sensoriais de entrada.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                    Text(
                        "Quando você ensina, o texto vira um padrão de estimulação sensorial. O circuito " +
                            "dispara e, ao fim de cada ensaio confirmado por você, o app libera uma rajada " +
                            "de dopamina. A dopamina converte a elegibilidade STDP em mudança real de peso " +
                            "sináptico — o mesmo princípio de recompensa dos corpos pedunculados.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                    Text(
                        "Isso não é um chatbot: a resposta vem do fato que você ensinou, indexado pela " +
                            "assinatura de disparo do circuito.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Teal,
                    )
                }
            }
        }
    }
}

@Composable
private fun EnvironmentPanel(state: FlyUiState) {
    Panel(
        title = "Ambiente simulado",
        subtitle = "Estímulo sensorial → conectoma → recompensa",
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricPill("visual", "luz", Teal)
                MetricPill("olfatório", "odor", Amber)
                MetricPill("mecânico", "toque", Rose)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                LegendDot(Teal, "atividade")
                LegendDot(Amber, "dopamina")
            }
            val progress = state.progress
            if (state.teaching) {
                LinearProgressIndicator(
                    progress = {
                        if (progress == null) 0f
                        else progress.trial.toFloat() / progress.totalTrials
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = Teal,
                    trackColor = Teal.copy(alpha = 0.15f),
                )
                Text(
                    if (progress == null) "Preparando estímulo..."
                    else "Ensaio ${progress.trial} de ${progress.totalTrials} · ${progress.spikes} disparos",
                    fontSize = 12.sp,
                    color = TextSecondary,
                )
                if (progress != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MetricPill("dopamina", "${progress.daPulses} pulsos", Amber)
                        MetricPill(
                            "estabilidade",
                            "${(progress.stability * 100).toInt()}%",
                            Teal,
                        )
                        MetricPill("assembleia", "${progress.activeNeurons}", Rose)
                    }
                }
            } else {
                Text(
                    "Circuito em repouso. A recompensa dopaminérgica só é liberada durante o treino.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                )
                TraceChart(state.lastTrace)
            }
        }
    }
}

@Composable
private fun TrainResultPanel(outcome: TrainingOutcome) {
    Panel(
        title = "Memória consolidada",
        subtitle = "Resultado do último treino",
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricPill("estabilidade", "${(outcome.stability * 100).toInt()}%", Teal)
                MetricPill("disparos", "${outcome.spikes}", Rose)
                MetricPill("dopamina", "${outcome.daPulses} pulsos", Amber)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricPill("plasticidade", String.format("%.1f%%", outcome.weightDelta * 100f), Amber)
                MetricPill("assembleia", "${outcome.assembly} neurônios", Teal)
            }
            Text("Engrama e atividade do último ensaio", fontSize = 11.sp, color = TextSecondary)
            TraceChart(outcome.trace)
            Text(
                "A barra de dopamina mostra a recompensa; a linha clara mostra a fração de neurônios " +
                    "disparando a cada milissegundo.",
                fontSize = 11.sp,
                color = TextSecondary,
            )
        }
    }
}
