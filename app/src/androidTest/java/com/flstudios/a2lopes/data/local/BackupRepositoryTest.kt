package com.flstudios.a2lopes.data.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.flstudios.a2lopes.data.backup.*
import com.flstudios.a2lopes.data.local.entity.*
import com.flstudios.a2lopes.data.repository.*
import com.flstudios.a2lopes.domain.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.*

@RunWith(AndroidJUnit4::class)
class BackupRepositoryTest {
    private lateinit var source: AppDatabase
    private lateinit var target: AppDatabase
    private fun database() = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, AppDatabase::class.java).build()
    @Before fun setup() { source = database(); target = database() }
    @After fun close() { source.close(); target.close() }
    private suspend fun populate() {
        source.flavorDao().insert(FlavorEntity(41, "Coco", 550))
        source.flavorDao().insert(FlavorEntity(42, "Morango"))
        source.flavorDao().insert(FlavorEntity(43, "Inativo", 0, false))
        val production = RoomProductionRepository(source) { 1000 }
        production.record(ProductionRequest("production-key", 500, listOf(ProductionQuantity(41, 20), ProductionQuantity(42, 10)), "Lote original"))
        production.record(ProductionRequest("reversed-production", 600, listOf(ProductionQuantity(41, 2)), null))
        RoomSaleRepository(source) { 2000 }.record(SaleRequest("sale-key", 2000, listOf(SaleQuantity(41, 3, 550), SaleQuantity(42, 2, null))))
        source.flavorDao().updateDetails(41, "Coco", "coco", 600)
        val corrections = RoomCorrectionRepository(source) { 3000 }
        corrections.record(CorrectionRequest("loss", CorrectionType.LOSS, "Embalagem", 41, 1))
        corrections.record(CorrectionRequest("up", CorrectionType.ADJUSTMENT, "Contagem", 42, 12))
        corrections.record(CorrectionRequest("same", CorrectionType.ADJUSTMENT, "Conferência", 42, 12))
        corrections.record(CorrectionRequest("down", CorrectionType.ADJUSTMENT, "Contagem", 42, 9))
        corrections.record(CorrectionRequest("reverse-sale", CorrectionType.SALE_REVERSAL, "Erro", sourceId = "sale-key"))
        corrections.record(CorrectionRequest("reverse-production", CorrectionType.PRODUCTION_REVERSAL, "Erro", sourceId = "reversed-production"))
    }
    private fun encode(b: BackupSnapshot): ByteArray = ByteArrayOutputStream().also { BackupJson.write(b, it) }.toByteArray()
    private fun equivalent(a: BackupSnapshot, b: BackupSnapshot) { assertEquals(a.copy(createdAt = b.createdAt, datasetId = b.datasetId), b) }
    @Test fun publicWritesRespectBackupTextLimitsWithoutPartialChanges() = runBlocking {
        val flavors = RoomFlavorRepository(source)
        val production = RoomProductionRepository(source)
        val corrections = RoomCorrectionRepository(source)
        val boundary = "a".repeat(16_384)
        flavors.save(null, boundary, "")
        val id = source.flavorDao().observeAll().first().single().id
        production.record(ProductionRequest("boundary", 1, listOf(ProductionQuantity(id, 2)), boundary))
        corrections.record(CorrectionRequest("boundary-count", CorrectionType.ADJUSTMENT, boundary, id, 2))
        val before = RoomBackupRepository(source).snapshot()
        val tooLong = boundary + "a"
        val rejected = listOf(
            runCatching { flavors.save(null, tooLong, "") }.exceptionOrNull(),
            runCatching { flavors.save(id, tooLong + "b", "") }.exceptionOrNull(),
            // Lowercasing can expand a name even when the display text fits.
            runCatching { flavors.save(null, "İ".repeat(8_193), "") }.exceptionOrNull(),
            runCatching { production.record(ProductionRequest("too-long", 1, listOf(ProductionQuantity(id, 1)), tooLong)) }.exceptionOrNull(),
            runCatching { corrections.record(CorrectionRequest("too-long", CorrectionType.LOSS, tooLong, id, 1)) }.exceptionOrNull(),
        )
        assertTrue("Textos incompatíveis com backup devem ser rejeitados na persistência: $rejected",
            rejected.all { it is IllegalArgumentException })
        equivalent(before, RoomBackupRepository(source).snapshot())
        val output = ByteArrayOutputStream()
        RoomBackupRepository(source).export { output }
        RoomBackupRepository(target).restore(BackupJson.read(output.toByteArray().inputStream()))
        equivalent(before, RoomBackupRepository(target).snapshot())
    }
    @Test fun productionDatesOutsideBackupRangeAreRejectedWithoutWrites() = runBlocking {
        source.flavorDao().insert(FlavorEntity(1, "Coco"))
        val before = RoomBackupRepository(source).snapshot()
        val rejected = listOf(-1L, 253402300800000L).map { date ->
            runCatching { RoomProductionRepository(source).record(
                ProductionRequest("date-$date", date, listOf(ProductionQuantity(1, 1)))) }.exceptionOrNull()
        }
        assertTrue("Datas incompatíveis com backup devem ser rejeitadas: $rejected",
            rejected.all { it is IllegalArgumentException })
        equivalent(before, RoomBackupRepository(source).snapshot())
        for (date in listOf(0L, 253402300799999L)) {
            RoomProductionRepository(source).record(
                ProductionRequest("date-$date", date, listOf(ProductionQuantity(1, 1))))
        }
        val output = ByteArrayOutputStream()
        RoomBackupRepository(source).export { output }
        RoomBackupRepository(target).restore(BackupJson.read(output.toByteArray().inputStream()))
        equivalent(RoomBackupRepository(source).snapshot(), RoomBackupRepository(target).snapshot())
    }
    @Test fun fullRoundTripPreservesEveryEntityAndStockAndReplacesOldDatabase() = runBlocking {
        populate()
        target.flavorDao().insert(FlavorEntity(99, "Anterior"))
        val original = RoomBackupRepository(source).snapshot()
        val out = ByteArrayOutputStream()
        RoomBackupRepository(source).export { out }
        val repository = RoomBackupRepository(target)
        val inspected = repository.inspect { ByteArrayInputStream(out.toByteArray()) }
        assertNotNull(target.flavorDao().findById(99)) // inspection is read-only
        repository.restore(inspected)
        equivalent(original, repository.snapshot())
        assertNull(target.flavorDao().findById(99))
        for (id in listOf(41L, 42L, 43L)) assertEquals(source.stockDao().quantity(id), target.stockDao().quantity(id))
        assertEquals(550L, target.saleDao().itemsForSale("sale-key").first().unitPriceCents)
        assertNull(target.saleDao().itemsForSale("sale-key").last().unitPriceCents)
        assertFalse(target.flavorDao().findById(43)!!.active)
        assertEquals(3, target.flavorDao().observeAll().first().size)
        RoomProductionRepository(target).record(ProductionRequest("production-key", 500, listOf(ProductionQuantity(41, 20), ProductionQuantity(42, 10)), "Lote original"))
        RoomSaleRepository(target).record(SaleRequest("sale-key", 2000, listOf(SaleQuantity(41, 3, 550), SaleQuantity(42, 2, null))))
        RoomCorrectionRepository(target).record(CorrectionRequest("loss", CorrectionType.LOSS, "Embalagem", 41, 1))
        equivalent(original, repository.snapshot()) // replay remains idempotent after restoring
    }
    @Test fun emptyRoundTripDeletesAllPreviousRecords() = runBlocking {
        populate()
        val empty = RoomBackupRepository(target).snapshot()
        RoomBackupRepository(source).restore(BackupJson.read(ByteArrayInputStream(encode(empty))))
        equivalent(empty, RoomBackupRepository(source).snapshot())
    }
    @Test fun invalidJsonTypesVersionsDuplicateFieldsAndReferencesAreRejectedWithoutWrites() = runBlocking {
        populate()
        val original = RoomBackupRepository(source).snapshot()
        val json = String(encode(original), Charsets.UTF_8)
        val invalid = listOf("{", "[]", json + "{}", json.replace("2lopes-backup", "other"),
            json.replace("\"formatVersion\":2", "\"formatVersion\":99"),
            json.replace("\"schemaVersion\":6", "\"schemaVersion\":7"),
            json.replace("\"formatVersion\":2", "\"formatVersion\":2,\"formatVersion\":2"),
            json.replace("\"active\":true", "\"active\":1"),
            json.replace("\"quantity\":20", "\"quantity\":20.0"),
            json.replace("\"flavorId\":41", "\"flavorId\":999"),
            json.replace("\"quantityDelta\":20", "\"quantityDelta\":19"),
            json.replace("\"unitPriceCents\":550", "\"unitPriceCents\":-1"),
            json.replace("\"datasetId\"", "\"unknown\""))
        for (text in invalid) {
            try { RoomBackupRepository(source).inspect { text.byteInputStream() }; fail("Deveria rejeitar: ${text.take(60)}") }
            catch (_: Exception) { }
            equivalent(original, RoomBackupRepository(source).snapshot())
        }
    }
    @Test fun failureAfterDeletesAndPartialInsertsRollsBackEveryTable() = runBlocking {
        populate()
        val original = RoomBackupRepository(source).snapshot()
        target.flavorDao().insert(FlavorEntity(88, "Destino"))
        target.flavorDao().insert(FlavorEntity(89, "Falha posterior"))
        val replacement = RoomBackupRepository(target).snapshot()
        source.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_restore BEFORE INSERT ON flavors WHEN NEW.id = 89 BEGIN SELECT RAISE(ABORT, 'injected failure'); END")
        try { RoomBackupRepository(source).restore(replacement); fail("Deveria falhar") } catch (_: Exception) { }
        equivalent(original, RoomBackupRepository(source).snapshot())
    }
    @Test fun readWriteAndCloseFailuresNeverReportSuccessOrChangeDatabase() = runBlocking {
        populate()
        val repository = RoomBackupRepository(source)
        val original = repository.snapshot()
        val actions: List<suspend () -> Unit> = listOf(
            { repository.inspect { null }; Unit },
            { repository.inspect { object : InputStream() { override fun read(): Int = throw IOException("read") } }; Unit },
            { repository.export { null } },
            { repository.export { object : OutputStream() { override fun write(b: Int) { throw IOException("write") } } } },
            { repository.export { object : ByteArrayOutputStream() { override fun close() { throw IOException("close") } } } },
        )
        actions.forEach { action -> try { action(); fail("Deveria falhar") } catch (_: IOException) { } }
        equivalent(original, repository.snapshot())
    }
    @Test fun sizeRecordAndTextLimitsAreEnforced() = runBlocking<Unit> {
        val empty = RoomBackupRepository(source).snapshot()
        val json = String(encode(empty), Charsets.UTF_8)
        val huge = json.replace("\"flavors\":[]", "\"flavors\":[" + List(100001) { "{\"id\":1,\"name\":\"Coco\",\"defaultPriceCents\":null,\"active\":true,\"normalizedName\":\"coco\"}" }.joinToString(",") + "]")
        assertThrows(BackupInvalid::class.java) { BackupJson.read(huge.byteInputStream()) }
        assertThrows(Exception::class.java) { BackupJson.read((" ".repeat(BackupJson.MAX_BYTES + 1) + json).byteInputStream()) }
        val longText = json.replace("\"flavors\":[]", "\"flavors\":[{\"id\":1,\"name\":\"${"a".repeat(16385)}\",\"defaultPriceCents\":null,\"active\":true,\"normalizedName\":\"a\"}]")
        assertThrows(BackupInvalid::class.java) { BackupJson.read(longText.byteInputStream()) }
    }
    @Test fun snapshotsRemainConsistentWhileProductionWritesAndFlowsInvalidateAfterRestore() = runBlocking {
        source.flavorDao().insert(FlavorEntity(1, "Coco"))
        val producer = launch(Dispatchers.Default) {
            repeat(15) { RoomProductionRepository(source).record(ProductionRequest("p-$it", 1, listOf(ProductionQuantity(1, 1)))) }
        }
        repeat(15) { BackupValidator.validate(RoomBackupRepository(source).snapshot()) }
        producer.join()
        target.flavorDao().insert(FlavorEntity(99, "Anterior"))
        val observer = async(start = CoroutineStart.UNDISPATCHED) {
            withTimeout(5000) { target.flavorDao().observeAll().first { rows -> rows.map { it.id } == listOf(1L) } }
        }
        RoomBackupRepository(target).restore(RoomBackupRepository(source).snapshot())
        assertEquals("Coco", observer.await().single().name)
        assertEquals(15L, target.stockDao().quantity(1))
    }
}
