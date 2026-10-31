package com.flymind.brain

import com.flymind.data.Fact

data class ComposedAnswer(
    val text: String,
    val facts: List<Fact>,
    val sources: List<String>,
    val linked: List<String>,
    val confidence: Float,
)

/**
 * Monta a resposta livre: parte da memória mais próxima da pergunta e costura
 * com as outras que compartilham ideias, usando conectivos variados.
 *
 * Nada é inventado — cada frase vem de um fato realmente ensinado. O que a
 * composição faz é escolher, ordenar e ligar, o que é exatamente o que uma
 * resposta fora do padrão exige.
 */
object Composer {
    /** Abaixo disto nem vale responder: é melhor dizer que não aprendeu. */
    const val FLOOR = 0.16f
    const val SUPPORT_RATIO = 0.55f
    private const val MAX_SUPPORTS = 2
    private const val MAX_LENGTH = 700

    private val bridges = listOf(
        "Também guardei algo ligado a isso: ",
        "Isso se conecta com outro fato que aprendi: ",
        "Na mesma rede de ideias, tenho também: ",
        "Aproximei outra memória a isso: ",
    )

    fun compose(
        query: String,
        ranked: List<FactMatch>,
        expanded: List<String> = emptyList(),
    ): ComposedAnswer? {
        if (ranked.isEmpty()) return null
        val top = ranked.first()
        if (top.score < FLOOR) return null

        val topAnswer = top.fact.answer.trim()
        val supports = ranked.drop(1)
            .filter { it.score >= top.score * SUPPORT_RATIO }
            .filter { it.fact.answer.trim() != topAnswer }
            .distinctBy { it.fact.answer.trim().lowercase() }
            .take(MAX_SUPPORTS)

        val used = listOf(top) + supports
        val bridge = bridges[Math.floorMod(query.hashCode(), bridges.size)]
        val text = StringBuilder(sentence(topAnswer, capitalize = true))
        supports.forEachIndexed { index, match ->
            text.append(' ')
            text.append(if (index == 0) bridge else "E ainda: ")
            text.append(sentence(match.fact.answer, capitalize = true))
        }

        val vocabulary = used
            .flatMap { TextCode.tokens("${it.fact.topic} ${it.fact.question} ${it.fact.answer}") }
            .toSet()
        val linked = expanded.filter { it in vocabulary }.distinct().take(4)

        return ComposedAnswer(
            text = text.toString().replace(Regex("\\s+"), " ").take(MAX_LENGTH).trim(),
            facts = used.map { it.fact },
            sources = used.map { it.fact.topic }.distinct(),
            linked = linked,
            confidence = top.score,
        )
    }

    private fun sentence(raw: String, capitalize: Boolean): String {
        var body = raw.trim().replace(Regex("\\s+"), " ")
        if (body.isEmpty()) return body
        body = if (capitalize) body.replaceFirstChar { it.uppercase() } else body
        return if (body.last() in ".!?") body else "$body."
    }
}
