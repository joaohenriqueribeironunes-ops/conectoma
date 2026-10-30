package com.flymind.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.flymind.ui.theme.Amber
import com.flymind.ui.theme.Fill
import com.flymind.ui.theme.Rose
import com.flymind.ui.theme.Separator
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
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Panel(
                title = "Conectoma FlyWire FAFB v783",
                subtitle = "Subcircuito de aprendizado embarcado no APK",
            ) {
                if (status == null) {
                    Text(
                        state.brainError ?: "Carregando conectoma...",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (state.brainError != null) Rose else TextSecondary,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            StatBlock("neurônios", "${status.stats.neurons}", Modifier.weight(1f))
                            StatBlock("sinapses", "${status.stats.synapses}", Modifier.weight(1f))
                            StatBlock("tipos celulares", "${status.stats.labels}", Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            StatBlock("sensoriais", "${status.stats.inputs}", Modifier.weight(1f))
                            StatBlock("dopaminérgicos", "${status.stats.dopaminergic}", Modifier.weight(1f))
                            StatBlock("saídas", "${status.stats.outputs}", Modifier.weight(1f))
                        }
                        Text(
                            "${status.stats.interneurons} interneurônios · grau médio de saída " +
                                String.format("%.1f", status.stats.meanOutDegree),
                            fontSize = 11.sp,
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
            Panel(
                title = "Plasticidade sináptica",
                subtitle = "Pesos alterados pela dopamina desde o estado original",
            ) {
                if (status == null) {
                    Text("Sem dados do circuito.", color = TextSecondary, fontSize = 12.sp)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    String.format("%.2f%%", status.weightDelta * 100f),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Amber,
                                )
                                Text(
                                    "mudança média absoluta de peso",
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "${status.plasticSynapses}",
                                    fontWeight = FontWeight.SemiBold,
                                    color = Teal,
                                )
                                Text("sinapses plásticas", fontSize = 11.sp, color = TextSecondary)
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
                            fontSize = 11.sp,
                            color = TextSecondary,
                        )
                        OutlinedButton(
                            onClick = { confirmReset = true },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(0.5.dp, Separator),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Fill,
                                contentColor = Teal,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Rounded.RestartAlt, contentDescription = null)
                            Spacer(Modifier.weight(1f))
                            Text("Voltar aos pesos originais")
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        item {
            Panel(
                title = "Atividade do circuito",
                subtitle = "Estimulação espontânea por 1,4 segundo",
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onSpontaneous,
                        enabled = !state.spontaneousRunning && status != null,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Rounded.Bolt, contentDescription = null)
                        Spacer(Modifier.weight(1f))
                        Text(if (state.spontaneousRunning) "Estimulando..." else "Estimular circuito")
                        Spacer(Modifier.weight(1f))
                    }
                    if (state.spontaneousRunning) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp),
                            color = Teal,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        LegendDot(Teal, "fração de neurônios ativos")
                        LegendDot(Amber, "dopamina")
                    }
                    TraceChart(state.lastTrace)
                    Text("Raster de disparos (tempo x neurônio)", fontSize = 11.sp, color = TextSecondary)
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
            Panel(
                title = "O que este circuito faz",
                subtitle = "Escopo científico do modelo",
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Neurônios leaky integrate-and-fire com atraso sináptico de 1–2 ms. " +
                            "A plasticidade é STDP de três fatores: pares pré/pós geram elegibilidade, " +
                            "e a dopamina — liberada quando a resposta é reforçada — converte elegibilidade " +
                            "em mudança de peso.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                    Text(
                        "As conexões, os tipos celulares e os neurotransmissores vêm do FlyWire FAFB v783. " +
                            "O recorte prioriza corpos pedunculados, neurônios dopaminérgicos e vias sensoriais; " +
                            "é um modelo de aprendizado associativo, não uma réplica completa do cérebro da mosca.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                    Text(
                        "Ela não entende linguagem livre como um LLM. Ela associa a assinatura de disparo " +
                            "da pergunta aos fatos que você ensinou e responde com a memória mais próxima.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Teal,
                    )
                }
            }
        }

        item {
            Text(
                "Dados: Dorkenwald et al., Nature 2024 (FlyWire) e Schlegel et al., Nature 2024 " +
                    "(anotações), CC-BY-4.0. Recorte e simulação deste app são independentes do consórcio.",
                fontSize = 10.sp,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            )
        }
    }

    if (confirmReset) {
        androidx.compose.material3.AlertDialog(
            shape = RoundedCornerShape(14.dp),
            onDismissRequest = { confirmReset = false },
            title = { Text("Restaurar pesos originais?") },
            text = {
                Text(
                    "Toda a plasticidade aprendida será descartada. As memórias continuam na base, " +
                        "mas os engramas ficam desatualizados.",
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    confirmReset = false
                    onResetPlasticity()
                }) { Text("Restaurar", color = Rose) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { confirmReset = false }) {
                    Text("Cancelar")
                }
            },
        )
    }
}
