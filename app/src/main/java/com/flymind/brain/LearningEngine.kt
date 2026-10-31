package com.flymind.brain

import android.content.Context
import com.flymind.data.Fact
import java.io.File
import kotlin.math.sqrt

/** Ensaios usados quando o chamador não pede um número específico. */
const val DEFAULT_TRIALS = 6

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
    val trialsUsed: Int = 0,
    val strengthened: Int = 0,
    val expertise: Float = 0f,
)

data class FactMatch(
    val fact: Fact,
    val score: Float,
    val lexical: Float,
    val engramSimilarity: Float,
    val semantic: Float = 0f,
)

data class RecallOutcome(
    val best: FactMatch?,
    val alternatives: List<FactMatch>,
    val trace: SimTrace,
    val engram: FloatArray,
    val threshold: Float,
    val divergent: List<DivergentIdea> = emptyList(),
    val composed: ComposedAnswer? = null,
    val expanded: List<String> = emptyList(),
)

data class BrainStatus(
    val stats: ConnectomeStats,
    val weightDelta: Float,
    val snapshotLoaded: Boolean,
    val plasticSynapses: Int,
    val strengthened: Int = 0,
    val expertise: Float = 0f,
    val learnings: Int = 0,
    val recalls: Int = 0,
    val trialsNow: Int = DEFAULT_TRIALS,
)

/**
 * Orquestra o circuito: codifica a pergunta, roda a simulação, entrega
 * dopamina nos acertos e consolida o engrama.
 *
 * Dois efeitos acumulam com o uso, como numa rede neural biológica:
 *
 *  - cada aprendizado fortalece as sinapses do caminho daquela memória e eleva
 *    a facilidade de aprender (os próximos fatos precisam de menos ensaios);
 *  - cada lembrança bem-sucedida reconsolida a memória, então responder também
 *    reforça o circuito.
 *
 * A resposta usa o que foi ensinado: engrama do conectoma, semântica
 * distribucional do vocabulário próprio e composição entre memórias ligadas.
 */
class LearningEngine(
    val connectome: Connectome,
    filesDir: File,
) {

    constructor(context: Context) : this(Connectome.load(context), context.filesDir)

    private val brain = SpikingBrain(connectome)
    private val snapshotFile = File(filesDir, "connectome_weights.bin")
    private val plasticityFile = File(filesDir, "plasticity.txt")
    private var snapshotLoaded = brain.loadSnapshot(snapshotFile)
    private var plasticity = Plasticity.load(plasticityFile)

    /** Estado acumulado de plasticidade, para leitura externa. */
    val plasticityState: Plasticity get() = plasticity

    private fun applyMetaplasticity() {
        brain.metaplasticity = plasticity.expertise
        brain.weightCapFactor = plasticity.weightCap
        brain.learningRateFactor = plasticity.learningRateGain
    }

    @Synchronized
    fun train(
        question: String,
        trials: Int = DEFAULT_TRIALS,
        onProgress: ((TrainProgress) -> Unit)? = null,
    ): TrainingOutcome = trainInternal(question, trials, replay = false, onProgress = onProgress)

    /**
     * Reconsolidação: repete uma memória já guardada. Soma para a experiência
     * (metade do peso de um fato novo) e fortalece de novo o caminho dela.
     */
    @Synchronized
    fun replay(
        question: String,
        trials: Int = DEFAULT_TRIALS,
        onProgress: ((TrainProgress) -> Unit)? = null,
    ): TrainingOutcome = trainInternal(question, trials, replay = true, onProgress = onProgress)

    private fun trainInternal(
        question: String,
        trials: Int,
        replay: Boolean,
        onProgress: ((TrainProgress) -> Unit)?,
    ): TrainingOutcome {
        applyMetaplasticity()
        // Quanto mais experiente, menos ensaios o mesmo fato exige.
        val plan = plasticity.trialsFor(trials)
        val pattern = TextCode.pattern(question, connectome.inputIndices)
        val engrams = ArrayList<FloatArray>()
        var spikes = 0
        var pulses = 0
        var assembly = 0
        var lastTrace: SimTrace? = null
        val baseSeed = (question.hashCode().toLong() and 0x7FFFFFFF) * 31L + 7L

        for (trial in 1..plan) {
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
                    totalTrials = plan,
                    spikes = spikes,
                    daPulses = pulses,
                    stability = meanConsecutiveCosine(engrams),
                    activeNeurons = assembly,
                ),
            )
        }

        val stability = meanConsecutiveCosine(engrams)
        val finalEngram = meanEngram(engrams.takeLast(minOf(3, engrams.size)))
        // Consolidação: o caminho desta memória ganha força, proporcional ao
        // quanto ela ficou estável entre os ensaios.
        val gain = CONSOLIDATION_GAIN * (0.5f + 0.5f * stability)
        val strengthened = brain.consolidate(pattern, gain)
        plasticity = if (replay) {
            plasticity.afterReplay(gain)
        } else {
            plasticity.afterLearning(gain)
        }
        plasticity.save(plasticityFile)
        brain.saveSnapshot(snapshotFile)
        snapshotLoaded = true

        val trace = lastTrace ?: brain.run(1)
        return TrainingOutcome(
            engram = finalEngram,
            stability = stability,
            spikes = spikes,
            daPulses = pulses,
            weightDelta = brain.weightDelta(),
            assembly = assembly,
            trace = trace,
            trialsUsed = plan,
            strengthened = strengthened,
            expertise = plasticity.expertise,
        )
    }

    @Synchronized
    fun recall(question: String, facts: List<Fact>): RecallOutcome {
        applyMetaplasticity()
        val space = SemanticSpace.build(facts)
        val query = space.analyse(question)
        val pattern = TextCode.pattern(question, connectome.inputIndices)
        brain.resetState()
        brain.reseed(991L)
        brain.setInput(pattern)
        val trace = brain.run(160)
        brain.clearInput()
        val engram = brain.engram()

        val scored = facts.map { fact ->
            val reference = "${fact.topic} ${fact.question} ${fact.answer}"
            val lexical = TextCode.lexicalSimilarity(question, "${fact.topic} ${fact.question}")
            val semantic = query.overlap(reference)
            val raw = cosine(engram, fact.engram)
            // A similaridade bruta entre engramas de assuntos diferentes fica em
            // torno de 0,5; calibramos para que só assembleias próximas contem.
            val calibrated = ((raw - 0.35f) / 0.65f).coerceIn(0f, 1f)
            // Memórias mais consolidadas pesam um pouco mais no desempate.
            val consolidation = 0.8f + 0.2f * fact.strength.coerceIn(0f, 1f)
            val score = (
                LEXICAL_WEIGHT * lexical +
                    SEMANTIC_WEIGHT * semantic +
                    ENGRAM_WEIGHT * calibrated
                ) * consolidation
            FactMatch(fact, score.coerceIn(0f, 1f), lexical, calibrated, semantic)
        }.sortedByDescending { it.score }

        val threshold = (ACCEPT_THRESHOLD - plasticity.thresholdRelax).coerceAtLeast(MIN_THRESHOLD)
        val best = scored.firstOrNull()?.takeIf {
            it.score >= threshold || it.lexical >= LEXICAL_ACCEPT
        }
        // Associação divergente: propaga pela rede de conceitos as ideias que
        // se conectam à pergunta, mesmo quando não são a resposta direta.
        val ideas = Concepts.divergent(
            query = question,
            facts = facts,
            queryEngram = engram,
            excludeId = best?.fact?.id,
        )
        val composed = if (best == null) null else Composer.compose(question, scored, query.expanded)

        // Lembrar também reforça: a memória recuperada é reconsolidada.
        if (best != null) {
            brain.consolidate(pattern, RECALL_CONSOLIDATION * (0.5f + best.score))
            plasticity = plasticity.afterRecall()
            plasticity.save(plasticityFile)
            brain.saveSnapshot(snapshotFile)
            snapshotLoaded = true
        }

        return RecallOutcome(
            best = best,
            alternatives = scored.take(4),
            trace = trace,
            engram = engram,
            threshold = threshold,
            divergent = ideas,
            composed = composed,
            expanded = query.expanded,
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
            strengthened = brain.strengthenedSynapses(),
            expertise = plasticity.expertise,
            learnings = plasticity.learnings,
            recalls = plasticity.recalls,
            trialsNow = plasticity.trialsFor(DEFAULT_TRIALS),
        )
    }

    @Synchronized
    fun resetPlasticity() {
        brain.resetPlasticity()
        snapshotFile.delete()
        plasticity = Plasticity()
        plasticity.save(plasticityFile)
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
        const val MIN_THRESHOLD = 0.30f
        const val LEXICAL_WEIGHT = 0.40f
        const val SEMANTIC_WEIGHT = 0.22f
        const val ENGRAM_WEIGHT = 0.38f
        const val CONSOLIDATION_GAIN = 0.022f
        const val RECALL_CONSOLIDATION = 0.006f
    }
}
