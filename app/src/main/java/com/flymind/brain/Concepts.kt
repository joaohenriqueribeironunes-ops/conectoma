package com.flymind.brain

import com.flymind.data.Fact

/**
 * Associação divergente: fatos são ligados por ideias (conceitos) em comum e a
 * ativação se propaga por essa rede, trazendo ideias vizinhas mesmo quando as
 * palavras da pergunta não batem direto com a memória.
 */
data class DivergentIdea(
    val fact: Fact,
    val score: Float,
    val via: String,
)

object Concepts {

    const val MAX_CONCEPTS = 30
    const val MIN_SCORE = 0.07f
    private const val PROPAGATION = 0.6f
    private const val HOPS = 2

    /** Termos que representam a ideia de um texto (sem palavras de ligação). */
    fun extract(text: String): List<String> = TextCode.tokens(text)
        .asSequence()
        .filter { it.length >= 3 }
        .distinct()
        .take(MAX_CONCEPTS)
        .toList()

    fun shared(a: List<String>, b: List<String>): List<String> {
        if (a.isEmpty() || b.isEmpty()) return emptyList()
        val setB = b.toSet()
        return a.filter { it in setB }
    }

    /** Ideias que ligam este fato aos demais já aprendidos. */
    fun linksTo(fact: Fact, others: List<Fact>): List<Pair<Fact, List<String>>> {
        val own = extract("${fact.topic} ${fact.question} ${fact.answer}")
        return others.mapNotNull { other ->
            val common = shared(own, extract("${other.topic} ${other.question} ${other.answer}"))
            if (common.size >= 2) other to common.take(3) else null
        }.sortedByDescending { it.second.size }
    }

    /**
     * Propaga ativação pela rede de conceitos: parte da consulta, sobe e desce
     * dois saltos pelas ligações entre fatos e soma a similaridade do engrama
     * do circuito. Devolve as ideias conectadas, menos a que já virou resposta.
     */
    fun divergent(
        query: String,
        facts: List<Fact>,
        queryEngram: FloatArray? = null,
        excludeId: Long? = null,
        limit: Int = 3,
    ): List<DivergentIdea> {
        if (facts.size < 2) return emptyList()

        val concepts = HashMap<Long, List<String>>()
        for (fact in facts) {
            concepts[fact.id] = extract("${fact.topic} ${fact.question} ${fact.answer}")
        }
        val queryConcepts = extract(query)

        val seed = HashMap<Long, Float>()
        for (fact in facts) {
            val lexical = TextCode.lexicalSimilarity(query, "${fact.topic} ${fact.question}")
            val overlap = if (queryConcepts.isEmpty()) {
                0f
            } else {
                shared(queryConcepts, concepts.getValue(fact.id)).size.toFloat() / queryConcepts.size
            }
            seed[fact.id] = 0.7f * lexical + 0.3f * overlap
        }

        val activation = HashMap(seed)
        repeat(HOPS) {
            val next = HashMap<Long, Float>()
            for (i in facts.indices) {
                for (j in i + 1 until facts.size) {
                    val a = facts[i]
                    val b = facts[j]
                    val common = shared(concepts.getValue(a.id), concepts.getValue(b.id))
                    if (common.isEmpty()) continue
                    val union = (concepts.getValue(a.id).toSet() + concepts.getValue(b.id).toSet()).size
                    if (union == 0) continue
                    val weight = common.size.toFloat() / union
                    val actA = activation[a.id] ?: 0f
                    val actB = activation[b.id] ?: 0f
                    next[a.id] = (next[a.id] ?: 0f) + PROPAGATION * weight * actB
                    next[b.id] = (next[b.id] ?: 0f) + PROPAGATION * weight * actA
                }
            }
            for ((id, value) in next) {
                activation[id] = (activation[id] ?: 0f) + value
            }
        }

        return facts.mapNotNull { fact ->
            if (fact.id == excludeId) return@mapNotNull null
            var score = activation[fact.id] ?: 0f
            if (queryEngram != null) {
                val raw = cosine(queryEngram, fact.engram)
                score += 0.35f * (((raw - 0.35f) / 0.65f).coerceIn(0f, 1f))
            }
            if (score < MIN_SCORE) return@mapNotNull null
            DivergentIdea(fact, score.coerceIn0Or1(), viaFor(fact, queryConcepts, concepts, facts, seed))
        }.sortedByDescending { it.score }.take(limit)
    }

    private fun Float.coerceIn0Or1(): Float = coerceIn(0f, 1f)

    private fun viaFor(
        fact: Fact,
        queryConcepts: List<String>,
        concepts: Map<Long, List<String>>,
        facts: List<Fact>,
        seed: Map<Long, Float>,
    ): String {
        val own = concepts.getValue(fact.id)
        val direct = shared(queryConcepts, own)
        if (direct.isNotEmpty()) return direct.take(3).joinToString(", ")

        var best: List<String> = emptyList()
        var bestScore = 0f
        for (other in facts) {
            if (other.id == fact.id) continue
            val linkScore = (seed[other.id] ?: 0f) * shared(concepts.getValue(other.id), own).size
            if (linkScore > bestScore) {
                bestScore = linkScore
                best = shared(concepts.getValue(other.id), own)
            }
        }
        return if (best.isNotEmpty()) best.take(3).joinToString(", ") else "rede de conceitos"
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
        return dot / (sqrt(na) * sqrt(nb))
    }

    private fun sqrt(value: Float): Float = kotlin.math.sqrt(value)
}
