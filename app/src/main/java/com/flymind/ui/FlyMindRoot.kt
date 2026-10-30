package com.flymind.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flymind.ui.ios.IosCircleButton
import com.flymind.ui.ios.IosLargeHeader
import com.flymind.ui.ios.IosNavBar
import com.flymind.ui.ios.IosSnackbarHost
import com.flymind.ui.ios.IosTabBar
import com.flymind.ui.ios.IosTabItem
import com.flymind.ui.screens.AskScreen
import com.flymind.ui.screens.BrainScreen
import com.flymind.ui.screens.MemoryScreen
import com.flymind.ui.screens.SystemScreen
import com.flymind.ui.screens.TeachScreen
import com.flymind.ui.screens.VoiceScreen
import com.flymind.util.ShareUtil

private val tabs = listOf(
    IosTabItem("Ensinar", Icons.Rounded.School),
    IosTabItem("Conversar", Icons.Rounded.Forum),
    IosTabItem("Memória", Icons.Rounded.Storage),
    IosTabItem("Voz", Icons.Rounded.RecordVoiceOver),
    IosTabItem("Cérebro", Icons.Rounded.Psychology),
)

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
        // As barras de sistema são tratadas pela própria chrome do iOS
        // (statusBarsPadding no cabeçalho e navigationBarsPadding na tab bar).
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { IosSnackbarHost(snackbarHostState) },
        bottomBar = {
            IosTabBar(
                items = tabs,
                selectedIndex = if (systemOpen) -1 else state.tab,
                onSelect = { index ->
                    systemOpen = false
                    vm.setTab(index)
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            if (systemOpen) {
                IosNavBar(title = "Ajustes", onBack = { systemOpen = false })
                SystemScreen(
                    state = state,
                    onKeepScreenOn = vm::setKeepScreenOn,
                )
            } else {
                IosLargeHeader(
                    title = "Conectoma",
                    subtitle = state.brain?.let {
                        "${it.stats.neurons} neurônios · ${it.stats.synapses} sinapses"
                    } ?: "conectoma FlyWire",
                    trailing = {
                        IosCircleButton(
                            icon = Icons.Rounded.Settings,
                            contentDescription = "Ajustes do sistema",
                            onClick = { systemOpen = true },
                        )
                    },
                )
                Box {
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
}
