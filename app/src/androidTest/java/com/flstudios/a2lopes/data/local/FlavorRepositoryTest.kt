package com.flstudios.a2lopes.data.local

import android.database.sqlite.SQLiteConstraintException
import androidx.room.withTransaction
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.flstudios.a2lopes.data.local.entity.FlavorEntity
import com.flstudios.a2lopes.data.repository.RoomFlavorRepository
import com.flstudios.a2lopes.domain.model.FlavorValidationException
import java.util.UUID
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FlavorRepositoryTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val databaseName = "flavor-test-${UUID.randomUUID()}.db"
    private lateinit var database: AppDatabase
    private lateinit var repository: RoomFlavorRepository

    @Before
    fun setUp() { openDatabase() }

    private fun openDatabase() {
        database = Room.databaseBuilder(context, AppDatabase::class.java, databaseName)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6).build()
        repository = RoomFlavorRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
        context.deleteDatabase(databaseName)
    }

    @Test
    fun validCreationSurvivesDatabaseReopen() = runBlocking {
        repository.save(null, "  Coco   Cremoso ", "5,50")
        val saved = repository.observeAll().first().single()
        assertEquals("Coco Cremoso", saved.name)
        assertEquals(550L, saved.defaultPriceCents)
        assertTrue(saved.active)
        assertTrue(saved.id > 0)
        database.close()
        openDatabase()
        assertEquals(saved, repository.observeAll().first().single())
    }

    @Test
    fun validationFailuresDoNotPersistAndPriceIsOptional() = runBlocking {
        for (name in listOf("", "  ", "\t\n")) {
            expectValidation { repository.save(null, name, "") }
        }
        for (price in listOf("abc", "-1", "1,234", "92233720368547758,08")) {
            expectValidation { repository.save(null, "Coco", price) }
        }
        assertTrue(repository.observeAll().first().isEmpty())
        repository.save(null, "Coco", "")
        assertNull(repository.observeAll().first().single().defaultPriceCents)
    }

    @Test
    fun rejectsDuplicatesOnCreationAndEditIncludingInactiveNames() = runBlocking {
        repository.save(null, "Coco Cremoso", "")
        repository.save(null, "Morango", "0")
        val flavors = repository.observeAll().first()
        val coco = flavors.first { it.name == "Coco Cremoso" }
        val morango = flavors.first { it.name == "Morango" }
        repository.setActive(coco.id, false)
        expectValidation { repository.save(null, " COCO   CREMOSO ", "1") }
        expectValidation { repository.save(morango.id, "coco cremoso", "2") }
        assertEquals("Morango", database.flavorDao().findById(morango.id)?.name)
        assertEquals(2, repository.observeAll().first().size)
    }

    @Test
    fun editAndStatusChangesPreserveIdAndDoNotDeleteRows() = runBlocking {
        repository.save(null, "Coco", "5")
        val original = repository.observeAll().first().single()
        repository.setActive(original.id, false)
        repository.save(original.id, " COCO   Cremoso ", "")
        val edited = repository.observeAll().first().single()
        assertEquals(original.id, edited.id)
        assertEquals("COCO Cremoso", edited.name)
        assertNull(edited.defaultPriceCents)
        assertFalse(edited.active)
        repository.save(original.id, "coco cremoso", "0,01")
        repository.setActive(original.id, true)
        val active = repository.observeAll().first().single()
        assertTrue(active.active)
        assertEquals(original.id, active.id)
        assertEquals(1L, active.defaultPriceCents)
    }

    @Test
    fun sqliteRejectsDuplicateNormalizedKeysOnInsertAndUpdate() = runBlocking {
        val dao = database.flavorDao()
        val cocoId = dao.insert(FlavorEntity(name = "Coco"))
        val morangoId = dao.insert(FlavorEntity(name = "Morango"))
        try {
            dao.insert(FlavorEntity(name = " COCO "))
            fail("SQLite deveria rejeitar a chave duplicada")
        } catch (_: SQLiteConstraintException) { }
        try {
            dao.updateDetails(morangoId, "COCO", "coco", 100L)
            fail("SQLite deveria rejeitar a edição duplicada")
        } catch (_: SQLiteConstraintException) { }
        assertEquals("Coco", dao.findById(cocoId)?.name)
        assertEquals("Morango", dao.findById(morangoId)?.name)
        assertEquals(2, dao.observeAll().first().size)
    }

    @Test
    fun concurrentDuplicateSubmissionsPersistOnlyOneRow() = runBlocking {
        val results = coroutineScope {
            listOf("Coco", " COCO ").map { name ->
                async { runCatching { repository.save(null, name, "5,50") } }
            }.map { it.await() }
        }
        assertEquals(1, results.count { it.isSuccess })
        assertEquals(1, results.count { it.exceptionOrNull() is FlavorValidationException })
        assertEquals(1, repository.observeAll().first().size)
    }

    @Test
    fun deletesActiveAndInactiveFlavorsAndSurvivesReopen() = runBlocking {
        for (active in listOf(true, false)) {
            repository.save(null, "Coco", "5")
            val id = repository.observeAll().first().single().id
            repository.setActive(id, active)
            repository.delete(id)
            assertNull(database.flavorDao().findById(id))
            assertTrue(repository.observeAll().first().isEmpty())
        }
        database.close()
        openDatabase()
        assertTrue(repository.observeAll().first().isEmpty())
    }

    @Test
    fun missingFlavorFailsAndRepeatedDeletionDoesNotReportSuccess() = runBlocking {
        repository.save(null, "Coco", "")
        val id = repository.observeAll().first().single().id
        repository.delete(id)
        try {
            repository.delete(id)
            fail("Missing flavor must fail")
        } catch (_: IllegalStateException) { }
        assertTrue(repository.observeAll().first().isEmpty())
    }

    @Test
    fun deletionParticipatesInTransactionAndRollsBackOnFailure() = runBlocking {
        repository.save(null, "Coco", "5")
        val original = repository.observeAll().first().single()
        try {
            database.withTransaction {
                repository.delete(original.id)
                assertNull(database.flavorDao().findById(original.id))
                error("Abort transaction")
            }
        } catch (_: IllegalStateException) { }
        assertEquals(original, repository.observeAll().first().single())
    }

    private suspend fun expectValidation(block: suspend () -> Unit) {
        try {
            block()
            fail("Era esperada uma rejeição de validação")
        } catch (_: FlavorValidationException) { }
    }
}
