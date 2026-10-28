package com.flstudios.a2lopes.presentation.stock

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.flstudios.a2lopes.domain.model.FlavorStock
import com.flstudios.a2lopes.domain.repository.StockRepository
import kotlinx.coroutines.flow.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StockViewModelTest {
    @Test fun readFailureCanBeRetriedAndZeroAndInactiveStockRemainVisible() {
        var fail = true
        val rows = listOf(FlavorStock(1, "Coco", true, 0), FlavorStock(2, "Uva", false, 12))
        val repository = object : StockRepository {
            override fun observeAll(): Flow<List<FlavorStock>> = if (fail) flow { error("Leitura indisponível") } else flowOf(rows)
            override fun observeQuantity(flavorId: Long) = flowOf(0L)
        }
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val store = ViewModelStore()
        try {
            instrumentation.runOnMainSync {
                val vm = ViewModelProvider(store, StockViewModel.Factory(repository))[StockViewModel::class.java]
                assertNotNull(vm.state.value.error)
                assertFalse(vm.state.value.loading)
                fail = false
                vm.reload()
                assertNull(vm.state.value.error)
                assertEquals(rows, vm.state.value.flavors)
                assertEquals("12", vm.state.value.total)
                assertEquals(10L, vm.state.value.lowStockThreshold)
            }
        } finally { instrumentation.runOnMainSync { store.clear() } }
    }
}
