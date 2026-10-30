package com.flymind.brain

import java.text.Normalizer
import java.util.Locale

/**
 * Converte texto em padrões de estimulação dos neurônios sensoriais e mede
 * similaridade entre pergunta lembrada e pergunta atual.
 *
 * Não há modelo de linguagem: o app só responde com fatos que foram ensinados.
 * A similaridade lexical é a rede de segurança quando o engrama do circuito
 * está ruidoso; o engrama é o que carrega a assinatura do conectoma.
 */
object TextCode {
    private val stopwords = setOf(
        "a", "o", "as", "os", "um", "uma", "uns", "umas", "de", "do", "da", "dos", "das",
        "em", "no", "na", "nos", "nas", "por", "pelo", "pela", "para", "pra", "com", "sem",
        "e", "ou", "que", "qual", "quais", "quem", "onde", "quando", "como", "porque",
        "porquê", "se", "ao", "à", "às", "é", "são", "ser", "foi", "era", "tem", "têm",
        "ter", "há", "haver", "me", "te", "se", "lhe", "nos", "vos", "lhes", "meu", "minha",
        "seu", "sua", "este", "esta", "isso", "isto", "aquilo", "esse", "essa", "ele", "ela",
        "eles", "elas", "voce", "você", "eu", "nós", "muito", "mais", "menos", "sobre",
        "entre", "depois", "antes", "também", "nao", "não", "sim", "ja", "já", "só", "todo",
        "toda", "todos", "todas", "pode", "podem", "fazer", "faz", "feito", "coisa", "coisas",
        "apenas", "cada", "outro", "outra", "qualquer", "quanto", "quantos", "quantas",
        // Verbos de pergunta: não carregam o assunto, só o formato do pedido.
        "funciona", "funcionar", "funcionamento", "acontece", "acontecer", "ocorre", "ocorrer",
        "explica", "explicar", "explique", "explicacao", "significa", "significar", "significado",
        "define", "definir", "definicao", "descreve", "descrever", "descreva", "fale", "falar",
        "conte", "contar", "diga", "dizer", "sabe", "saber", "entende", "entender",
    )

    fun normalize(text: String): String {
        val decomposed = Normalizer.normalize(text.lowercase(Locale.ROOT), Normalizer.Form.NFD)
        return decomposed
            .replace(Regex("\\p{M}+"), "")
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun tokens(text: String): List<String> {
        return normalize(text)
            .split(' ')
            .filter { it.length >= 2 && it !in stopwords }
            .map { stem(it) }
    }

    fun terms(text: String): Set<String> {
        val base = tokens(text)
        val out = LinkedHashSet<String>(base)
        for (i in 0 until base.size - 1) out.add(base[i] + "_" + base[i + 1])
        return out
    }

    private fun stem(word: String): String {
        if (word.length > 4 && word.endsWith("s")) return word.dropLast(1)
        return word
    }

    fun lexicalSimilarity(query: String, reference: String): Float {
        val q = terms(query)
        val r = terms(reference)
        if (q.isEmpty() || r.isEmpty()) return 0f
        var common = 0
        for (term in q) if (term in r) common++
        val union = q.size + r.size - common
        val jaccard = if (union == 0) 0f else common.toFloat() / union
        val coverage = common.toFloat() / q.size

        val qTri = trigrams(normalize(query))
        val rTri = trigrams(normalize(reference))
        var triCommon = 0
        for (tri in qTri) if (tri in rTri) triCommon++
        val triUnion = qTri.size + rTri.size - triCommon
        val triJaccard = if (triUnion == 0) 0f else triCommon.toFloat() / triUnion

        return (0.42f * jaccard + 0.38f * coverage + 0.20f * triJaccard).coerceIn(0f, 1f)
    }

    private fun trigrams(text: String): Set<String> {
        val clean = text.replace(" ", "")
        if (clean.length < 3) return if (clean.isEmpty()) emptySet() else setOf(clean)
        val out = HashSet<String>()
        for (i in 0..clean.length - 3) out.add(clean.substring(i, i + 3))
        return out
    }

    /**
     * Seleciona posições do grupo de entrada de forma determinística.
     * O mesmo texto sempre gera o mesmo padrão de estimulação.
     */
    fun pattern(text: String, group: IntArray, slots: Int = 60, salt: Int = 0): IntArray {
        if (group.isEmpty()) return IntArray(0)
        val words = tokens(text).ifEmpty { listOf(normalize(text)) }
        val hashes = ArrayList<Int>(words.size * 3)
        for (i in words.indices) {
            val h = fnv1a(words[i]) xor salt
            hashes.add(h)
            hashes.add(mix(h + 0x9E3779B9.toInt()))
            val next = words.getOrNull(i + 1)
            if (next != null) hashes.add(fnv1a(words[i] + "#" + next) xor salt)
        }
        val positions = LinkedHashSet<Int>()
        val limit = minOf(slots, group.size)
        // Código distribuído: expande os hashes até preencher o número de canais.
        // Textos curtos ainda recrutam uma assembleia ampla, como um contexto sensorial.
        var round = 0
        while (positions.size < limit && round < 64) {
            for (h in hashes) {
                var x = h + round * 0x9E3779B9.toInt()
                repeat(4) {
                    x = mix(x + 0x85EBCA6B.toInt())
                    positions.add(((x % group.size) + group.size) % group.size)
                    if (positions.size >= limit) break
                }
                if (positions.size >= limit) break
            }
            round++
        }
        return IntArray(positions.size) { group[positions.elementAt(it)] }
    }

    /** Aceita "pergunta = resposta", "pergunta | resposta" ou "pergunta => resposta". */
    fun parseFacts(raw: String): List<Pair<String, String>> {
        val out = ArrayList<Pair<String, String>>()
        for (line in raw.lines()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("//")) continue
            val separators = listOf("=>", "|", "=")
            for (sep in separators) {
                val idx = trimmed.indexOf(sep)
                if (idx > 0) {
                    val question = trimmed.substring(0, idx).trim()
                    val answer = trimmed.substring(idx + sep.length).trim()
                    if (question.isNotEmpty() && answer.isNotEmpty()) {
                        out.add(question to answer)
                        break
                    }
                }
            }
        }
        return out
    }

    private fun fnv1a(text: String): Int {
        var hash = -0x7ee3623b
        for (byte in text.toByteArray(Charsets.UTF_8)) {
            hash = hash xor (byte.toInt() and 0xFF)
            hash *= 0x01000193
        }
        return hash
    }

    private fun mix(input: Int): Int {
        var h = input
        h = h xor (h ushr 16)
        h *= 0x7FEB352D
        h = h xor (h ushr 15)
        h *= 0x846CA68B.toInt()
        h = h xor (h ushr 16)
        return h
    }
}
