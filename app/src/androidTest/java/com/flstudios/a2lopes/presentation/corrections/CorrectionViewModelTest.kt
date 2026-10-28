package com.flstudios.a2lopes.presentation.corrections

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.flstudios.a2lopes.domain.model.*
import com.flstudios.a2lopes.domain.repository.CorrectionRepository
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CorrectionViewModelTest {
    private fun onMain(repository: CorrectionRepository, block: (CorrectionViewModel) -> Unit) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val store = ViewModelStore()
        try {
            instrumentation.runOnMainSync {
                block(ViewModelProvider(store, CorrectionViewModel.Factory(repository))[CorrectionViewModel::class.java])
            }
        } finally { instrumentation.runOnMainSync { store.clear() } }
    }

    @Test fun validationPrecedesWritingAndPendingSubmissionBlocksDoubleTapAndEditing() {
        val requests = mutableListOf<CorrectionRequest>()
        val completion = CompletableDeferred<CorrectionReceipt>()
        val repository = object : CorrectionRepository {
            override suspend fun record(request: CorrectionRequest): CorrectionReceipt { requests += request; return completion.await() }
        }
        onMain(repository) { vm ->
            vm.openStock(CorrectionType.LOSS, FlavorStock(1, "Coco", true, 5))
            vm.confirm()
            assertTrue(requests.isEmpty())
            vm.changeQuantity("2")
            vm.confirm()
            assertTrue(requests.isEmpty())
            vm.changeReason("  Embalagem   rompida  ")
            vm.confirm()
            vm.confirm()
            assertEquals(1, requests.size)
            assertEquals("Embalagem rompida", requests.single().reason)
            assertTrue(vm.state.value.busy)
            vm.changeQuantity("3")
            vm.dismiss()
            assertEquals("2", vm.state.value.quantity)
            assertEquals(CorrectionType.LOSS, vm.state.value.type)
            completion.complete(CorrectionReceipt(requests.single().operationId, 1, true))
            assertNull(vm.state.value.type)
            assertFalse(vm.state.value.busy)
            assertNotNull(vm.state.value.message)
        }
    }

    @Test fun uncertainResultFreezesExactRequestAndAllowsIdempotentRetryEvenAfterClosingIsAttempted() {
        val requests = mutableListOf<CorrectionRequest>()
        val repository = object : CorrectionRepository {
            override suspend fun record(request: CorrectionRequest): CorrectionReceipt {
                requests += request
                if (requests.size == 1) error("Resultado incerto após gravação")
                return CorrectionReceipt(request.operationId, 1, true)
            }
        }
        onMain(repository) { vm ->
            vm.openReversal(HistoryDetail("p", HistoryKind.PRODUCTION, 1, 2, null, emptyList()))
            vm.changeReason("Erro")
            vm.confirm()
            assertTrue(vm.state.value.uncertain)
            vm.changeReason("Outro")
            vm.dismiss()
            assertEquals("Erro", vm.state.value.reason)
            assertEquals("p", vm.state.value.sourceId)
            vm.confirm()
            assertEquals(2, requests.size)
            assertEquals(requests.first(), requests.last())
            assertNull(vm.state.value.type)
        }
    }

    @Test fun knownRejectionKeepsFormEditableAndZeroCountReportsNoMovement() {
        var reject = true
        val requests = mutableListOf<CorrectionRequest>()
        val repository = object : CorrectionRepository {
            override suspend fun record(request: CorrectionRequest): CorrectionReceipt {
                requests += request
                if (reject) throw CorrectionRejected("Saldo insuficiente")
                return CorrectionReceipt(request.operationId, 1, false)
            }
        }
        onMain(repository) { vm ->
            vm.openStock(CorrectionType.ADJUSTMENT, FlavorStock(1, "Coco", false, 0))
            vm.changeQuantity("2")
            vm.changeReason("Contagem")
            vm.confirm()
            assertTrue(vm.state.value.editable)
            assertEquals("Saldo insuficiente", vm.state.value.error)
            reject = false
            vm.changeQuantity("0")
            vm.confirm()
            assertEquals(0L, requests.last().quantity)
            assertEquals(requests.first().operationId, requests.last().operationId)
            assertTrue(vm.state.value.message!!.contains("nenhum movimento"))
        }
    }
}
