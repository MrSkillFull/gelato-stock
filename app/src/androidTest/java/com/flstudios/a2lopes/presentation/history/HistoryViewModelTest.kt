package com.flstudios.a2lopes.presentation.history

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.flstudios.a2lopes.domain.model.*
import com.flstudios.a2lopes.domain.repository.HistoryRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HistoryViewModelTest {
    private class Reader : HistoryRepository {
        var failList = false
        var failDetail = false
        val requests = mutableListOf<Pair<HistoryFilter, Int>>()
        val delayed = CompletableDeferred<HistoryDetail?>()
        override fun observePage(filter: HistoryFilter, page: Int): Flow<HistoryPage> = flow {
            requests += filter to page
            if (failList) error("Falha")
            emit(HistoryPage(listOf(HistoryEntry("p$page", HistoryKind.PRODUCTION, 1, "2", 1)), page == 0))
        }
        override suspend fun detail(kind: HistoryKind, id: String): HistoryDetail? {
            if (failDetail) error("Falha")
            return if (id == "delayed") delayed.await() else if (id == "missing") null
                else HistoryDetail(id, kind, 1, 2, null, listOf(HistoryItem(1, "Coco", 2, null)))
        }
    }

    private fun onMain(block: (HistoryViewModel, Reader) -> Unit) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val store = ViewModelStore()
        try {
            instrumentation.runOnMainSync {
                val reader = Reader()
                val vm = ViewModelProvider(store, HistoryViewModel.Factory(reader))[HistoryViewModel::class.java]
                block(vm, reader)
            }
        } finally { instrumentation.runOnMainSync { store.clear() } }
    }

    @Test fun pagesAndFiltersRequestOnlyTheSelectedPageAndListFailureCanRetry() = onMain { vm, reader ->
        assertEquals(listOf(HistoryFilter.ALL to 0), reader.requests)
        vm.nextPage()
        assertEquals(1, vm.state.value.page)
        assertFalse(vm.state.value.hasNext)
        vm.nextPage()
        assertEquals(2, reader.requests.size)
        vm.previousPage()
        vm.nextPage()
        vm.selectFilter(HistoryFilter.MOVEMENTS)
        assertEquals(0, vm.state.value.page)
        assertEquals(HistoryFilter.MOVEMENTS to 0, reader.requests.last())
        reader.failList = true
        vm.reload()
        assertNotNull(vm.state.value.error)
        assertFalse(vm.state.value.loading)
        reader.failList = false
        vm.reload()
        assertNull(vm.state.value.error)
        assertEquals(1, vm.state.value.entries.size)
    }

    @Test fun detailsAreLoadedOnDemandAndFailuresAndMissingRecordsAreRecoverable() = onMain { vm, reader ->
        assertNull(vm.state.value.detail)
        reader.failDetail = true
        vm.openDetail(HistoryKind.SALE, "sale")
        assertNotNull(vm.state.value.detailError)
        assertFalse(vm.state.value.detailLoading)
        reader.failDetail = false
        vm.retryDetail()
        assertEquals("sale", vm.state.value.detail!!.id)
        assertNull(vm.state.value.detailError)
        vm.openDetail(HistoryKind.PRODUCTION, "missing")
        assertEquals("Registro não encontrado.", vm.state.value.detailError)
        vm.closeDetail()
        assertNull(vm.state.value.selectedId)
        assertNull(vm.state.value.detail)
    }

    @Test fun closingPendingDetailPreventsLateResultReplacingTheCurrentSelection() = onMain { vm, reader ->
        vm.openDetail(HistoryKind.PRODUCTION, "delayed")
        assertTrue(vm.state.value.detailLoading)
        vm.closeDetail()
        vm.openDetail(HistoryKind.SALE, "new")
        reader.delayed.complete(HistoryDetail("delayed", HistoryKind.PRODUCTION, 0, 1, null, emptyList()))
        assertEquals("new", vm.state.value.selectedId)
        assertEquals("new", vm.state.value.detail!!.id)
        assertFalse(vm.state.value.detailLoading)
    }
}
