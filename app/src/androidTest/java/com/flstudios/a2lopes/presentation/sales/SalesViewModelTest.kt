package com.flstudios.a2lopes.presentation.sales

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.flstudios.a2lopes.domain.model.*
import com.flstudios.a2lopes.domain.repository.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SalesViewModelTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val store = ViewModelStore()
    private val flavors = MutableStateFlow(listOf(Flavor(1, "Coco", 550, true), Flavor(2, "Morango", null, true), Flavor(3, "Uva", 100, false)))
    private val balances = MutableStateFlow(listOf(FlavorStock(1, "Coco", true, 2), FlavorStock(2, "Morango", true, 3), FlavorStock(3, "Uva", false, 9)))
    private val flavorRepository = object : FlavorRepository {
        override fun observeAll() = flavors
        override suspend fun save(id: Long?, name: String, price: String) = Unit
        override suspend fun setActive(id: Long, active: Boolean) = Unit
        override suspend fun delete(id: Long) = Unit
    }
    private val stockRepository = object : StockRepository {
        override fun observeAll() = balances
        override fun observeQuantity(flavorId: Long) = balances.map { list -> list.firstOrNull { it.flavorId == flavorId }?.quantity ?: 0 }
    }
    private lateinit var factory: SalesViewModel.Factory

    private fun create(record: suspend (SaleRequest) -> Sale): SalesViewModel {
        val repository = object : SaleRepository { override suspend fun record(request: SaleRequest) = record.invoke(request) }
        factory = SalesViewModel.Factory(flavorRepository, stockRepository, repository)
        lateinit var vm: SalesViewModel
        instrumentation.runOnMainSync { vm = ViewModelProvider(store, factory)[SalesViewModel::class.java] }
        instrumentation.waitForIdleSync()
        return vm
    }
    @After fun tearDown() { instrumentation.runOnMainSync { store.clear() } }

    @Test fun quantitiesRespectStockAndCartKeepsChangedAvailabilityForCorrection() {
        var calls = 0
        val vm = create { calls++; Sale(it.operationId, it.soldAt, 2000) }
        instrumentation.runOnMainSync {
            assertEquals(listOf(1L, 2L), vm.state.value.products.map { it.id })
            assertFalse(vm.state.value.canRegister)
            vm.increment(1); vm.increment(1); vm.increment(1); vm.increment(3)
            assertEquals(2L, vm.state.value.cart[1]!!.quantity)
            assertFalse(vm.state.value.cart.containsKey(3))
            assertEquals(1100L, vm.state.value.financialTotal.cents)
            vm.increment(2)
            assertEquals("3", vm.state.value.totalUnits)
            assertNull(vm.state.value.financialTotal.cents)
            vm.decrement(2)
            assertEquals(0, calls)
            balances.value = balances.value.map { if (it.flavorId == 1L) it.copy(quantity = 1) else it }
        }
        instrumentation.waitForIdleSync()
        instrumentation.runOnMainSync {
            assertEquals(2L, vm.state.value.cart[1]!!.quantity)
            assertFalse(vm.state.value.canRegister)
            assertNotNull(vm.state.value.error)
            vm.register()
            assertEquals(0, calls)
            vm.decrement(1)
            assertTrue(vm.state.value.canRegister)
            vm.register()
            assertEquals(1, calls)
        }
    }

    @Test fun duplicateTouchesAreIgnoredAndCompletedSaleGetsNewKey() {
        val gate = CompletableDeferred<Unit>()
        val requests = mutableListOf<SaleRequest>()
        val vm = create { request -> requests += request; gate.await(); Sale(request.operationId, request.soldAt, 2000) }
        instrumentation.runOnMainSync {
            vm.increment(1)
            assertEquals(0, requests.size)
            vm.register(); vm.register()
            vm.increment(2); vm.decrement(1)
            assertEquals(1, requests.size)
            assertEquals(1L, vm.state.value.cart[1]!!.quantity)
            assertTrue(vm.state.value.busy)
            assertFalse(vm.state.value.canRegister)
            assertSame(vm, ViewModelProvider(store, factory)[SalesViewModel::class.java])
            gate.complete(Unit)
            assertTrue(vm.state.value.cart.isEmpty())
            assertNull(vm.state.value.pendingRequest)
            assertEquals("Venda registrada. Estoque atualizado.", vm.state.value.message)
            vm.register()
            assertEquals(1, requests.size)
            vm.increment(1)
            vm.register()
            assertEquals(2, requests.size)
            assertNotEquals(requests[0].operationId, requests[1].operationId)
        }
    }

    @Test fun uncertainResponsePreservesExactRequestEvenAfterStockDisappears() {
        val requests = mutableListOf<SaleRequest>()
        val vm = create { request ->
            requests += request
            if (requests.size == 1) error("Resposta perdida após commit")
            Sale(request.operationId, request.soldAt, 2000)
        }
        instrumentation.runOnMainSync {
            vm.increment(1); vm.increment(1)
            vm.register()
            assertTrue(vm.state.value.outcomeUncertain)
            assertNull(vm.state.value.message)
            assertNotNull(vm.state.value.error)
            balances.value = balances.value.map { if (it.flavorId == 1L) it.copy(quantity = 0) else it }
            flavors.value = flavors.value.map { if (it.id == 1L) it.copy(defaultPriceCents = 800) else it }
        }
        instrumentation.waitForIdleSync()
        instrumentation.runOnMainSync {
            assertTrue(vm.state.value.products.none { it.id == 1L })
            assertTrue(vm.state.value.canRegister)
            vm.decrement(1); vm.increment(2)
            assertEquals(2L, vm.state.value.cart[1]!!.quantity)
            assertEquals(550L, vm.state.value.cart[1]!!.product.unitPriceCents)
            vm.register()
            assertEquals(2, requests.size)
            assertEquals(requests[0], requests[1])
            assertTrue(vm.state.value.cart.isEmpty())
            assertNotNull(vm.state.value.message)
        }
    }

    @Test fun rejectedSaleKeepsCartAllowsEditingAndUsesUpdatedQuote() {
        var calls = 0
        val requests = mutableListOf<SaleRequest>()
        val vm = create { request ->
            requests += request
            if (++calls == 1) throw IllegalArgumentException("Estoque alterado. Confira o carrinho.")
            Sale(request.operationId, request.soldAt, 2000)
        }
        instrumentation.runOnMainSync {
            vm.increment(1); vm.register()
            assertEquals(1L, vm.state.value.cart[1]!!.quantity)
            assertNull(vm.state.value.message)
            assertNull(vm.state.value.pendingRequest)
            assertTrue(vm.state.value.editable)
            flavors.value = flavors.value.map { if (it.id == 1L) it.copy(defaultPriceCents = 800) else it }
        }
        instrumentation.waitForIdleSync()
        instrumentation.runOnMainSync {
            assertEquals(800L, vm.state.value.financialTotal.cents)
            assertNotNull(vm.state.value.error)
            vm.register()
            assertEquals(800L, requests.last().items.single().unitPriceCents)
            assertNotNull(vm.state.value.message)
        }
    }
}
