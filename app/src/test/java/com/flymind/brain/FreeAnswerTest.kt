package com.flymind.brain

import com.flymind.data.Fact
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class FreeAnswerTest {

    private fun fact(id: Long, topic: String, question: String, answer: String): Fact = Fact(
        id = id,
        topic = topic,
        question = question,
        answer = answer,
        strength = 0.9f,
        stability = 0.9f,
        trials = 4,
        spikes = 900,
        daPulses = 12,
        assembly = 110,
        engram = FloatArray(Connectome.READOUT_SIZE) { ((it * 37 + id) % 17) / 17f },
        createdAt = System.currentTimeMillis(),
    )

    private val fotossintese = fact(
        1,
        "Biologia",
        "O que e fotossintese?",
        "A fotossintese converte luz em energia quimica na planta.",
    )
    private val clorofila = fact(
        2,
        "Biologia",
        "O que e clorofila?",
        "A clorofila capta a luz do sol usada na fotossintese.",
    )
    private val futebol = fact(
        3,
        "Esporte",
        "Quem inventou o futebol?",
        "As regras modernas do futebol nasceram na Inglaterra.",
    )

    @Test
    fun vocabularioAproximaPalavrasQueDividemContexto() {
        val space = SemanticSpace.build(listOf(fotossintese, clorofila, futebol))
        val near = space.neighbours("fotossintese", 5).map { it.first }
        println("vizinhos de fotossintese: $near")
        assertTrue(
            "nada proximo de fotossintese: $near",
            near.contains("clorofila") || near.contains("planta") || near.contains("luz"),
        )

        val query = space.analyse("de que forma a planta capta a luz do sol")
        println("expansao: ${query.expanded}")
        assertTrue("nao expandiu a pergunta", query.expanded.isNotEmpty())

        val overlap = query.overlap("A clorofila capta a luz do sol usada na fotossintese.")
        println("sobreposicao semantica: $overlap")
        assertTrue("sobreposicao baixa: $overlap", overlap > 0.4f)
        assertEquals(0f, query.overlap("As regras do futebol nasceram na Inglaterra."), 0.0001f)
    }

    @Test
    fun respostaLivreCosturaMemoriasLigadas() {
        val ranked = listOf(
            FactMatch(clorofila, 0.70f, 0.60f, 0.55f, 0.80f),
            FactMatch(fotossintese, 0.52f, 0.40f, 0.45f, 0.60f),
            FactMatch(futebol, 0.10f, 0f, 0.20f, 0f),
        )
        val composed = Composer.compose("como a planta capta luz", ranked, listOf("planta", "luz"))
        assertNotNull("nao compôs", composed)
        val text = composed!!.text
        println("resposta composta: $text")
        assertTrue("faltou a memoria principal", text.contains("clorofila"))
        assertTrue("nao costurou a segunda memoria", text.contains("fotossintese"))
        assertFalse("inventou futebol", text.contains("futebol"))
        assertTrue("texto sem ponto final", text.endsWith("."))
        assertFalse(
            "frases coladas sem espaço: $text",
            Regex("[.!?][A-ZÀ-Ú]").containsMatchIn(text),
        )
        assertEquals(listOf("Biologia"), composed.sources)
        assertTrue("perdeu a ligacao semantica", composed.linked.contains("luz"))
    }

    @Test
    fun semEvidenciaNaoCompoe() {
        val ranked = listOf(FactMatch(futebol, 0.10f, 0f, 0.10f, 0f))
        assertNull(Composer.compose("qual e a capital da Australia", ranked))
        assertNull(Composer.compose("qual e a capital da Australia", emptyList()))
    }

    @Test
    fun perguntaForaDoPadraoRecebeRespostaDoConectoma() {
        val connectome = File("src/main/assets/connectome/flywire_mb.bin").inputStream()
            .use { Connectome.fromStream(it) }
        val dir = Files.createTempDirectory("flymind-free").toFile()
        val engine = LearningEngine(connectome, dir)

        val learned = listOf(
            "Como a planta capta a luz do sol?" to
                "A clorofila capta a luz do sol e alimenta a fotossintese.",
            "O que e fotossintese?" to
                "A fotossintese converte luz em energia quimica na planta.",
        ).mapIndexed { index, (question, answer) ->
            val outcome = engine.train(question, 3)
            Fact(
                id = index + 1L,
                topic = "Biologia",
                question = question,
                answer = answer,
                strength = 0.9f,
                stability = outcome.stability,
                trials = 3,
                spikes = outcome.spikes,
                daPulses = outcome.daPulses,
                assembly = outcome.assembly,
                engram = outcome.engram,
                createdAt = System.currentTimeMillis(),
            )
        }

        val outcome = engine.recall("de que forma a planta capta a luz do sol?", learned)
        println(
            "livre: score=${outcome.best?.score} lexico=${outcome.best?.lexical} " +
                "semantico=${outcome.best?.semantic} composta=${outcome.composed?.text}",
        )
        assertNotNull("nao respondeu a pergunta livre", outcome.best)
        assertNotNull("nao compôs a resposta", outcome.composed)
        val text = outcome.composed!!.text
        assertTrue(
            "resposta sem conteudo aprendido: $text",
            text.contains("clorofila") || text.contains("fotossintese"),
        )
        assertTrue(
            "nao usou a memoria certa",
            outcome.best!!.fact.question.contains("planta", ignoreCase = true),
        )
    }
}
