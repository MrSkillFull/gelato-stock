package com.flstudios.a2lopes.data.local

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.flstudios.a2lopes.data.local.entity.*
import com.flstudios.a2lopes.data.repository.*
import com.flstudios.a2lopes.domain.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProductionRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var production: RoomProductionRepository
    private lateinit var flavors: RoomFlavorRepository
    private lateinit var stock: RoomStockRepository
    private var clock = 2000L

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, AppDatabase::class.java).build()
        production = RoomProductionRepository(db) { clock }
        flavors = RoomFlavorRepository(db)
        stock = RoomStockRepository(db)
    }

    @After fun tearDown() { db.close() }

    private suspend fun flavor(name: String = "Coco", active: Boolean = true) =
        db.flavorDao().insert(FlavorEntity(name = name, active = active))

    private fun request(id: String, vararg items: Pair<Long, Long>) =
        ProductionRequest(id, 1000, items.map { ProductionQuantity(it.first, it.second) }, "Prontos")

    private suspend fun rejected(block: suspend () -> Unit) {
        try { block(); fail("A operação deveria falhar") } catch (_: IllegalArgumentException) { }
    }

    private fun count(table: String): Long = db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM $table").use {
        it.moveToFirst(); it.getLong(0)
    }

    private fun assertNoProduction() {
        listOf("production_sessions", "production_items", "stock_movements").forEach { assertEquals(0L, count(it)) }
    }

    @Test fun singleAndMultipleProductionsSumStockAndPreserveTraceAndDates() = runBlocking {
        val coco = flavor()
        val morango = flavor("Morango")
        val unused = flavor("Uva", false)
        assertEquals(0L, stock.observeQuantity(coco).first())
        val session = production.record(request("one", coco to 3))
        assertEquals(1000L, session.producedAt)
        assertEquals(2000L, session.createdAt)
        assertEquals("Prontos", session.notes)
        clock = 3000
        production.record(request("two", coco to 7, morango to 5))
        val items = db.productionDao().itemsForSession("two")
        val movements = db.productionDao().movementsForSession("two")
        assertEquals(2, items.size)
        assertEquals(2, movements.size)
        items.zip(movements).forEach { (item, movement) ->
            assertEquals(item.id, movement.referenceId)
            assertEquals(item.id, movement.productionItemId)
            assertEquals(item.flavorId, movement.flavorId)
            assertEquals(item.quantity, movement.quantityDelta)
            assertEquals("PRODUCTION", movement.type)
            assertEquals("PRODUCTION_ITEM", movement.referenceType)
            assertEquals(3000L, movement.createdAt)
        }
        val quantities = stock.observeAll().first().associate { it.flavorId to it.quantity }
        assertEquals(mapOf(coco to 10L, morango to 5L, unused to 0L), quantities)
    }

    @Test fun invalidRequestsAndInactiveOrMissingFlavorsLeaveNoRows() = runBlocking {
        val active = flavor()
        val inactive = flavor("Uva", false)
        listOf(
            request("empty"), request("zero", active to 0), request("negative", active to -1),
            request("duplicate", active to 1, active to 2), request("inactive", inactive to 1),
            request("missing", 999L to 1),
        ).forEach { rejected { production.record(it) }; assertNoProduction() }
        assertEquals(0L, stock.observeQuantity(active).first())
    }

    @Test fun failureOnLaterItemRollsBackSessionItemsAndMovements() = runBlocking {
        val first = flavor()
        val second = flavor("Morango")
        // Falha SQLite na segunda movimentação, depois da primeira já ter sido inserida.
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_second BEFORE INSERT ON stock_movements WHEN NEW.flavor_id = $second BEGIN SELECT RAISE(ABORT, 'injected failure'); END")
        try { production.record(request("fail", first to 3, second to 5)); fail("Falha esperada") }
        catch (_: SQLiteConstraintException) { }
        assertNoProduction()
        assertEquals(0L, stock.observeQuantity(first).first())
        db.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_second")
        production.record(request("fail", first to 3, second to 5))
        assertEquals(3L, stock.observeQuantity(first).first())
    }

    @Test fun repeatedAndConcurrentRequestsCreateOnlyOneSession() = runBlocking {
        val coco = flavor()
        val morango = flavor("Morango")
        val input = request("stable", coco to 3, morango to 5)
        val sessions = coroutineScope { (1..4).map { async(Dispatchers.Default) { production.record(input) } }.awaitAll() }
        assertTrue(sessions.all { it == sessions.first() })
        clock = 9000
        assertEquals(sessions.first(), production.record(input.copy(items = input.items.reversed())))
        rejected { production.record(request("stable", coco to 4)) }
        assertEquals(1L, count("production_sessions"))
        assertEquals(2L, count("production_items"))
        assertEquals(2L, count("stock_movements"))
        assertEquals(3L, stock.observeQuantity(coco).first())
    }

    @Test fun flowEmitsCommittedStockUpdate() = runBlocking {
        val coco = flavor()
        val initial = CompletableDeferred<Unit>()
        val observation = async {
            withTimeout(5000) {
                stock.observeQuantity(coco).onEach { if (it == 0L) initial.complete(Unit) }.first { it == 8L }
            }
        }
        withTimeout(5000) { initial.await() }
        production.record(request("flow", coco to 8))
        assertEquals(8L, observation.await())
    }

    @Test fun overflowIsRejectedWithoutChangingExistingProduction() = runBlocking {
        val coco = flavor()
        production.record(request("max", coco to Long.MAX_VALUE))
        try { production.record(request("overflow", coco to 1)); fail("Overflow aceito") }
        catch (_: IllegalArgumentException) { }
        assertNull(db.productionDao().findSession("overflow"))
        assertEquals(1L, count("stock_movements"))
        assertEquals(Long.MAX_VALUE, stock.observeQuantity(coco).first())
    }

    @Test fun historyAndForeignKeysPreventDeletionAndPositiveStockPreventsDeactivation() = runBlocking {
        val coco = flavor()
        val unused = flavor("Uva", false)
        production.record(request("history", coco to 1))
        try { flavors.delete(coco); fail("Histórico excluído") } catch (_: FlavorHistoryException) { }
        try { db.flavorDao().deleteById(coco); fail("FK ignorada") } catch (_: SQLiteConstraintException) { }
        try { flavors.setActive(coco, false); fail("Desativação com saldo") } catch (_: IllegalStateException) { }
        flavors.delete(unused)
        assertNull(db.flavorDao().findById(unused))
        assertEquals(1L, count("production_items"))
        assertEquals(1L, count("stock_movements"))
    }

    @Test fun itemOnlyAndMovementOnlyHistoryBlockDeletionEvenAtZeroStock() = runBlocking {
        val itemFlavor = flavor()
        val movementFlavor = flavor("Morango", false)
        // Fixtures de integridade: não implementam fluxos de ajuste ou correção.
        db.productionDao().insertSession(ProductionSessionEntity("fixture", 1, 2, null))
        db.productionDao().insertItem(ProductionItemEntity(sessionId = "fixture", flavorId = itemFlavor, quantity = 1))
        db.productionDao().insertMovement(StockMovementEntity(
            flavorId = movementFlavor, type = "FIXTURE", quantityDelta = 0,
            referenceType = "FIXTURE", referenceId = 1, createdAt = 2, notes = null,
        ))
        for (id in listOf(itemFlavor, movementFlavor)) {
            assertEquals(0L, stock.observeQuantity(id).first())
            try { flavors.delete(id); fail("Histórico ignorado") } catch (_: FlavorHistoryException) { }
            try { db.flavorDao().deleteById(id); fail("FK ignorada") } catch (_: SQLiteConstraintException) { }
            assertNotNull(db.flavorDao().findById(id))
        }
        assertEquals(1L, count("production_items"))
        assertEquals(1L, count("stock_movements"))
    }

    @Test fun sqliteRejectsDuplicateItemsMovementsWrongFlavorAndDeletingReferencedParents() = runBlocking {
        val coco = flavor()
        val other = flavor("Uva")
        production.record(request("integrity", coco to 2))
        val item = db.productionDao().itemsForSession("integrity").single()
        val movement = db.productionDao().movementsForSession("integrity").single()
        val operations: List<suspend () -> Unit> = listOf(
            { db.productionDao().insertItem(item.copy(id = 0)) },
            { db.productionDao().insertMovement(movement.copy(id = 0)) },
            { db.productionDao().insertMovement(movement.copy(id = 0, flavorId = other)) },
            { db.openHelper.writableDatabase.execSQL("DELETE FROM production_sessions WHERE id = 'integrity'") },
            { db.openHelper.writableDatabase.execSQL("DELETE FROM production_items WHERE id = ${item.id}") },
        )
        operations.forEach { operation ->
            try { operation(); fail("Restrição SQLite ignorada") } catch (_: SQLiteConstraintException) { }
        }
        assertEquals(1L, count("production_sessions"))
        assertEquals(1L, count("production_items"))
        assertEquals(1L, count("stock_movements"))
    }
}
