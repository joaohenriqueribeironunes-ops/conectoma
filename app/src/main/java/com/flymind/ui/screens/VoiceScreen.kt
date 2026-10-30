package com.flymind.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Stop
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
import androidx.compose.ui.window.Dialog
import com.flymind.ui.FlyUiState
import com.flymind.ui.Panel
import com.flymind.ui.formatDuration
import com.flymind.ui.formatSize
import com.flymind.ui.ios.IosButton
import com.flymind.ui.ios.IosDivider
import com.flymind.ui.ios.IosGrabber
import com.flymind.ui.ios.IosGrayButton
import com.flymind.ui.ios.IosIconTile
import com.flymind.ui.ios.IosPlainButton
import com.flymind.ui.ios.IosSlider
import com.flymind.ui.ios.IosSwitch
import com.flymind.ui.ios.IosTextField
import com.flymind.ui.ios.IosTintedButton
import com.flymind.ui.theme.Fill
import com.flymind.ui.theme.Green
import com.flymind.ui.theme.Outline
import com.flymind.ui.theme.Rose
import com.flymind.ui.theme.Surface
import com.flymind.ui.theme.Teal
import com.flymind.ui.theme.TextPrimary
import com.flymind.ui.theme.TextSecondary
import com.flymind.ui.theme.TextTertiary
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
        mutableStateOf("Olá. Eu sou a mosca do Conectoma. Aprendi sobre o mundo com você.")
    }
    val settings = state.voiceSettings
    val selected = state.voices.firstOrNull { it.name == settings.voiceName }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        item {
            Panel(
                title = "Voz da mosca",
                footer = "A voz é gerada pelo motor TTS do Android. Qualquer voz instalada no " +
                    "aparelho pode ser escolhida — inclusive as neurais do Google TTS.",
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IosIconTile(
                            Icons.Rounded.RecordVoiceOver,
                            if (state.ttsReady) Teal else Rose,
                            size = 34,
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (state.ttsReady) "Motor de voz pronto" else "Motor de voz indisponível",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                state.ttsEngine.ifBlank { "TTS do sistema" },
                                fontSize = 13.sp,
                                color = TextSecondary,
                            )
                        }
                    }
                    if (state.ttsError != null) {
                        Text(state.ttsError.orEmpty(), fontSize = 13.sp, color = Rose)
                        Text(
                            "No HyperOS: Configurações > Idiomas e entrada > Saída de texto para fala. " +
                                "Instale o Google TTS ou outra voz e volte ao app.",
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = TextSecondary,
                        )
                    }

                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Fill)
                            .clickable(enabled = state.voices.isNotEmpty()) { showPicker = true }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Voz", fontSize = 17.sp)
                            Text(
                                selected?.displayName
                                    ?: "Padrão do sistema · ${state.voices.size} disponíveis",
                                fontSize = 13.sp,
                                color = TextSecondary,
                                maxLines = 1,
                            )
                        }
                        Icon(
                            Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = Outline,
                            modifier = Modifier.size(17.dp),
                        )
                    }

                    SliderRow("Tom da voz", "${(settings.pitch * 100).toInt()}%") {
                        IosSlider(
                            value = settings.pitch,
                            onValueChange = { onSettings(settings.copy(pitch = it)) },
                            valueRange = 0.5f..2f,
                        )
                    }
                    SliderRow("Velocidade", "${(settings.rate * 100).toInt()}%") {
                        IosSlider(
                            value = settings.rate,
                            onValueChange = { onSettings(settings.copy(rate = it)) },
                            valueRange = 0.5f..2f,
                        )
                    }

                    IosDivider()

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Falar respostas automaticamente", fontSize = 17.sp)
                            Text(
                                "Quando desligado, toque no alto-falante da resposta para ouvir.",
                                fontSize = 13.sp,
                                lineHeight = 17.sp,
                                color = TextSecondary,
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        IosSwitch(
                            checked = settings.autoSpeak,
                            onCheckedChange = { onSettings(settings.copy(autoSpeak = it)) },
                        )
                    }

                    if (settings.voiceName != null) {
                        IosPlainButton(
                            text = "Voltar para a voz padrão",
                            onClick = { onSettings(settings.copy(voiceName = null)) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }

        item {
            Panel(
                title = "Minha voz (arquivo de IA)",
                footer = "Aceita MP3, M4A/AAC, WAV, OGG, FLAC, AMR e 3GP. O arquivo é tocado " +
                    "inteiro como a fala da mosca — o app não sintetiza texto novo a partir dele.",
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    val file = state.voiceFile
                    if (file != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IosIconTile(Icons.Rounded.MusicNote, Teal, size = 34)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    file.displayName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 2,
                                )
                                Text(
                                    "${formatSize(file.sizeBytes)} · ${formatDuration(file.durationMs)}",
                                    fontSize = 13.sp,
                                    color = TextSecondary,
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            IosTintedButton(
                                text = if (state.voiceFilePlaying) "Parar" else "Ouvir",
                                onClick = onPlayFile,
                                icon = if (state.voiceFilePlaying) Icons.Rounded.Stop
                                else Icons.Rounded.PlayArrow,
                                modifier = Modifier.weight(1f),
                            )
                            IosGrayButton(
                                text = "Remover",
                                onClick = onRemoveFile,
                                tint = Rose,
                                icon = Icons.Rounded.Delete,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    } else {
                        Text(
                            "Nenhum arquivo escolhido ainda.",
                            fontSize = 15.sp,
                            color = TextSecondary,
                        )
                    }

                    IosButton(
                        text = if (state.voiceFile == null) "Escolher arquivo de voz" else "Trocar arquivo",
                        onClick = { filePicker.launch(arrayOf("audio/*")) },
                        icon = Icons.Rounded.MusicNote,
                    )

                    if (state.voiceFile != null) {
                        IosDivider()
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Usar meu arquivo nas respostas", fontSize = 17.sp)
                                Text(
                                    "No lugar do TTS do sistema, a mosca toca este arquivo ao responder.",
                                    fontSize = 13.sp,
                                    lineHeight = 17.sp,
                                    color = TextSecondary,
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            IosSwitch(checked = state.useVoiceFile, onCheckedChange = onToggleFile)
                        }
                    }

                    if (state.voiceFileError != null) {
                        Text(state.voiceFileError.orEmpty(), fontSize = 13.sp, color = Rose)
                    }
                }
            }
        }

        item {
            Panel(title = "Testar a voz") {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    IosTextField(
                        value = testPhrase,
                        onValueChange = { testPhrase = it },
                        label = "Frase de teste",
                        singleLine = false,
                        minLines = 2,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        IosButton(
                            text = "Ouvir",
                            onClick = { onTest(testPhrase) },
                            enabled = state.ttsReady,
                            icon = Icons.Rounded.PlayArrow,
                            modifier = Modifier.weight(1f),
                        )
                        IosGrayButton(
                            text = "Parar",
                            onClick = onStop,
                            icon = Icons.Rounded.Stop,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        item {
            Panel(title = "Sobre a voz") {
                Text(
                    "A voz é gerada pelo motor TTS do Android instalado no Redmi Note 11S, " +
                        "sem nuvem: nada é enviado para servidores. Uma voz neural baixada no " +
                        "sistema aparece aqui automaticamente.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = TextSecondary,
                )
            }
        }
    }

    if (showPicker) {
        VoicePickerDialog(
            voices = state.voices,
            settings = settings,
            onSelect = { name ->
                onSettings(settings.copy(voiceName = name))
                showPicker = false
            },
            onDismiss = { showPicker = false },
        )
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

/** Seletor de voz em folha branca, com marca de seleção como nos Ajustes. */
@Composable
private fun VoicePickerDialog(
    voices: List<VoiceInfo>,
    settings: VoiceSettings,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Surface)
                .padding(bottom = 8.dp),
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { IosGrabber() }
            Text(
                "Escolher voz",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onSelect(null) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Padrão do sistema", fontSize = 17.sp)
                    Text("Usa a voz e o idioma padrão", fontSize = 13.sp, color = TextSecondary)
                }
                if (settings.voiceName == null) {
                    Icon(Icons.Rounded.Check, null, tint = Teal, modifier = Modifier.size(20.dp))
                }
            }
            IosDivider(startIndent = 16.dp)
            if (voices.isEmpty()) {
                Text(
                    "Nenhuma voz encontrada. Instale uma voz em Configurações do Android.",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(16.dp),
                )
            } else {
                LazyColumn(Modifier.weight(1f, fill = false)) {
                    items(voices, key = { it.name }) { voice ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) { onSelect(voice.name) }
                                .padding(horizontal = 16.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(voice.locale.displayName, fontSize = 17.sp, maxLines = 1)
                                Text(
                                    voice.displayName,
                                    fontSize = 13.sp,
                                    color = TextSecondary,
                                    maxLines = 2,
                                )
                            }
                            if (settings.voiceName == voice.name) {
                                Icon(Icons.Rounded.Check, null, tint = Teal, modifier = Modifier.size(20.dp))
                            }
                        }
                        IosDivider(startIndent = 16.dp)
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IosPlainButton("Fechar", onDismiss, modifier = Modifier.padding(end = 8.dp))
            }
        }
    }
}
