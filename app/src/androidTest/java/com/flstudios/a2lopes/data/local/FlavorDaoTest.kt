package com.flstudios.a2lopes.data.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.flstudios.a2lopes.data.local.entity.FlavorEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FlavorDaoTest {
    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java,
        ).build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertGeneratesDistinctIdsAndPreservesOptionalPrice() = runBlocking {
        val dao = database.flavorDao()
        assertTrue(dao.observeAll().first().isEmpty())
        val first = FlavorEntity(name = "Coco")
        val second = FlavorEntity(name = "Morango", defaultPriceCents = 550L)

        val firstId = dao.insert(first)
        val secondId = dao.insert(second)

        assertTrue(firstId > 0)
        assertTrue(secondId > firstId)
        assertEquals(first.copy(id = firstId), dao.findById(firstId))
        assertEquals(second.copy(id = secondId), dao.findById(secondId))
        assertEquals(2, dao.observeAll().first().size)
    }

    @Test
    fun updatePreservesIdAndDoesNotInsertMissingRow() = runBlocking {
        val dao = database.flavorDao()
        val id = dao.insert(FlavorEntity(name = "Coco", defaultPriceCents = 500L))
        val updated = FlavorEntity(id, "Coco cremoso", null, false)

        assertEquals(1, dao.update(updated))
        assertEquals(updated, dao.findById(id))
        assertEquals(0, dao.update(updated.copy(id = id + 1)))
        assertEquals(listOf(updated), dao.observeAll().first())
    }
}
