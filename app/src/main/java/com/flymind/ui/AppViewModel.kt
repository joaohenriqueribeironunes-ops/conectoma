package com.flymind.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flymind.brain.BrainStatus
import com.flymind.brain.Concepts
import com.flymind.brain.DivergentIdea
import com.flymind.brain.LearningEngine
import com.flymind.brain.RecallOutcome
import com.flymind.brain.SimTrace
import com.flymind.brain.TextCode
import com.flymind.brain.TrainProgress
import com.flymind.brain.TrainingOutcome
import com.flymind.brain.WebPage
import com.flymind.brain.WebSource
import com.flymind.data.EventKind
import com.flymind.data.Fact
import com.flymind.data.FlyMindDatabase
import com.flymind.data.LearningEvent
import com.flymind.voice.VoiceEngine
import com.flymind.voice.VoiceFileInfo
import com.flymind.voice.VoiceFilePlayer
import com.flymind.voice.VoiceInfo
import com.flymind.voice.VoiceSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class RelatedIdea(
    val question: String,
    val via: String,
)

data class ChatMessage(
    val id: Long,
    val fromUser: Boolean,
    val text: String,
    val confidence: Float? = null,
    val engram: Float? = null,
    val source: String? = null,
    val sources: List<String> = emptyList(),
    val linked: List<String> = emptyList(),
    val related: List<RelatedIdea> = emptyList(),
    val at: Long = System.currentTimeMillis(),
)

/** Fato guardado junto com o resultado do treino que o consolidou. */
private data class LearnedFact(
    val fact: Fact,
    val outcome: TrainingOutcome,
)

data class FlyUiState(
    val tab: Int = 0,
    val facts: List<Fact> = emptyList(),
    val events: List<LearningEvent> = emptyList(),
    val brain: BrainStatus? = null,
    val brainError: String? = null,
    val teaching: Boolean = false,
    val batchLabel: String? = null,
    val progress: TrainProgress? = null,
    val lastTraining: TrainingOutcome? = null,
    val lastTrace: SimTrace? = null,
    val chat: List<ChatMessage> = emptyList(),
    val asking: Boolean = false,
    val snackbar: String? = null,
    val voiceSettings: VoiceSettings = VoiceSettings(),
    val voices: List<VoiceInfo> = emptyList(),
    val ttsReady: Boolean = false,
    val ttsError: String? = null,
    val ttsEngine: String = "",
    val voiceFile: VoiceFileInfo? = null,
    val voiceFilePlaying: Boolean = false,
    val voiceFileError: String? = null,
    val useVoiceFile: Boolean = false,
    val webLoading: Boolean = false,
    val webError: String? = null,
    val webPreview: WebPage? = null,
    val keepScreenOn: Boolean = false,
    val spontaneousRunning: Boolean = false,
)

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val db = FlyMindDatabase(application)
    private val voice = VoiceEngine(application)
    private val voiceFile = VoiceFilePlayer(application)
    private val prefs = application.getSharedPreferences("ui", Application.MODE_PRIVATE)

    private var engineError: String? = null
    private val engine: LearningEngine? = try {
        LearningEngine(application)
    } catch (error: Throwable) {
        engineError = "Falha ao carregar o conectoma: ${error.message}"
        null
    }

    private val _state = MutableStateFlow(
        FlyUiState(
            brainError = engineError,
            voiceSettings = voice.settings(),
            voiceFile = voiceFile.info.value,
            voiceFilePlaying = voiceFile.playing.value,
            voiceFileError = voiceFile.error.value,
            useVoiceFile = prefs.getBoolean("use_voice_file", false),
            keepScreenOn = prefs.getBoolean("keep_screen_on", false),
        ),
    )
    val state: StateFlow<FlyUiState> = _state.asStateFlow()

    init {
        voice.init()
        viewModelScope.launch {
            voice.ready.collect { ready -> _state.update { it.copy(ttsReady = ready) } }
        }
        viewModelScope.launch {
            voice.voices.collect { list -> _state.update { it.copy(voices = list) } }
        }
        viewModelScope.launch {
            voice.error.collect { error -> _state.update { it.copy(ttsError = error) } }
        }
        viewModelScope.launch {
            voice.engineLabel.collect { label -> _state.update { it.copy(ttsEngine = label) } }
        }
        viewModelScope.launch {
            voiceFile.info.collect { file -> _state.update { it.copy(voiceFile = file) } }
        }
        viewModelScope.launch {
            voiceFile.playing.collect { playing -> _state.update { it.copy(voiceFilePlaying = playing) } }
        }
        viewModelScope.launch {
            voiceFile.error.collect { error -> _state.update { it.copy(voiceFileError = error) } }
        }
        refresh()
    }

    fun setTab(tab: Int) {
        _state.update { it.copy(tab = tab) }
    }

    fun refresh() {
        viewModelScope.launch {
            val facts = withContext(Dispatchers.IO) { db.listFacts() }
            val events = withContext(Dispatchers.IO) { db.listEvents() }
            _state.update { it.copy(facts = facts, events = events) }
        }
        viewModelScope.launch {
            val status = withContext(Dispatchers.Default) { engine?.status() }
            _state.update { it.copy(brain = status, brainError = engineError) }
        }
    }

    fun teach(topic: String, question: String, answer: String, trials: Int) {
        val q = question.trim()
        val a = answer.trim()
        if (q.isEmpty() || a.isEmpty()) {
            snackbar("Preencha a pergunta e a resposta.")
            return
        }
        if (_state.value.teaching) return
        if (engine == null) {
            snackbar(engineError ?: "Conectoma indisponível.")
            return
        }
        _state.update { it.copy(teaching = true, progress = null, batchLabel = null) }
        viewModelScope.launch {
            try {
                val learned = learnFact(topic, q, a, trials)
                _state.update { it.copy(teaching = false, progress = null) }
                if (learned != null) {
                    refresh()
                    snackbar(
                        "Memória consolidada: ${learned.fact.topic} — " +
                            "${learned.outcome.trialsUsed} ensaios, " +
                            "${(learned.outcome.expertise * 100).toInt()}% de facilidade.",
                    )
                    if (_state.value.voiceSettings.autoSpeak) {
                        speakOrPlay("Aprendi sobre ${learned.fact.topic}.")
                    }
                }
            } catch (error: Throwable) {
                _state.update { it.copy(teaching = false, progress = null) }
                snackbar("Erro ao ensinar: ${error.message}")
            }
        }
    }

    fun teachBatch(topic: String, notebook: String, trials: Int) {
        val items = TextCode.parseFacts(notebook)
        if (items.isEmpty()) {
            snackbar("Nenhuma linha válida. Use pergunta = resposta em cada linha.")
            return
        }
        if (_state.value.teaching) return
        if (engine == null) {
            snackbar(engineError ?: "Conectoma indisponível.")
            return
        }
        _state.update { it.copy(teaching = true, progress = null) }
        viewModelScope.launch {
            var learned = 0
            var ease = 0f
            try {
                items.forEachIndexed { index, (question, answer) ->
                    _state.update {
                        it.copy(batchLabel = "${index + 1}/${items.size}: ${question.take(48)}")
                    }
                    val result = learnFact(topic, question, answer, trials)
                    if (result != null) {
                        learned++
                        ease = result.outcome.expertise
                    }
                }
                _state.update { it.copy(teaching = false, progress = null, batchLabel = null) }
                refresh()
                snackbar(
                    "$learned de ${items.size} fatos consolidados — " +
                        "${(ease * 100).toInt()}% de facilidade.",
                )
                if (learned > 0 && _state.value.voiceSettings.autoSpeak) {
                    speakOrPlay("Aprendi $learned fatos novos.")
                }
            } catch (error: Throwable) {
                _state.update { it.copy(teaching = false, progress = null, batchLabel = null) }
                snackbar("Erro no caderno: ${error.message}")
            }
        }
    }

    /** Busca a página para mostrar o que ela contém antes de ensinar. */
    fun previewUrl(rawUrl: String) {
        val target = rawUrl.trim()
        if (target.isEmpty()) {
            snackbar("Cole a URL da página que deseja ensinar.")
            return
        }
        if (_state.value.webLoading) return
        _state.update { it.copy(webLoading = true, webError = null, webPreview = null) }
        viewModelScope.launch {
            try {
                val page = withContext(Dispatchers.IO) { WebSource.fetch(target) }
                if (page.sections.isEmpty()) {
                    _state.update {
                        it.copy(webLoading = false, webError = "A página não trouxe texto legível.")
                    }
                } else {
                    _state.update { it.copy(webLoading = false, webPreview = page) }
                }
            } catch (error: Throwable) {
                _state.update {
                    it.copy(webLoading = false, webError = "Não consegui buscar: ${error.message}")
                }
            }
        }
    }

    /** Ensina, trecho a trecho, a página já buscada. */
    fun teachFromUrl(rawUrl: String, topic: String, trials: Int) {
        val page = _state.value.webPreview ?: run {
            snackbar("Busque a página antes de ensinar.")
            return
        }
        val wanted = runCatching { WebSource.normalize(rawUrl) }.getOrNull()
        if (wanted != null && rawUrl.isNotBlank() && page.url != wanted) {
            snackbar("A URL mudou depois da busca. Busque a página de novo.")
            return
        }
        if (_state.value.teaching) return
        if (engine == null) {
            snackbar(engineError ?: "Conectoma indisponível.")
            return
        }
        _state.update { it.copy(teaching = true, progress = null, batchLabel = "Lendo a página...") }
        viewModelScope.launch {
            var learned = 0
            var ease = 0f
            try {
                page.sections.forEachIndexed { index, section ->
                    _state.update {
                        it.copy(batchLabel = "${index + 1}/${page.sections.size}: ${section.title.take(44)}")
                    }
                    val chosenTopic = topic.trim().ifEmpty {
                        page.title.ifEmpty { page.host }
                    }
                    val reference = section.title.ifEmpty {
                        section.body.split(Regex("[.!?]")).first().take(90)
                    }
                    val result = learnFact(chosenTopic, reference, section.body, trials)
                    if (result != null) {
                        learned++
                        ease = result.outcome.expertise
                    }
                }
                _state.update { it.copy(teaching = false, progress = null, batchLabel = null) }
                refresh()
                snackbar(
                    "$learned de ${page.sections.size} trechos aprendidos de ${page.host} — " +
                        "${(ease * 100).toInt()}% de facilidade.",
                )
                if (learned > 0 && _state.value.voiceSettings.autoSpeak) {
                    speakOrPlay("Aprendi $learned trechos de ${page.host}.")
                }
            } catch (error: Throwable) {
                _state.update { it.copy(teaching = false, progress = null, batchLabel = null) }
                snackbar("Erro ao aprender a página: ${error.message}")
            }
        }
    }

    private suspend fun learnFact(
        topic: String,
        question: String,
        answer: String,
        trials: Int,
    ): LearnedFact? {
        val engine = engine ?: return null
        val outcome = withContext(Dispatchers.Default) {
            engine.train(question, trials) { progress ->
                _state.update { it.copy(progress = progress) }
            }
        }
        val safeTopic = topic.trim().ifEmpty { "geral" }
        val fact = Fact(
            topic = safeTopic,
            question = question,
            answer = answer,
            strength = (0.45f + 0.55f * outcome.stability).coerceIn(0f, 1f),
            stability = outcome.stability,
            trials = trials,
            spikes = outcome.spikes,
            daPulses = outcome.daPulses,
            assembly = outcome.assembly,
            engram = outcome.engram,
            createdAt = System.currentTimeMillis(),
        )
        val id = withContext(Dispatchers.IO) { db.insertFact(fact) }
        withContext(Dispatchers.IO) {
            db.addEvent(
                LearningEvent(
                    kind = EventKind.LEARN,
                    topic = safeTopic,
                    title = question,
                    detail = answer,
                    strength = fact.strength,
                    spikes = outcome.spikes,
                    daPulses = outcome.daPulses,
                    createdAt = fact.createdAt,
                ),
            )
        }
        // Liga a nova memória às ideias que já existem e registra no diário.
        val existing = withContext(Dispatchers.IO) { db.listFacts() }.filter { it.id != id }
        val connections = Concepts.linksTo(fact.copy(id = id), existing)
        if (connections.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                db.addEvent(
                    LearningEvent(
                        kind = EventKind.LINK,
                        topic = safeTopic,
                        title = question,
                        detail = connections.take(3).joinToString("; ") { (other, shared) ->
                            "${other.question.take(44)} ← ${shared.joinToString(", ")}"
                        },
                        strength = (connections.first().second.size / 6f).coerceIn(0f, 1f),
                        spikes = 0,
                        daPulses = 0,
                        createdAt = System.currentTimeMillis(),
                    ),
                )
            }
        }
        _state.update { it.copy(lastTraining = outcome, lastTrace = outcome.trace) }
        return LearnedFact(fact.copy(id = id), outcome)
    }

    fun ask(question: String) {
        val text = question.trim()
        if (text.isEmpty() || _state.value.asking) return
        addMessage(ChatMessage(System.nanoTime(), fromUser = true, text = text))
        _state.update { it.copy(asking = true) }

        viewModelScope.launch {
            val facts = _state.value.facts
            if (facts.isEmpty()) {
                addMessage(
                    ChatMessage(
                        System.nanoTime(),
                        fromUser = false,
                        text = "Ainda não aprendi nada. Me ensina na aba Ensinar e eu guardo na minha " +
                            "memória de conectoma.",
                    ),
                )
                _state.update { it.copy(asking = false) }
                return@launch
            }
            val engine = engine
            if (engine == null) {
                addMessage(
                    ChatMessage(
                        System.nanoTime(),
                        fromUser = false,
                        text = engineError ?: "Conectoma indisponível.",
                    ),
                )
                _state.update { it.copy(asking = false) }
                return@launch
            }
            try {
                val outcome: RecallOutcome = withContext(Dispatchers.Default) {
                    engine.recall(text, facts)
                }
                val best = outcome.best
                if (best != null) {
                    // Resposta livre: a composição costura as memórias ligadas;
                    // sem composição, vale o texto do fato mais próximo.
                    val composed = outcome.composed
                    val answerText = composed?.text ?: best.fact.answer
                    withContext(Dispatchers.IO) { db.recordRecall(best.fact.id) }
                    withContext(Dispatchers.IO) {
                        db.addEvent(
                            LearningEvent(
                                kind = EventKind.RECALL,
                                topic = best.fact.topic,
                                title = text,
                                detail = answerText,
                                strength = best.score,
                                spikes = outcome.trace.spikes,
                                daPulses = outcome.trace.pulses,
                                createdAt = System.currentTimeMillis(),
                            ),
                        )
                    }
                    addMessage(
                        ChatMessage(
                            id = System.nanoTime(),
                            fromUser = false,
                            text = answerText,
                            confidence = best.score,
                            engram = best.engramSimilarity,
                            source = composed?.sources?.joinToString(" · ") ?: best.fact.topic,
                            sources = composed?.sources ?: listOf(best.fact.topic),
                            linked = composed?.linked ?: emptyList(),
                            related = relatedOf(outcome.divergent),
                        ),
                    )
                    if (_state.value.voiceSettings.autoSpeak) {
                        speakOrPlay(answerText)
                    }
                } else {
                    val topics = facts.map { it.topic }.distinct().take(6).joinToString(", ")
                    val message = buildString {
                        append("Ainda não aprendi isso.")
                        if (topics.isNotEmpty()) append(" As memórias que eu tenho são sobre: $topics.")
                        append(" Me ensina na aba Ensinar?")
                    }
                    addMessage(
                        ChatMessage(
                            System.nanoTime(),
                            fromUser = false,
                            text = message,
                            related = relatedOf(outcome.divergent),
                        ),
                    )
                    withContext(Dispatchers.IO) {
                        db.addEvent(
                            LearningEvent(
                                kind = EventKind.MISS,
                                topic = "",
                                title = text,
                                detail = message,
                                strength = outcome.best?.score ?: 0f,
                                spikes = outcome.trace.spikes,
                                daPulses = outcome.trace.pulses,
                                createdAt = System.currentTimeMillis(),
                            ),
                        )
                    }
                    if (_state.value.voiceSettings.autoSpeak) {
                        speakOrPlay("Ainda não aprendi isso.")
                    }
                }
                _state.update { it.copy(asking = false, lastTrace = outcome.trace) }
                refresh()
            } catch (error: Throwable) {
                addMessage(
                    ChatMessage(
                        System.nanoTime(),
                        fromUser = false,
                        text = "Tive um problema ao lembrar: ${error.message}",
                    ),
                )
                _state.update { it.copy(asking = false) }
            }
        }
    }

    fun clearChat() {
        _state.update { it.copy(chat = emptyList()) }
        voice.stop()
    }

    fun replay(fact: Fact) {
        if (_state.value.teaching) return
        _state.update { it.copy(teaching = true, progress = null, batchLabel = "Reconsolidando memória") }
        viewModelScope.launch {
            try {
                val engine = engine ?: return@launch
                val outcome = withContext(Dispatchers.Default) {
                    engine.replay(fact.question, fact.trials.coerceAtLeast(3)) { progress ->
                        _state.update { it.copy(progress = progress) }
                    }
                }
                val strength = (0.45f + 0.55f * outcome.stability).coerceIn(0f, 1f)
                withContext(Dispatchers.IO) {
                    db.updateFactAfterReplay(
                        id = fact.id,
                        strength = strength,
                        stability = outcome.stability,
                        spikes = outcome.spikes,
                        daPulses = outcome.daPulses,
                        assembly = outcome.assembly,
                        engram = outcome.engram,
                    )
                    db.addEvent(
                        LearningEvent(
                            kind = EventKind.REPLAY,
                            topic = fact.topic,
                            title = fact.question,
                            detail = "Memória reconsolidada com ${outcome.daPulses} pulsos de dopamina.",
                            strength = strength,
                            spikes = outcome.spikes,
                            daPulses = outcome.daPulses,
                            createdAt = System.currentTimeMillis(),
                        ),
                    )
                }
                _state.update {
                    it.copy(teaching = false, progress = null, batchLabel = null, lastTraining = outcome, lastTrace = outcome.trace)
                }
                refresh()
                snackbar("Memória reconsolidada.")
            } catch (error: Throwable) {
                _state.update { it.copy(teaching = false, progress = null, batchLabel = null) }
                snackbar("Erro ao reconsolidar: ${error.message}")
            }
        }
    }

    fun deleteFact(fact: Fact) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                db.deleteFact(fact.id)
                db.addEvent(
                    LearningEvent(
                        kind = EventKind.FORGET,
                        topic = fact.topic,
                        title = fact.question,
                        detail = "Memória apagada.",
                        strength = 0f,
                        spikes = 0,
                        daPulses = 0,
                        createdAt = System.currentTimeMillis(),
                    ),
                )
            }
            refresh()
            snackbar("Memória apagada.")
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { db.clearAll() }
            withContext(Dispatchers.Default) { engine?.resetPlasticity() }
            _state.update { it.copy(chat = emptyList(), lastTraining = null, lastTrace = null) }
            refresh()
            snackbar("Memórias, plasticidade e facilidade de aprender zeradas.")
        }
    }

    fun resetPlasticity() {
        viewModelScope.launch {
            withContext(Dispatchers.Default) { engine?.resetPlasticity() }
            refresh()
            snackbar("Sinapses voltaram ao peso original do FlyWire.")
        }
    }

    fun runSpontaneous() {
        if (_state.value.spontaneousRunning) return
        _state.update { it.copy(spontaneousRunning = true) }
        viewModelScope.launch {
            val trace = withContext(Dispatchers.Default) { engine?.spontaneous(1400) }
            _state.update { it.copy(spontaneousRunning = false, lastTrace = trace) }
        }
    }

    fun setVoiceSettings(settings: VoiceSettings) {
        voice.saveSettings(settings)
        _state.update { it.copy(voiceSettings = settings) }
    }

    fun testVoice(text: String) {
        speakOrPlay(text.ifBlank { "Olá. Eu sou a mosca do Conectoma." })
    }

    fun stopVoice() {
        voice.stop()
        voiceFile.stop()
    }

    /** Toca o arquivo de voz importado, fora do fluxo de respostas. */
    fun playVoiceFile() {
        if (_state.value.voiceFilePlaying) voiceFile.stop() else voiceFile.play()
    }

    fun importVoiceFile(uri: Uri) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { voiceFile.importFromUri(uri) }
            if (ok) {
                snackbar("Voz importada: ${voiceFile.info.value?.displayName}")
            } else {
                snackbar(voiceFile.error.value ?: "Arquivo de voz rejeitado.")
            }
        }
    }

    fun removeVoiceFile() {
        voiceFile.remove()
        setUseVoiceFile(false)
        snackbar("Arquivo de voz removido.")
    }

    fun setUseVoiceFile(enabled: Boolean) {
        prefs.edit().putBoolean("use_voice_file", enabled).apply()
        _state.update { it.copy(useVoiceFile = enabled) }
    }

    /** Voz da mosca: arquivo de IA quando ativo, senão o TTS do sistema. */
    private fun speakOrPlay(text: String) {
        if (_state.value.useVoiceFile && _state.value.voiceFile != null) {
            voiceFile.play()
        } else {
            voice.speak(text, _state.value.voiceSettings)
        }
    }

    fun setKeepScreenOn(enabled: Boolean) {
        prefs.edit().putBoolean("keep_screen_on", enabled).apply()
        _state.update { it.copy(keepScreenOn = enabled) }
    }

    fun exportJson(): String {
        val facts = _state.value.facts
        val array = JSONArray()
        for (fact in facts) {
            array.put(
                JSONObject()
                    .put("id", fact.id)
                    .put("topic", fact.topic)
                    .put("question", fact.question)
                    .put("answer", fact.answer)
                    .put("strength", fact.strength.toDouble())
                    .put("stability", fact.stability.toDouble())
                    .put("trials", fact.trials)
                    .put("spikes", fact.spikes)
                    .put("daPulses", fact.daPulses)
                    .put("assembly", fact.assembly)
                    .put("createdAt", fact.createdAt)
                    .put("recallCount", fact.recallCount)
                    .put("engram", JSONArray(fact.engram.map { it.toDouble() })),
            )
        }
        return JSONObject()
            .put("app", "Conectoma")
            .put("dataset", "FlyWire FAFB v783 (CC-BY-4.0)")
            .put("exportedAt", System.currentTimeMillis())
            .put("facts", array)
            .toString(2)
    }

    fun consumeSnackbar() {
        _state.update { it.copy(snackbar = null) }
    }

    private fun snackbar(message: String) {
        _state.update { it.copy(snackbar = message) }
    }

    private fun relatedOf(ideas: List<DivergentIdea>): List<RelatedIdea> =
        ideas.map { RelatedIdea(question = it.fact.question, via = it.via) }

    private fun addMessage(message: ChatMessage) {
        _state.update { it.copy(chat = (it.chat + message).takeLast(200)) }
    }

    override fun onCleared() {
        voice.shutdown()
        voiceFile.close()
        db.close()
        super.onCleared()
    }
}
