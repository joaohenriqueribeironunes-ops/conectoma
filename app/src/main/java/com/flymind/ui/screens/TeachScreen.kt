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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flymind.brain.TrainingOutcome
import com.flymind.ui.FlyUiState
import com.flymind.ui.IosSegmented
import com.flymind.ui.LegendDot
import com.flymind.ui.MetricPill
import com.flymind.ui.Panel
import com.flymind.ui.TraceChart
import com.flymind.ui.ios.IosButton
import com.flymind.ui.ios.IosProgress
import com.flymind.ui.ios.IosSlider
import com.flymind.ui.ios.IosTextField
import com.flymind.ui.ios.IosTintedButton
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
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        item { EnvironmentPanel(state) }

        item {
            IosSegmented(
                options = listOf("Fato único", "Caderno"),
                selectedIndex = if (batchMode) 1 else 0,
                onSelect = { batchMode = it == 1 },
            )
        }

        if (!batchMode) {
            item {
                Panel(
                    title = "O que a mosca deve aprender",
                    footer = "A pergunta vira o padrão sensorial; a resposta vira a memória.",
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        IosTextField(
                            value = topic,
                            onValueChange = { topic = it },
                            label = "Tema",
                            placeholder = "Biologia",
                        )
                        IosTextField(
                            value = question,
                            onValueChange = { question = it },
                            label = "Pergunta ou conceito",
                            placeholder = "Ex.: O que é fotossíntese?",
                            singleLine = false,
                            minLines = 2,
                        )
                        IosTextField(
                            value = answer,
                            onValueChange = { answer = it },
                            label = "Resposta que ela deve dar",
                            placeholder = "Ex.: plantas usam luz, água e CO2 para produzir glicose e oxigênio.",
                            singleLine = false,
                            minLines = 3,
                        )
                        SliderRow(
                            label = "Tentativas de treino",
                            value = "${trials.toInt()}",
                        ) {
                            IosSlider(
                                value = trials,
                                onValueChange = { trials = it },
                                valueRange = 3f..12f,
                                steps = 8,
                            )
                        }
                        IosButton(
                            text = if (state.teaching) "Ensinando..." else "Ensinar à mosca",
                            onClick = { onTeach(topic, question, answer, trials.toInt()) },
                            enabled = !state.teaching && question.isNotBlank() && answer.isNotBlank(),
                            icon = Icons.Rounded.Bolt,
                        )
                    }
                }
            }
        } else {
            item {
                Panel(
                    title = "Caderno de ensino",
                    footer = "Aceita os separadores  =  |  =>  e ignora linhas com #.",
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        IosTextField(
                            value = topic,
                            onValueChange = { topic = it },
                            label = "Tema do caderno",
                            placeholder = "Biologia",
                        )
                        IosTextField(
                            value = notebook,
                            onValueChange = { notebook = it },
                            label = "Uma memória por linha",
                            placeholder = "O que é fotossíntese? = Processo que converte luz em energia química.",
                            singleLine = false,
                            minLines = 6,
                        )
                        SliderRow(
                            label = "Tentativas por fato",
                            value = "${trials.toInt()}",
                        ) {
                            IosSlider(
                                value = trials,
                                onValueChange = { trials = it },
                                valueRange = 3f..12f,
                                steps = 8,
                            )
                        }
                        IosButton(
                            text = if (state.teaching) "Aprendendo caderno..." else "Aprender caderno inteiro",
                            onClick = { onTeachBatch(topic, notebook, trials.toInt()) },
                            enabled = !state.teaching && notebook.isNotBlank(),
                            icon = Icons.Rounded.MenuBook,
                        )
                        if (state.batchLabel != null) {
                            Text(state.batchLabel.orEmpty(), fontSize = 13.sp, color = Amber)
                        }
                    }
                }
            }
        }

        item {
            Panel(
                title = "Aprender de um site",
                footer = "A busca usa a internet só para baixar esta página — nada é enviado. " +
                    "O app não percorre o site inteiro: aprende o conteúdo da URL colada e usa " +
                    "o tema acima para agrupar os trechos.",
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    IosTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = "URL da página",
                        placeholder = "https://...",
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Uri,
                        ),
                    )
                    if (state.webLoading) {
                        IosProgress(progress = null)
                    }
                    if (state.webError != null) {
                        Text(state.webError.orEmpty(), fontSize = 13.sp, color = Rose)
                    }
                    state.webPreview?.let { page ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                page.title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                "${page.sections.size} trechos · ${page.chars} caracteres · ${page.host}",
                                fontSize = 13.sp,
                                color = TextSecondary,
                            )
                            page.sections.take(2).forEach { section ->
                                Text(
                                    "• ${section.title.ifEmpty { section.body.take(60) }}",
                                    fontSize = 13.sp,
                                    color = TextSecondary,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        IosTintedButton(
                            text = if (state.webLoading) "Buscando..." else "Buscar página",
                            onClick = { onPreviewUrl(url) },
                            enabled = !state.teaching && !state.webLoading,
                            icon = Icons.Rounded.Language,
                            modifier = Modifier.weight(1f),
                        )
                        IosButton(
                            text = "Aprender site",
                            onClick = { onTeachUrl(url, topic, trials.toInt()) },
                            enabled = !state.teaching && state.webPreview != null,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        if (state.lastTraining != null && !state.teaching) {
            item { TrainResultPanel(state.lastTraining) }
        }

        item {
            Panel(title = "Como o aprendizado acontece") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "O app embarca 6.909 neurônios e 44.893 sinapses do FlyWire FAFB v783 " +
                            "(Drosophila melanogaster), incluindo 3.000 células de Kenyon, 331 neurônios " +
                            "dopaminérgicos e as vias sensoriais de entrada.",
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = TextSecondary,
                    )
                    Text(
                        "Quando você ensina, o texto vira um padrão de estimulação sensorial. O circuito " +
                            "dispara e, ao fim de cada ensaio confirmado por você, o app libera uma rajada " +
                            "de dopamina. A dopamina converte a elegibilidade STDP em mudança real de peso " +
                            "sináptico — o mesmo princípio de recompensa dos corpos pedunculados.",
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = TextSecondary,
                    )
                    Text(
                        "Isso não é um chatbot: a resposta vem do fato que você ensinou, indexado pela " +
                            "assinatura de disparo do circuito.",
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = Teal,
                    )
                }
            }
        }
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: String,
    slider: @Composable () -> Unit,
) {
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontSize = 17.sp)
            Spacer(Modifier.weight(1f))
            Text(value, fontSize = 17.sp, color = TextSecondary)
        }
        Spacer(Modifier.height(2.dp))
        slider()
    }
}

@Composable
private fun EnvironmentPanel(state: FlyUiState) {
    Panel(title = "Ambiente simulado") {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
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
                IosProgress(
                    progress = progress?.let { it.trial.toFloat() / it.totalTrials },
                )
                Text(
                    if (progress == null) "Preparando estímulo..."
                    else "Ensaio ${progress.trial} de ${progress.totalTrials} · ${progress.spikes} disparos",
                    fontSize = 13.sp,
                    color = TextSecondary,
                )
                if (progress != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MetricPill("dopamina", "${progress.daPulses} pulsos", Amber)
                        MetricPill("estabilidade", "${(progress.stability * 100).toInt()}%", Teal)
                        MetricPill("assembleia", "${progress.activeNeurons}", Rose)
                    }
                }
            } else {
                Text(
                    "Circuito em repouso. A recompensa dopaminérgica só é liberada durante o treino.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = TextSecondary,
                )
                TraceChart(state.lastTrace)
            }
        }
    }
}

@Composable
private fun TrainResultPanel(outcome: TrainingOutcome) {
    Panel(title = "Memória consolidada") {
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
            Text("Engrama e atividade do último ensaio", fontSize = 13.sp, color = TextSecondary)
            TraceChart(outcome.trace)
            Text(
                "A barra de dopamina mostra a recompensa; a linha azul mostra a fração de neurônios " +
                    "disparando a cada milissegundo.",
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = TextSecondary,
            )
        }
    }
}
