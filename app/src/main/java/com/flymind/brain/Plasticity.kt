package com.flymind.brain

import java.io.File
import kotlin.math.exp
import kotlin.math.roundToInt

/**
 * Plasticidade acumulada da mosca.
 *
 * Cada aprendizado deixa um rastro físico no circuito: as sinapses do caminho
 * daquela memória ficam mais fortes (potenciação de longa duração) e a própria
 * capacidade de aprender aumenta (metaplasticidade). O acúmulo vira uma
 * "facilidade de aprender" normalizada em 0..1 que controla três coisas:
 *
 *  - quantos ensaios um fato novo precisa;
 *  - quanto o critério de reconhecimento relaxa com a vivência;
 *  - quanto ainda cabe de fortalecimento em cada sinapse.
 *
 * A curva é exponencial saturante: as primeiras memórias ensinam muito, as
 * seguintes somam cada vez menos, como a consolidação de um circuito real.
 */
data class Plasticity(
    val learnings: Int = 0,
    val replays: Int = 0,
    val recalls: Int = 0,
    val potentiation: Float = 0f,
    val lastUpdated: Long = 0L,
) {

    /** Experiência ponderada: ensinar vale 1, reconsolidar 0,5, lembrar 0,15. */
    val experience: Float
        get() = learnings + REPLAY_WEIGHT * replays + RECALL_WEIGHT * recalls

    /** Facilidade de aprender, 0..1, saturando com a experiência. */
    val expertise: Float
        get() = (1f - exp(-experience / SATURATION)).coerceIn(0f, 1f)

    /** Ensaios necessários: até 50% menos no topo, nunca abaixo de [MIN_TRIALS]. */
    fun trialsFor(requested: Int): Int {
        val ceiling = requested.coerceAtLeast(MIN_TRIALS)
        val eased = (ceiling * (1f - MAX_EASING * expertise)).roundToInt()
        return eased.coerceIn(MIN_TRIALS, ceiling)
    }

    /** Alívio do critério de aceitação de uma lembrança. */
    val thresholdRelax: Float
        get() = MAX_RELAX * expertise

    /** Multiplicador do ganho de plasticidade por pulso de dopamina. */
    val learningRateGain: Float
        get() = 1f + RATE_BOOST * expertise

    /** Teto de fortalecimento de uma sinapse, relativo ao peso original. */
    val weightCap: Float
        get() = BASE_CAP + (MAX_CAP - BASE_CAP) * expertise

    fun afterLearning(gain: Float, now: Long = System.currentTimeMillis()): Plasticity = copy(
        learnings = learnings + 1,
        potentiation = (potentiation + gain).coerceIn(0f, MAX_POTENTIATION),
        lastUpdated = now,
    )

    fun afterReplay(gain: Float, now: Long = System.currentTimeMillis()): Plasticity = copy(
        replays = replays + 1,
        potentiation = (potentiation + gain).coerceIn(0f, MAX_POTENTIATION),
        lastUpdated = now,
    )

    fun afterRecall(now: Long = System.currentTimeMillis()): Plasticity =
        copy(recalls = recalls + 1, lastUpdated = now)

    fun encode(): String = buildString {
        appendLine("version=$VERSION")
        appendLine("learnings=$learnings")
        appendLine("replays=$replays")
        appendLine("recalls=$recalls")
        appendLine("potentiation=$potentiation")
        appendLine("last_updated=$lastUpdated")
    }

    fun save(file: File) {
        runCatching {
            file.parentFile?.mkdirs()
            file.writeText(encode())
        }
    }

    companion object {
        const val VERSION = 1
        const val MIN_TRIALS = 2
        const val MAX_EASING = 0.5f
        const val MAX_RELAX = 0.10f
        const val RATE_BOOST = 1.6f
        const val BASE_CAP = 1.75f
        const val MAX_CAP = 2.6f
        const val SATURATION = 20f
        const val REPLAY_WEIGHT = 0.5f
        const val RECALL_WEIGHT = 0.15f
        const val MAX_POTENTIATION = 10_000f

        fun load(file: File): Plasticity =
            if (!file.exists()) Plasticity() else runCatching { parse(file.readText()) }
                .getOrDefault(Plasticity())

        fun parse(text: String): Plasticity {
            val values = HashMap<String, String>()
            for (line in text.lines()) {
                val idx = line.indexOf('=')
                if (idx <= 0) continue
                values[line.substring(0, idx).trim()] = line.substring(idx + 1).trim()
            }
            return Plasticity(
                learnings = values["learnings"]?.toIntOrNull()?.coerceAtLeast(0) ?: 0,
                replays = values["replays"]?.toIntOrNull()?.coerceAtLeast(0) ?: 0,
                recalls = values["recalls"]?.toIntOrNull()?.coerceAtLeast(0) ?: 0,
                potentiation = values["potentiation"]?.toFloatOrNull()?.takeIf { it.isFinite() } ?: 0f,
                lastUpdated = values["last_updated"]?.toLongOrNull() ?: 0L,
            )
        }
    }
}
