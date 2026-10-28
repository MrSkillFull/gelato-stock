package com.flstudios.a2lopes.data.local

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.flstudios.a2lopes.data.local.entity.*
import com.flstudios.a2lopes.data.repository.*
import com.flstudios.a2lopes.domain.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CorrectionRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var corrections: RoomCorrectionRepository
    private var coco = 0L
    private var morango = 0L
    private var clock = 3000L

    @Before fun setUp() = runBlocking<Unit> {
        db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, AppDatabase::class.java).build()
        corrections = RoomCorrectionRepository(db) { clock }
        coco = db.flavorDao().insert(FlavorEntity(name = "Coco", defaultPriceCents = 550))
        morango = db.flavorDao().insert(FlavorEntity(name = "Morango"))
        RoomProductionRepository(db) { 1000 }.record(ProductionRequest("p", 100,
            listOf(ProductionQuantity(coco, 10), ProductionQuantity(morango, 5)), "Original"))
    }
    @After fun tearDown() { db.close() }
    private fun loss(id: String = "loss", amount: Long = 2, flavor: Long = coco) =
        CorrectionRequest(id, CorrectionType.LOSS, "  Embalagem   rompida \n", flavor, amount)
    private fun count(id: String, amount: Long, flavor: Long = coco) =
        CorrectionRequest(id, CorrectionType.ADJUSTMENT, "Contagem física", flavor, amount)
    private fun reverse(type: CorrectionType, source: String, id: String = "reverse") =
        CorrectionRequest(id, type, "Lançamento incorreto", sourceId = source)
    private suspend fun sell(id: String = "s", multiple: Boolean = true) {
        RoomSaleRepository(db) { 2000 }.record(SaleRequest(id, 200,
            listOf(SaleQuantity(coco, 2, 550)) + if (multiple) listOf(SaleQuantity(morango, 1, null)) else emptyList()))
    }
    private suspend fun rejected(action: suspend () -> Unit) {
        try { action(); fail("Operação deveria ser rejeitada") } catch (_: CorrectionRejected) { }
    }
    private fun rows(table: String): Long = db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM $table").use { it.moveToFirst(); it.getLong(0) }

    @Test fun lossNormalizesReasonAndReplaysSameContentEvenAfterStockChanges() = runBlocking {
        val original = db.productionDao().movementsForSession("p")
        val request = loss()
        val receipt = corrections.record(request)
        assertEquals(8L, db.stockDao().quantity(coco))
        assertEquals("Embalagem rompida", db.correctionDao().find(receipt.id)!!.reason)
        val item = db.correctionDao().items(receipt.id).single()
        assertEquals(10L, item.previousQuantity)
        assertEquals(8L, item.resultingQuantity)
        assertEquals(-2L, item.quantityDelta)
        corrections.record(count("later", 0))
        clock = 4000
        assertEquals(receipt, corrections.record(request.copy(reason = "Embalagem rompida")))
        assertEquals(0L, db.stockDao().quantity(coco))
        assertEquals(3000L, receipt.createdAt)
        assertEquals(original, db.productionDao().movementsForSession("p"))
        val before = rows("stock_movements")
        listOf(request.copy(quantity = 3), request.copy(reason = "Outro"), request.copy(flavorId = morango),
            request.copy(type = CorrectionType.ADJUSTMENT), reverse(CorrectionType.PRODUCTION_REVERSAL, "p", request.operationId)).forEach {
            rejected { corrections.record(it) }
        }
        assertEquals(before, rows("stock_movements"))
        assertEquals(2L, rows("corrections"))
    }

    @Test fun invalidOrExcessiveLossNeverWritesAndInactiveFlavorWithStockCanLoseUnits() = runBlocking {
        listOf(loss(amount = 11), loss(amount = 0), loss(amount = -1), loss(flavor = 999), loss().copy(reason = " \n\u00a0 ")).forEach {
            rejected { corrections.record(it) }
        }
        assertEquals(0L, rows("corrections"))
        assertEquals(10L, db.stockDao().quantity(coco))
        val inactive = db.flavorDao().insert(FlavorEntity(name = "Inativo", active = false))
        corrections.record(count("inactive-stock", 4, inactive))
        corrections.record(loss("inactive-loss", 3, inactive))
        assertEquals(1L, db.stockDao().quantity(inactive))
    }

    @Test fun countsRecordPositiveNegativeZeroAndNoDifferenceIncludingInactiveFlavor() = runBlocking {
        corrections.record(count("up", 14))
        corrections.record(count("down", 6))
        corrections.record(count("zero", 0))
        assertEquals(listOf(4L, -8L, -6L), listOf("up", "down", "zero").map { db.correctionDao().items(it).single().quantityDelta })
        val movements = rows("stock_movements")
        val same = corrections.record(count("same", 0))
        assertFalse(same.changedStock)
        assertEquals(movements, rows("stock_movements"))
        assertEquals(0L, db.correctionDao().items("same").single().previousQuantity)
        corrections.record(count("later", 3))
        assertEquals(same, corrections.record(count("same", 0)))
        assertEquals(3L, db.stockDao().quantity(coco))
        val inactive = db.flavorDao().insert(FlavorEntity(name = "Inativo", active = false))
        corrections.record(count("inactive", 9, inactive))
        assertEquals(9L, db.stockDao().quantity(inactive))
        rejected { corrections.record(count("negative", -1)) }
    }

    @Test fun saleReversalsRestoreSingleAndMultipleItemsWithoutChangingHistoricalPricesOrOriginals() = runBlocking {
        sell("single", false)
        sell("multi", true)
        val originals = listOf("single", "multi").associateWith { db.saleDao().itemsForSale(it) to db.saleDao().movementsForSale(it) }
        db.flavorDao().updateDetails(coco, "Coco", "coco", 999)
        listOf("single", "multi").forEach { id ->
            val request = reverse(CorrectionType.SALE_REVERSAL, id, "r-$id")
            val receipt = corrections.record(request)
            assertEquals(receipt, corrections.record(request))
            rejected { corrections.record(request.copy(operationId = "other-$id")) }
            assertEquals(originals[id]!!.first, db.saleDao().itemsForSale(id))
            assertEquals(originals[id]!!.second, db.saleDao().movementsForSale(id))
            assertTrue(db.correctionDao().items(receipt.id).all { it.sourceSaleItemId != null && it.quantityDelta > 0 })
        }
        assertEquals(10L, db.stockDao().quantity(coco))
        assertEquals(5L, db.stockDao().quantity(morango))
        assertEquals(2L, rows("sales"))
        assertEquals(1100L, RoomHistoryRepository(db).detail(HistoryKind.SALE, "single")!!.totalCents)
        assertNull(RoomHistoryRepository(db).detail(HistoryKind.SALE, "multi")!!.totalCents)
    }

    @Test fun productionReversalIsIntegralAndCannotBeRepeatedWithAnotherKey() = runBlocking {
        val originalSession = db.productionDao().findSession("p")
        val originalItems = db.productionDao().itemsForSession("p")
        val originalMovements = db.productionDao().movementsForSession("p")
        val request = reverse(CorrectionType.PRODUCTION_REVERSAL, "p")
        val receipt = corrections.record(request)
        assertEquals(0L, db.stockDao().quantity(coco))
        assertEquals(0L, db.stockDao().quantity(morango))
        assertEquals(receipt, corrections.record(request))
        rejected { corrections.record(request.copy(operationId = "second")) }
        assertEquals(originalSession, db.productionDao().findSession("p"))
        assertEquals(originalItems, db.productionDao().itemsForSession("p"))
        assertEquals(originalMovements, db.productionDao().movementsForSession("p"))
        assertTrue(db.correctionDao().items(receipt.id).all { it.sourceProductionItemId != null && it.quantityDelta < 0 })
    }

    @Test fun insufficientLaterFlavorBlocksWholeProductionReversalAndRetryCanUseAggregateStock() = runBlocking {
        RoomSaleRepository(db).record(SaleRequest("consume", 200, listOf(SaleQuantity(morango, 4, null))))
        val before = rows("stock_movements")
        val request = reverse(CorrectionType.PRODUCTION_REVERSAL, "p")
        rejected { corrections.record(request) }
        assertEquals(0L, rows("corrections"))
        assertEquals(0L, rows("correction_items"))
        assertEquals(before, rows("stock_movements"))
        assertEquals(10L, db.stockDao().quantity(coco))
        assertEquals(1L, db.stockDao().quantity(morango))
        corrections.record(count("replenish", 5, morango))
        corrections.record(request)
        assertEquals(0L, db.stockDao().quantity(coco))
        assertEquals(0L, db.stockDao().quantity(morango))
    }

    @Test fun sqliteFailureOnSecondCompensationRollsBackEverythingAndAllowsSameKeyRetry() = runBlocking {
        sell()
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_correction BEFORE INSERT ON stock_movements WHEN NEW.type = 'SALE_REVERSAL' AND NEW.flavor_id = $morango BEGIN SELECT RAISE(ABORT, 'injected'); END")
        val request = reverse(CorrectionType.SALE_REVERSAL, "s")
        try { corrections.record(request); fail("Falha esperada") } catch (_: SQLiteConstraintException) { }
        assertEquals(0L, rows("corrections"))
        assertEquals(0L, rows("correction_items"))
        assertEquals(4L, rows("stock_movements"))
        assertEquals(8L, db.stockDao().quantity(coco))
        assertEquals(4L, db.stockDao().quantity(morango))
        db.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_correction")
        corrections.record(request)
        assertEquals(10L, db.stockDao().quantity(coco))
        assertEquals(5L, db.stockDao().quantity(morango))
    }

    @Test fun concurrentSameRequestAppliesOnceAndCompetingReversalsReturnOnlyOnce() = runBlocking {
        val request = loss(amount = 3)
        val receipts = coroutineScope { (1..5).map { async(Dispatchers.Default) { corrections.record(request) } }.awaitAll() }
        assertTrue(receipts.all { it == receipts.first() })
        assertEquals(7L, db.stockDao().quantity(coco))
        sell()
        val attempts = coroutineScope { (1..5).map { n -> async(Dispatchers.Default) {
            runCatching { corrections.record(reverse(CorrectionType.SALE_REVERSAL, "s", "reverse-$n")) }
        } }.awaitAll() }
        assertEquals(1, attempts.count { it.isSuccess })
        assertTrue(attempts.filter { it.isFailure }.all { it.exceptionOrNull() is CorrectionRejected })
        assertEquals(7L, db.stockDao().quantity(coco))
        assertEquals(5L, db.stockDao().quantity(morango))
    }

    @Test fun lossAndSaleRaceCannotConsumeTheSameStockAndCountsUseTheTransactionalBalance() = runBlocking {
        val attempts = coroutineScope {
            listOf(async(Dispatchers.Default) { runCatching { corrections.record(loss(amount = 10)) } },
                async(Dispatchers.Default) { runCatching { RoomSaleRepository(db).record(SaleRequest("race", 1, listOf(SaleQuantity(coco, 10, 550)))) } }).awaitAll()
        }
        assertEquals(1, attempts.count { it.isSuccess })
        assertEquals(0L, db.stockDao().quantity(coco))
        coroutineScope { listOf(3L, 7L).map { amount -> async(Dispatchers.Default) { corrections.record(count("count-$amount", amount)) } }.awaitAll() }
        val items = listOf("count-3", "count-7").map { db.correctionDao().items(it).single() }
        val first = items.single { it.previousQuantity == 0L }
        assertEquals(first.resultingQuantity, items.single { it !== first }.previousQuantity)
        assertTrue(db.stockDao().quantity(coco) in listOf(3L, 7L))
    }

    @Test fun saleReversalRejectsStockOverflowWithoutAnyCompensation() = runBlocking {
        sell(multiple = false)
        corrections.record(count("maximum", Long.MAX_VALUE))
        rejected { corrections.record(reverse(CorrectionType.SALE_REVERSAL, "s")) }
        assertNull(db.correctionDao().reversal("s", null))
        assertEquals(Long.MAX_VALUE, db.stockDao().quantity(coco))
    }

    @Test fun zeroDifferenceAuditPreventsFlavorDeletionAndCompositeFkRejectsWrongFlavor() = runBlocking {
        val unused = db.flavorDao().insert(FlavorEntity(name = "Zero", active = false))
        corrections.record(count("zero-audit", 0, unused))
        try { RoomFlavorRepository(db).delete(unused); fail("Histórico deve impedir exclusão") } catch (_: FlavorHistoryException) { }
        try { db.flavorDao().deleteById(unused); fail("FK deve impedir exclusão") } catch (_: SQLiteConstraintException) { }
        val item = db.correctionDao().items("zero-audit").single()
        try {
            db.correctionDao().insertMovement(StockMovementEntity(flavorId = coco, type = "ADJUSTMENT", quantityDelta = 1,
                referenceType = "CORRECTION_ITEM", referenceId = item.id, createdAt = 1, notes = "Falha", correctionItemId = item.id))
            fail("Origem deve corresponder ao sabor")
        } catch (_: SQLiteConstraintException) { }
        assertEquals(0L, db.stockDao().quantity(unused))
    }

    @Test fun historyShowsAuditsAndOriginsAndObservesReversedStatus() = runBlocking {
        sell()
        val history = RoomHistoryRepository(db)
        corrections.record(loss())
        corrections.record(count("same", 6))
        val observed = async { withTimeout(5000) { history.observeDetail(HistoryKind.SALE, "s").first { it?.reversalId != null } } }
        corrections.record(reverse(CorrectionType.SALE_REVERSAL, "s"))
        assertEquals("reverse", observed.await()!!.reversalId)
        val entries = history.observePage(HistoryFilter.ALL, 0).first().entries
        assertEquals(5, entries.size)
        val lossEntry = entries.single { it.correctionType == CorrectionType.LOSS }
        assertEquals("Coco", lossEntry.flavorName)
        assertEquals("Embalagem rompida", lossEntry.notes)
        assertEquals("reverse", entries.single { it.kind == HistoryKind.SALE }.reversalId)
        assertEquals(3, history.observePage(HistoryFilter.CORRECTIONS, 0).first().entries.size)
        val detail = history.detail(HistoryKind.CORRECTION, "reverse")!!
        assertEquals(HistoryKind.SALE, detail.sourceKind)
        assertEquals("s", detail.sourceId)
        assertEquals("Lançamento incorreto", detail.notes)
        assertTrue(detail.correctionItems.all { it.movementId != null })
        val noChange = history.detail(HistoryKind.CORRECTION, "same")!!.correctionItems.single()
        assertNull(noChange.movementId)
        assertEquals(6L, noChange.previousQuantity)
        val movement = history.observePage(HistoryFilter.MOVEMENTS, 0).first().entries.first { it.movementType == "LOSS" }
        assertEquals(HistoryKind.CORRECTION, movement.originKind)
        assertEquals("loss", movement.originId)
    }
}
