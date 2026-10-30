package com.flymind.brain

import android.content.Context
import com.flymind.data.Fact
import java.io.File
import kotlin.math.sqrt

data class TrainProgress(
    val trial: Int,
    val totalTrials: Int,
    val spikes: Int,
    val daPulses: Int,
    val stability: Float,
    val activeNeurons: Int,
)

data class TrainingOutcome(
    val engram: FloatArray,
    val stability: Float,
    val spikes: Int,
    val daPulses: Int,
    val weightDelta: Float,
    val assembly: Int,
    val trace: SimTrace,
)

data class FactMatch(
    val fact: Fact,
    val score: Float,
    val lexical: Float,
    val engramSimilarity: Float,
)

data class RecallOutcome(
    val best: FactMatch?,
    val alternatives: List<FactMatch>,
    val trace: SimTrace,
    val engram: FloatArray,
    val threshold: Float,
)

data class BrainStatus(
    val stats: ConnectomeStats,
    val weightDelta: Float,
    val snapshotLoaded: Boolean,
    val plasticSynapses: Int,
)

/**
 * Orquestra o circuito: codifica a pergunta, roda a simulação, entrega
 * dopamina nos acertos, consolida o engrama e responde apenas com fatos
 * que foram realmente ensinados.
 */
class LearningEngine(
    val connectome: Connectome,
    filesDir: File,
) {

    constructor(context: Context) : this(Connectome.load(context), context.filesDir)

    private val brain = SpikingBrain(connectome)
    private val snapshotFile = File(filesDir, "connectome_weights.bin")
    private var snapshotLoaded = brain.loadSnapshot(snapshotFile)

    @Synchronized
    fun train(
        question: String,
        trials: Int = 6,
        onProgress: ((TrainProgress) -> Unit)? = null,
    ): TrainingOutcome {
        val pattern = TextCode.pattern(question, connectome.inputIndices)
        val engrams = ArrayList<FloatArray>()
        var spikes = 0
        var pulses = 0
        var assembly = 0
        var lastTrace: SimTrace? = null
        val baseSeed = (question.hashCode().toLong() and 0x7FFFFFFF) * 31L + 7L

        for (trial in 1..trials) {
            brain.resetState()
            brain.reseed(baseSeed + trial * 7919L)
            brain.setInput(pattern)
            val during = brain.run(160)
            brain.clearInput()
            brain.rewardPulses(3)
            val after = brain.run(120)
            val engram = brain.engram()
            engrams.add(engram)
            spikes += during.spikes + after.spikes
            pulses += after.pulses
            assembly = maxOf(assembly, during.activeNeurons)
            lastTrace = after
            onProgress?.invoke(
                TrainProgress(
                    trial = trial,
                    totalTrials = trials,
                    spikes = spikes,
                    daPulses = pulses,
                    stability = meanConsecutiveCosine(engrams),
                    activeNeurons = assembly,
                ),
            )
        }

        val finalEngram = meanEngram(engrams.takeLast(minOf(3, engrams.size)))
        if (brain.weightDelta() > 0f) {
            brain.saveSnapshot(snapshotFile)
            snapshotLoaded = true
        }
        val trace = lastTrace ?: brain.run(1)
        return TrainingOutcome(
            engram = finalEngram,
            stability = meanConsecutiveCosine(engrams),
            spikes = spikes,
            daPulses = pulses,
            weightDelta = brain.weightDelta(),
            assembly = assembly,
            trace = trace,
        )
    }

    @Synchronized
    fun recall(question: String, facts: List<Fact>): RecallOutcome {
        val pattern = TextCode.pattern(question, connectome.inputIndices)
        brain.resetState()
        brain.reseed(991L)
        brain.setInput(pattern)
        val trace = brain.run(160)
        brain.clearInput()
        val engram = brain.engram()

        val scored = facts.map { fact ->
            val lexical = TextCode.lexicalSimilarity(question, "${fact.topic} ${fact.question}")
            val raw = cosine(engram, fact.engram)
            // A similaridade bruta entre engramas de assuntos diferentes fica
            // em torno de 0,5; calibramos para que só assembleias próximas contem.
            val calibrated = ((raw - 0.35f) / 0.65f).coerceIn(0f, 1f)
            val score = 0.6f * lexical + 0.4f * calibrated
            FactMatch(fact, score, lexical, calibrated)
        }.sortedByDescending { it.score }

        val best = scored.firstOrNull()?.takeIf { it.score >= ACCEPT_THRESHOLD || it.lexical >= LEXICAL_ACCEPT }
        return RecallOutcome(
            best = best,
            alternatives = scored.take(4),
            trace = trace,
            engram = engram,
            threshold = ACCEPT_THRESHOLD,
        )
    }

    @Synchronized
    fun spontaneous(durationMs: Int = 1200): SimTrace {
        brain.resetState()
        brain.reseed(System.nanoTime())
        return brain.run(durationMs)
    }

    @Synchronized
    fun status(): BrainStatus {
        var plastic = 0
        val baseline = brain.baselineWeights()
        for (e in 0 until connectome.edges) {
            if (kotlin.math.abs(brain.weights[e] - baseline[e]) > 1e-4f) plastic++
        }
        return BrainStatus(
            stats = connectome.stats(),
            weightDelta = brain.weightDelta(),
            snapshotLoaded = snapshotLoaded,
            plasticSynapses = plastic,
        )
    }

    @Synchronized
    fun resetPlasticity() {
        brain.resetPlasticity()
        snapshotFile.delete()
        snapshotLoaded = false
    }

    private fun meanConsecutiveCosine(engrams: List<FloatArray>): Float {
        if (engrams.size < 2) return 1f
        var sum = 0f
        for (i in 1 until engrams.size) sum += cosine(engrams[i - 1], engrams[i])
        return sum / (engrams.size - 1)
    }

    private fun meanEngram(engrams: List<FloatArray>): FloatArray {
        if (engrams.isEmpty()) return FloatArray(Connectome.READOUT_SIZE)
        val out = FloatArray(Connectome.READOUT_SIZE)
        for (engram in engrams) {
            for (i in out.indices) out[i] += engram.getOrElse(i) { 0f }
        }
        return normalize(out)
    }

    private fun normalize(values: FloatArray): FloatArray {
        val out = values.copyOf()
        var norm = 0f
        for (x in out) norm += x * x
        norm = sqrt(norm)
        if (norm > 1e-6f) for (i in out.indices) out[i] /= norm
        return out
    }

    private fun cosine(a: FloatArray, b: FloatArray): Float {
        if (a.isEmpty() || b.isEmpty()) return 0f
        val size = minOf(a.size, b.size)
        var dot = 0f
        var na = 0f
        var nb = 0f
        for (i in 0 until size) {
            dot += a[i] * b[i]
            na += a[i] * a[i]
            nb += b[i] * b[i]
        }
        if (na <= 1e-9f || nb <= 1e-9f) return 0f
        return dot / sqrt(na * nb)
    }

    companion object {
        const val ACCEPT_THRESHOLD = 0.42f
        const val LEXICAL_ACCEPT = 0.55f
    }
}
