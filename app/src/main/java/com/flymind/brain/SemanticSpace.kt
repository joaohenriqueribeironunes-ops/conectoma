package com.flymind.brain

import com.flymind.data.Fact
import kotlin.math.sqrt

/**
 * Semântica distribucional construída apenas com o vocabulário que a mosca já
 * tem. Cada palavra vira um vetor esparso das palavras que aparecem perto dela
 * nas memórias aprendidas (janela de [WINDOW] palavras, simétrica); palavras
 * que dividem contexto ficam matematicamente próximas pelo cosseno dos vetores.
 *
 * É o que permite reconhecer "capta", "sol" e "clorofila" como vizinhas mesmo
 * quando a pergunta usa uma palavra que não aparece na memória original. Nada é
 * baixado nem pré-treinado: o espaço nasce e cresce com o que foi ensinado.
 */
class SemanticSpace private constructor(
    private val contexts: Map<String, Map<String, Float>>,
) {

    val vocabularySize: Int get() = contexts.size

    fun knows(term: String): Boolean = contexts.containsKey(term)

    /** Palavras que dividem contexto com [term], das mais próximas às menos. */
    fun neighbours(term: String, limit: Int = 4): List<Pair<String, Float>> {
        val vector = contexts[term] ?: return emptyList()
        val out = ArrayList<Pair<String, Float>>()
        for ((candidate, other) in contexts) {
            if (candidate == term) continue
            val score = cosine(vector, other)
            if (score > 0f) out.add(candidate to score)
        }
        out.sortByDescending { it.second }
        return out.take(limit)
    }

    fun similarity(a: String, b: String): Float {
        val va = contexts[a] ?: return 0f
        val vb = contexts[b] ?: return 0f
        return cosine(va, vb)
    }

    /**
     * Lê a pergunta uma única vez: guarda os termos, os vizinhos de cada termo e
     * a expansão (vizinhos que ainda não estão na pergunta). Reaproveitar isso
     * em todas as memórias é o que mantém a busca barata no celular.
     */
    fun analyse(query: String, limit: Int = 6): SemanticQuery {
        val terms = TextCode.tokens(query).distinct()
        val near = LinkedHashMap<String, List<Pair<String, Float>>>()
        for (term in terms) near[term] = neighbours(term, 6)

        val own = terms.toSet()
        val best = HashMap<String, Float>()
        for (term in terms) {
            for ((candidate, score) in near[term].orEmpty()) {
                if (candidate in own || score < MIN_EXPANSION) continue
                best[candidate] = maxOf(best[candidate] ?: 0f, score)
            }
        }
        val expanded = best.entries.sortedByDescending { it.value }.take(limit).map { it.key }
        return SemanticQuery(terms, near, expanded)
    }

    /** Consulta já analisada: mede quanto uma memória cobre os conceitos dela. */
    class SemanticQuery(
        val terms: List<String>,
        val neighbours: Map<String, List<Pair<String, Float>>>,
        val expanded: List<String>,
    ) {

        /**
         * Fração dos conceitos da pergunta presente na referência, aceitando
         * uma palavra vizinha no lugar da exata (crédito proporcional à
         * proximidade semântica).
         */
        fun overlap(reference: String): Float {
            if (terms.isEmpty()) return 0f
            val ref = TextCode.tokens(reference).toSet()
            if (ref.isEmpty()) return 0f
            var sum = 0f
            for (term in terms) {
                if (term in ref) {
                    sum += 1f
                    continue
                }
                val via = neighbours[term].orEmpty().firstOrNull { it.first in ref } ?: continue
                sum += NEIGHBOUR_CREDIT * via.second
            }
            return (sum / terms.size).coerceIn(0f, 1f)
        }
    }

    companion object {
        const val WINDOW = 3
        const val MIN_EXPANSION = 0.18f
        const val DOCUMENT_LIMIT = 60
        const val DOCUMENT_WEIGHT = 0.35f
        private const val NEIGHBOUR_CREDIT = 0.8f

        fun build(facts: List<Fact>): SemanticSpace =
            fromDocuments(facts.map { TextCode.tokens("${it.topic} ${it.question} ${it.answer}") })

        fun fromDocuments(documents: List<List<String>>): SemanticSpace {
            val contexts = HashMap<String, HashMap<String, Float>>()
            for (doc in documents) {
                if (doc.size < 2) {
                    doc.firstOrNull()?.let { contexts.getOrPut(it) { HashMap() } }
                    continue
                }
                // Contexto local: vizinhança de janela, pesada pela distância.
                for (i in doc.indices) {
                    val word = doc[i]
                    val bucket = contexts.getOrPut(word) { HashMap() }
                    val from = (i - WINDOW).coerceAtLeast(0)
                    val to = (i + WINDOW).coerceAtMost(doc.lastIndex)
                    for (j in from..to) {
                        if (j == i) continue
                        val other = doc[j]
                        bucket[other] = (bucket[other] ?: 0f) + 1f / kotlin.math.abs(i - j)
                    }
                }
                // Co-ocorrência de documento: numa memória curta, as palavras do
                // mesmo fato se explicam mesmo quando estão longe no texto.
                if (doc.size <= DOCUMENT_LIMIT) {
                    val unique = doc.distinct()
                    for (i in unique.indices) {
                        for (j in i + 1 until unique.size) {
                            val a = unique[i]
                            val b = unique[j]
                            contexts.getOrPut(a) { HashMap() }
                                .let { it[b] = (it[b] ?: 0f) + DOCUMENT_WEIGHT }
                            contexts.getOrPut(b) { HashMap() }
                                .let { it[a] = (it[a] ?: 0f) + DOCUMENT_WEIGHT }
                        }
                    }
                }
            }
            return SemanticSpace(contexts)
        }

        private fun cosine(a: Map<String, Float>, b: Map<String, Float>): Float {
            if (a.isEmpty() || b.isEmpty()) return 0f
            val small = if (a.size <= b.size) a else b
            val large = if (a.size <= b.size) b else a
            var dot = 0f
            var na = 0f
            var nb = 0f
            for (value in a.values) na += value * value
            for (value in b.values) nb += value * value
            for ((key, value) in small) {
                val other = large[key] ?: continue
                dot += value * other
            }
            if (na <= 1e-9f || nb <= 1e-9f) return 0f
            return (dot / (sqrt(na) * sqrt(nb))).coerceIn(0f, 1f)
        }
    }
}
