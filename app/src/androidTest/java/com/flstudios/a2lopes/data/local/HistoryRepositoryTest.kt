package com.flstudios.a2lopes.data.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.flstudios.a2lopes.data.local.entity.FlavorEntity
import com.flstudios.a2lopes.data.repository.*
import com.flstudios.a2lopes.domain.model.*
import com.flstudios.a2lopes.domain.repository.HistoryRepository.Companion.PAGE_SIZE
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HistoryRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var history: RoomHistoryRepository
    private var coco = 0L
    private var morango = 0L

    @Before fun setUp() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, AppDatabase::class.java).build()
        history = RoomHistoryRepository(db)
        coco = db.flavorDao().insert(FlavorEntity(name = "Coco", defaultPriceCents = 550))
        morango = db.flavorDao().insert(FlavorEntity(name = "Morango", defaultPriceCents = 700))
    }
    @After fun tearDown() { db.close() }

    private suspend fun produce(id: String = "p1", at: Long = 100, created: Long = 1000) {
        RoomProductionRepository(db) { created }.record(ProductionRequest(id, at,
            listOf(ProductionQuantity(coco, 10), ProductionQuantity(morango, 5)), "Lote pronto"))
    }
    private suspend fun sell(id: String = "s1", at: Long = 200, price: Long? = 700) {
        RoomSaleRepository(db) { 2000 }.record(SaleRequest(id, at,
            listOf(SaleQuantity(coco, 2, 550), SaleQuantity(morango, 1, price))))
    }
    private suspend fun page(filter: HistoryFilter = HistoryFilter.ALL, number: Int = 0) = history.observePage(filter, number).first()

    @Test fun ordersByOperationDateAndFiltersWithoutDuplicatingMovements() = runBlocking {
        produce()
        sell()
        // A backdated production registered later is ordered by produced_at, not created_at.
        produce("p2", 50, 3000)
        assertEquals(listOf("s1", "p1", "p2"), page().entries.map { it.id })
        assertEquals(listOf("p1", "p2"), page(HistoryFilter.PRODUCTIONS).entries.map { it.id })
        assertEquals(listOf("s1"), page(HistoryFilter.SALES).entries.map { it.id })
        assertEquals(listOf("3", "15", "15"), page().entries.map { it.units })
        assertTrue(page().entries.all { it.flavorCount == 2L && it.kind != HistoryKind.MOVEMENT })
        val movements = page(HistoryFilter.MOVEMENTS).entries
        assertEquals(6, movements.size)
        assertEquals(listOf(3000L, 3000L, 2000L, 2000L, 1000L, 1000L), movements.map { it.occurredAt })
        assertTrue(movements.all { it.kind == HistoryKind.MOVEMENT })
    }

    @Test fun productionDetailIncludesAllItemsNotesAndDistinctDates() = runBlocking {
        produce()
        val detail = history.detail(HistoryKind.PRODUCTION, "p1")!!
        assertEquals(100L, detail.occurredAt)
        assertEquals(1000L, detail.createdAt)
        assertEquals("Lote pronto", detail.notes)
        assertEquals(listOf("Coco", "Morango"), detail.items.map { it.flavorName })
        assertEquals(listOf(10L, 5L), detail.items.map { it.quantity })
        assertEquals("15", detail.totalUnits)
    }

    @Test fun saleUsesStoredPricesAfterCatalogChangesAndKeepsMissingPriceDistinctFromZero() = runBlocking {
        produce()
        sell()
        db.flavorDao().updateDetails(coco, "Coco", "coco", 999)
        db.flavorDao().updateDetails(morango, "Morango", "morango", null)
        val detail = history.detail(HistoryKind.SALE, "s1")!!
        assertEquals(listOf(550L, 700L), detail.items.map { it.unitPriceCents })
        assertEquals(listOf(2L, 1L), detail.items.map { it.quantity })
        assertEquals("3", detail.totalUnits)
        assertEquals(1800L, detail.totalCents)
        assertEquals(200L, detail.occurredAt)

        db.flavorDao().updateDetails(coco, "Coco", "coco", 550)
        sell("missing", 300, null)
        db.flavorDao().updateDetails(morango, "Morango", "morango", 0)
        sell("zero", 400, 0)
        assertNull(history.detail(HistoryKind.SALE, "missing")!!.totalCents)
        assertNull(history.detail(HistoryKind.SALE, "missing")!!.items.last().unitPriceCents)
        assertEquals(1100L, history.detail(HistoryKind.SALE, "zero")!!.totalCents)
        assertEquals(0L, history.detail(HistoryKind.SALE, "zero")!!.items.last().unitPriceCents)
    }

    @Test fun movementOriginsOpenTheCorrectRecordsAndReadingNeverChangesStock() = runBlocking {
        produce()
        sell()
        val before = listOf(db.stockDao().quantity(coco), db.stockDao().quantity(morango))
        val beforeRows = snapshot()
        val movements = page(HistoryFilter.MOVEMENTS).entries
        assertEquals(setOf("-2", "-1"), movements.filter { it.originKind == HistoryKind.SALE }.map { it.units }.toSet())
        assertEquals(setOf("+10", "+5"), movements.filter { it.originKind == HistoryKind.PRODUCTION }.map { it.units }.toSet())
        movements.forEach { movement ->
            val detail = history.detail(movement.originKind!!, movement.originId!!)!!
            assertEquals(if (movement.movementType == "SALE") "s1" else "p1", detail.id)
            assertTrue(detail.items.any { it.flavorName == movement.flavorName })
            assertNotNull(movement.reference)
            if (movement.originKind == HistoryKind.PRODUCTION) assertEquals("Lote pronto", movement.notes)
        }
        HistoryFilter.entries.forEach { page(it) }
        assertEquals(before, listOf(db.stockDao().quantity(coco), db.stockDao().quantity(morango)))
        assertEquals(beforeRows, snapshot())
    }

    private fun snapshot(): List<String> = listOf("production_sessions", "production_items", "sales", "sale_items", "stock_movements").flatMap { table ->
        db.openHelper.readableDatabase.query("SELECT * FROM $table ORDER BY id").use { cursor ->
            buildList { while (cursor.moveToNext()) add((0 until cursor.columnCount).joinToString("|") { cursor.getString(it) ?: "NULL" }) }
        }
    }

    @Test fun emptyFiltersAndUnknownDetailsAreSafe() = runBlocking {
        HistoryFilter.entries.forEach {
            assertTrue(page(it).entries.isEmpty())
            assertFalse(page(it).hasNext)
        }
        assertNull(history.detail(HistoryKind.PRODUCTION, "missing"))
        assertNull(history.detail(HistoryKind.SALE, "missing"))
    }

    @Test fun pagesAreBoundedAndTiesAreDeterministic() = runBlocking {
        repeat(PAGE_SIZE + 2) { produce("p${it.toString().padStart(3, '0')}") }
        val first = page()
        val second = page(number = 1)
        assertEquals(PAGE_SIZE, first.entries.size)
        assertTrue(first.hasNext)
        assertEquals(2, second.entries.size)
        assertFalse(second.hasNext)
        val ids = (first.entries + second.entries).map { it.id }
        assertEquals(PAGE_SIZE + 2, ids.distinct().size)
        assertEquals(ids.sortedDescending(), ids)
        // The movement filter has its own bounded pages, ordered by numeric movement ID on ties.
        val movementFirst = page(HistoryFilter.MOVEMENTS)
        assertEquals(PAGE_SIZE, movementFirst.entries.size)
        assertTrue(movementFirst.hasNext)
        assertEquals(movementFirst.entries.map { it.id.toLong() }.sortedDescending(), movementFirst.entries.map { it.id.toLong() })
    }

    @Test fun productionTotalsDoNotOverflowSqliteOrLong() = runBlocking {
        RoomProductionRepository(db).record(ProductionRequest("large", 1,
            listOf(ProductionQuantity(coco, Long.MAX_VALUE), ProductionQuantity(morango, Long.MAX_VALUE))))
        assertEquals("18446744073709551614", page().entries.single().units)
        assertEquals("18446744073709551614", history.detail(HistoryKind.PRODUCTION, "large")!!.totalUnits)
    }
}
