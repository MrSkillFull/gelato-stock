package com.flstudios.a2lopes.presentation.corrections

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.flstudios.a2lopes.domain.model.*
import com.flstudios.a2lopes.domain.repository.CorrectionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class CorrectionUiState(
    val type: CorrectionType? = null,
    val targetName: String = "",
    val flavorId: Long? = null,
    val sourceId: String? = null,
    val operationId: String = "",
    val quantity: String = "",
    val reason: String = "",
    val busy: Boolean = false,
    val uncertain: Boolean = false,
    val error: String? = null,
    val message: String? = null,
) { val editable: Boolean get() = !busy && !uncertain }

class CorrectionViewModel(private val repository: CorrectionRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(CorrectionUiState())
    val state = mutableState.asStateFlow()
    private var pending: CorrectionRequest? = null

    fun openStock(type: CorrectionType, flavor: FlavorStock) {
        if (state.value.type != null) return
        require(type == CorrectionType.LOSS || type == CorrectionType.ADJUSTMENT)
        mutableState.value = CorrectionUiState(type = type, targetName = flavor.name,
            flavorId = flavor.flavorId, operationId = UUID.randomUUID().toString())
    }
    fun openReversal(detail: HistoryDetail) {
        if (state.value.type != null || detail.reversalId != null) return
        val type = when (detail.kind) {
            HistoryKind.SALE -> CorrectionType.SALE_REVERSAL
            HistoryKind.PRODUCTION -> CorrectionType.PRODUCTION_REVERSAL
            else -> return
        }
        mutableState.value = CorrectionUiState(type = type, sourceId = detail.id,
            targetName = "#${detail.id}", operationId = UUID.randomUUID().toString())
    }
    fun changeQuantity(value: String) { if (state.value.editable) mutableState.update { it.copy(quantity = value, error = null) } }
    fun changeReason(value: String) { if (state.value.editable) mutableState.update { it.copy(reason = value, error = null) } }
    fun dismiss() {
        if (!state.value.editable) return
        pending = null
        mutableState.value = CorrectionUiState()
    }
    fun clearMessage() { mutableState.update { it.copy(message = null) } }

    fun confirm() {
        val snapshot = state.value
        if (snapshot.busy || snapshot.type == null) return
        val input = if (snapshot.uncertain) pending!! else {
            try {
                val quantity = if (snapshot.flavorId != null) {
                    val text = snapshot.quantity.trim()
                    require(text.isNotEmpty() && text.all { it in '0'..'9' }) { "Informe uma quantidade inteira válida." }
                    requireNotNull(text.toLongOrNull()) { "A quantidade ultrapassa o limite permitido." }
                } else null
                CorrectionRequest(snapshot.operationId, snapshot.type, snapshot.reason, snapshot.flavorId, quantity, snapshot.sourceId).normalized()
            } catch (e: IllegalArgumentException) {
                mutableState.update { it.copy(error = e.message) }
                return
            }
        }
        pending = input
        mutableState.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try {
                val receipt = repository.record(input)
                pending = null
                mutableState.value = CorrectionUiState(message = if (!receipt.changedStock)
                    "Conferência registrada. A contagem corresponde ao saldo; nenhum movimento foi criado."
                    else "Operação registrada. Estoque e histórico atualizados.")
            } catch (e: CancellationException) { throw e
            } catch (e: CorrectionRejected) {
                pending = null
                mutableState.update { it.copy(busy = false, uncertain = false, error = e.message) }
            } catch (_: Exception) {
                mutableState.update { it.copy(busy = false, uncertain = true,
                    error = "Não foi possível confirmar o resultado. Repita a mesma confirmação antes de editar ou fechar; a operação não será duplicada.") }
            }
        }
    }

    class Factory(private val repository: CorrectionRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(CorrectionViewModel::class.java))
            @Suppress("UNCHECKED_CAST")
            return CorrectionViewModel(repository) as T
        }
    }
}
