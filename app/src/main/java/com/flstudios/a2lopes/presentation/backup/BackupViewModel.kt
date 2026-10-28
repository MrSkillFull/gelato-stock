package com.flstudios.a2lopes.presentation.backup

import androidx.lifecycle.*
import com.flstudios.a2lopes.domain.model.BackupInvalid
import com.flstudios.a2lopes.domain.repository.BackupDocument
import com.flstudios.a2lopes.domain.repository.BackupRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream

data class BackupState(
    val busy: Boolean = false,
    val selecting: Boolean = false,
    val pending: BackupDocument? = null,
    val message: String? = null,
    val restored: Boolean = false,
)

class BackupViewModel(private val repository: BackupRepository) : ViewModel() {
    private val mutable = MutableStateFlow(BackupState())
    val state = mutable.asStateFlow()
    fun beginSelection(): Boolean {
        if (mutable.value.busy || mutable.value.selecting || mutable.value.pending != null) return false
        mutable.value = BackupState(selecting = true)
        return true
    }
    fun cancelSelection() { mutable.update { it.copy(selecting = false) } }
    fun selectionFailed() { mutable.value = BackupState(message = "Não foi possível abrir o seletor de arquivos.") }
    fun cancelRestore() { if (!mutable.value.busy) mutable.update { it.copy(pending = null) } }
    private fun run(success: String, action: suspend () -> Unit) {
        if (mutable.value.busy) return
        mutable.update { it.copy(busy = true, selecting = false, message = null) }
        viewModelScope.launch {
            try {
                action()
                mutable.update { it.copy(message = success) }
            } catch (e: CancellationException) { throw e }
            catch (e: BackupInvalid) { mutable.update { it.copy(message = e.message) } }
            catch (_: Exception) { mutable.update { it.copy(message = "Não foi possível concluir a operação. Confira o arquivo e o armazenamento e tente novamente.") } }
            finally { mutable.update { it.copy(busy = false) } }
        }
    }
    fun export(open: () -> OutputStream?) = run("Backup exportado com sucesso.") { repository.export(open) }
    fun inspect(open: () -> InputStream?) = run("Confira o resumo antes de restaurar.") {
        val data = repository.inspect(open)
        mutable.update { it.copy(pending = data) }
    }
    fun confirmRestore() {
        val data = mutable.value.pending ?: return
        run("Backup restaurado com sucesso.") {
            repository.restore(data)
            mutable.update { it.copy(pending = null, restored = true) }
        }
    }
    class Factory(private val repository: BackupRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(BackupViewModel::class.java))
            @Suppress("UNCHECKED_CAST") return BackupViewModel(repository) as T
        }
    }
}
