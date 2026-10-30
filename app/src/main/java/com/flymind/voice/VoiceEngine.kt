package com.flymind.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

data class VoiceInfo(
    val name: String,
    val locale: Locale,
    val displayName: String,
    val requiresNetwork: Boolean,
    val quality: Int,
    val latency: Int,
)

data class VoiceSettings(
    val voiceName: String? = null,
    val pitch: Float = 1f,
    val rate: Float = 1f,
    val autoSpeak: Boolean = true,
)

/**
 * Voz artificial da mosca. Usa o motor TTS instalado no aparelho (Google TTS,
 * Samsung, etc.), permitindo escolher qualquer voz disponível e ajustar tom e
 * velocidade. Nenhum áudio sai do aparelho.
 */
class VoiceEngine(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("voice", Context.MODE_PRIVATE)
    private var tts: TextToSpeech? = null

    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _voices = MutableStateFlow<List<VoiceInfo>>(emptyList())
    val voices: StateFlow<List<VoiceInfo>> = _voices

    private val _engineLabel = MutableStateFlow("")
    val engineLabel: StateFlow<String> = _engineLabel

    fun init() {
        if (tts != null) return
        tts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val engine = tts
                if (engine == null) {
                    _error.value = "Motor de voz não disponível."
                    return@TextToSpeech
                }
                engine.language = Locale("pt", "BR")
                _engineLabel.value = "motor TTS do sistema"
                _ready.value = true
                _error.value = null
                loadVoices(engine)
            } else {
                _ready.value = false
                _error.value = "Não foi possível iniciar o TTS. Instale ou ative uma voz em " +
                    "Configurações > Sistema > Idiomas > Saída de texto para fala."
            }
        }
    }

    private fun loadVoices(engine: TextToSpeech) {
        val list = runCatching { engine.voices }.getOrNull().orEmpty()
        val mapped = list.mapNotNull { voice: Voice ->
            runCatching {
                VoiceInfo(
                    name = voice.name,
                    locale = voice.locale,
                    displayName = friendlyName(voice),
                    requiresNetwork = voice.isNetworkConnectionRequired,
                    quality = voice.quality,
                    latency = voice.latency,
                )
            }.getOrNull()
        }.sortedWith(
            compareByDescending<VoiceInfo> { it.locale.language == "pt" }
                .thenByDescending { it.locale.country == "BR" }
                .thenBy { it.locale.displayName }
                .thenBy { it.displayName },
        )
        _voices.value = mapped
    }

    private fun friendlyName(voice: Voice): String {
        val language = voice.locale.getDisplayLanguage(Locale("pt", "BR"))
            .replaceFirstChar { it.uppercase() }
        val region = voice.locale.getDisplayCountry(Locale("pt", "BR"))
        val where = if (region.isBlank()) language else "$language ($region)"
        val net = if (voice.isNetworkConnectionRequired) " · online" else ""
        return "$where · ${voice.name}$net"
    }

    fun settings(): VoiceSettings = VoiceSettings(
        voiceName = prefs.getString("voice_name", null),
        pitch = prefs.getFloat("pitch", 1f),
        rate = prefs.getFloat("rate", 1f),
        autoSpeak = prefs.getBoolean("auto_speak", true),
    )

    fun saveSettings(settings: VoiceSettings) {
        prefs.edit()
            .putString("voice_name", settings.voiceName)
            .putFloat("pitch", settings.pitch)
            .putFloat("rate", settings.rate)
            .putBoolean("auto_speak", settings.autoSpeak)
            .apply()
    }

    fun speak(text: String, settings: VoiceSettings = settings()) {
        val engine = tts ?: return
        if (!_ready.value || text.isBlank()) return
        runCatching {
            if (settings.voiceName != null) {
                val voice = engine.voices?.firstOrNull { it.name == settings.voiceName }
                if (voice != null) {
                    engine.voice = voice
                    engine.language = voice.locale
                }
            }
            engine.setPitch(settings.pitch.coerceIn(0.5f, 2f))
            engine.setSpeechRate(settings.rate.coerceIn(0.5f, 2f))
        }
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "flymind-${System.nanoTime()}")
    }

    fun stop() {
        runCatching { tts?.stop() }
    }

    fun shutdown() {
        runCatching { tts?.shutdown() }
        tts = null
        _ready.value = false
    }
}
