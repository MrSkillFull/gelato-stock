package com.flstudios.a2lopes.data.local

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.flstudios.a2lopes.data.repository.RoomCorrectionRepository
import com.flstudios.a2lopes.domain.model.*
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class CorrectionMigrationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val name = "correction-migration-${UUID.randomUUID()}.db"
    private var database: AppDatabase? = null
    @After fun tearDown() { database?.close(); context.deleteDatabase(name) }

    private fun createVersionFour(withData: Boolean, broken: Boolean = false) {
        val schema = InstrumentationRegistry.getInstrumentation().context.assets
            .open("com.flstudios.a2lopes.data.local.AppDatabase/4.json").bufferedReader()
            .use { JSONObject(it.readText()).getJSONObject("database") }
        context.getDatabasePath(name).parentFile!!.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).use { db ->
            val entities = schema.getJSONArray("entities")
            for (index in 0 until entities.length()) {
                val entity = entities.getJSONObject(index)
                val table = entity.getString("tableName")
                db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indices = entity.optJSONArray("indices")
                if (indices != null) for (i in 0 until indices.length()) db.execSQL(indices.getJSONObject(i).getString("createSql").replace("\${TABLE_NAME}", table))
            }
            val setup = schema.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) db.execSQL(setup.getString(i))
            if (withData) {
                db.execSQL("INSERT INTO flavors VALUES (11, 'Coco', 550, 1, 'coco')")
                db.execSQL("INSERT INTO flavors VALUES (12, 'Uva', NULL, 0, 'uva')")
                db.execSQL("INSERT INTO production_sessions VALUES ('legacy', 100, 200, 'Original')")
                db.execSQL("INSERT INTO production_items VALUES (41, 'legacy', 11, 5)")
                db.execSQL("INSERT INTO sales VALUES ('old-sale', 300, 400)")
                db.execSQL("INSERT INTO sale_items VALUES (51, 'old-sale', 11, 2, 550)")
                val flavorId = if (broken) 999 else 11
                db.execSQL("INSERT INTO stock_movements VALUES (77, $flavorId, 'PRODUCTION', 5, 'PRODUCTION_ITEM', 41, 200, 'Original', 41, NULL)")
                db.execSQL("INSERT INTO stock_movements VALUES (78, 11, 'SALE', -2, 'SALE_ITEM', 51, 400, NULL, NULL, 51)")
            }
            db.version = 4
        }
    }

    private fun open() = Room.databaseBuilder(context, AppDatabase::class.java, name)
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6).build().also { database = it }

    @Test fun migrationPreservesAllLegacyRecordsAndAllowsReversalsAndReopening() = runBlocking {
        createVersionFour(true)
        val db = open()
        assertEquals(6, db.openHelper.writableDatabase.version) // Room validates exported v6 schema.
        assertEquals(3L, db.stockDao().quantity(11))
        assertEquals("coco", db.flavorDao().findById(11)!!.normalizedName)
        assertFalse(db.flavorDao().findById(12)!!.active)
        assertNull(db.flavorDao().findById(12)!!.defaultPriceCents)
        val production = db.productionDao().findSession("legacy")!!
        assertEquals(100L, production.producedAt)
        assertEquals(200L, production.createdAt)
        assertEquals("Original", production.notes)
        val productionItem = db.productionDao().itemsForSession("legacy").single()
        assertEquals(41L, productionItem.id)
        assertEquals(5L, productionItem.quantity)
        val sale = db.saleDao().findSale("old-sale")!!
        assertEquals(300L, sale.soldAt)
        assertEquals(400L, sale.createdAt)
        val saleItem = db.saleDao().itemsForSale("old-sale").single()
        assertEquals(51L, saleItem.id)
        assertEquals(550L, saleItem.unitPriceCents)
        val originalMovements = db.productionDao().movementsForSession("legacy") + db.saleDao().movementsForSale("old-sale")
        assertEquals(listOf(77L, 78L), originalMovements.map { it.id })
        assertEquals(listOf(5L, -2L), originalMovements.map { it.quantityDelta })
        assertTrue(originalMovements.all { it.correctionItemId == null })
        val repository = RoomCorrectionRepository(db) { 500 }
        val request = CorrectionRequest("reverse-sale", CorrectionType.SALE_REVERSAL, "Erro", sourceId = "old-sale")
        val receipt = repository.record(request)
        assertEquals(5L, db.stockDao().quantity(11))
        repository.record(CorrectionRequest("reverse-production", CorrectionType.PRODUCTION_REVERSAL, "Erro", sourceId = "legacy"))
        assertEquals(0L, db.stockDao().quantity(11))
        assertEquals(production, db.productionDao().findSession("legacy"))
        assertEquals(saleItem, db.saleDao().itemsForSale("old-sale").single())
        assertEquals(originalMovements, db.productionDao().movementsForSession("legacy") + db.saleDao().movementsForSale("old-sale"))
        db.openHelper.readableDatabase.query("SELECT MIN(id) FROM stock_movements WHERE correction_item_id IS NOT NULL").use {
            it.moveToFirst(); assertTrue(it.getLong(0) > 78)
        }
        db.openHelper.readableDatabase.query("PRAGMA foreign_key_check").use { assertEquals(0, it.count) }
        db.close()
        val reopened = open()
        assertEquals(receipt, RoomCorrectionRepository(reopened).record(request))
        assertEquals(0L, reopened.stockDao().quantity(11))
    }

    @Test fun migratesEmptyDatabase() = runBlocking {
        createVersionFour(false)
        val db = open()
        assertEquals(6, db.openHelper.writableDatabase.version)
        assertNull(db.correctionDao().find("none"))
    }

    @Test fun invalidLegacyReferenceAbortsWithoutPartialReplacement() {
        createVersionFour(true, broken = true)
        try { open().openHelper.writableDatabase; fail("Deveria rejeitar vínculo inválido") } catch (_: Exception) { }
        database!!.close()
        SQLiteDatabase.openDatabase(context.getDatabasePath(name).path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            assertEquals(4, db.version)
            db.rawQuery("SELECT flavor_id, production_item_id FROM stock_movements WHERE id = 77", null).use {
                assertTrue(it.moveToFirst()); assertEquals(999L, it.getLong(0)); assertEquals(41L, it.getLong(1))
            }
            db.rawQuery("SELECT COUNT(*) FROM stock_movements", null).use { it.moveToFirst(); assertEquals(2, it.getInt(0)) }
            db.rawQuery("SELECT name FROM sqlite_master WHERE name IN ('corrections','correction_items','stock_movements_new')", null).use {
                assertEquals(0, it.count)
            }
        }
    }
}
