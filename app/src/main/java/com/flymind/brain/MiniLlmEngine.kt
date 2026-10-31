package com.flymind.brain

import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.math.tanh
import kotlin.random.Random

/**
 * Motor matematico em miniatura inspirado em IAs LLM.
 *
 * Arquitetura: embedding de tokens + embedding posicional + camadas de
 * atencao multi-cabeça com softmax causal + feed-forward com residual +
 * projeção final para vocabulário. O aprendizado é por gradiente descendente
 * (cross-entropy sobre a próxima palavra) e a conversa é gerada por amostragem
 * da distribuição de próxima palavra com temperatura.
 *
 * O vocabulário é o dado que o usuário ensina: cada palavra vira token e a
 * sequência passa pelo motor. As respostas "espontâneas" são a próxima palavra
 * que o motor amostra a partir do que ele aprendeu dos dados recebidos.
 */
class MiniLlmEngine(
    val vocabulary: List<String>,
    private val dim: Int = 32,
    private val layers: Int = 2,
    private val heads: Int = 2,
    private val ffnDim: Int = 64,
    private val maxSeq: Int = 48,
    private val seed: Int = 20261031,
) {

    private val dimPerHead = dim / heads
    private val rng = Random(seed)
    private val learningRate = 0.05f

    private val embed: Array<FloatArray> = Array(vocabulary.size) { FloatArray(dim) { gaussian() } }
    private val pos: Array<FloatArray> = Array(maxSeq) { FloatArray(dim) { gaussian() } }
    private val attn = Array(layers) { FloatArray(4 * dim * dim) { gaussian() } }
    private val ffnW1 = Array(layers) { FloatArray(dim * ffnDim) { gaussian() } }
    private val ffnW2 = Array(layers) { FloatArray(ffnDim * dim) { gaussian() } }
    private val outW = FloatArray(vocabulary.size * dim) { gaussian() }

    private val gOutW = FloatArray(vocabulary.size * dim)
    private val gAttn = Array(layers) { FloatArray(4 * dim * dim) }
    private val gW1 = Array(layers) { FloatArray(dim * ffnDim) }
    private val gW2 = Array(layers) { FloatArray(ffnDim * dim) }
    private val gEmbed = Array(vocabulary.size) { FloatArray(dim) }
    private val gPos = Array(maxSeq) { FloatArray(dim) }

    val vocabSize: Int get() = vocabulary.size

    fun knows(word: String): Boolean = vocabulary.any { it.equals(word, ignoreCase = true) }

    fun tokenId(word: String): Int {
        val idx = vocabulary.indexOfFirst { it.equals(word, ignoreCase = true) }
        if (idx < 0) throw IllegalArgumentException("palavra fora do vocabulario do motor: $word")
        return idx
    }

    fun toWord(id: Int): String = vocabulary[id]

    private fun gaussian(): Float = rng.nextFloat() * 0.08f - 0.04f

    private fun attnW(l: Int, k: Int, i: Int, j: Int): Float = attn[l * 4 * dim * dim + k * dim * dim + i * dim + j]

    private fun attnWArr(l: Int, k: Int): FloatArray = FloatArray(dim * dim) { attnW(l, k, it / dim, it % dim) }

    private fun ffnW1Arr(l: Int): FloatArray = FloatArray(dim * ffnDim) { ffnW1[l][it] }

    private fun ffnW2Arr(l: Int): FloatArray = FloatArray(ffnDim * dim) { ffnW2[l][it] }

    private fun batchMatVec(w: FloatArray, x: FloatArray, n: Int, rows: Int, cols: Int): FloatArray {
        val out = FloatArray(n * rows)
        for (i in 0 until n) {
            for (r in 0 until rows) {
                var s = 0f
                for (c in 0 until cols) s += w[r * cols + c] * x[i * cols + c]
                out[i * rows + r] = s
            }
        }
        return out
    }

    private fun batchVecMat(x: FloatArray, w: FloatArray, n: Int, rows: Int, cols: Int): FloatArray {
        val out = FloatArray(n * cols)
        for (i in 0 until n) {
            for (c in 0 until cols) {
                var s = 0f
                for (r in 0 until rows) s += x[i * rows + r] * w[r * cols + c]
                out[i * cols + c] = s
            }
        }
        return out
    }

    private fun softmax(x: FloatArray, n: Int, size: Int): FloatArray {
        val out = FloatArray(n * size)
        for (i in 0 until n) {
            var max = -1e38f
            for (j in 0 until size) if (x[i * size + j] > max) max = x[i * size + j]
            var sum = 0f
            val exps = FloatArray(size)
            for (j in 0 until size) {
                val e = exp(x[i * size + j] - max)
                exps[j] = e
                sum += e
            }
            for (j in 0 until size) out[i * size + j] = exps[j] / sum
        }
        return out
    }

    private class AttentionState {
        var q: FloatArray = FloatArray(0)
        var k: FloatArray = FloatArray(0)
        var v: FloatArray = FloatArray(0)
        var probs: FloatArray = FloatArray(0)
        var ctx: FloatArray = FloatArray(0)
    }

    private class LayerState {
        var x: FloatArray = FloatArray(0)
        var attnOut: FloatArray = FloatArray(0)
        var y: FloatArray = FloatArray(0)
        var fPre: FloatArray = FloatArray(0)
        var fAct: FloatArray = FloatArray(0)
        var z: FloatArray = FloatArray(0)
        val attn = AttentionState()
    }

    private val states = Array(layers) { LayerState() }
    private var seqLen = 0

    fun forward(ids: IntArray, startPos: Int): FloatArray {
        val n = minOf(ids.size, maxSeq)
        if (n == 0) throw IllegalArgumentException("motor de linguagem precisa de pelo menos um token")
        val seq = IntArray(n) { ids[it].coerceIn(0, vocabSize - 1) }
        seqLen = n
        var x = FloatArray(n * dim)
        for (i in 0 until n) {
            val t = seq[i]
            val p = (startPos + i).coerceIn(0, maxSeq - 1)
            for (d in 0 until dim) x[i * dim + d] = embed[t][d] + pos[p][d]
        }
        for (l in layers.indices) {
            val st = states[l]
            st.x = x.copyOf()
            val q = batchMatVec(attnWArr(l, 0), x, n, dim, dim)
            val k = batchMatVec(attnWArr(l, 1), x, n, dim, dim)
            val v = batchMatVec(attnWArr(l, 2), x, n, dim, dim)
            st.attn.q = q
            st.attn.k = k
            st.attn.v = v
            val s = FloatArray(n * n)
            val probs = FloatArray(n * n)
            for (i in 0 until n) {
                var max = -1e38f
                for (j in 0 until n) {
                    val dot = if (j <= i) {
                        var acc = 0f
                        for (r in 0 until dim) acc += q[i * dim + r] * k[j * dim + r]
                        acc / sqrt(dimPerHead.toDouble()).toFloat()
                    } else {
                        -1e38f
                    }
                    s[i * n + j] = dot
                    if (dot > max) max = dot
                }
                var sum = 0f
                val exps = FloatArray(n)
                for (j in 0 until n) {
                    val e = exp(s[i * n + j] - max)
                    exps[j] = e
                    sum += e
                }
                for (j in 0 until n) probs[i * n + j] = exps[j] / sum
            }
            st.attn.probs = probs
            val ctx = FloatArray(n * dim)
            for (i in 0 until n) {
                for (r in 0 until dim) {
                    var acc = 0f
                    for (j in 0 until n) {
                        if (j <= i) acc += probs[i * n + j] * v[j * dim + r]
                    }
                    ctx[i * dim + r] = acc
                }
            }
            st.attn.ctx = ctx
            val b = batchVecMat(ctx, attnWArr(l, 3), n, dim, dim)
            st.attnOut = b
            val y = FloatArray(n * dim) { x[it] + b[it] }
            st.y = y
            val fPre = batchVecMat(y, ffnW1Arr(l), n, dim, ffnDim)
            val fAct = FloatArray(fPre.size) { tanh(fPre[it]) }
            st.fPre = fPre
            st.fAct = fAct
            val fA = batchVecMat(fAct, ffnW2Arr(l), n, ffnDim, dim)
            val z = FloatArray(n * dim) { y[it] + fA[it] }
            st.z = z
            x = z
        }
        val lastVec = x.copyOfRange((n - 1) * dim, n * dim)
        val logits = FloatArray(vocabSize)
        for (o in 0 until vocabSize) {
            var acc = 0f
            for (d in 0 until dim) acc += lastVec[d] * outW[o * dim + d]
            logits[o] = acc
        }
        return softmax(logits, 1, vocabSize)
    }

    private fun attentionBackprop(l: Int, st: LayerState, dB: FloatArray): FloatArray {
        val n = seqLen
        val Wo = attnWArr(l, 3)
        val dCtx = FloatArray(n * dim)
        for (i in 0 until n) {
            for (c in 0 until dim) {
                var s = 0f
                for (o in 0 until dim) s += dB[i * dim + o] * Wo[c * dim + o]
                dCtx[i * dim + c] = s
            }
        }
        val q = st.attn.q
        val k = st.attn.k
        val v = st.attn.v
        val probs = st.attn.probs
        val dQ = FloatArray(n * dim)
        val dK = FloatArray(n * dim)
        val dV = FloatArray(n * dim)
        val dP = FloatArray(n * n)
        val dS = FloatArray(n * n)
        for (h in 0 until heads) {
            val dh = dimPerHead
            for (i in 0 until n) {
                for (j in 0 until n) {
                    if (j > i) continue
                    var accD = 0f
                    for (d in 0 until dh) accD += dCtx[i * dim + h * dh + d] * v[j * dim + h * dh + d]
                    dP[i * n + j] += accD
                }
            }
            for (i in 0 until n) {
                var sumP = 0f
                for (j in 0 until n) sumP += probs[i * n + j] * dP[i * n + j]
                for (j in 0 until n) {
                    if (j > i) continue
                    dS[i * n + j] = probs[i * n + j] * (dP[i * n + j] - sumP)
                }
            }
            for (i in 0 until n) {
                for (j in 0 until n) {
                    if (j > i) continue
                    val coef = 1f / sqrt(dimPerHead.toDouble()).toFloat()
                    for (r in 0 until dh) {
                        dQ[i * dim + h * dh + r] += dS[i * n + j] * k[j * dim + h * dh + r] * coef
                        dK[j * dim + h * dh + r] += dS[i * n + j] * q[i * dim + h * dh + r] * coef
                    }
                }
            }
            for (j in 0 until n) {
                for (d in 0 until dh) {
                    var acc = 0f
                    for (i in 0 until n) {
                        if (j <= i) acc += dCtx[i * dim + h * dh + d] * probs[i * n + j]
                    }
                    dV[j * dim + h * dh + d] = acc
                }
            }
        }
        val x = st.x
        for (r in 0 until dim) {
            for (c in 0 until dim) {
                var gq = 0f
                var gk = 0f
                var gv = 0f
                for (i in 0 until n) {
                    gq += dQ[i * dim + r] * x[i * dim + c]
                    gk += dK[i * dim + r] * x[i * dim + c]
                    gv += dV[i * dim + r] * x[i * dim + c]
                }
                gAttn[l][r * dim + c] += gq
                gAttn[l][dim * dim + r * dim + c] += gk
                gAttn[l][2 * dim * dim + r * dim + c] += gv
            }
        }
        for (c in 0 until dim) {
            for (o in 0 until dim) {
                var g = 0f
                for (i in 0 until n) g += dB[i * dim + o] * st.attn.ctx[i * dim + c]
                gAttn[l][3 * dim * dim + c * dim + o] += g
            }
        }
        val dxAttn = FloatArray(n * dim)
        for (i in 0 until n) {
            for (c in 0 until dim) {
                var acc = 0f
                for (r in 0 until dim) {
                    acc += dQ[i * dim + r] * attnW(l, 0, r, c)
                    acc += dK[i * dim + r] * attnW(l, 1, r, c)
                    acc += dV[i * dim + r] * attnW(l, 2, r, c)
                }
                dxAttn[i * dim + c] = acc
            }
        }
        return dxAttn
    }

    private fun backward(dLogits: FloatArray, seq: IntArray, startPos: Int, target: Int): FloatArray {
        val n = seqLen
        val nLayers = layers
        val zLast = states[nLayers - 1].z
        val dx = FloatArray(n * dim)
        for (o in 0 until vocabSize) {
            var g = dLogits[o]
            if (o == target) g -= 1f
            for (d in 0 until dim) {
                gOutW[o * dim + d] += zLast[d] * g
                dx[d] += g * outW[o * dim + d]
            }
        }
        var dz = dx
        for (l in nLayers - 1 downTo 0) {
            val st = states[l]
            val dA = dz.copyOf()
            val dLdyFromFfn = FloatArray(n * dim)
            for (i in 0 until n) {
                for (d in 0 until dim) {
                    var s = 0f
                    for (c in 0 until ffnDim) s += dA[i * ffnDim + c] * ffnW2[l][c * dim + d]
                    dLdyFromFfn[i * dim + d] = s
                }
            }
            val dy = FloatArray(n * dim) { dz[it] + dLdyFromFfn[it] }
            for (i in 0 until n) {
                for (c in 0 until ffnDim) {
                    var dfpre = dA[i * ffnDim + c]
                    val f = st.fAct[i * ffnDim + c]
                    dfpre *= 1f - f * f
                    for (r in 0 until dim) gW1[l][r * ffnDim + c] += st.y[i * dim + r] * dfpre
                    for (d in 0 until dim) gW2[l][c * dim + d] += f * dA[i * dim + d]
                }
            }
            val dxAttn = attentionBackprop(l, st, dy)
            val dxLayer = FloatArray(n * dim) { dy[it] + dxAttn[it] }
            dz = dxLayer
        }
        for (i in 0 until n) {
            for (d in 0 until dim) {
                gEmbed[seq[i] * dim + d] += dz[i * dim + d]
                gPos[(startPos + i) * dim + d] += dz[i * dim + d]
            }
        }
        return dz
    }

    private fun sgd() {
        for (l in layers.indices) {
            for (i in 0 until 4 * dim * dim) attn[l][i] -= learningRate * gAttn[l][i]
            for (i in 0 until dim * ffnDim) ffnW1[l][i] -= learningRate * gW1[l][i]
            for (i in 0 until ffnDim * dim) ffnW2[l][i] -= learningRate * gW2[l][i]
            for (i in 0 until 4 * dim * dim) gAttn[l][i] = 0f
            for (i in 0 until dim * ffnDim) gW1[l][i] = 0f
            for (i in 0 until ffnDim * dim) gW2[l][i] = 0f
        }
        for (t in 0 until vocabulary.size) {
            for (d in 0 until dim) {
                embed[t][d] -= learningRate * gEmbed[t][d]
                gEmbed[t][d] = 0f
            }
        }
        for (p in 0 until maxSeq) {
            for (d in 0 until dim) pos[p][d] -= learningRate * gPos[p][d]
            for (d in 0 until dim) gPos[p][d] = 0f
        }
        for (i in 0 until vocabulary.size * dim) gOutW[i] = 0f
    }

    fun train(questionIds: IntArray, answerIds: IntArray, epochs: Int = 6): Float {
        val seq = IntArray(questionIds.size + answerIds.size)
        System.arraycopy(questionIds, 0, seq, 0, questionIds.size)
        System.arraycopy(answerIds, 0, seq, questionIds.size, answerIds.size)
        var totalLoss = 0f
        repeat(epochs) {
            for (i in seq.indices) {
                val input = seq.copyOfRange(0, i + 1)
                val target = seq[i + 1]
                val probs = forward(input, 0)
                var loss = 0f
                val dLogits = FloatArray(vocabSize)
                for (o in 0 until vocabSize) {
                    val p = probs[o]
                    loss -= kotlin.math.ln(p.coerceAtLeast(1e-12f).toDouble()).toFloat() * (if (o == target) 1f else 0f)
                    dLogits[o] = p - (if (o == target) 1f else 0f)
                }
                backward(dLogits, seq, 0, target)
                sgd()
                totalLoss += loss
            }
        }
        return totalLoss / (epochs * seq.size)
    }

    private fun sample(probs: FloatArray, temperature: Float): Int {
        val weights = FloatArray(probs.size) { if (probs[it] > 0.0001f) pow(probs[it].toDouble(), temperature.toDouble()).toFloat() else 0f }
        val total = weights.sum()
        var r = kotlin.math.random().toFloat() * total
        var acc = 0f
        for (i in weights.indices) {
            acc += weights[i]
            if (r <= acc) return i
        }
        return weights.lastIndex
    }

    fun respond(inputIds: IntArray, maxNewTokens: Int = 10, temperature: Float = 0.9): String {
        var seq = IntArray(inputIds.size) { inputIds[it].coerceIn(0, vocabSize - 1) }
        val words = ArrayList<String>()
        for (t in 0 until maxNewTokens) {
            val probs = forward(seq, 0)
            val next = sample(probs, temperature)
            words.add(vocabulary[next])
            if (seq.size >= maxSeq) break
            val newSeq = IntArray(seq.size + 1)
            System.arraycopy(seq, 0, newSeq, 0, seq.size)
            newSeq[seq.size] = next
            seq = newSeq
        }
        return words.joinToString(" ")
    }
}
