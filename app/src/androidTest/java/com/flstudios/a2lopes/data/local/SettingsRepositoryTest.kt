package com.flstudios.a2lopes.data.local

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.flstudios.a2lopes.data.backup.*
import com.flstudios.a2lopes.data.local.entity.FlavorEntity
import com.flstudios.a2lopes.data.repository.RoomSettingsRepository
import com.flstudios.a2lopes.domain.model.BackupInvalid
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class SettingsRepositoryTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val databases = mutableListOf<AppDatabase>()
    private val diskNames = mutableListOf<String>()
    private fun memory() = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build().also { databases += it }
    private fun disk(name: String) = Room.databaseBuilder(context, AppDatabase::class.java, name).addMigrations(
        MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6).build().also { databases += it }
    @After fun close() { databases.forEach { it.close() }; diskNames.forEach { context.deleteDatabase(it) } }
    private fun encode(data: BackupSnapshot) = ByteArrayOutputStream().also { BackupJson.write(data, it) }.toString("UTF-8")

    @Test fun defaultValidationAndPersistenceAfterReopen() = runBlocking {
        val name = "settings-${UUID.randomUUID()}.db".also { diskNames += it }
        var db = disk(name)
        var repository = RoomSettingsRepository(db)
        assertEquals(10L, repository.observeLowStockThreshold().first())
        repository.setLowStockThreshold(0)
        try { repository.setLowStockThreshold(-1); fail("Deveria rejeitar") } catch (_: IllegalArgumentException) { }
        assertEquals(0L, repository.observeLowStockThreshold().first())
        repository.setLowStockThreshold(27)
        db.close()
        db = disk(name); repository = RoomSettingsRepository(db)
        assertEquals(27L, repository.observeLowStockThreshold().first())
    }

    @Test fun versionFiveMigratesWithoutLosingExistingDataAndAddsDefault() = runBlocking {
        val name = "settings-migration-${UUID.randomUUID()}.db".also { diskNames += it }
        val schema = InstrumentationRegistry.getInstrumentation().context.assets
            .open("com.flstudios.a2lopes.data.local.AppDatabase/5.json").bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
        context.getDatabasePath(name).parentFile!!.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).use { db ->
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i); val table = entity.getString("tableName")
                db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indices = entity.optJSONArray("indices")
                if (indices != null) for (j in 0 until indices.length()) db.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
            }
            val setup = schema.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) db.execSQL(setup.getString(i))
            db.execSQL("INSERT INTO flavors VALUES (17, 'Coco', 550, 1, 'coco')")
            db.execSQL("INSERT INTO production_sessions VALUES ('old-key', 100, 200, 'Preservado')")
            db.execSQL("INSERT INTO production_items VALUES (21, 'old-key', 17, 8)")
            db.execSQL("INSERT INTO stock_movements VALUES (31, 17, 'PRODUCTION', 8, 'PRODUCTION_ITEM', 21, 200, 'Preservado', 21, NULL, NULL)")
            db.version = 5
        }
        val db = disk(name)
        assertEquals(6, db.openHelper.writableDatabase.version)
        assertEquals(10L, RoomSettingsRepository(db).observeLowStockThreshold().first())
        assertEquals(8L, db.stockDao().quantity(17))
        assertEquals("Preservado", db.productionDao().findSession("old-key")!!.notes)
        assertEquals(21L, db.productionDao().itemsForSession("old-key").single().id)
        db.openHelper.readableDatabase.query("PRAGMA foreign_key_check").use { assertEquals(0, it.count) }
    }

    @Test fun newBackupPreservesLimitAndLegacyBackupReplacesItWithDefault() = runBlocking {
        val source = memory(); val target = memory()
        source.flavorDao().insert(FlavorEntity(9, "Novo"))
        RoomSettingsRepository(source).setLowStockThreshold(35)
        RoomSettingsRepository(target).setLowStockThreshold(77)
        val json = encode(RoomBackupRepository(source).snapshot())
        assertEquals(2, JSONObject(json).getInt("formatVersion"))
        val imported = RoomBackupRepository(target).inspect { json.byteInputStream() }
        RoomBackupRepository(target).restore(imported)
        assertEquals(35L, RoomSettingsRepository(target).observeLowStockThreshold().first())
        assertEquals("Novo", target.flavorDao().findById(9)!!.name)
        val legacy = JSONObject(json).put("formatVersion", 1).put("schemaVersion", 5).put("settings", JSONObject()).toString()
        RoomBackupRepository(target).restore(RoomBackupRepository(target).inspect { legacy.byteInputStream() })
        assertEquals(10L, RoomSettingsRepository(target).observeLowStockThreshold().first())
    }

    @Test fun invalidSettingsAreRejectedBeforeAnyReplacement() = runBlocking<Unit> {
        val db = memory()
        db.flavorDao().insert(FlavorEntity(1, "Original"))
        RoomSettingsRepository(db).setLowStockThreshold(12)
        val json = encode(RoomBackupRepository(db).snapshot())
        for (settings in listOf("{}", "{\"lowStockThreshold\":-1}", "{\"lowStockThreshold\":1.5}",
            "{\"lowStockThreshold\":null}", "{\"lowStockThreshold\":true}", "{\"lowStockThreshold\":\"10\"}",
            "{\"lowStockThreshold\":9223372036854775808}", "{\"unknown\":1}", "{\"lowStockThreshold\":1,\"lowStockThreshold\":2}")) {
            val invalid = json.replace("{\"lowStockThreshold\":12}", settings)
            assertThrows(BackupInvalid::class.java) { BackupJson.read(invalid.byteInputStream()) }
        }
        assertEquals(12L, RoomSettingsRepository(db).observeLowStockThreshold().first())
        assertNotNull(db.flavorDao().findById(1))
        val negative = RoomBackupRepository(db).snapshot().copy(lowStockThreshold = -1)
        assertThrows(BackupInvalid::class.java) { BackupJson.write(negative, ByteArrayOutputStream()) }
    }

    @Test fun settingsFailureRollsBackRestoredDatabaseAndOldSettingTogether() = runBlocking {
        val source = memory(); val target = memory()
        source.flavorDao().insert(FlavorEntity(9, "Novo"))
        target.flavorDao().insert(FlavorEntity(1, "Anterior"))
        RoomSettingsRepository(source).setLowStockThreshold(30)
        RoomSettingsRepository(target).setLowStockThreshold(77)
        val before = RoomBackupRepository(target).snapshot()
        val replacement = RoomBackupRepository(source).snapshot()
        target.openHelper.writableDatabase.execSQL("CREATE TRIGGER reject_settings BEFORE INSERT ON app_settings BEGIN SELECT RAISE(ABORT, 'settings failure'); END")
        try { RoomBackupRepository(target).restore(replacement); fail("Deveria falhar") } catch (_: Exception) { }
        val after = RoomBackupRepository(target).snapshot()
        assertEquals(before.copy(createdAt = after.createdAt, datasetId = after.datasetId), after)
        assertEquals(77L, RoomSettingsRepository(target).observeLowStockThreshold().first())
        assertNull(target.flavorDao().findById(9))
    }
}
