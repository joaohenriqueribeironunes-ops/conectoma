package com.flymind.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Base de memória da mosca. Fatos aprendidos guardam o engrama (assinatura de
 * 512 dimensões do circuito) e o diário registra cada sessão de aprendizado.
 */
class FlyMindDatabase(context: Context) :
    SQLiteOpenHelper(context.applicationContext, "flymind.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE facts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                topic TEXT NOT NULL,
                question TEXT NOT NULL,
                answer TEXT NOT NULL,
                strength REAL NOT NULL,
                stability REAL NOT NULL,
                trials INTEGER NOT NULL,
                spikes INTEGER NOT NULL,
                da_pulses INTEGER NOT NULL,
                assembly INTEGER NOT NULL,
                engram BLOB NOT NULL,
                created_at INTEGER NOT NULL,
                last_recalled_at INTEGER NOT NULL DEFAULT 0,
                recall_count INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE events (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                kind TEXT NOT NULL,
                topic TEXT NOT NULL,
                title TEXT NOT NULL,
                detail TEXT NOT NULL,
                strength REAL NOT NULL,
                spikes INTEGER NOT NULL,
                da_pulses INTEGER NOT NULL,
                created_at INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX idx_facts_topic ON facts(topic)")
        db.execSQL("CREATE INDEX idx_facts_created ON facts(created_at)")
        db.execSQL("CREATE INDEX idx_events_created ON events(created_at)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS facts")
        db.execSQL("DROP TABLE IF EXISTS events")
        onCreate(db)
    }

    fun insertFact(fact: Fact): Long {
        val values = ContentValues().apply {
            put("topic", fact.topic)
            put("question", fact.question)
            put("answer", fact.answer)
            put("strength", fact.strength)
            put("stability", fact.stability)
            put("trials", fact.trials)
            put("spikes", fact.spikes)
            put("da_pulses", fact.daPulses)
            put("assembly", fact.assembly)
            put("engram", toBlob(fact.engram))
            put("created_at", fact.createdAt)
            put("last_recalled_at", fact.lastRecalledAt)
            put("recall_count", fact.recallCount)
        }
        return writableDatabase.insert("facts", null, values)
    }

    fun listFacts(): List<Fact> {
        val out = ArrayList<Fact>()
        readableDatabase.query(
            "facts", null, null, null, null, null, "created_at DESC",
        ).use { cursor ->
            while (cursor.moveToNext()) out.add(readFact(cursor))
        }
        return out
    }

    fun deleteFact(id: Long) {
        writableDatabase.delete("facts", "id = ?", arrayOf(id.toString()))
    }

    fun recordRecall(id: Long) {
        val now = System.currentTimeMillis()
        writableDatabase.execSQL(
            "UPDATE facts SET recall_count = recall_count + 1, last_recalled_at = ? WHERE id = ?",
            arrayOf(now, id),
        )
    }

    fun updateFactAfterReplay(id: Long, strength: Float, stability: Float, spikes: Int, daPulses: Int, assembly: Int, engram: FloatArray) {
        val values = ContentValues().apply {
            put("strength", strength)
            put("stability", stability)
            put("spikes", spikes)
            put("da_pulses", daPulses)
            put("assembly", assembly)
            put("engram", toBlob(engram))
        }
        writableDatabase.update("facts", values, "id = ?", arrayOf(id.toString()))
    }

    fun addEvent(event: LearningEvent): Long {
        val values = ContentValues().apply {
            put("kind", event.kind)
            put("topic", event.topic)
            put("title", event.title)
            put("detail", event.detail)
            put("strength", event.strength)
            put("spikes", event.spikes)
            put("da_pulses", event.daPulses)
            put("created_at", event.createdAt)
        }
        return writableDatabase.insert("events", null, values)
    }

    fun listEvents(limit: Int = 200): List<LearningEvent> {
        val out = ArrayList<LearningEvent>()
        readableDatabase.query(
            "events", null, null, null, null, null, "created_at DESC", limit.toString(),
        ).use { cursor ->
            while (cursor.moveToNext()) {
                out.add(
                    LearningEvent(
                        id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                        kind = cursor.getString(cursor.getColumnIndexOrThrow("kind")),
                        topic = cursor.getString(cursor.getColumnIndexOrThrow("topic")),
                        title = cursor.getString(cursor.getColumnIndexOrThrow("title")),
                        detail = cursor.getString(cursor.getColumnIndexOrThrow("detail")),
                        strength = cursor.getFloat(cursor.getColumnIndexOrThrow("strength")),
                        spikes = cursor.getInt(cursor.getColumnIndexOrThrow("spikes")),
                        daPulses = cursor.getInt(cursor.getColumnIndexOrThrow("da_pulses")),
                        createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at")),
                    ),
                )
            }
        }
        return out
    }

    fun clearAll() {
        writableDatabase.delete("facts", null, null)
        writableDatabase.delete("events", null, null)
    }

    fun countFacts(): Int {
        readableDatabase.rawQuery("SELECT COUNT(*) FROM facts", null).use { c ->
            return if (c.moveToFirst()) c.getInt(0) else 0
        }
    }

    private fun readFact(cursor: Cursor): Fact = Fact(
        id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
        topic = cursor.getString(cursor.getColumnIndexOrThrow("topic")),
        question = cursor.getString(cursor.getColumnIndexOrThrow("question")),
        answer = cursor.getString(cursor.getColumnIndexOrThrow("answer")),
        strength = cursor.getFloat(cursor.getColumnIndexOrThrow("strength")),
        stability = cursor.getFloat(cursor.getColumnIndexOrThrow("stability")),
        trials = cursor.getInt(cursor.getColumnIndexOrThrow("trials")),
        spikes = cursor.getInt(cursor.getColumnIndexOrThrow("spikes")),
        daPulses = cursor.getInt(cursor.getColumnIndexOrThrow("da_pulses")),
        assembly = cursor.getInt(cursor.getColumnIndexOrThrow("assembly")),
        engram = fromBlob(cursor.getBlob(cursor.getColumnIndexOrThrow("engram"))),
        createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at")),
        lastRecalledAt = cursor.getLong(cursor.getColumnIndexOrThrow("last_recalled_at")),
        recallCount = cursor.getInt(cursor.getColumnIndexOrThrow("recall_count")),
    )

    private fun toBlob(array: FloatArray): ByteArray {
        val buffer = ByteBuffer.allocate(array.size * Float.SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN)
        for (value in array) buffer.putFloat(value)
        return buffer.array()
    }

    private fun fromBlob(bytes: ByteArray?): FloatArray {
        if (bytes == null || bytes.size < Float.SIZE_BYTES) return FloatArray(0)
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val out = FloatArray(bytes.size / Float.SIZE_BYTES)
        for (i in out.indices) out[i] = buffer.float
        return out
    }
}
