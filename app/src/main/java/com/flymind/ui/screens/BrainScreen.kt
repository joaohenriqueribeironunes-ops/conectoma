package com.flymind.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.RestartAlt
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
import com.flymind.ui.FlyUiState
import com.flymind.ui.LegendDot
import com.flymind.ui.MetricPill
import com.flymind.ui.Panel
import com.flymind.ui.SpikeRaster
import com.flymind.ui.StatBlock
import com.flymind.ui.StrengthBar
import com.flymind.ui.TraceChart
import com.flymind.ui.ios.IosAlert
import com.flymind.ui.ios.IosButton
import com.flymind.ui.ios.IosGrayButton
import com.flymind.ui.ios.IosProgress
import com.flymind.ui.theme.Amber
import com.flymind.ui.theme.Rose
import com.flymind.ui.theme.Teal
import com.flymind.ui.theme.TextSecondary

@Composable
fun BrainScreen(
    state: FlyUiState,
    onSpontaneous: () -> Unit,
    onResetPlasticity: () -> Unit,
) {
    var confirmReset by rememberSaveable { mutableStateOf(false) }
    val status = state.brain

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        item {
            Panel(
                title = "Conectoma FlyWire FAFB v783",
                footer = "Recorte de aprendizado embarcado no APK, extraído do conectoma público.",
            ) {
                if (status == null) {
                    Text(
                        state.brainError ?: "Carregando conectoma...",
                        fontSize = 15.sp,
                        color = if (state.brainError != null) Rose else TextSecondary,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            StatBlock("neurônios", "${status.stats.neurons}", Modifier.weight(1f))
                            StatBlock("sinapses", "${status.stats.synapses}", Modifier.weight(1f))
                            StatBlock("tipos", "${status.stats.labels}", Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            StatBlock("sensoriais", "${status.stats.inputs}", Modifier.weight(1f))
                            StatBlock("dopaminérgicos", "${status.stats.dopaminergic}", Modifier.weight(1f))
                            StatBlock("saídas", "${status.stats.outputs}", Modifier.weight(1f))
                        }
                        Text(
                            "${status.stats.interneurons} interneurônios · grau médio de saída " +
                                String.format("%.1f", status.stats.meanOutDegree),
                            fontSize = 13.sp,
                            color = TextSecondary,
                        )
                        status.stats.topLabels.chunked(2).forEach { pair ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                pair.forEach { (label, count) ->
                                    MetricPill(label, "$count", Teal)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Panel(title = "Plasticidade sináptica") {
                if (status == null) {
                    Text("Sem dados do circuito.", color = TextSecondary, fontSize = 15.sp)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    String.format("%.2f%%", status.weightDelta * 100f),
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.3).sp,
                                    color = Amber,
                                )
                                Text(
                                    "mudança média absoluta de peso",
                                    fontSize = 13.sp,
                                    color = TextSecondary,
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "${status.plasticSynapses}",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Teal,
                                )
                                Text("sinapses plásticas", fontSize = 13.sp, color = TextSecondary)
                            }
                        }
                        StrengthBar(
                            fraction = (status.weightDelta * 4f).coerceIn(0f, 1f),
                            color = Amber,
                        )
                        Text(
                            if (status.snapshotLoaded) {
                                "Snapshot carregado: o aprendizado sobrevive ao fechar o app."
                            } else {
                                "Sem snapshot: o circuito está no estado original do FlyWire."
                            },
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = TextSecondary,
                        )
                        IosGrayButton(
                            text = "Voltar aos pesos originais",
                            onClick = { confirmReset = true },
                            icon = Icons.Rounded.RestartAlt,
                        )
                    }
                }
            }
        }

        item {
            Panel(
                title = "Atividade do circuito",
                footer = "Estimulação espontânea por 1,4 segundo, sem recompensa.",
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    IosButton(
                        text = if (state.spontaneousRunning) "Estimulando..." else "Estimular circuito",
                        onClick = onSpontaneous,
                        enabled = !state.spontaneousRunning && status != null,
                        icon = Icons.Rounded.Bolt,
                    )
                    if (state.spontaneousRunning) {
                        IosProgress(progress = null)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        LegendDot(Teal, "fração de neurônios ativos")
                        LegendDot(Amber, "dopamina")
                    }
                    TraceChart(state.lastTrace)
                    Text(
                        "Raster de disparos (tempo x neurônio)",
                        fontSize = 13.sp,
                        color = TextSecondary,
                    )
                    SpikeRaster(state.lastTrace)
                    val trace = state.lastTrace
                    if (trace != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MetricPill("disparos", "${trace.spikes}", Teal)
                            MetricPill("ativos", "${trace.activeNeurons}", TextSecondary)
                            MetricPill("dopamina", String.format("%.2f", trace.meanDopamine), Amber)
                        }
                    }
                }
            }
        }

        item {
            Panel(title = "O que este circuito faz") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Neurônios leaky integrate-and-fire com atraso sináptico de 1–2 ms. " +
                            "A plasticidade é STDP de três fatores: pares pré/pós geram elegibilidade, " +
                            "e a dopamina — liberada quando a resposta é reforçada — converte elegibilidade " +
                            "em mudança de peso.",
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = TextSecondary,
                    )
                    Text(
                        "As conexões, os tipos celulares e os neurotransmissores vêm do FlyWire FAFB v783. " +
                            "O recorte prioriza corpos pedunculados, neurônios dopaminérgicos e vias sensoriais; " +
                            "é um modelo de aprendizado associativo, não uma réplica completa do cérebro da mosca.",
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = TextSecondary,
                    )
                    Text(
                        "Ela não entende linguagem livre como um LLM. Ela associa a assinatura de disparo " +
                            "da pergunta aos fatos que você ensinou e responde com a memória mais próxima.",
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = Teal,
                    )
                }
            }
        }

        item {
            Text(
                "Dados: Dorkenwald et al., Nature 2024 (FlyWire) e Schlegel et al., Nature 2024 " +
                    "(anotações), CC-BY-4.0. Recorte e simulação deste app são independentes do consórcio.",
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }

    if (confirmReset) {
        IosAlert(
            title = "Restaurar pesos originais?",
            text = "Toda a plasticidade aprendida será descartada. As memórias continuam na base, " +
                "mas os engramas ficam desatualizados.",
            confirmLabel = "Restaurar",
            destructive = true,
            onConfirm = {
                confirmReset = false
                onResetPlasticity()
            },
            onDismiss = { confirmReset = false },
        )
    }
}
