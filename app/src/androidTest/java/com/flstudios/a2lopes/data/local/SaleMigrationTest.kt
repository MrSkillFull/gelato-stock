package com.flstudios.a2lopes.data.local

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.flstudios.a2lopes.data.repository.RoomSaleRepository
import com.flstudios.a2lopes.domain.model.*
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class SaleMigrationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val name = "sale-migration-${UUID.randomUUID()}.db"
    private var database: AppDatabase? = null
    @After fun tearDown() { database?.close(); context.deleteDatabase(name) }

    private fun createVersionThree(withData: Boolean, brokenReference: Boolean = false) {
        val schema = InstrumentationRegistry.getInstrumentation().context.assets.open("com.flstudios.a2lopes.data.local.AppDatabase/3.json")
            .bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
        context.getDatabasePath(name).parentFile!!.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).use { db ->
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                val table = entity.getString("tableName")
                db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indices = entity.optJSONArray("indices")
                if (indices != null) for (j in 0 until indices.length()) {
                    db.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
                }
            }
            val setup = schema.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) db.execSQL(setup.getString(i))
            if (withData) {
                db.execSQL("INSERT INTO flavors VALUES (11, 'Coco', 550, 1, 'coco')")
                db.execSQL("INSERT INTO flavors VALUES (12, 'Uva', NULL, 0, 'uva')")
                db.execSQL("INSERT INTO production_sessions VALUES ('legacy', 100, 200, 'Original')")
                db.execSQL("INSERT INTO production_items VALUES (41, 'legacy', 11, 5)")
                val flavor = if (brokenReference) 999 else 11
                db.execSQL("INSERT INTO stock_movements VALUES (77, $flavor, 'PRODUCTION', 5, 'PRODUCTION_ITEM', 41, 200, 'Original', 41)")
            }
            db.version = 3
        }
    }

    private fun open() = Room.databaseBuilder(context, AppDatabase::class.java, name)
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6).build().also { database = it }

    @Test fun migrationPreservesFlavorProductionAndMovementFieldsAndSupportsSale() = runBlocking {
        createVersionThree(true)
        val db = open()
        assertEquals(6, db.openHelper.writableDatabase.version)
        val coco = db.flavorDao().findById(11)!!
        assertEquals("Coco", coco.name)
        assertEquals("coco", coco.normalizedName)
        assertEquals(550L, coco.defaultPriceCents)
        assertTrue(coco.active)
        assertFalse(db.flavorDao().findById(12)!!.active)
        assertNull(db.flavorDao().findById(12)!!.defaultPriceCents)
        val session = db.productionDao().findSession("legacy")!!
        assertEquals(100L, session.producedAt)
        assertEquals(200L, session.createdAt)
        assertEquals("Original", session.notes)
        val item = db.productionDao().itemsForSession("legacy").single()
        assertEquals(41L, item.id)
        assertEquals(5L, item.quantity)
        val movement = db.productionDao().movementsForSession("legacy").single()
        assertEquals(77L, movement.id)
        assertEquals(11L, movement.flavorId)
        assertEquals(41L, movement.productionItemId)
        assertEquals(41L, movement.referenceId)
        assertEquals("PRODUCTION_ITEM", movement.referenceType)
        assertEquals("PRODUCTION", movement.type)
        assertEquals(5L, movement.quantityDelta)
        assertEquals(200L, movement.createdAt)
        assertEquals("Original", movement.notes)
        assertNull(movement.saleItemId)
        assertEquals(5L, db.stockDao().quantity(11))
        val request = SaleRequest("new-sale", 300, listOf(SaleQuantity(11, 2, 550)))
        val sale = RoomSaleRepository(db) { 400 }.record(request)
        assertEquals(3L, db.stockDao().quantity(11))
        assertTrue(db.saleDao().movementsForSale(sale.id).single().id > 77)
        assertEquals(movement, db.productionDao().movementsForSession("legacy").single())
        db.openHelper.readableDatabase.query("PRAGMA foreign_key_check").use { assertEquals(0, it.count) }
        db.close()
        val reopened = open()
        assertEquals(3L, reopened.stockDao().quantity(11))
        assertEquals(sale, RoomSaleRepository(reopened).record(request))
    }

    @Test fun migratesEmptyVersionThreeDatabase() = runBlocking {
        createVersionThree(false)
        val db = open()
        assertEquals(6, db.openHelper.writableDatabase.version)
        assertNull(db.saleDao().findSale("none"))
    }

    @Test fun failedMigrationKeepsOriginalSchemaAndHistory() {
        createVersionThree(true, brokenReference = true)
        try { open().openHelper.writableDatabase; fail("Referência inválida deveria abortar") } catch (_: Exception) { }
        database!!.close()
        SQLiteDatabase.openDatabase(context.getDatabasePath(name).path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            assertEquals(3, db.version)
            db.rawQuery("SELECT id, flavor_id, production_item_id FROM stock_movements", null).use {
                assertTrue(it.moveToFirst()); assertEquals(77L, it.getLong(0)); assertEquals(999L, it.getLong(1)); assertEquals(41L, it.getLong(2))
            }
            db.rawQuery("SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'sales'", null).use { assertEquals(0, it.count) }
        }
    }
}
