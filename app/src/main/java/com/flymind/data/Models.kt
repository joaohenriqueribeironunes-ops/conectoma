package com.flymind.data

data class Fact(
    val id: Long = 0,
    val topic: String,
    val question: String,
    val answer: String,
    val strength: Float,
    val stability: Float,
    val trials: Int,
    val spikes: Int,
    val daPulses: Int,
    val assembly: Int,
    val engram: FloatArray,
    val createdAt: Long,
    val lastRecalledAt: Long = 0L,
    val recallCount: Int = 0,
)

data class LearningEvent(
    val id: Long = 0,
    val kind: String,
    val topic: String,
    val title: String,
    val detail: String,
    val strength: Float,
    val spikes: Int,
    val daPulses: Int,
    val createdAt: Long,
)

object EventKind {
    const val LEARN = "learn"
    const val RECALL = "recall"
    const val MISS = "miss"
    const val REPLAY = "replay"
    const val FORGET = "forget"
    const val RESET = "reset"
    const val LINK = "link"
}
