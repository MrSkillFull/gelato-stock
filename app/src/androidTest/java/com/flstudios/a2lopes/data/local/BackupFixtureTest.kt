package com.flstudios.a2lopes.data.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.flstudios.a2lopes.data.backup.BackupJson
import com.flstudios.a2lopes.data.backup.RoomBackupRepository
import com.flstudios.a2lopes.data.local.entity.FlavorEntity
import com.flstudios.a2lopes.data.repository.RoomSettingsRepository
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream

/** Fixed synthetic documents, deliberately independent of the current JSON writer. */
@RunWith(AndroidJUnit4::class)
class BackupFixtureTest {
    @Test fun legacyFormatOneSchemaFiveRestoresWithDefaultSetting() = verifyFixture("format1-schema5.json", 10)

    @Test fun formatTwoSchemaSixRestoresItsSetting() = verifyFixture("format2-schema6.json", 35)

    private fun verifyFixture(file: String, expectedThreshold: Long) = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val document = instrumentation.context.assets.open("backup-fixtures/$file").use(BackupJson::read)
        assertEquals(1700000003000L, document.createdAt)
        assertEquals("11111111-2222-4333-8444-555555555555", document.datasetId)
        assertEquals(expectedThreshold, document.lowStockThreshold)
        val db = Room.inMemoryDatabaseBuilder(instrumentation.targetContext, AppDatabase::class.java).build()
        try {
            db.flavorDao().insert(FlavorEntity(99, "Anterior"))
            RoomSettingsRepository(db).setLowStockThreshold(77)
            val repository = RoomBackupRepository(db)
            repository.restore(document)
            val restored = repository.snapshot()
            assertEquals(document.copy(createdAt = restored.createdAt, datasetId = restored.datasetId), restored)
            assertNull(db.flavorDao().findById(99))
            assertEquals(6L, db.stockDao().quantity(17))
            assertEquals(550L, restored.sale_items.single().unitPriceCents)
            assertEquals(600L, restored.flavors.single().defaultPriceCents)
            assertFalse(restored.flavors.single().active)

            val bytes = ByteArrayOutputStream().also { BackupJson.write(restored, it) }.toByteArray()
            val envelope = JSONObject(String(bytes, Charsets.UTF_8))
            assertEquals("2lopes-backup", envelope.getString("format"))
            assertEquals(2, envelope.getInt("formatVersion"))
            assertEquals(6, envelope.getInt("schemaVersion"))
            assertEquals(restored, BackupJson.read(bytes.inputStream()))
        } finally {
            db.close()
        }
    }
}
