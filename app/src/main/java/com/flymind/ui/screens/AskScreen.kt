package com.flymind.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flymind.ui.ChatMessage
import com.flymind.ui.FlyUiState
import com.flymind.ui.formatTime
import com.flymind.ui.theme.Amber
import com.flymind.ui.theme.Rose
import com.flymind.ui.theme.SurfaceHigh
import com.flymind.ui.theme.Teal
import com.flymind.ui.theme.TextSecondary

@Composable
fun AskScreen(
    state: FlyUiState,
    onAsk: (String) -> Unit,
    onClearChat: () -> Unit,
    onSpeak: (String) -> Unit,
) {
    var input by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(state.chat.size, state.asking) {
        if (state.chat.isNotEmpty()) {
            listState.animateScrollToItem(state.chat.lastIndex)
        }
    }

    Column(Modifier.fillMaxSize()) {
        if (state.chat.isEmpty()) {
            Box(Modifier.weight(1f)) {
                EmptyConversation(state, onSuggestion = { suggestion ->
                    input = suggestion
                    onAsk(suggestion)
                    input = ""
                })
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.chat, key = { it.id }) { message ->
                    ChatBubble(message, onSpeak)
                }
                if (state.asking) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Teal,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "consultando o conectoma...",
                                fontSize = 12.sp,
                                color = TextSecondary,
                            )
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
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("${trace.spikes} disparos", fontSize = 11.sp, color = TextSecondary)
                Text("${trace.pulses} pulsos de dopamina", fontSize = 11.sp, color = Amber)
                Text("${trace.activeNeurons} neurônios ativos", fontSize = 11.sp, color = TextSecondary)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(12.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Pergunte algo que ela aprendeu...") },
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (input.isNotBlank() && !state.asking) {
                            onAsk(input)
                            input = ""
                        }
                    },
                ),
            )
            Spacer(Modifier.width(8.dp))
            FilledIconButton(
                onClick = {
                    if (input.isNotBlank() && !state.asking) {
                        onAsk(input)
                        input = ""
                    }
                },
                modifier = Modifier.size(52.dp),
                enabled = input.isNotBlank() && !state.asking,
            ) {
                Icon(Icons.Rounded.Send, contentDescription = "Perguntar")
            }
        }
        if (state.chat.isNotEmpty()) {
            TextButton(
                onClick = onClearChat,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
            ) {
                Text("Limpar conversa", fontSize = 12.sp)
            }
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
            modifier = Modifier.size(54.dp),
            tint = Teal,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Pergunte à mosca",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            if (state.facts.isEmpty()) {
                "Ela ainda não tem memórias. Ensine o primeiro fato na aba Ensinar e volte para conversar."
            } else {
                "Ela responde com o que aprendeu (${state.facts.size} memórias). Se não souber, diz que não aprendeu."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        if (state.facts.isNotEmpty()) {
            Spacer(Modifier.height(18.dp))
            state.facts.take(3).forEach { fact ->
                TextButton(onClick = { onSuggestion(fact.question) }) {
                    Text(fact.question, color = Teal, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage, onSpeak: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start,
    ) {
        if (!message.fromUser) {
            Box(
                Modifier
                    .size(30.dp)
                    .padding(top = 2.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                Icon(Icons.Rounded.Psychology, contentDescription = null, tint = Teal)
            }
            Spacer(Modifier.width(6.dp))
        }
        Card(
            modifier = Modifier.fillMaxWidth(0.86f),
            shape = RoundedCornerShape(
                topStart = if (message.fromUser) 18.dp else 4.dp,
                topEnd = if (message.fromUser) 4.dp else 18.dp,
                bottomStart = 18.dp,
                bottomEnd = 18.dp,
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (message.fromUser) Teal.copy(alpha = 0.16f) else SurfaceHigh,
            ),
        ) {
            Column(Modifier.padding(12.dp)) {
                if (message.source != null) {
                    Text(
                        message.source.uppercase(),
                        fontSize = 10.sp,
                        color = Amber,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(3.dp))
                }
                Text(message.text, style = MaterialTheme.typography.bodyMedium)
                if (!message.fromUser) {
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (message.confidence != null) {
                            Text(
                                "confiança ${(message.confidence * 100).toInt()}%",
                                fontSize = 10.sp,
                                color = if (message.confidence >= 0.6f) Teal else Rose,
                            )
                        }
                        if (message.engram != null) {
                            Text(
                                " · engrama ${(message.engram * 100).toInt()}%",
                                fontSize = 10.sp,
                                color = TextSecondary,
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        Text(formatTime(message.at), fontSize = 9.sp, color = TextSecondary)
                        Spacer(Modifier.width(4.dp))
                        IconButton(
                            onClick = { onSpeak(message.text) },
                            modifier = Modifier.size(26.dp),
                        ) {
                            Icon(
                                Icons.Rounded.VolumeUp,
                                contentDescription = "Ouvir",
                                tint = Teal,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
