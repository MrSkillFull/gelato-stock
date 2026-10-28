package com.flstudios.a2lopes.data.local

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.flstudios.a2lopes.data.repository.RoomProductionRepository
import com.flstudios.a2lopes.domain.model.ProductionQuantity
import com.flstudios.a2lopes.domain.model.ProductionRequest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ProductionMigrationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val name = "production-migration-${UUID.randomUUID()}.db"
    private var database: AppDatabase? = null

    @After fun tearDown() { database?.close(); context.deleteDatabase(name) }

    private fun createVersionTwo(withFlavors: Boolean) {
        val schema = InstrumentationRegistry.getInstrumentation().context.assets
            .open("com.flstudios.a2lopes.data.local.AppDatabase/2.json").bufferedReader().use {
                JSONObject(it.readText()).getJSONObject("database")
            }
        context.getDatabasePath(name).parentFile!!.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).use { db ->
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", entity.getString("tableName")))
                val indices = entity.getJSONArray("indices")
                for (j in 0 until indices.length()) db.execSQL(indices.getJSONObject(j).getString("createSql")
                    .replace("\${TABLE_NAME}", entity.getString("tableName")))
            }
            val setup = schema.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) db.execSQL(setup.getString(i))
            if (withFlavors) {
                db.execSQL("INSERT INTO flavors VALUES (11, 'Coco', 550, 1, 'coco')")
                db.execSQL("INSERT INTO flavors VALUES (12, 'Uva', NULL, 0, 'uva')")
            }
            db.version = 2
        }
    }

    private fun open(): AppDatabase = Room.databaseBuilder(context, AppDatabase::class.java, name)
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6).build().also { database = it }

    @Test fun migratesVersionTwoPreservingAllFlavorFieldsAndEnablesProduction() = runBlocking {
        createVersionTwo(true)
        val db = open()
        assertEquals(6, db.openHelper.writableDatabase.version) // Room valida o schema migrado.
        val coco = db.flavorDao().findById(11)!!
        assertEquals("Coco", coco.name)
        assertEquals("coco", coco.normalizedName)
        assertEquals(550L, coco.defaultPriceCents)
        assertTrue(coco.active)
        val uva = db.flavorDao().findById(12)!!
        assertEquals("Uva", uva.name)
        assertEquals("uva", uva.normalizedName)
        assertNull(uva.defaultPriceCents)
        assertFalse(uva.active)
        assertEquals(0L, db.stockDao().quantity(11))
        RoomProductionRepository(db) { 300 }.record(ProductionRequest("migrated", 100, listOf(ProductionQuantity(11, 5))))
        assertEquals(5L, db.stockDao().quantity(11))
        db.close()
        val reopened = open()
        assertEquals(2, reopened.flavorDao().observeAll().first().size)
        assertEquals(5L, reopened.stockDao().quantity(11))
        reopened.openHelper.readableDatabase.query("PRAGMA foreign_key_check").use { assertEquals(0, it.count) }
        reopened.openHelper.readableDatabase.query("PRAGMA foreign_keys").use { it.moveToFirst(); assertEquals(1, it.getInt(0)) }
    }

    @Test fun migratesEmptyVersionTwoDatabase() = runBlocking {
        createVersionTwo(false)
        assertTrue(open().stockDao().observeAll().first().isEmpty())
        assertEquals(6, database!!.openHelper.readableDatabase.version)
    }
}
