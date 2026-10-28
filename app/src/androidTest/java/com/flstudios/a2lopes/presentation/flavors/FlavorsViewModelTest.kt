package com.flstudios.a2lopes.presentation.flavors

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.flstudios.a2lopes.domain.model.Flavor
import com.flstudios.a2lopes.domain.model.FlavorHistoryException
import com.flstudios.a2lopes.domain.repository.FlavorRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FlavorsViewModelTest {
    @Test
    fun historicalDeletionShowsDeactivationGuidanceWithoutSuccess() {
        val flavor = Flavor(1, "Coco", null, true)
        val repository = object : FlavorRepository {
            override fun observeAll() = flowOf(listOf(flavor))
            override suspend fun save(id: Long?, name: String, price: String) = Unit
            override suspend fun setActive(id: Long, active: Boolean) = Unit
            override suspend fun delete(id: Long) { throw FlavorHistoryException() }
        }
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val store = ViewModelStore()
        try {
            instrumentation.runOnMainSync {
                val vm = ViewModelProvider(store, FlavorsViewModel.Factory(repository))[FlavorsViewModel::class.java]
                vm.requestDeletion(flavor)
                vm.confirmDeletion()
                assertEquals(FlavorHistoryException().message, vm.state.value.message)
                assertEquals(listOf(flavor), vm.state.value.flavors)
                assertNull(vm.state.value.pendingDeletion)
                assertFalse(vm.state.value.busy)
            }
        } finally { instrumentation.runOnMainSync { store.clear() } }
    }

    @Test
    fun doubleSubmitIsIgnoredWhileSavingAndStateSurvivesAnotherProvider() {
        val gate = CompletableDeferred<Unit>()
        var saves = 0
        val repository = object : FlavorRepository {
            override fun observeAll() = flowOf(emptyList<Flavor>())
            override suspend fun save(id: Long?, name: String, price: String) {
                saves++
                gate.await()
            }
            override suspend fun delete(id: Long) = Unit
            override suspend fun setActive(id: Long, active: Boolean) = Unit
        }
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val store = ViewModelStore()
        try {
            instrumentation.runOnMainSync {
                val factory = FlavorsViewModel.Factory(repository)
                val viewModel = ViewModelProvider(store, factory)[FlavorsViewModel::class.java]
                viewModel.openForm()
                viewModel.changeName("Coco")
                viewModel.changePrice("5,50")
                viewModel.save()
                viewModel.save()
                viewModel.closeForm()
                assertTrue(viewModel.state.value.busy)
                assertNotNull(viewModel.state.value.form)
                assertEquals(1, saves)
                assertSame(viewModel, ViewModelProvider(store, factory)[FlavorsViewModel::class.java])
                gate.complete(Unit)
                assertFalse(viewModel.state.value.busy)
                assertNull(viewModel.state.value.form)
                assertEquals("Sabor cadastrado.", viewModel.state.value.message)
            }
        } finally {
            instrumentation.runOnMainSync { store.clear() }
        }
    }
    @Test
    fun deletionFailureClearsPreviousSuccessAndDoubleConfirmationIsIgnored() {
        val gate = CompletableDeferred<Unit>()
        var deletes = 0
        val repository = object : FlavorRepository {
            override fun observeAll() = flowOf(listOf(Flavor(1, "Coco", null, true)))
            override suspend fun save(id: Long?, name: String, price: String) = Unit
            override suspend fun setActive(id: Long, active: Boolean) = Unit
            override suspend fun delete(id: Long) {
                deletes++
                gate.await()
                error("Storage failure")
            }
        }
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val store = ViewModelStore()
        try {
            instrumentation.runOnMainSync {
                val vm = ViewModelProvider(store, FlavorsViewModel.Factory(repository))[FlavorsViewModel::class.java]
                val flavor = Flavor(1, "Coco", null, true)
                vm.setActive(flavor)
                assertNotNull(vm.state.value.message)
                vm.requestDeletion(flavor)
                assertNull(vm.state.value.message)
                vm.confirmDeletion()
                vm.confirmDeletion()
                vm.cancelDeletion()
                assertTrue(vm.state.value.busy)
                assertNotNull(vm.state.value.pendingDeletion)
                assertEquals(1, deletes)
                gate.complete(Unit)
                assertFalse(vm.state.value.busy)
                assertNull(vm.state.value.pendingDeletion)
                assertTrue(vm.state.value.message!!.contains("Tente novamente"))
                assertEquals(listOf(flavor), vm.state.value.flavors)
            }
        } finally {
            instrumentation.runOnMainSync { store.clear() }
        }
    }

}
