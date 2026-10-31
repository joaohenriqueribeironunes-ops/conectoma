package com.flymind.brain

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.util.Random
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sqrt

data class SimTrace(
    val activity: FloatArray,
    val dopamine: FloatArray,
    val rasterNeuron: IntArray,
    val rasterStep: IntArray,
    val spikes: Int,
    val activeNeurons: Int,
    val pulses: Int,
    val meanDopamine: Float,
    val weightDelta: Float,
    val engram: FloatArray,
)

/**
 * Rede de neurônios LIF (leaky integrate-and-fire) sobre o conectoma real.
 *
 * Plasticidade: STDP de três fatores. O par pré/pós deixa uma elegibilidade na
 * sinapse; um pulso de dopamina ("recompensa") converte essa elegibilidade em
 * mudança de peso. É o mesmo princípio do aprendizado dependente de recompensa
 * nos corpos pedunculados da mosca.
 */
class SpikingBrain(
    val graph: Connectome,
    seed: Long = 42L,
    private val synapticGain: Float = SYNAPTIC_GAIN,
    private val synapticDecay: Float = SYNAPTIC_DECAY,
) {
    private val n = graph.neurons
    private val edgeCount = graph.edges

    val weights: FloatArray = graph.outWeight.copyOf()
    private val baseline: FloatArray = graph.outWeight.copyOf()

    private val v = FloatArray(n) { REST }
    private val refractory = IntArray(n)
    private val current = FloatArray(n)
    private val nextCurrent = FloatArray(n)
    private val inputCurrent = FloatArray(n)
    private val lastSpike = IntArray(n) { -1_000_000 }
    private val eligibility = FloatArray(edgeCount)
    private val eligibilityAt = IntArray(edgeCount) { -1_000_000 }

    private val engramSlot = IntArray(n) { -1 }
    private val engramTrace = FloatArray(Connectome.READOUT_SIZE)

    private var lastPattern: IntArray = IntArray(0)
    private val pulses = ArrayDeque<Int>()
    private var lastDopamineApply = -1_000_000
    private var time = 0
    private var pulseCount = 0
    var dopamine: Float = 0f
        private set

    /**
     * Ajustes de metaplasticidade vindos de [Plasticity]: quanto a mosca já
     * aprendeu muda o ganho por pulso de dopamina, o teto de fortalecimento de
     * cada sinapse e o quanto a consolidação estrutural soma.
     */
    var metaplasticity: Float = 0f
    var weightCapFactor: Float = MAX_WEIGHT_FACTOR
    var learningRateFactor: Float = 1f

    private val rasterNeuron = IntArray(MAX_RASTER)
    private val rasterStep = IntArray(MAX_RASTER)
    private var rasterCount = 0

    private val random = Random(seed)

    init {
        // O engrama é a assembleia de células de Kenyon: cada KC ativa uma das
        // 512 dimensões. É a população clássica de memória dos corpos pedunculados.
        for (i in 0 until n) {
            if (graph.labelOf(i) == "Kenyon_Cell") {
                engramSlot[i] = i % Connectome.READOUT_SIZE
            }
        }
    }

    fun resetState() {
        v.fill(REST)
        refractory.fill(0)
        current.fill(0f)
        nextCurrent.fill(0f)
        inputCurrent.fill(0f)
        lastSpike.fill(-1_000_000)
        eligibility.fill(0f)
        eligibilityAt.fill(-1_000_000)
        engramTrace.fill(0f)
        pulses.clear()
        lastDopamineApply = -1_000_000
        time = 0
        pulseCount = 0
        dopamine = 0f
        rasterCount = 0
    }

    /** Aplica corrente aos neurônios sensoriais que codificam o texto. */
    fun setInput(pattern: IntArray, drive: Float = INPUT_DRIVE) {
        inputCurrent.fill(0f)
        lastPattern = pattern.copyOf()
        for (neuron in pattern) {
            if (neuron in 0 until n) inputCurrent[neuron] += drive
        }
    }

    fun clearInput() {
        inputCurrent.fill(0f)
    }

    /** Agenda uma rajada de dopamina (recompensa) nos próximos milissegundos. */
    fun rewardPulses(count: Int = 3) {
        for (k in 0 until count) pulses.addLast(time + 8 + k * 25)
    }

    fun run(durationMs: Int): SimTrace {
        rasterCount = 0
        val activity = FloatArray(durationMs)
        val da = FloatArray(durationMs)
        val start = time
        var spikes = 0
        for (ms in 0 until durationMs) {
            val s = stepOnce()
            spikes += s
            activity[ms] = s.toFloat() / n
            da[ms] = dopamine
        }
        var active = 0
        for (i in 0 until n) if (lastSpike[i] >= start) active++
        var meanDa = 0f
        for (x in da) meanDa += x
        if (da.isNotEmpty()) meanDa /= da.size
        return SimTrace(
            activity = activity,
            dopamine = da,
            rasterNeuron = rasterNeuron.copyOf(rasterCount),
            rasterStep = rasterStep.copyOf(rasterCount),
            spikes = spikes,
            activeNeurons = active,
            pulses = pulseCount,
            meanDopamine = meanDa,
            weightDelta = weightDelta(),
            engram = engram(),
        )
    }

    /**
     * Engrama = assembleia de KCs recrutada pelas sinapses reais das entradas
     * (80%) + atividade observada no circuito (20%). A parte estrutural é estável
     * e específica; a parte dinâmica carrega a plasticidade do treino.
     */
    fun engram(): FloatArray {
        val structural = structuralEngram()
        val dynamic = normalized(engramTrace)
        val out = FloatArray(Connectome.READOUT_SIZE) { k ->
            0.8f * structural[k] + 0.2f * dynamic[k]
        }
        val combined = normalized(out)
        if (combined.any { it != 0f } || structural.any { it != 0f }) return combined
        return dynamic
    }

    private fun structuralEngram(): FloatArray {
        val out = FloatArray(Connectome.READOUT_SIZE)
        for (pre in lastPattern) {
            if (pre !in 0 until n) continue
            val end = graph.outStart[pre + 1]
            var e = graph.outStart[pre]
            while (e < end) {
                val post = graph.outPost[e]
                val slot = engramSlot[post]
                if (slot >= 0 && graph.outSign[e] > 0) out[slot] += weights[e]
                e++
            }
        }
        return normalized(out)
    }

    private fun normalized(values: FloatArray): FloatArray {
        val out = values.copyOf()
        var norm = 0f
        for (x in out) norm += x * x
        norm = sqrt(norm)
        if (norm > 1e-6f) {
            for (k in out.indices) out[k] /= norm
        }
        return out
    }

    fun weightDelta(): Float {
        var sum = 0f
        var base = 0f
        for (e in 0 until edgeCount) {
            sum += abs(weights[e] - baseline[e])
            base += baseline[e]
        }
        return if (base <= 1e-6f) 0f else sum / base
    }

    fun meanWeightChangePercent(): Float = weightDelta() * 100f

    fun saveSnapshot(file: File) {
        file.parentFile?.mkdirs()
        DataOutputStream(file.outputStream().buffered()).use { out ->
            out.writeBytes("FLYW")
            out.writeInt(edgeCount)
            for (w in weights) out.writeFloat(w)
        }
    }

    fun loadSnapshot(file: File): Boolean {
        if (!file.exists() || file.length() < 12 + edgeCount * 4L) return false
        return runCatching {
            DataInputStream(file.inputStream().buffered()).use { input ->
                val magic = ByteArray(4)
                input.readFully(magic)
                if (String(magic, Charsets.US_ASCII) != "FLYW") return false
                val count = input.readInt()
                if (count != edgeCount) return false
                for (e in 0 until edgeCount) {
                    val w = input.readFloat()
                    if (w.isFinite() && w > 0f) weights[e] = w
                }
            }
            true
        }.getOrDefault(false)
    }

    fun reseed(seed: Long) {
        random.setSeed(seed)
    }

    fun lastSpikeSnapshot(): IntArray = lastSpike.copyOf()

    fun resetPlasticity() {
        baseline.copyInto(weights)
        eligibility.fill(0f)
        eligibilityAt.fill(-1_000_000)
    }

    fun baselineWeights(): FloatArray = baseline

    /**
     * Consolidação estrutural do caminho da memória (entrada → interneurônios →
     * saída): cada aprendizado soma força às sinapses excitatórias que essa
     * memória percorre. É a captura do traço de longa duração — vale mesmo
     * quando o STDP daquela passagem foi ruidoso, e é o que faz a mesma memória
     * ser recrutada com menos ensaios da próxima vez.
     */
    fun consolidate(pattern: IntArray, gain: Float): Int {
        if (pattern.isEmpty() || gain <= 0f) return 0
        val boost = gain * (1f + CONSOLIDATION_BOOST * metaplasticity)
        val touched = HashSet<Int>()
        for (pre in pattern) {
            if (pre !in 0 until n) continue
            var e = graph.outStart[pre]
            val end = graph.outStart[pre + 1]
            while (e < end) {
                if (graph.outSign[e] > 0) {
                    val base = baseline[e]
                    val cap = base * weightCapFactor
                    if (weights[e] < cap) {
                        weights[e] = (weights[e] + boost * base).coerceAtMost(cap)
                        touched.add(e)
                    }
                }
                e++
            }
        }
        if (touched.isNotEmpty()) scaleHomeostatic()
        return touched.size
    }

    /** Sinapses que ficaram acima do peso original do FlyWire. */
    fun strengthenedSynapses(): Int {
        var count = 0
        for (e in 0 until edgeCount) {
            if (weights[e] > baseline[e] * STRENGTHENED_MARGIN) count++
        }
        return count
    }

    /**
     * Escalonamento homeostático: se a excitação total cresceu demais, todos os
     * pesos descem na mesma proporção. Mantém o circuito longe da saturação sem
     * apagar o padrão relativo que as memórias construíram.
     */
    private fun scaleHomeostatic() {
        var sum = 0f
        var base = 0f
        for (e in 0 until edgeCount) {
            sum += weights[e]
            base += baseline[e]
        }
        if (base <= 1e-6f) return
        val ratio = sum / base
        if (ratio <= HOMEOSTASIS_LIMIT) return
        val factor = HOMEOSTASIS_LIMIT / ratio
        for (e in 0 until edgeCount) weights[e] *= factor
    }

    private fun stepOnce(): Int {
        time++
        dopamine *= DA_DECAY

        while (pulses.isNotEmpty() && pulses.first() <= time) {
            pulses.removeFirst()
            pulseCount++
            dopamine = (dopamine + 1f).coerceAtMost(2f)
            for (neuron in graph.dopamineIndices) current[neuron] += DA_STIMULUS
        }

        repeat(NOISE_EVENTS) {
            current[random.nextInt(n)] += NOISE_MV
        }

        var spikes = 0
        for (i in 0 until n) {
            if (refractory[i] > 0) {
                refractory[i]--
                continue
            }
            v[i] += ((REST - v[i]) + current[i] + inputCurrent[i] + TONIC) / TAU_MEMBRANE
            if (v[i] >= THRESHOLD) {
                v[i] = RESET
                refractory[i] = REFRACTORY_MS
                spikes++
                onSpike(i)
            }
        }

        for (i in 0 until n) {
            current[i] = nextCurrent[i] + current[i] * synapticDecay
            nextCurrent[i] = 0f
        }
        for (k in engramTrace.indices) engramTrace[k] *= ENGRAM_DECAY

        applyDopamineIfNeeded()
        return spikes
    }

    private fun onSpike(neuron: Int) {
        lastSpike[neuron] = time
        val slot = engramSlot[neuron]
        if (slot >= 0) engramTrace[slot] = minOf(engramTrace[slot] + 1f, 1f)

        if (rasterCount < MAX_RASTER) {
            rasterNeuron[rasterCount] = neuron
            rasterStep[rasterCount] = time
            rasterCount++
        }

        // LTP: o neurônio pós-disparou logo depois de um pré.
        var e = graph.inStart[neuron]
        val inEnd = graph.inStart[neuron + 1]
        while (e < inEnd) {
            val pre = graph.inPre[e]
            val dt = time - lastSpike[pre]
            if (dt in 1..STDP_WINDOW_MS) {
                addEligibility(graph.inEdge[e], A_PLUS * exp(-dt / TAU_STDP))
            }
            e++
        }

        // LTD: o neurônio pré-disparou logo depois de um pós.
        e = graph.outStart[neuron]
        val outEnd = graph.outStart[neuron + 1]
        while (e < outEnd) {
            val post = graph.outPost[e]
            val dt = time - lastSpike[post]
            if (dt in 1..STDP_WINDOW_MS) {
                addEligibility(e, -A_MINUS * exp(-dt / TAU_STDP))
            }
            val sign = graph.outSign[e].toInt()
            if (sign != 0) {
                nextCurrent[post] += weights[e] * sign * synapticGain
            }
            e++
        }
    }

    private fun addEligibility(edge: Int, delta: Float) {
        val decayed = eligibility[edge] * exp(-(time - eligibilityAt[edge]) / TAU_ELIGIBILITY)
        eligibility[edge] = decayed + delta
        eligibilityAt[edge] = time
    }

    private fun applyDopamineIfNeeded() {
        if (dopamine < DA_THRESHOLD || time - lastDopamineApply < DA_APPLY_INTERVAL_MS) return
        lastDopamineApply = time
        for (e in 0 until edgeCount) {
            val eff = eligibility[e] * exp(-(time - eligibilityAt[e]) / TAU_ELIGIBILITY)
            if (abs(eff) < 1e-3f) {
                eligibility[e] = eff
                eligibilityAt[e] = time
                continue
            }
            val base = baseline[e]
            val delta = DA_LEARNING_RATE * learningRateFactor * dopamine * eff
            weights[e] = (weights[e] + delta).coerceIn(base * MIN_WEIGHT_FACTOR, base * weightCapFactor)
            eligibility[e] = eff * 0.5f
            eligibilityAt[e] = time
        }
    }

    companion object {
        const val REST = -65f
        const val RESET = -65f
        const val THRESHOLD = -50f
        const val TAU_MEMBRANE = 20f
        const val REFRACTORY_MS = 2
        const val SYNAPTIC_DECAY = 0.95f
        const val TONIC = 3.5f
        const val NOISE_EVENTS = 20
        const val NOISE_MV = 8f
        const val INPUT_DRIVE = 26f
        const val SYNAPTIC_GAIN = 18f
        const val DA_STIMULUS = 15f

        const val STDP_WINDOW_MS = 25
        const val TAU_STDP = 18f
        const val TAU_ELIGIBILITY = 260f
        const val A_PLUS = 0.9f
        const val A_MINUS = 0.9f

        const val DA_THRESHOLD = 0.05f
        const val DA_DECAY = 0.9944f
        const val DA_LEARNING_RATE = 0.035f
        const val DA_APPLY_INTERVAL_MS = 20

        const val MIN_WEIGHT_FACTOR = 0.45f
        const val MAX_WEIGHT_FACTOR = 1.75f
        const val CONSOLIDATION_BOOST = 1.4f
        const val HOMEOSTASIS_LIMIT = 1.35f
        const val STRENGTHENED_MARGIN = 1.02f
        const val ENGRAM_DECAY = 0.995f
        const val MAX_RASTER = 1200
    }
}
