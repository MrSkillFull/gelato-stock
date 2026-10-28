package com.flstudios.a2lopes.data.local

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.flstudios.a2lopes.data.local.entity.*
import com.flstudios.a2lopes.data.repository.*
import com.flstudios.a2lopes.domain.model.*
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SaleRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repository: RoomSaleRepository
    private var clock = 2000L

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, AppDatabase::class.java).build()
        repository = RoomSaleRepository(db) { clock }
    }
    @After fun tearDown() { db.close() }

    private suspend fun flavor(name: String = "Coco", price: Long? = 550, quantity: Long = 10): Long {
        val id = db.flavorDao().insert(FlavorEntity(name = name, defaultPriceCents = price))
        if (quantity > 0) RoomProductionRepository(db).record(ProductionRequest("seed-$id", 1, listOf(ProductionQuantity(id, quantity))))
        return id
    }
    private fun request(id: String, vararg items: SaleQuantity) = SaleRequest(id, 1000, items.toList())
    private fun count(table: String) = db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM $table").use { it.moveToFirst(); it.getLong(0) }
    private fun assertNoSales() { assertEquals(0L, count("sales")); assertEquals(0L, count("sale_items")) }
    private suspend fun rejected(block: suspend () -> Unit) {
        try { block(); fail("Venda deveria ser rejeitada") } catch (_: IllegalArgumentException) { }
    }

    @Test fun singleSaleDebitsStockAndRecordsDatesAndConcreteOrigin() = runBlocking {
        val id = flavor(quantity = 8)
        val sale = repository.record(request("single", SaleQuantity(id, 3, 550)))
        assertEquals(1000L, sale.soldAt)
        assertEquals(2000L, sale.createdAt)
        assertEquals(5L, db.stockDao().quantity(id))
        val item = db.saleDao().itemsForSale(sale.id).single()
        val movement = db.saleDao().movementsForSale(sale.id).single()
        assertEquals(3L, item.quantity)
        assertEquals(550L, item.unitPriceCents)
        assertEquals(item.id, movement.saleItemId)
        assertEquals(item.id, movement.referenceId)
        assertEquals(item.flavorId, movement.flavorId)
        assertEquals(-3L, movement.quantityDelta)
        assertEquals("SALE", movement.type)
        assertEquals("SALE_ITEM", movement.referenceType)
        assertEquals(2000L, movement.createdAt)
        assertNull(movement.productionItemId)
    }

    @Test fun multipleItemsIncludingMissingPriceDebitOnlyTheirQuantities() = runBlocking {
        val coco = flavor()
        val morango = flavor("Morango", null, 5)
        repository.record(request("multiple", SaleQuantity(coco, 4, 550), SaleQuantity(morango, 2, null)))
        assertEquals(6L, db.stockDao().quantity(coco))
        assertEquals(3L, db.stockDao().quantity(morango))
        val items = db.saleDao().itemsForSale("multiple")
        assertEquals(2, items.size)
        assertNull(items.single { it.flavorId == morango }.unitPriceCents)
        assertEquals(2, db.saleDao().movementsForSale("multiple").size)
    }

    @Test fun rejectsInvalidInputsInactiveMissingAndChangedPriceWithoutWrites() = runBlocking {
        val coco = flavor()
        val inactive = flavor("Uva")
        db.flavorDao().setActive(inactive, false)
        val invalid = listOf(
            request("empty"), request("zero", SaleQuantity(coco, 0, 550)),
            request("negative", SaleQuantity(coco, -1, 550)),
            request("duplicate", SaleQuantity(coco, 1, 550), SaleQuantity(coco, 2, 550)),
            request("inactive", SaleQuantity(inactive, 1, 550)),
            request("missing", SaleQuantity(999, 1, 550)),
            request("price", SaleQuantity(coco, 1, 100)), request("unpriced", SaleQuantity(coco, 1, null)),
        )
        invalid.forEach { rejected { repository.record(it) }; assertNoSales() }
        assertEquals(10L, db.stockDao().quantity(coco))
        assertEquals(2L, count("stock_movements"))
    }

    @Test fun insufficientLaterItemRollsBackEarlierDebitAndNeverMakesStockNegative() = runBlocking {
        val first = flavor(quantity = 3)
        val second = flavor("Morango", quantity = 1)
        rejected { repository.record(request("insufficient", SaleQuantity(first, 2, 550), SaleQuantity(second, 2, 550))) }
        assertNoSales()
        assertEquals(3L, db.stockDao().quantity(first))
        assertEquals(1L, db.stockDao().quantity(second))
        assertEquals(2L, count("stock_movements"))
        rejected { repository.record(request("missing-later", SaleQuantity(first, 1, 550), SaleQuantity(999, 1, 550))) }
        assertNoSales()
        assertEquals(3L, db.stockDao().quantity(first))
    }

    @Test fun sqliteFailureOnSecondMovementLeavesNoPartialSaleAndSameKeyCanRetry() = runBlocking {
        val first = flavor()
        val second = flavor("Morango")
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_sale BEFORE INSERT ON stock_movements WHEN NEW.type = 'SALE' AND NEW.flavor_id = $second BEGIN SELECT RAISE(ABORT, 'injected'); END")
        val input = request("fail", SaleQuantity(first, 2, 550), SaleQuantity(second, 3, 550))
        try { repository.record(input); fail("Falha esperada") } catch (_: SQLiteConstraintException) { }
        assertNoSales()
        assertEquals(2L, count("stock_movements"))
        assertEquals(10L, db.stockDao().quantity(first))
        assertEquals(10L, db.stockDao().quantity(second))
        db.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_sale")
        repository.record(input)
        assertEquals(8L, db.stockDao().quantity(first))
        assertEquals(7L, db.stockDao().quantity(second))
    }

    @Test fun repeatedConcurrentRequestsAreIdempotentAndContentConflictsAreRejected() = runBlocking {
        val first = flavor()
        val second = flavor("Morango", null)
        val input = request("stable", SaleQuantity(first, 2, 550), SaleQuantity(second, 3, null))
        val results = coroutineScope { (1..4).map { async(Dispatchers.Default) { repository.record(input) } }.awaitAll() }
        assertTrue(results.all { it == results.first() })
        clock = 3000
        assertEquals(results.first(), repository.record(input.copy(items = input.items.reversed())))
        rejected { repository.record(input.copy(soldAt = 1001)) }
        rejected { repository.record(input.copy(items = listOf(SaleQuantity(first, 3, 550), input.items[1]))) }
        rejected { repository.record(input.copy(items = listOf(SaleQuantity(first, 2, 551), input.items[1]))) }
        rejected { repository.record(input.copy(items = listOf(input.items[0]))) }
        assertEquals(1L, count("sales"))
        assertEquals(2L, count("sale_items"))
        assertEquals(4L, count("stock_movements"))
        assertEquals(8L, db.stockDao().quantity(first))
    }

    @Test fun competingSalesForLastUnitPersistOnlyOneSale() = runBlocking {
        val id = flavor(quantity = 1)
        val results = coroutineScope {
            listOf("first", "second").map { key -> async(Dispatchers.Default) {
                runCatching { repository.record(request(key, SaleQuantity(id, 1, 550))) }
            } }.awaitAll()
        }
        assertEquals(1, results.count { it.isSuccess })
        assertEquals(1, results.count { it.exceptionOrNull() is IllegalArgumentException })
        assertEquals(0L, db.stockDao().quantity(id))
        assertEquals(1L, count("sales"))
        assertEquals(1L, count("sale_items"))
        assertEquals(2L, count("stock_movements"))
    }

    @Test fun historicalPricesAndReplaySurvivePriceChangesAndDeactivation() = runBlocking {
        val first = flavor(quantity = 1)
        val second = flavor("Morango", null, 1)
        val input = request("historic", SaleQuantity(first, 1, 550), SaleQuantity(second, 1, null))
        val sale = repository.record(input)
        RoomFlavorRepository(db).save(first, "Coco", "8")
        RoomFlavorRepository(db).save(second, "Morango", "2")
        RoomFlavorRepository(db).setActive(first, false)
        RoomFlavorRepository(db).setActive(second, false)
        val items = db.saleDao().itemsForSale("historic")
        assertEquals(550L, items.first().unitPriceCents)
        assertNull(items.last().unitPriceCents)
        assertEquals(sale, repository.record(input))
        assertEquals(0L, db.stockDao().quantity(first))
        assertEquals(1L, count("sales"))
    }

    @Test fun saleHistoryBlocksFlavorDeletionAtZeroAndSqlitePreservesParents() = runBlocking {
        val id = flavor(quantity = 1)
        repository.record(request("history", SaleQuantity(id, 1, 550)))
        assertEquals(0L, db.stockDao().quantity(id))
        try { RoomFlavorRepository(db).delete(id); fail("Histórico apagado") } catch (_: FlavorHistoryException) { }
        try { db.flavorDao().deleteById(id); fail("FK ignorada") } catch (_: SQLiteConstraintException) { }
        assertEquals(1L, count("sale_items"))
        assertEquals(2L, count("stock_movements"))
    }

    @Test fun saleItemWithoutMovementStillBlocksDeletionAndSqliteRejectsBrokenLinks() = runBlocking {
        val id = flavor(quantity = 0)
        val other = flavor("Uva", quantity = 0)
        db.saleDao().insertSale(SaleEntity("fixture", 1, 2))
        val itemId = db.saleDao().insertItem(SaleItemEntity(saleId = "fixture", flavorId = id, quantity = 1, unitPriceCents = null))
        try { RoomFlavorRepository(db).delete(id); fail("Item histórico ignorado") } catch (_: FlavorHistoryException) { }
        try { db.flavorDao().deleteById(id); fail("FK ignorada") } catch (_: SQLiteConstraintException) { }
        val movement = StockMovementEntity(flavorId = id, type = "SALE", quantityDelta = -1, referenceType = "SALE_ITEM", referenceId = itemId, createdAt = 2, notes = null, saleItemId = itemId)
        // Checar FK/índices em fixtures não cria uma operação pública de baixa sem validação.
        try { db.saleDao().insertMovement(movement.copy(flavorId = other)); fail("Sabor da origem incorreto") } catch (_: SQLiteConstraintException) { }
        db.saleDao().insertMovement(movement.copy(quantityDelta = 0))
        try { db.saleDao().insertMovement(movement); fail("Baixa duplicada") } catch (_: SQLiteConstraintException) { }
        try { db.saleDao().insertItem(SaleItemEntity(saleId = "fixture", flavorId = id, quantity = 1, unitPriceCents = null)); fail("Item duplicado") } catch (_: SQLiteConstraintException) { }
        for (sql in listOf("DELETE FROM sales WHERE id = 'fixture'", "DELETE FROM sale_items WHERE id = $itemId")) {
            try { db.openHelper.writableDatabase.execSQL(sql); fail("Exclusão em cascata") } catch (_: SQLiteConstraintException) { }
        }
        assertEquals(1L, count("sales"))
        assertEquals(1L, count("sale_items"))
        assertEquals(1L, count("stock_movements"))
    }

    @Test fun excessiveFinancialTotalDoesNotPersistAnything() = runBlocking {
        val id = flavor(price = Long.MAX_VALUE, quantity = 2)
        rejected { repository.record(request("overflow", SaleQuantity(id, 2, Long.MAX_VALUE))) }
        assertNoSales()
        assertEquals(2L, db.stockDao().quantity(id))
    }
}
