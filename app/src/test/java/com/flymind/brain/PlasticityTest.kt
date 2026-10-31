package com.flymind.brain

import com.flymind.data.Fact
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class PlasticityTest {

    private lateinit var dir: File

    @Before
    fun setUp() {
        dir = Files.createTempDirectory("flymind-plasticity").toFile()
    }

    private fun engine(): LearningEngine {
        val connectome = File("src/main/assets/connectome/flywire_mb.bin").inputStream()
            .use { Connectome.fromStream(it) }
        return LearningEngine(connectome, dir)
    }

    @Test
    fun aprendizadoFortaleceSinapsesESobraExperiencia() {
        val engine = engine()
        val before = engine.status()
        assertEquals(0, before.learnings)
        assertEquals(0f, before.expertise, 0.0001f)
        assertEquals(DEFAULT_TRIALS, before.trialsNow)

        val outcome = engine.train("O que e a fotossintese?", DEFAULT_TRIALS)
        println(
            "treino: ensaios=${outcome.trialsUsed} fortalecidas=${outcome.strengthened} " +
                "expertise=${outcome.expertise} plasticidade=${outcome.weightDelta}",
        )
        assertEquals(DEFAULT_TRIALS, outcome.trialsUsed)
        assertTrue("nao fortaleceu o caminho da memoria", outcome.strengthened > 0)
        assertTrue("experiencia nao acumulou", outcome.expertise > 0f)

        val after = engine.status()
        assertEquals(1, after.learnings)
        assertTrue("nenhuma sinapse plastica", after.plasticSynapses > 0)
        assertTrue("nenhuma sinapse acima do peso original", after.strengthened > 0)
        assertTrue("pesos nao mudaram", after.weightDelta > 0.001f)
        assertTrue("estado nao persistido", File(dir, "plasticity.txt").exists())
    }

    @Test
    fun comOtempoAprendeComMenosEnsaios() {
        val engine = engine()
        var lastTrials = DEFAULT_TRIALS
        repeat(6) { index ->
            lastTrials = engine.train("Fato numero $index sobre ciencia", DEFAULT_TRIALS).trialsUsed
        }
        val status = engine.status()
        println("depois de 6 fatos: expertise=${status.expertise} ensaios=$lastTrials")
        assertTrue("expertise nao cresceu", status.expertise > 0.2f)
        assertTrue("ensaios nao diminuiram: $lastTrials", lastTrials < DEFAULT_TRIALS)
        assertTrue("criterio nao relaxou", engine.plasticityState.thresholdRelax > 0f)
        assertTrue("teto de peso nao subiu", engine.plasticityState.weightCap > Plasticity.BASE_CAP)
    }

    @Test
    fun facilidadeSobreviveAoFecharOApp() {
        val first = engine()
        first.train("Primeira memoria", 3)
        first.train("Segunda memoria", 3)
        first.recall(
            "Primeira memoria",
            listOf(
                Fact(
                    id = 1,
                    topic = "Teste",
                    question = "Primeira memoria",
                    answer = "Resposta",
                    strength = 0.9f,
                    stability = 0.9f,
                    trials = 3,
                    spikes = 10,
                    daPulses = 9,
                    assembly = 10,
                    engram = FloatArray(Connectome.READOUT_SIZE) { 0.1f },
                    createdAt = System.currentTimeMillis(),
                ),
            ),
        )

        val reopened = engine()
        assertEquals(2, reopened.plasticityState.learnings)
        assertEquals(1, reopened.plasticityState.recalls)
        assertTrue(reopened.plasticityState.expertise > 0f)
        assertEquals(2, reopened.status().learnings)
    }

    @Test
    fun resetZeraPesosEFacilidade() {
        val engine = engine()
        engine.train("Memoria", 3)
        engine.resetPlasticity()
        val status = engine.status()
        assertEquals(0, status.learnings)
        assertEquals(0, status.recalls)
        assertEquals(0, status.plasticSynapses)
        assertEquals(0f, status.expertise, 0.0001f)
        assertEquals(DEFAULT_TRIALS, status.trialsNow)
    }
}
