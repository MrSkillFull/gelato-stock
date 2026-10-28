package com.flstudios.a2lopes.presentation.production

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.flstudios.a2lopes.domain.model.*
import com.flstudios.a2lopes.domain.repository.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProductionViewModelTest {
    private val store = ViewModelStore()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val flavors = object : FlavorRepository {
        override fun observeAll() = flowOf(listOf(Flavor(1, "Coco", null, true), Flavor(2, "Morango", null, true), Flavor(3, "Uva", null, false)))
        override suspend fun save(id: Long?, name: String, price: String) = Unit
        override suspend fun setActive(id: Long, active: Boolean) = Unit
        override suspend fun delete(id: Long) = Unit
    }

    @After fun tearDown() { instrumentation.runOnMainSync { store.clear() } }

    @Test fun reviewDoesNotWriteAndDoubleConfirmationIsBlockedAcrossProviders() {
        val gate = CompletableDeferred<Unit>()
        val requests = mutableListOf<ProductionRequest>()
        val repository = object : ProductionRepository {
            override suspend fun record(request: ProductionRequest): ProductionSession {
                requests += request
                gate.await()
                return ProductionSession(request.operationId, request.producedAt, 2000, request.notes)
            }
        }
        instrumentation.runOnMainSync {
            val factory = ProductionViewModel.Factory(flavors, repository)
            val vm = ViewModelProvider(store, factory)[ProductionViewModel::class.java]
            assertEquals(listOf(1L, 2L), vm.state.value.flavors.map { it.id })
            vm.changeQuantity(1, "12")
            vm.changeQuantity(2, " ")
            vm.changeNotes("Lote de teste")
            vm.review()
            assertEquals(0, requests.size)
            assertEquals("12", vm.state.value.review!!.total)
            val request = vm.state.value.review!!.request
            assertEquals(listOf(ProductionQuantity(1, 12)), request.items)
            vm.backToEdit()
            assertEquals("12", vm.state.value.quantities[1])
            vm.changeQuantity(2, "3")
            vm.review()
            vm.confirm()
            vm.confirm()
            vm.backToEdit()
            vm.changeNotes("Não deve alterar o envio")
            assertEquals(1, requests.size)
            assertTrue(vm.state.value.busy)
            assertSame(vm, ViewModelProvider(store, factory)[ProductionViewModel::class.java])
            assertEquals("Lote de teste", requests.single().notes)
            assertNotNull(vm.state.value.review)
            gate.complete(Unit)
            assertFalse(vm.state.value.busy)
            assertNull(vm.state.value.review)
            assertTrue(vm.state.value.quantities.isEmpty())
            assertEquals("", vm.state.value.notes)
            assertEquals("Produção registrada. Estoque atualizado.", vm.state.value.message)
            vm.confirm()
            assertEquals(1, requests.size)
            vm.changeQuantity(1, "1")
            vm.review()
            assertNotEquals(request.operationId, vm.state.value.review!!.request.operationId)
        }
    }

    @Test fun uncertainResponseKeepsExactRequestAndDataForRetry() {
        val requests = mutableListOf<ProductionRequest>()
        val repository = object : ProductionRepository {
            override suspend fun record(request: ProductionRequest): ProductionSession {
                requests += request
                if (requests.size == 1) error("Resposta indisponível após registro")
                return ProductionSession(request.operationId, request.producedAt, 2000, request.notes)
            }
        }
        instrumentation.runOnMainSync {
            val vm = ViewModelProvider(store, ProductionViewModel.Factory(flavors, repository))[ProductionViewModel::class.java]
            vm.changeQuantity(1, "8")
            vm.changeNotes("Teste")
            vm.review()
            val review = vm.state.value.review!!
            vm.confirm()
            assertEquals("8", vm.state.value.quantities[1])
            assertEquals("Teste", vm.state.value.notes)
            assertNull(vm.state.value.message)
            assertNotNull(vm.state.value.error)
            assertTrue(vm.state.value.outcomeUncertain)
            vm.backToEdit()
            vm.changeQuantity(1, "99")
            vm.review()
            assertEquals(review, vm.state.value.review)
            vm.confirm()
            assertEquals(2, requests.size)
            assertEquals(requests[0], requests[1])
            assertNotNull(vm.state.value.message)
        }
    }

    @Test fun validationFailureAllowsEditingAndInvalidFieldsNeverReachRepository() {
        var attempts = 0
        val repository = object : ProductionRepository {
            override suspend fun record(request: ProductionRequest): ProductionSession {
                attempts++
                throw IllegalArgumentException("Quantidade acima do limite. Reduza a quantidade.")
            }
        }
        instrumentation.runOnMainSync {
            val vm = ViewModelProvider(store, ProductionViewModel.Factory(flavors, repository))[ProductionViewModel::class.java]
            vm.review()
            assertNull(vm.state.value.review)
            assertNotNull(vm.state.value.error)
            vm.changeQuantity(1, "0")
            vm.review()
            assertNotNull(vm.state.value.quantityErrors[1])
            assertEquals(0, attempts)
            vm.changeQuantity(1, "4")
            vm.review()
            vm.confirm()
            assertFalse(vm.state.value.outcomeUncertain)
            assertEquals("4", vm.state.value.quantities[1])
            assertNull(vm.state.value.message)
            vm.backToEdit()
            assertNull(vm.state.value.review)
            vm.changeQuantity(1, "2")
            assertEquals("2", vm.state.value.quantities[1])
        }
    }

    @Test fun loadingFailureCanBeRetriedWithoutDiscardingDraft() {
        var fail = true
        val repository = object : FlavorRepository by flavors {
            override fun observeAll(): Flow<List<Flavor>> = if (fail) flow { error("Falha de leitura") } else flavors.observeAll()
        }
        val production = object : ProductionRepository {
            override suspend fun record(request: ProductionRequest): ProductionSession = error("Não deveria gravar")
        }
        instrumentation.runOnMainSync {
            val vm = ViewModelProvider(store, ProductionViewModel.Factory(repository, production))[ProductionViewModel::class.java]
            assertNotNull(vm.state.value.loadError)
            vm.changeNotes("Preservar")
            vm.review()
            assertNull(vm.state.value.review)
            fail = false
            vm.reload()
            assertNull(vm.state.value.loadError)
            assertEquals("Preservar", vm.state.value.notes)
            assertEquals(2, vm.state.value.flavors.size)
        }
    }
}
