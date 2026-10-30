package com.flymind.brain

import com.flymind.data.Fact
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class DivergenceTest {

    @Test
    fun extraiTrechosDeHtmlSemRuido() {
        val html = """
            <html><head><title>Página</title><style>.x{color:red}</style>
            <script>alert(1)</script></head><body>
            <h1>Fotossíntese</h1>
            <p>A fotossíntese converte luz em energia química e produz glicose e oxigênio para a planta.</p>
            <h2>Respiração celular</h2>
            <p>A respiração celular quebra a glicose para liberar energia usada pelas células do corpo.</p>
            </body></html>
        """.trimIndent()

        val sections = WebSource.parseHtml(html)
        assertEquals(2, sections.size)
        assertEquals("Fotossíntese", sections[0].title)
        assertTrue(sections[0].body.contains("energia química"))
        assertFalse(sections[0].body.contains("alert"))
        assertFalse(sections[0].body.contains("color"))
        assertEquals("Respiração celular", sections[1].title)
    }

    @Test
    fun decodificaEntidadesENormalizaUrl() {
        assertEquals("a & b á", WebSource.decodeEntities("a &amp; b &aacute;"))
        assertEquals("https://pt.exemplo.org/x", WebSource.normalize("pt.exemplo.org/x"))
        val rejected = runCatching { WebSource.normalize("ftp://exemplo.org/arquivo") }
        assertTrue("recusou ftp", rejected.isFailure)
    }

    @Test
    fun conectaIdeiasDivergentesENaoInventa() {
        val fotossintese = fact(
            id = 1,
            topic = "Biologia",
            question = "O que é fotossíntese?",
            answer = "A fotossíntese usa luz, água e gás carbônico para produzir glicose e oxigênio na planta.",
        )
        val clorofila = fact(
            id = 2,
            topic = "Biologia",
            question = "O que é clorofila?",
            answer = "A clorofila é o pigmento verde da planta que capta a luz usada na fotossíntese.",
        )
        val futebol = fact(
            id = 3,
            topic = "Esporte",
            question = "Quem inventou o futebol?",
            answer = "As regras modernas do futebol foram criadas na Inglaterra no século XIX.",
        )

        val ideas = Concepts.divergent(
            query = "fotossíntese e planta",
            facts = listOf(fotossintese, clorofila, futebol),
            excludeId = fotossintese.id,
        )

        assertTrue("não conectou nenhuma ideia", ideas.isNotEmpty())
        assertEquals("não trouxe a ideia vizinha", clorofila.id, ideas.first().fact.id)
        assertTrue("via vazia", ideas.first().via.isNotBlank())
        assertFalse("inventou ligação com futebol", ideas.any { it.fact.id == futebol.id })
    }

    @Test
    fun ligaFatosPorConceitosCompartilhados() {
        val fotossintese = fact(
            id = 1,
            topic = "Biologia",
            question = "O que é fotossíntese?",
            answer = "A fotossíntese usa luz e água para produzir alimento na planta.",
        )
        val clorofila = fact(
            id = 2,
            topic = "Biologia",
            question = "O que é clorofila?",
            answer = "A clorofila capta a luz do sol na planta e alimenta a fotossíntese.",
        )
        val futebol = fact(
            id = 3,
            topic = "Esporte",
            question = "Quem inventou o futebol?",
            answer = "As regras do futebol moderno nasceram na Inglaterra no século XIX.",
        )

        val links = Concepts.linksTo(fotossintese, listOf(clorofila, futebol))
        assertEquals(1, links.size)
        assertEquals(clorofila.id, links.first().first.id)
        assertTrue(links.first().second.size >= 2)
    }

    @Test
    fun recallDevolveTambemAsConexoesDivergentes() {
        val connectome = File("src/main/assets/connectome/flywire_mb.bin").inputStream()
            .use { Connectome.fromStream(it) }
        val dir = Files.createTempDirectory("flymind-div").toFile()
        val engine = LearningEngine(connectome, dir)

        val fotossintese = fact(
            id = 1,
            topic = "Biologia",
            question = "O que é fotossíntese?",
            answer = "A fotossíntese usa luz, água e gás carbônico para produzir glicose na planta.",
        )
        val clorofila = fact(
            id = 2,
            topic = "Biologia",
            question = "O que é clorofila?",
            answer = "A clorofila é o pigmento verde da planta que capta a luz usada na fotossíntese.",
        )
        val futebol = fact(
            id = 3,
            topic = "Esporte",
            question = "Quem inventou o futebol?",
            answer = "As regras modernas do futebol foram criadas na Inglaterra no século XIX.",
        )

        val outcome = engine.recall(
            "como a planta usa luz na fotossíntese",
            listOf(fotossintese, clorofila, futebol),
        )

        assertTrue("sem conexões divergentes", outcome.divergent.isNotEmpty())
        assertTrue(
            "conexão sem a ideia que a liga",
            outcome.divergent.all { it.via.isNotBlank() },
        )
        if (outcome.best != null) {
            assertFalse(
                "repetiu a própria resposta como conexão",
                outcome.divergent.any { it.fact.id == outcome.best?.fact?.id },
            )
        }
        assertTrue(
            "a resposta direta veio da memória certa",
            outcome.best == null || outcome.best.fact.id != futebol.id ||
                outcome.best.score >= LearningEngine.ACCEPT_THRESHOLD,
        )
    }

    private fun fact(id: Long, topic: String, question: String, answer: String): Fact = Fact(
        id = id,
        topic = topic,
        question = question,
        answer = answer,
        strength = 0.9f,
        stability = 0.95f,
        trials = 5,
        spikes = 1000,
        daPulses = 15,
        assembly = 120,
        engram = FloatArray(Connectome.READOUT_SIZE) { ((it * 37 + id) % 17) / 17f },
        createdAt = System.currentTimeMillis(),
    )
}
