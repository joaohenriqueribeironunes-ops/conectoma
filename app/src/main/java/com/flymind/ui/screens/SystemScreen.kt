package com.flymind.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
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
import com.flymind.ui.ios.IosGrayButton
import com.flymind.ui.ios.IosIconTile
import com.flymind.ui.ios.IosSwitch
import com.flymind.ui.theme.Amber
import com.flymind.ui.theme.Teal
import com.flymind.ui.theme.TextSecondary

@Composable
fun SystemScreen(
    state: FlyUiState,
    onKeepScreenOn: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        item {
            Panel(title = "Aparelho") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IosIconTile(Icons.Rounded.PhoneAndroid, Teal, size = 34)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(HyperOs.deviceLabel(), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        Text(HyperOs.androidLabel(), fontSize = 13.sp, color = TextSecondary)
                        Text(HyperOs.hyperOsLabel(), fontSize = 13.sp, color = Amber)
                    }
                }
            }
        }

        item {
            Panel(
                title = "Desempenho no HyperOS",
                footer = "A MIUI/HyperOS costuma restringir apps em segundo plano. O Conectoma não usa " +
                    "serviço persistente: a simulação roda só enquanto a tela está ativa.",
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    IosGrayButton(
                        text = "Otimização de bateria",
                        onClick = { HyperOs.openBatterySettings(context) },
                        icon = Icons.Rounded.BatterySaver,
                    )
                    IosGrayButton(
                        text = "Autorização de inicialização automática",
                        onClick = { HyperOs.openAutostart(context) },
                        icon = Icons.Rounded.PowerSettingsNew,
                    )
                    IosGrayButton(
                        text = "Restrições de economia de energia",
                        onClick = { HyperOs.openPowerKeeper(context) },
                        icon = Icons.Rounded.Settings,
                    )
                }
            }
        }

        item {
            Panel(title = "Sessões longas") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Manter tela ligada no app", fontSize = 17.sp)
                        Text(
                            "Aplicado imediatamente ao voltar para o app.",
                            fontSize = 13.sp,
                            color = TextSecondary,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    IosSwitch(checked = state.keepScreenOn, onCheckedChange = onKeepScreenOn)
                }
            }
        }

        item {
            Panel(
                title = "Como o app se integra",
                footer = "Sem contas, sem nuvem e sem telemetria.",
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Bullet("Tema claro no estilo iOS, com ícone monocromático para os ícones temáticos do HyperOS.")
                    Bullet("Permissão única de internet, usada só para buscar as URLs que você pedir.")
                    Bullet("Atalhos de bateria e autostart abrem as telas nativas da MIUI/HyperOS.")
                    Bullet("Sem serviço em segundo plano e sem notificação permanente.")
                }
            }
        }

        item {
            Panel(title = "Escopo e limites") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IosIconTile(Icons.Rounded.Info, Amber, size = 30)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Transparência científica",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Amber,
                        )
                    }
                    Text(
                        "O Conectoma simula um recorte de 6.909 neurônios do conectoma FlyWire (44.893 " +
                            "sinapses). Não é o cérebro inteiro (139.255 neurônios) nem um modelo de " +
                            "consciência. A resposta é recuperação associativa de fatos ensinados, com " +
                            "similaridade lexical como apoio — não geração livre de texto.",
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = TextSecondary,
                    )
                    Text(
                        "A dopamina aqui é um sinal computacional de recompensa: ela modula a " +
                            "plasticidade STDP e altera pesos reais do recorte. O app mede e mostra " +
                            "essa mudança, mas não afirma que a mosca biológica 'aprenda' o conteúdo.",
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = TextSecondary,
                    )
                }
            }
        }

        item {
            val version = runCatching {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            }.getOrNull().orEmpty()
            val label = if (version.isBlank()) "Conectoma" else "Conectoma $version"
            Text(
                "$label · dados CC-BY-4.0 · Dorkenwald et al. 2024; Schlegel et al. 2024",
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}

@Composable
private fun Bullet(text: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text("•", fontSize = 13.sp, color = TextSecondary)
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 13.sp, lineHeight = 18.sp, color = TextSecondary)
    }
}
