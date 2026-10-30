package com.flymind.voice

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream

data class VoiceFileInfo(
    val file: File,
    val displayName: String,
    val sizeBytes: Long,
    val durationMs: Long,
)

/**
 * Reproduz um arquivo de voz pronto (gerado por IA, gravado ou baixado).
 *
 * O arquivo é copiado para o armazenamento interno do app, então continua
 * disponível mesmo se a permissão de acesso ao original expirar. Ele não
 * sintetiza texto novo: é a fala da mosca usada nas respostas, no lugar do
 * TTS do sistema.
 */
class VoiceFilePlayer(context: Context) {

    private val appContext = context.applicationContext
    private val dir = File(appContext.filesDir, "voz")
    private var player: MediaPlayer? = null

    private val _info = MutableStateFlow(readExisting())
    val info: StateFlow<VoiceFileInfo?> = _info

    private val _playing = MutableStateFlow(false)
    val playing: StateFlow<Boolean> = _playing

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    /** Copia o arquivo escolhido e lê a duração. Roda melhor em thread de IO. */
    fun importFromUri(uri: Uri): Boolean {
        return runCatching {
            val name = displayNameOf(uri)
            val extension = extensionOf(name)
            dir.mkdirs()
            val target = File(dir, "voz_custom.$extension")
            val copied = appContext.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(target).use { output -> input.copyTo(output) }
            } ?: error("não consegui abrir o arquivo escolhido")
            check(copied > 0L) { "o arquivo escolhido está vazio" }
            val duration = probeDuration(target)
            stop()
            _info.value = VoiceFileInfo(target, name, target.length(), duration)
            _error.value = null
        }.onFailure { error ->
            _error.value = "Não consegui usar esse arquivo: ${error.message}"
        }.isSuccess
    }

    fun play() {
        val file = _info.value?.file ?: return
        if (!file.exists()) {
            _error.value = "O arquivo de voz não existe mais."
            return
        }
        stopInternal()
        val media = MediaPlayer()
        runCatching {
            media.setDataSource(file.absolutePath)
            media.setOnPreparedListener { mp ->
                _playing.value = true
                mp.start()
            }
            media.setOnCompletionListener {
                _playing.value = false
            }
            media.setOnErrorListener { mp, _, _ ->
                mp.release()
                if (player === mp) player = null
                _playing.value = false
                _error.value = "Não consegui tocar esse formato de áudio."
                true
            }
            player = media
            media.prepareAsync()
        }.onFailure {
            media.release()
            _error.value = "Falha ao tocar a voz: ${it.message}"
        }
    }

    fun stop() {
        stopInternal()
        _playing.value = false
    }

    fun remove() {
        stop()
        _info.value?.file?.delete()
        _info.value = null
        _error.value = null
    }

    fun close() {
        stopInternal()
    }

    private fun stopInternal() {
        val media = player ?: return
        player = null
        runCatching {
            if (media.isPlaying) media.stop()
        }
        media.release()
    }

    private fun readExisting(): VoiceFileInfo? {
        val existing = dir.listFiles()?.firstOrNull { it.isFile && it.length() > 0L } ?: return null
        return VoiceFileInfo(
            file = existing,
            displayName = existing.name,
            sizeBytes = existing.length(),
            durationMs = probeDuration(existing),
        )
    }

    private fun probeDuration(file: File): Long {
        return runCatching {
            val media = MediaPlayer()
            media.setDataSource(file.absolutePath)
            media.prepare()
            val duration = media.duration.toLong()
            media.release()
            duration.coerceAtLeast(0L)
        }.getOrDefault(0L)
    }

    private fun displayNameOf(uri: Uri): String {
        return runCatching {
            appContext.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
            }
        }.getOrNull() ?: uri.lastPathSegment ?: "voz.ia"
    }

    private fun extensionOf(name: String): String {
        val dot = name.lastIndexOf('.')
        val raw = if (dot > 0 && dot < name.length - 1) name.substring(dot + 1).lowercase() else ""
        return when (raw) {
            "mp3", "wav", "ogg", "m4a", "aac", "flac", "3gp", "mp4", "amr", "opus" -> raw
            else -> "audio"
        }
    }
}
