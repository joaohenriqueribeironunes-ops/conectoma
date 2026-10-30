package com.flymind.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flymind.hyperos.HyperOs
import com.flymind.ui.FlyUiState
import com.flymind.ui.Panel
import com.flymind.ui.theme.Amber
import com.flymind.ui.theme.Teal
import com.flymind.ui.theme.TextSecondary

@Composable
fun SystemScreen(
    state: FlyUiState,
    onKeepScreenOn: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Voltar")
                }
                Text(
                    "Sistema e HyperOS",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        item {
            Panel(
                title = "Aparelho",
                subtitle = "Integração com o HyperOS 1 do Redmi Note 11S",
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.PhoneAndroid,
                        contentDescription = null,
                        tint = Teal,
                        modifier = Modifier.size(30.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(HyperOs.deviceLabel(), fontWeight = FontWeight.SemiBold)
                        Text(HyperOs.androidLabel(), fontSize = 11.sp, color = TextSecondary)
                        Text(HyperOs.hyperOsLabel(), fontSize = 11.sp, color = Amber)
                    }
                }
            }
        }

        item {
            Panel(
                title = "Desempenho no HyperOS",
                subtitle = "Libere o processamento do circuito quando o app estiver aberto",
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "A MIUI/HyperOS costuma restringir apps em segundo plano. O FlyMind não usa " +
                            "serviço persistente: a simulação roda só enquanto a tela está ativa. Ainda assim, " +
                            "estes atalhos evitam que o sistema interrompa o treino.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                    Button(
                        onClick = { HyperOs.openBatterySettings(context) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Rounded.BatterySaver, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Otimização de bateria")
                    }
                    Button(
                        onClick = { HyperOs.openAutostart(context) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Rounded.PowerSettingsNew, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Autorização de inicialização automática")
                    }
                    OutlinedButton(
                        onClick = { HyperOs.openPowerKeeper(context) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Rounded.Settings, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Restrições de economia de energia (MIUI)")
                    }
                }
            }
        }

        item {
            Panel(
                title = "Sessões longas",
                subtitle = "Mantenha a tela ligada enquanto ensina um caderno grande",
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Manter tela ligada no app", fontSize = 13.sp)
                        Text(
                            "Aplicado imediatamente ao voltar para o app.",
                            fontSize = 10.sp,
                            color = TextSecondary,
                        )
                    }
                    Switch(
                        checked = state.keepScreenOn,
                        onCheckedChange = onKeepScreenOn,
                    )
                }
            }
        }

        item {
            Panel(
                title = "Como o app se integra",
                subtitle = "Sem permissões, sem nuvem, sem telemetria",
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "• Tema escuro e ícone monocromático compatíveis com ícones temáticos do HyperOS.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                    )
                    Text(
                        "• Nenhuma permissão sensível: o TTS é do sistema e o banco fica local.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                    )
                    Text(
                        "• Atalhos de bateria/autostart abrem as telas nativas da MIUI/HyperOS.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                    )
                    Text(
                        "• Sem serviço em segundo plano e sem notificação permanente.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                    )
                }
            }
        }

        item {
            Panel(
                title = "Escopo e limites",
                subtitle = "Transparência científica",
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Rounded.Info, contentDescription = null, tint = Amber)
                    Text(
                        "O FlyMind simula um recorte de 6.909 neurônios do conectoma FlyWire (44.893 " +
                            "sinapses). Não é o cérebro inteiro (139.255 neurônios) nem um modelo de " +
                            "consciência. A resposta é recuperação associativa de fatos ensinados, com " +
                            "similaridade lexical como apoio — não geração livre de texto.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                    Text(
                        "A dopamina aqui é um sinal computacional de recompensa: ela modula a " +
                            "plasticidade STDP e altera pesos reais do recorte. O app mede e mostra " +
                            "essa mudança, mas não afirma que a mosca biológica 'aprenda' o conteúdo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
            }
        }

        item {
            Text(
                "FlyMind 1.0 · dados CC-BY-4.0 · Dorkenwald et al. 2024; Schlegel et al. 2024",
                fontSize = 10.sp,
                color = TextSecondary,
                modifier = Modifier.padding(4.dp),
            )
        }
    }
}
