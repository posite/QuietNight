package com.example.quietnight.data

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class QuietNightDatabaseTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val databaseName = "quiet_night_test_${UUID.randomUUID()}.db"
    private var database: QuietNightDatabase? = null

    @After
    fun tearDown() {
        database?.close()
        context.deleteDatabase(databaseName)
    }

    @Test
    fun migrationFrom4RemovesPositionAndPreservesSleepHistory() = runBlocking {
        createLegacyDatabase(4)
        val db = openDatabase()
        val session = db.snoreDao().getSessionByDate(1000L)!!
        assertEquals(7L, session.id)
        assertEquals(80, session.score)
        assertEquals(2000L, session.snoreTime)
        assertEquals(10000L, session.sleepTime)
        assertEquals("", session.timelineCsv)
        db.openHelper.readableDatabase.query("PRAGMA table_info(sleep_sessions)").use { cursor ->
            val columns = mutableListOf<String>()
            while (cursor.moveToNext()) columns.add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
            assertEquals(setOf("id", "date", "score", "snoreTime", "sleepTime", "timelineCsv",
                "observedMs", "missingMs", "loudMs", "ambientMs", "ambientLoudMs",
                "vibrationEventsCsv", "morningFeedback"), columns.toSet())
        }
        assertEquals(0, db.snoreDao().countClipsByDate(1000L))
    }

    @Test
    fun migrationFrom5PreservesTimelineAndSupportsClips() = runBlocking {
        createLegacyDatabase(5)
        val dao = openDatabase().snoreDao()
        assertEquals("10,80,20", dao.getSessionByDate(1000L)!!.timelineCsv)
        dao.insertClip(SnoreClip(date = 1000, timestamp = 2000, dbLevel = 80,
            filePath = "test.wav", durationMs = 6000))
        assertEquals(1, dao.countClipsByDate(1000))
        assertEquals(6000L, dao.getClipsByDate(1000).single().durationMs)
    }

    @Test
    fun sameDayUpdateKeepsOneRowAndHistoryIsNewestFirst() = runBlocking {
        val dao = openDatabase().snoreDao()
        dao.insertSession(SleepSession(date = 1000, score = 90, snoreTime = 1000, sleepTime = 10000))
        val first = dao.getSessionByDate(1000)!!
        dao.updateSession(first.copy(sleepTime = 20000, snoreTime = 2000))
        dao.insertSession(SleepSession(date = 2000, score = 80, snoreTime = 2000, sleepTime = 10000))
        val history = dao.getHistory().first()
        assertEquals(listOf(2000L, 1000L), history.map { it.date })
        assertEquals(first.id, history.last().id)
        assertEquals(20000L, history.last().sleepTime)
    }

    @Test
    fun migrationFrom6PreservesRecordsAndClipsWithoutInventingNewMeasurements() = runBlocking {
        createLegacyDatabase(6)
        context.openOrCreateDatabase(databaseName, Context.MODE_PRIVATE, null).use { db ->
            db.execSQL("INSERT INTO snore_clips VALUES (3, 1000, 1500, 80, 'keep.wav', 6000)")
        }
        val dao = openDatabase().snoreDao()
        val row = dao.getSessionByDate(1000)!!
        assertEquals(7L, row.id)
        assertEquals("10,80,20", row.timelineCsv)
        assertEquals(0L, row.observedMs)
        assertEquals("", row.morningFeedback)
        assertEquals("", row.vibrationEventsCsv)
        assertEquals("keep.wav", dao.getClipsByDate(1000).single().filePath)
    }

    @Test
    fun monitoringFlushPreservesFeedbackWrittenAfterItsSnapshot() = runBlocking {
        val dao = openDatabase().snoreDao()
        dao.saveMonitoringSession(SleepSession(date = 1000, score = 80, snoreTime = 1000,
            sleepTime = 10000, observedMs = 9000, vibrationEventsCsv = "v1;1500:80:?"))
        val stale = dao.getSessionByDate(1000)!!
        dao.updateFeedback(1000, "DISTURBED")
        dao.saveMonitoringSession(stale.copy(observedMs = 19000, sleepTime = 20000))
        val updated = dao.getSessionByDate(1000)!!
        assertEquals("DISTURBED", updated.morningFeedback)
        assertEquals(19000L, updated.observedMs)
        assertEquals("v1;1500:80:?", updated.vibrationEventsCsv)
        assertEquals(1, dao.getHistory().first().size)
    }

    private fun openDatabase(): QuietNightDatabase = Room.databaseBuilder(
        context, QuietNightDatabase::class.java, databaseName
    ).addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7).build().also { database = it }

    // v4 스키마는 저장소에 없어 당시 컬럼을 재현한다. v5는 내보낸 스키마를 사용한다.
    private fun createLegacyDatabase(version: Int) {
        context.openOrCreateDatabase(databaseName, Context.MODE_PRIVATE, null).use { db ->
            if (version == 4) {
                db.execSQL("CREATE TABLE sleep_sessions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "date INTEGER NOT NULL, score INTEGER NOT NULL, snoreTime INTEGER NOT NULL, " +
                    "sleepTime INTEGER NOT NULL, positionStatsJson TEXT NOT NULL)")
                db.execSQL("CREATE UNIQUE INDEX index_sleep_sessions_date ON sleep_sessions(date)")
            } else {
                val assets = InstrumentationRegistry.getInstrumentation().context.assets
                val schema = assets.open("com.example.quietnight.data.QuietNightDatabase/$version.json")
                    .bufferedReader().use { org.json.JSONObject(it.readText()) }
                val entities = schema.getJSONObject("database").getJSONArray("entities")
                for (index in 0 until entities.length()) {
                    val entity = entities.getJSONObject(index)
                    val table = entity.getString("tableName")
                    db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                    val indices = entity.getJSONArray("indices")
                    for (i in 0 until indices.length()) {
                        db.execSQL(indices.getJSONObject(i).getString("createSql").replace("\${TABLE_NAME}", table))
                    }
                }
            }
            val lastColumn = if (version == 4) "positionStatsJson" else "timelineCsv"
            val value = if (version == 4) "{\"left\":100}" else "10,80,20"
            db.execSQL("INSERT INTO sleep_sessions (id, date, score, snoreTime, sleepTime, $lastColumn) " +
                "VALUES (7, 1000, 80, 2000, 10000, ?)", arrayOf(value))
            db.version = version
        }
    }
}
