package com.flymind.brain

import android.content.Context
import java.io.BufferedInputStream
import java.io.DataInputStream

/** Papel funcional de cada neurônio dentro do subcircuito embarcado. */
object Role {
    const val INPUT = 0
    const val INTER = 1
    const val DOPAMINE = 2
    const val OUTPUT = 3

    fun label(role: Int): String = when (role) {
        INPUT -> "sensorial"
        INTER -> "interneurônio"
        DOPAMINE -> "dopaminérgico"
        OUTPUT -> "saída"
        else -> "?"
    }
}

object Nt {
    const val ACH = 0
    const val GABA = 1
    const val GLUT = 2
    const val DA = 3
    const val SER = 4
    const val OCT = 5
    const val OTHER = 6

    fun name(nt: Int): String = when (nt) {
        ACH -> "acetilcolina"
        GABA -> "GABA"
        GLUT -> "glutamato"
        DA -> "dopamina"
        SER -> "serotonina"
        OCT -> "octopamina"
        else -> "desconhecido"
    }
}

data class ConnectomeStats(
    val neurons: Int,
    val synapses: Int,
    val inputs: Int,
    val interneurons: Int,
    val dopaminergic: Int,
    val outputs: Int,
    val labels: Int,
    val meanOutDegree: Float,
    val topLabels: List<Pair<String, Int>>,
)

/**
 * Subcircuito FlyWire FAFB v783 empacotado em assets/connectome/flywire_mb.bin.
 *
 * Inclui corpos pedunculados (Kenyon cells, MBON, MBIN), neurônios dopaminérgicos
 * (DAN), vias sensoriais e complexo central. As arestas têm peso derivado do
 * número de sinapses e sinal conforme o neurotransmissor predominante.
 */
class Connectome private constructor(
    val neurons: Int,
    val edges: Int,
    val rootIds: LongArray,
    val roles: IntArray,
    val neurotransmitters: IntArray,
    val labelIds: IntArray,
    val labels: Array<String>,
    val outStart: IntArray,
    val outPost: IntArray,
    val outWeight: FloatArray,
    val outSign: ByteArray,
    val outDelay: ByteArray,
    val inStart: IntArray,
    val inPre: IntArray,
    val inEdge: IntArray,
) {
    val inputIndices: IntArray by lazy {
        (0 until neurons).filter { roles[it] == Role.INPUT }.toIntArray()
    }

    val dopamineIndices: IntArray by lazy {
        (0 until neurons).filter { roles[it] == Role.DOPAMINE }.toIntArray()
    }

    /**
     * 512 neurônios de leitura (Kenyon cells / MBON / interneurônios) usados como
     * assinatura de disparo — o "engrama" que indexa a memória.
     */
    val readoutIndices: IntArray by lazy {
        val candidates = (0 until neurons).filter {
            roles[it] == Role.INTER || roles[it] == Role.OUTPUT
        }
        if (candidates.isEmpty()) {
            IntArray(0)
        } else {
            IntArray(READOUT_SIZE) { k ->
                val pos = (k.toDouble() * candidates.size / READOUT_SIZE).toInt()
                candidates[pos.coerceIn(0, candidates.size - 1)]
            }
        }
    }

    fun labelOf(neuron: Int): String {
        val id = labelIds[neuron]
        return labels.getOrElse(id) { "?" }
    }

    fun stats(): ConnectomeStats {
        val counts = HashMap<String, Int>()
        var inputs = 0
        var inter = 0
        var da = 0
        var outputs = 0
        for (i in 0 until neurons) {
            when (roles[i]) {
                Role.INPUT -> inputs++
                Role.DOPAMINE -> da++
                Role.OUTPUT -> outputs++
                else -> inter++
            }
            val label = labelOf(i)
            counts[label] = (counts[label] ?: 0) + 1
        }
        return ConnectomeStats(
            neurons = neurons,
            synapses = edges,
            inputs = inputs,
            interneurons = inter,
            dopaminergic = da,
            outputs = outputs,
            labels = labels.size,
            meanOutDegree = if (neurons == 0) 0f else edges.toFloat() / neurons,
            topLabels = counts.entries.sortedByDescending { it.value }.take(6)
                .map { it.key to it.value },
        )
    }

    companion object {
        const val READOUT_SIZE = 512
        private const val ASSET = "connectome/flywire_mb.bin"
        private val MAGIC = "FLYWIRE1".toByteArray(Charsets.US_ASCII)

        fun load(context: Context): Connectome =
            context.assets.open(ASSET).use { fromStream(it) }

        fun fromStream(stream: java.io.InputStream): Connectome {
            DataInputStream(BufferedInputStream(stream, 1 shl 16)).use { input ->
                val magic = ByteArray(8)
                input.readFully(magic)
                require(magic.contentEquals(MAGIC)) { "asset do conectoma inválido" }
                val version = input.readInt()
                require(version == 1) { "versão de conectoma não suportada: $version" }
                val neurons = input.readInt()
                val edges = input.readInt()
                val nLabels = input.readInt()
                require(neurons in 100..200_000) { "contagem de neurônios inválida: $neurons" }
                require(edges in 100..2_000_000) { "contagem de sinapses inválida: $edges" }

                val labels = Array(nLabels) {
                    val len = input.readUnsignedShort()
                    val bytes = ByteArray(len)
                    input.readFully(bytes)
                    String(bytes, Charsets.UTF_8)
                }
                val rootIds = LongArray(neurons)
                val roles = IntArray(neurons)
                val nts = IntArray(neurons)
                val labelIds = IntArray(neurons)
                for (i in 0 until neurons) {
                    rootIds[i] = input.readLong()
                    roles[i] = input.readByte().toInt()
                    nts[i] = input.readByte().toInt()
                    labelIds[i] = input.readUnsignedShort()
                }

                val pre = IntArray(edges)
                val post = IntArray(edges)
                val weight = FloatArray(edges)
                val sign = ByteArray(edges)
                val delay = ByteArray(edges)
                for (e in 0 until edges) {
                    pre[e] = input.readInt()
                    post[e] = input.readInt()
                    weight[e] = input.readFloat()
                    sign[e] = input.readByte()
                    delay[e] = input.readByte()
                }

                val outCount = IntArray(neurons + 1)
                val inCount = IntArray(neurons + 1)
                for (e in 0 until edges) {
                    outCount[pre[e] + 1]++
                    inCount[post[e] + 1]++
                }
                for (i in 0 until neurons) {
                    outCount[i + 1] += outCount[i]
                    inCount[i + 1] += inCount[i]
                }
                val outStart = outCount.copyOf()
                val inStart = inCount.copyOf()
                val outCursor = outCount.copyOf()
                val inCursor = inCount.copyOf()
                val outPost = IntArray(edges)
                val outWeight = FloatArray(edges)
                val outSign = ByteArray(edges)
                val outDelay = ByteArray(edges)
                val inPre = IntArray(edges)
                val inEdge = IntArray(edges)
                for (e in 0 until edges) {
                    val p = pre[e]
                    val q = post[e]
                    val o = outCursor[p]++
                    outPost[o] = q
                    outWeight[o] = weight[e]
                    outSign[o] = sign[e]
                    outDelay[o] = delay[e]
                    val k = inCursor[q]++
                    inPre[k] = p
                    inEdge[k] = o
                }

                return Connectome(
                    neurons = neurons,
                    edges = edges,
                    rootIds = rootIds,
                    roles = roles,
                    neurotransmitters = nts,
                    labelIds = labelIds,
                    labels = labels,
                    outStart = outStart,
                    outPost = outPost,
                    outWeight = outWeight,
                    outSign = outSign,
                    outDelay = outDelay,
                    inStart = inStart,
                    inPre = inPre,
                    inEdge = inEdge,
                )
            }
        }
    }
}
