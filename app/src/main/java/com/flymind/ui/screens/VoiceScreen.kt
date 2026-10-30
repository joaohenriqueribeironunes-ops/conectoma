package com.flymind.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.flymind.ui.Panel
import com.flymind.ui.formatDuration
import com.flymind.ui.formatSize
import com.flymind.ui.theme.Amber
import com.flymind.ui.theme.Fill
import com.flymind.ui.theme.Rose
import com.flymind.ui.theme.Separator
import com.flymind.ui.theme.Teal
import com.flymind.ui.theme.TextSecondary
import com.flymind.voice.VoiceInfo
import com.flymind.voice.VoiceSettings

@Composable
fun VoiceScreen(
    state: FlyUiState,
    onSettings: (VoiceSettings) -> Unit,
    onTest: (String) -> Unit,
    onStop: () -> Unit,
    onImportFile: (Uri) -> Unit,
    onPlayFile: () -> Unit,
    onRemoveFile: () -> Unit,
    onToggleFile: (Boolean) -> Unit,
) {
    var showPicker by rememberSaveable { mutableStateOf(false) }
    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) onImportFile(uri)
    }
    var testPhrase by rememberSaveable {
        mutableStateOf("Olá. Eu sou a mosca do FlyMind. Aprendi sobre o mundo com você.")
    }
    val settings = state.voiceSettings
    val selected = state.voices.firstOrNull { it.name == settings.voiceName }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Panel(
                title = "Voz da mosca",
                subtitle = "Escolha a voz instalada no aparelho, o tom e a velocidade.",
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.RecordVoiceOver,
                            contentDescription = null,
                            tint = if (state.ttsReady) Teal else Rose,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (state.ttsReady) "Motor de voz pronto" else "Motor de voz indisponível",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                state.ttsEngine.ifBlank { "TTS do sistema" },
                                fontSize = 11.sp,
                                color = TextSecondary,
                            )
                        }
                    }
                    if (state.ttsError != null) {
                        Text(
                            state.ttsError,
                            style = MaterialTheme.typography.bodySmall,
                            color = Rose,
                        )
                        Text(
                            "No HyperOS: Configurações > Idiomas e entrada > Saída de texto para fala. " +
                                "Instale o Google TTS ou outra voz e volte ao app.",
                            fontSize = 11.sp,
                            color = TextSecondary,
                        )
                    }

                    OutlinedButton(
                        onClick = { showPicker = true },
                        enabled = state.voices.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Rounded.GraphicEq, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            selected?.displayName ?: "Usar voz padrão do sistema (${state.voices.size} disponíveis)",
                            maxLines = 1,
                            fontSize = 12.sp,
                        )
                    }

                    Text("Tom da voz: ${(settings.pitch * 100).toInt()}%", fontSize = 12.sp)
                    Slider(
                        value = settings.pitch,
                        onValueChange = { onSettings(settings.copy(pitch = it)) },
                        valueRange = 0.5f..2f,
                    )
                    Text("Velocidade: ${(settings.rate * 100).toInt()}%", fontSize = 12.sp)
                    Slider(
                        value = settings.rate,
                        onValueChange = { onSettings(settings.copy(rate = it)) },
                        valueRange = 0.5f..2f,
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Falar respostas automaticamente", fontSize = 13.sp)
                            Text(
                                "Quando desligado, toque no alto-falante da resposta para ouvir.",
                                fontSize = 10.sp,
                                color = TextSecondary,
                            )
                        }
                        Switch(
                            checked = settings.autoSpeak,
                            onCheckedChange = { onSettings(settings.copy(autoSpeak = it)) },
                        )
                    }

                    if (settings.voiceName != null) {
                        TextButton(
                            onClick = { onSettings(settings.copy(voiceName = null)) },
                        ) { Text("Voltar para a voz padrão", fontSize = 12.sp) }
                    }
                }
            }
        }

        item {
            Panel(
                title = "Minha voz (arquivo de IA)",
                subtitle = "Importe um arquivo de voz pronto e use como a fala da mosca.",
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val file = state.voiceFile
                    if (file != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.MusicNote,
                                contentDescription = null,
                                tint = Teal,
                                modifier = Modifier.size(26.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    file.displayName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 2,
                                )
                                Text(
                                    "${formatSize(file.sizeBytes)} · ${formatDuration(file.durationMs)}",
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = onPlayFile,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(0.5.dp, Separator),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Fill,
                                    contentColor = Teal,
                                ),
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(
                                    if (state.voiceFilePlaying) Icons.Rounded.Stop
                                    else Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    if (state.voiceFilePlaying) "Parar" else "Ouvir",
                                    fontSize = 13.sp,
                                )
                            }
                            OutlinedButton(
                                onClick = onRemoveFile,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(0.5.dp, Separator),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Fill,
                                    contentColor = Rose,
                                ),
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(
                                    Icons.Rounded.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Remover", fontSize = 13.sp)
                            }
                        }
                    } else {
                        Text(
                            "Nenhum arquivo escolhido ainda.",
                            fontSize = 13.sp,
                            color = TextSecondary,
                        )
                    }

                    Button(
                        onClick = { filePicker.launch(arrayOf("audio/*")) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                    ) {
                        Icon(
                            Icons.Rounded.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (state.voiceFile == null) "Escolher arquivo de voz"
                            else "Trocar arquivo",
                            fontSize = 15.sp,
                        )
                    }

                    if (state.voiceFile != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Usar meu arquivo nas respostas", fontSize = 13.sp)
                                Text(
                                    "No lugar do TTS do sistema, a mosca toca este arquivo ao responder.",
                                    fontSize = 10.sp,
                                    color = TextSecondary,
                                )
                            }
                            Switch(
                                checked = state.useVoiceFile,
                                onCheckedChange = onToggleFile,
                            )
                        }
                    }

                    if (state.voiceFileError != null) {
                        Text(
                            state.voiceFileError,
                            fontSize = 12.sp,
                            color = Rose,
                        )
                    }

                    Text(
                        "Aceita MP3, M4A/AAC, WAV, OGG, FLAC, AMR e 3GP. O arquivo é tocado " +
                            "inteiro como a fala da mosca — o app não sintetiza texto novo a partir dele.",
                        fontSize = 11.sp,
                        color = TextSecondary,
                    )
                }
            }
        }

        item {
            Panel(
                title = "Testar a voz",
                subtitle = "O texto abaixo é falado com a voz e os ajustes atuais.",
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = testPhrase,
                        onValueChange = { testPhrase = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        label = { Text("Frase de teste") },
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { onTest(testPhrase) },
                            enabled = state.ttsReady,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Ouvir")
                        }
                        OutlinedButton(onClick = onStop, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.Stop, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Parar")
                        }
                    }
                }
            }
        }

        item {
            Panel(
                title = "Sobre a voz",
                subtitle = "Síntese local, sem nuvem",
            ) {
                Text(
                    "A voz é gerada pelo motor TTS do Android instalado no Redmi Note 11S. " +
                        "Qualquer voz disponível no sistema pode ser escolhida — inclusive vozes " +
                        "neurais do Google TTS, se estiverem baixadas. Nada é enviado para servidores.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
        }
    }

    if (showPicker) {
        AlertDialog(
            shape = RoundedCornerShape(14.dp),
            onDismissRequest = { showPicker = false },
            title = { Text("Escolher voz") },
            text = {
                if (state.voices.isEmpty()) {
                    Text("Nenhuma voz encontrada. Instale uma voz em Configurações do Android.")
                } else {
                    LazyColumn(Modifier.heightIn(max = 420.dp)) {
                        item {
                            VoiceRow(
                                label = "Padrão do sistema",
                                detail = "Usa a voz e o idioma padrão",
                                selected = settings.voiceName == null,
                                onClick = {
                                    onSettings(settings.copy(voiceName = null))
                                    showPicker = false
                                },
                            )
                        }
                        items(state.voices, key = { it.name }) { voice ->
                            VoiceRow(
                                label = voice.locale.displayName,
                                detail = voice.displayName,
                                selected = settings.voiceName == voice.name,
                                onClick = {
                                    onSettings(settings.copy(voiceName = voice.name))
                                    showPicker = false
                                },
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPicker = false }) { Text("Fechar") }
            },
        )
    }
}

@Composable
private fun VoiceRow(
    label: String,
    detail: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 7.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 13.sp, maxLines = 1)
            Text(detail, fontSize = 10.sp, color = TextSecondary, maxLines = 2)
        }
    }
}
