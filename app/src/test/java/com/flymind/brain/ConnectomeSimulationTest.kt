package com.flymind.brain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ConnectomeSimulationTest {

    private fun load(): Connectome {
        val file = File("src/main/assets/connectome/flywire_mb.bin")
        assertTrue("asset ausente: ${file.absolutePath}", file.exists())
        return file.inputStream().use { Connectome.fromStream(it) }
    }

    @Test
    fun carregaSubcircuitoReal() {
        val connectome = load()
        val stats = connectome.stats()
        println("conectoma: $stats")
        println("top tipos: ${stats.topLabels}")
        assertTrue(connectome.neurons > 5_000)
        assertTrue(connectome.edges > 30_000)
        assertTrue(connectome.dopamineIndices.size > 200)
        assertTrue(connectome.inputIndices.size > 500)
        assertEquals(512, connectome.readoutIndices.size)
    }

    @Test
    fun estimuloProduzDisparosEEngrama() {
        val connectome = load()
        val brain = SpikingBrain(connectome)
        brain.resetState()
        val pattern = TextCode.pattern("o que e fotossintese", connectome.inputIndices, 36, 7)
        assertTrue(pattern.isNotEmpty())
        brain.setInput(pattern)
        val during = brain.run(180)
        brain.clearInput()
        brain.rewardPulses(3)
        val after = brain.run(120)

        val spikes = during.spikes + after.spikes
        val engramNorm = kotlin.math.sqrt(after.engram.sumOf { (it * it).toDouble() }).toFloat()
        println(
            "estimulo: spikes=$spikes ativos=${after.activeNeurons} " +
                "pulsos=${after.pulses} engrama=$engramNorm peso=${after.weightDelta}",
        )
        assertTrue("o circuito não disparou", spikes > 50)
        assertTrue("engrama vazio", engramNorm > 0f)
        assertTrue("dopamina não alterou pesos", after.weightDelta > 0f)
    }

    @Test
    fun textosDiferentesGeramEngramasDiferentes() {
        val connectome = load()
        val brain = SpikingBrain(connectome)
        val a = TextCode.pattern("fotossintese clorofila planta", connectome.inputIndices, 36, 3)
        val b = TextCode.pattern("futebol bola campo", connectome.inputIndices, 36, 3)

        brain.resetState()
        brain.setInput(a)
        brain.run(160)
        val engramA = brain.engram()

        brain.resetState()
        brain.setInput(b)
        brain.run(160)
        val engramB = brain.engram()

        var dot = 0f
        for (i in engramA.indices) dot += engramA[i] * engramB[i]
        println("cosseno entre engramas distintos = $dot")
        assertNotEquals(0f, engramA.sum())
        assertTrue("engramas idênticos demais: $dot", dot < 0.98f)
    }
}
