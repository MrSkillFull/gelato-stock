package com.flstudios.a2lopes.data.local

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.flstudios.a2lopes.domain.model.FlavorDataException
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FlavorMigrationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val databaseName = "migration-test-${UUID.randomUUID()}.db"
    private var database: AppDatabase? = null

    @After
    fun tearDown() {
        database?.close()
        context.deleteDatabase(databaseName)
    }

    private fun createVersionOne(vararg names: String) {
        val schema = InstrumentationRegistry.getInstrumentation().context.assets
            .open("com.flstudios.a2lopes.data.local.AppDatabase/1.json")
            .bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
        context.getDatabasePath(databaseName).parentFile!!.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(databaseName), null).use { db ->
            db.execSQL(schema.getJSONArray("entities").getJSONObject(0).getString("createSql")
                .replace("\${TABLE_NAME}", "flavors"))
            val queries = schema.getJSONArray("setupQueries")
            for (index in 0 until queries.length()) db.execSQL(queries.getString(index))
            names.forEachIndexed { index, name ->
                db.execSQL("INSERT INTO flavors (id, name, default_price_cents, active) VALUES (?, ?, ?, ?)",
                    arrayOf<Any?>(index + 11, name, if (index == 0) 550L else null, if (index == 0) 0 else 1))
            }
            db.version = 1
        }
    }

    private fun openRoom(): AppDatabase = Room.databaseBuilder(context, AppDatabase::class.java, databaseName)
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6).build().also { database = it }

    @Test
    fun migratesEmptyDatabase() = runBlocking {
        createVersionOne()
        assertTrue(openRoom().flavorDao().observeAll().first().isEmpty())
        assertEquals(6, database!!.openHelper.readableDatabase.version)
    }

    @Test
    fun migrationPreservesIdsPricesAndStatusAndAddsUniqueKey() = runBlocking {
        createVersionOne("  COCO   Cremoso ", "Maracujá")
        val dao = openRoom().flavorDao()
        val rows = dao.observeAll().first()
        assertEquals(2, rows.size)
        val coco = dao.findById(11)!!
        assertEquals("COCO Cremoso", coco.name)
        assertEquals("coco cremoso", coco.normalizedName)
        assertEquals(550L, coco.defaultPriceCents)
        assertFalse(coco.active)
        assertNull(dao.findById(12)!!.defaultPriceCents)
        assertTrue(dao.findById(12)!!.active)
        assertEquals(6, database!!.openHelper.readableDatabase.version)
        try {
            dao.insert(com.flstudios.a2lopes.data.local.entity.FlavorEntity(name = "coco cremoso"))
            fail("O índice único deveria existir após a migração")
        } catch (_: android.database.sqlite.SQLiteConstraintException) { }
        assertTrue(dao.insert(com.flstudios.a2lopes.data.local.entity.FlavorEntity(name = "Morango")) > 12)
    }

    @Test
    fun duplicateLegacyNamesAbortWithoutChangingOriginalDatabase() {
        assertMigrationPreservesOnFailure("Coco", " COCO ")
    }

    @Test
    fun emptyLegacyNameAbortsWithoutChangingOriginalDatabase() {
        assertMigrationPreservesOnFailure(" \t ")
    }

    private fun assertMigrationPreservesOnFailure(vararg names: String) {
        createVersionOne(*names)
        try {
            openRoom().openHelper.writableDatabase
            fail("A migração deveria interromper por dados conflitantes")
        } catch (error: Exception) {
            assertTrue(generateSequence<Throwable>(error) { it.cause }.any { it is FlavorDataException })
        }
        database!!.close()
        SQLiteDatabase.openDatabase(context.getDatabasePath(databaseName).path, null,
            SQLiteDatabase.OPEN_READONLY).use { db ->
            assertEquals(1, db.version)
            db.rawQuery("SELECT name FROM flavors ORDER BY id", null).use { cursor ->
                assertEquals(names.size, cursor.count)
                names.forEach { name -> assertTrue(cursor.moveToNext()); assertEquals(name, cursor.getString(0)) }
            }
            db.rawQuery("PRAGMA table_info(flavors)", null).use { cursor ->
                assertEquals(4, cursor.count)
            }
        }
    }
}
