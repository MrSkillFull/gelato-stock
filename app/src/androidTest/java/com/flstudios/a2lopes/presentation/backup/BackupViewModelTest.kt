package com.flstudios.a2lopes.presentation.backup

import androidx.lifecycle.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.flstudios.a2lopes.domain.repository.*
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.*

@RunWith(AndroidJUnit4::class)
class BackupViewModelTest {
    private val document = object : BackupDocument {
        override val summary = BackupSummary(1000, 0, 0, 0, 0, 0, 0)
    }
    private class Repository(val document: BackupDocument) : BackupRepository {
        val completion = CompletableDeferred<Unit>()
        var restores = 0
        var fail = false
        override suspend fun inspect(open: () -> InputStream?) = document
        override suspend fun export(open: () -> OutputStream?) { throw IOException("write") }
        override suspend fun restore(data: BackupDocument) {
            restores++
            if (fail) throw IOException("restore")
            completion.await()
        }
    }
    private fun onMain(repository: BackupRepository, block: (BackupViewModel) -> Unit) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val store = ViewModelStore()
        try {
            instrumentation.runOnMainSync {
                block(ViewModelProvider(store, BackupViewModel.Factory(repository))[BackupViewModel::class.java])
            }
        } finally { instrumentation.runOnMainSync { store.clear() } }
    }
    @Test fun inspectionNeverRestoresAndCancelDiscardsDocument() {
        val repository = Repository(document)
        onMain(repository) { vm ->
            assertTrue(vm.beginSelection())
            assertFalse(vm.beginSelection())
            vm.cancelSelection()
            assertFalse(vm.state.value.selecting)
            vm.inspect { null }
            assertSame(document, vm.state.value.pending)
            assertEquals(0, repository.restores)
            vm.cancelRestore()
            vm.confirmRestore()
            assertNull(vm.state.value.pending)
            assertEquals(0, repository.restores)
        }
    }
    @Test fun doubleConfirmationAndCancelAreBlockedUntilCommitCompletes() {
        val repository = Repository(document)
        onMain(repository) { vm ->
            vm.inspect { null }
            vm.confirmRestore()
            vm.confirmRestore()
            vm.cancelRestore()
            assertEquals(1, repository.restores)
            assertTrue(vm.state.value.busy)
            assertSame(document, vm.state.value.pending)
            assertFalse(vm.state.value.restored)
            repository.completion.complete(Unit)
            assertFalse(vm.state.value.busy)
            assertTrue(vm.state.value.restored)
            assertNull(vm.state.value.pending)
        }
    }
    @Test fun failuresClearPreviousSuccessAndAllowRetryWithoutClaimingRestored() {
        val repository = Repository(document).also { it.fail = true }
        onMain(repository) { vm ->
            vm.inspect { null }
            vm.confirmRestore()
            assertFalse(vm.state.value.restored)
            assertFalse(vm.state.value.busy)
            assertTrue(vm.state.value.message!!.contains("Não foi possível"))
            assertSame(document, vm.state.value.pending)
            repository.fail = false
            repository.completion.complete(Unit)
            vm.confirmRestore()
            assertTrue(vm.state.value.restored)
        }
        onMain(Repository(document)) { vm ->
            vm.export { null }
            assertTrue(vm.state.value.message!!.contains("Não foi possível"))
            assertFalse(vm.state.value.restored)
        }
    }
}
