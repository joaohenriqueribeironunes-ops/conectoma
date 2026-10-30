package com.flymind.brain

import com.flymind.data.Fact
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files
import kotlin.math.max

class LearningEngineTest {

    private lateinit var engine: LearningEngine
    private lateinit var dir: File

    @Before
    fun setUp() {
        val connectome = File("src/main/assets/connectome/flywire_mb.bin").inputStream()
            .use { Connectome.fromStream(it) }
        dir = Files.createTempDirectory("flymind-test").toFile()
        engine = LearningEngine(connectome, dir)
    }

    @Test
    fun aprendeEResponde() {
        val question = "O que e a fotossintese?"
        val outcome = engine.train(question, 5)
        println(
            "treino: estabilidade=${outcome.stability} disparos=${outcome.spikes} " +
                "dopamina=${outcome.daPulses} plasticidade=${outcome.weightDelta}",
        )
        assertTrue("sem disparos", outcome.spikes > 100)
        assertTrue("dopamina nao entregue", outcome.daPulses == 15)
        assertTrue("sem plasticidade", outcome.weightDelta > 0.001f)
        assertTrue("engrama vazio", outcome.engram.any { it > 0f })
        assertTrue("snapshot nao salvo", File(dir, "connectome_weights.bin").exists())

        val fact = fact(question, outcome)
        val recall = engine.recall(question, listOf(fact))
        println(
            "recall exato: score=${recall.best?.score} lexical=${recall.best?.lexical} " +
                "engrama=${recall.best?.engramSimilarity}",
        )
        assertNotNull("nao lembrou a pergunta ensinada", recall.best)
        assertTrue("confianca baixa", (recall.best?.score ?: 0f) > 0.6f)
    }

    @Test
    fun distinguePerguntasDiferentes() {
        val question = "Quem foi Santos Dumont?"
        val outcome = engine.train(question, 4)
        assertTrue(outcome.stability > 0.3f)

        val fact = fact(question, outcome)
        val hit = engine.recall(question, listOf(fact))
        val miss = engine.recall("O que e a entropia?", listOf(fact))
        println("acerto=${hit.best?.score} erro=${miss.best?.score} alternativas=${miss.alternatives.size}")
        assertNotNull(hit.best)
        assertNull("respondeu algo que nao aprendeu", miss.best)
    }

    @Test
    fun respondeParafraseENaoInventa() {
        val facts = listOf(
            "O que e a fotossintese?" to "A fotossintese converte luz em energia quimica.",
            "Quem foi Santos Dumont?" to "Santos Dumont foi um pioneiro da aviacao.",
            "O que e entropia?" to "Entropia mede a desordem de um sistema.",
        ).map { (question, answer) ->
            val outcome = engine.train(question, 4)
            Fact(
                id = question.hashCode().toLong(),
                topic = "Teste",
                question = question,
                answer = answer,
                strength = outcome.stability,
                stability = outcome.stability,
                trials = 4,
                spikes = outcome.spikes,
                daPulses = outcome.daPulses,
                assembly = outcome.assembly,
                engram = outcome.engram,
                createdAt = System.currentTimeMillis(),
            )
        }

        val paraphrase = engine.recall("Como funciona a fotossintese?", facts)
        println(
            "parafrase: score=${paraphrase.best?.score} lexical=${paraphrase.best?.lexical} " +
                "engrama=${paraphrase.best?.engramSimilarity} fato=${paraphrase.best?.fact?.question} " +
                "alternativas=${paraphrase.alternatives.map { it.fact.question to it.score }}",
        )
        assertEquals("O que e a fotossintese?", paraphrase.best?.fact?.question)

        val unknown = engine.recall("Qual e a capital da Australia?", facts)
        println("desconhecido: score=${unknown.best?.score} alternativas=${unknown.alternatives.map { it.score }}")
        assertNull("inventou resposta", unknown.best)
    }

    private fun fact(question: String, outcome: TrainingOutcome): Fact = Fact(
        id = 1,
        topic = "Teste",
        question = question,
        answer = "Resposta de teste",
        strength = max(0.45f, outcome.stability),
        stability = outcome.stability,
        trials = 5,
        spikes = outcome.spikes,
        daPulses = outcome.daPulses,
        assembly = outcome.assembly,
        engram = outcome.engram,
        createdAt = System.currentTimeMillis(),
    )
}
