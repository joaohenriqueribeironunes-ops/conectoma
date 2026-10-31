package com.flymind.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flymind.ui.ChatMessage
import com.flymind.ui.FlyUiState
import com.flymind.ui.formatTime
import com.flymind.ui.ios.IosDivider
import com.flymind.ui.ios.IosPlainButton
import com.flymind.ui.ios.IosSectionHeader
import com.flymind.ui.theme.Amber
import com.flymind.ui.theme.Fill
import com.flymind.ui.theme.FillStrong
import com.flymind.ui.theme.IosTypeface
import com.flymind.ui.theme.Outline
import com.flymind.ui.theme.Rose
import com.flymind.ui.theme.Surface
import com.flymind.ui.theme.Teal
import com.flymind.ui.theme.TextPrimary
import com.flymind.ui.theme.TextSecondary
import com.flymind.ui.theme.TextTertiary

@Composable
fun AskScreen(
    state: FlyUiState,
    onAsk: (String) -> Unit,
    onClearChat: () -> Unit,
    onSpeak: (String) -> Unit,
) {
    var input by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    val keyboard = LocalSoftwareKeyboardController.current

    fun send() {
        if (input.isNotBlank() && !state.asking) {
            onAsk(input)
            input = ""
            keyboard?.hide()
        }
    }

    LaunchedEffect(state.chat.size, state.asking) {
        if (state.chat.isNotEmpty()) {
            listState.animateScrollToItem(state.chat.lastIndex)
        }
    }

    Column(Modifier.fillMaxSize()) {
        if (state.chat.isEmpty()) {
            Box(Modifier.weight(1f)) {
                EmptyConversation(state, onSuggestion = { suggestion ->
                    onAsk(suggestion)
                })
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.chat, key = { it.id }) { message ->
                    ChatBubble(message, onSpeak, onAsk)
                }
                if (state.asking) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 1.5.dp,
                                color = TextTertiary,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("consultando o conectoma...", fontSize = 13.sp, color = TextSecondary)
                        }
                    }
                }
            }
        }

        val trace = state.lastTrace
        if (trace != null) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("${trace.spikes} disparos", fontSize = 11.sp, color = TextTertiary)
                Text("${trace.pulses} pulsos de dopamina", fontSize = 11.sp, color = Amber)
                Text("${trace.activeNeurons} neurônios ativos", fontSize = 11.sp, color = TextTertiary)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 10.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(19.dp))
                    .background(Fill)
                    .padding(horizontal = 14.dp, vertical = 9.dp),
            ) {
                if (input.isEmpty()) {
                    Text("Pergunte algo que ela aprendeu...", fontSize = 17.sp, color = TextTertiary)
                }
                BasicTextField(
                    value = input,
                    onValueChange = { input = it },
                    textStyle = TextStyle(
                        fontFamily = IosTypeface,
                        fontSize = 17.sp,
                        color = TextPrimary,
                        letterSpacing = (-0.2).sp,
                    ),
                    cursorBrush = SolidColor(Teal),
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { send() }),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.width(8.dp))
            val canSend = input.isNotBlank() && !state.asking
            Box(
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (canSend) Teal else FillStrong)
                    .clickable(enabled = canSend) { send() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.ArrowUpward,
                    contentDescription = "Perguntar",
                    tint = if (canSend) Color.White else TextTertiary,
                    modifier = Modifier.size(19.dp),
                )
            }
        }

        if (state.chat.isNotEmpty()) {
            IosPlainButton(
                text = "Limpar conversa",
                onClick = onClearChat,
                tint = TextSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
            )
        }
    }
}

@Composable
private fun EmptyConversation(state: FlyUiState, onSuggestion: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Rounded.Psychology,
            contentDescription = null,
            modifier = Modifier.size(46.dp),
            tint = Teal,
        )
        Spacer(Modifier.height(14.dp))
        Text("Pergunte à mosca", fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp)
        Spacer(Modifier.height(6.dp))
        Text(
            if (state.facts.isEmpty()) {
                "Ela ainda não tem memórias. Ensine o primeiro fato na aba Ensinar e volte para conversar."
            } else {
                "Ela responde com o que aprendeu (${state.facts.size} memórias). Se não souber, diz que não aprendeu."
            },
            fontSize = 15.sp,
            lineHeight = 20.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
        )
        if (state.facts.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            Text(
                "SUGESTÕES",
                fontSize = 13.sp,
                color = TextSecondary,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(7.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Surface),
            ) {
                state.facts.take(3).forEachIndexed { index, fact ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onSuggestion(fact.question) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            fact.question,
                            fontSize = 17.sp,
                            color = Teal,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = Outline,
                            modifier = Modifier.size(17.dp),
                        )
                    }
                    if (index != 2 && index != state.facts.take(3).lastIndex) {
                        IosDivider(startIndent = 14.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessage,
    onSpeak: (String) -> Unit,
    onAsk: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            Modifier
                .fillMaxWidth(if (message.fromUser) 0.8f else 0.86f)
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (message.fromUser) 18.dp else 5.dp,
                        bottomEnd = if (message.fromUser) 5.dp else 18.dp,
                    ),
                )
                .background(if (message.fromUser) Teal else FillStrong)
                .padding(horizontal = 13.dp, vertical = 9.dp),
        ) {
            val label = message.sources.ifEmpty {
                listOfNotNull(message.source).filter { it.isNotBlank() }
            }
            if (label.isNotEmpty()) {
                Text(
                    label.joinToString(" · "),
                    fontSize = 11.sp,
                    color = if (message.fromUser) Color.White.copy(alpha = 0.8f) else TextSecondary,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(2.dp))
            }
            Text(
                message.text,
                fontSize = 17.sp,
                lineHeight = 22.sp,
                letterSpacing = (-0.2).sp,
                color = if (message.fromUser) Color.White else TextPrimary,
            )
            if (!message.fromUser && message.linked.isNotEmpty()) {
                Spacer(Modifier.height(9.dp))
                Text(
                    "LIGADA POR",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary,
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    message.linked.take(4).forEach { term ->
                        Text(
                            term,
                            fontSize = 12.sp,
                            color = Teal,
                            modifier = Modifier
                                .clip(RoundedCornerShape(7.dp))
                                .background(Teal.copy(alpha = 0.10f))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
            }
            if (!message.fromUser && message.related.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text("CONECTA COM", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                message.related.forEach { idea ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { onAsk(idea.question) }
                            .padding(top = 7.dp),
                    ) {
                        Text(
                            idea.question,
                            fontSize = 15.sp,
                            color = Teal,
                            maxLines = 2,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = Outline,
                            modifier = Modifier.size(15.dp),
                        )
                    }
                    Text(
                        "via ${idea.via}",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 1.dp, bottom = 2.dp),
                    )
                }
            }
            if (!message.fromUser) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (message.confidence != null) {
                        Text(
                            "confiança ${(message.confidence * 100).toInt()}%",
                            fontSize = 11.sp,
                            color = if (message.confidence >= 0.6f) Teal else Rose,
                        )
                    }
                    if (message.engram != null) {
                        Text(
                            " · engrama ${(message.engram * 100).toInt()}%",
                            fontSize = 11.sp,
                            color = TextSecondary,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Text(formatTime(message.at), fontSize = 11.sp, color = TextSecondary)
                    Spacer(Modifier.width(6.dp))
                    Box(
                        Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .clickable { onSpeak(message.text) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.VolumeUp,
                            contentDescription = "Ouvir",
                            tint = Teal,
                            modifier = Modifier.size(15.dp),
                        )
                    }
                }
            }
        }
    }
}
