package com.flymind.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flymind.ui.screens.AskScreen
import com.flymind.ui.screens.BrainScreen
import com.flymind.ui.screens.MemoryScreen
import com.flymind.ui.screens.SystemScreen
import com.flymind.ui.screens.TeachScreen
import com.flymind.ui.screens.VoiceScreen
import com.flymind.ui.theme.Fill
import com.flymind.ui.theme.Surface
import com.flymind.ui.theme.Teal
import com.flymind.ui.theme.TextSecondary
import com.flymind.util.ShareUtil

private enum class Tab(val label: String, val icon: ImageVector) {
    TEACH("Ensinar", Icons.Rounded.School),
    ASK("Conversar", Icons.Rounded.Forum),
    MEMORY("Memória", Icons.Rounded.Storage),
    VOICE("Voz", Icons.Rounded.RecordVoiceOver),
    BRAIN("Cérebro", Icons.Rounded.Psychology),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlyMindRoot(vm: AppViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var systemOpen by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(state.snackbar) {
        val message = state.snackbar
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            vm.consumeSnackbar()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (!systemOpen) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "FlyMind",
                                fontWeight = FontWeight.Bold,
                                fontSize = 28.sp,
                                letterSpacing = (-0.5).sp,
                            )
                            Text(
                                state.brain?.let {
                                    "${it.stats.neurons} neurônios · ${it.stats.synapses} sinapses"
                                } ?: "conectoma FlyWire",
                                fontSize = 12.sp,
                                color = TextSecondary,
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { systemOpen = true },
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .clip(CircleShape)
                                .background(Fill),
                        ) {
                            Icon(
                                Icons.Rounded.Settings,
                                contentDescription = "Sistema e HyperOS",
                                tint = Teal,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                )
            }
        },
        bottomBar = {
            if (!systemOpen) {
                NavigationBar(
                    containerColor = Surface,
                    tonalElevation = 0.dp,
                ) {
                    Tab.entries.forEachIndexed { index, tab ->
                        NavigationBarItem(
                            selected = state.tab == index,
                            onClick = { vm.setTab(index) },
                            icon = {
                                Icon(
                                    tab.icon,
                                    contentDescription = tab.label,
                                    modifier = Modifier.size(24.dp),
                                )
                            },
                            label = {
                                Text(
                                    tab.label,
                                    fontSize = 10.sp,
                                    fontWeight = if (state.tab == index) {
                                        FontWeight.SemiBold
                                    } else {
                                        FontWeight.Normal
                                    },
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Teal,
                                selectedTextColor = Teal,
                                indicatorColor = Color.Transparent,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            if (systemOpen) {
                SystemScreen(
                    state = state,
                    onKeepScreenOn = vm::setKeepScreenOn,
                    onBack = { systemOpen = false },
                )
            } else {
                when (state.tab) {
                    0 -> TeachScreen(
                        state = state,
                        onTeach = vm::teach,
                        onTeachBatch = vm::teachBatch,
                        onPreviewUrl = vm::previewUrl,
                        onTeachUrl = vm::teachFromUrl,
                    )
                    1 -> AskScreen(
                        state = state,
                        onAsk = vm::ask,
                        onClearChat = vm::clearChat,
                        onSpeak = vm::testVoice,
                    )
                    2 -> MemoryScreen(
                        state = state,
                        onReplay = vm::replay,
                        onDelete = vm::deleteFact,
                        onClearAll = vm::clearAll,
                        onExport = { ShareUtil.shareMemories(context, vm.exportJson()) },
                        onSpeak = vm::testVoice,
                    )
                    3 -> VoiceScreen(
                        state = state,
                        onSettings = vm::setVoiceSettings,
                        onTest = vm::testVoice,
                        onStop = vm::stopVoice,
                        onImportFile = vm::importVoiceFile,
                        onPlayFile = vm::playVoiceFile,
                        onRemoveFile = vm::removeVoiceFile,
                        onToggleFile = vm::setUseVoiceFile,
                    )
                    else -> BrainScreen(
                        state = state,
                        onSpontaneous = vm::runSpontaneous,
                        onResetPlasticity = vm::resetPlasticity,
                    )
                }
            }
        }
    }
}
