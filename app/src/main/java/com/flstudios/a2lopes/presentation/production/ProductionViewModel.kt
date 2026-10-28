package com.flstudios.a2lopes.presentation.production

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.flstudios.a2lopes.domain.model.Flavor
import com.flstudios.a2lopes.domain.model.ProductionRequest
import com.flstudios.a2lopes.domain.repository.FlavorRepository
import com.flstudios.a2lopes.domain.repository.ProductionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class ProductionReview(val request: ProductionRequest, val names: Map<Long, String>) {
    val total: String get() = totalUnits(request.items.map { it.quantity })
}

data class ProductionUiState(
    val loading: Boolean = true,
    val loadError: String? = null,
    val flavors: List<Flavor> = emptyList(),
    val quantities: Map<Long, String> = emptyMap(),
    val quantityErrors: Map<Long, String> = emptyMap(),
    val producedAt: Long,
    val notes: String = "",
    val review: ProductionReview? = null,
    val busy: Boolean = false,
    val outcomeUncertain: Boolean = false,
    val error: String? = null,
    val message: String? = null,
)

class ProductionViewModel(
    private val flavors: FlavorRepository,
    private val production: ProductionRepository,
    private val now: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ProductionUiState(producedAt = now()))
    val state = mutableState.asStateFlow()
    private var operationId = UUID.randomUUID().toString()
    private var observation: Job? = null

    init { reload() }

    fun reload() {
        observation?.cancel()
        mutableState.update { it.copy(loading = true, loadError = null) }
        observation = viewModelScope.launch {
            try {
                flavors.observeAll().collect { list ->
                    mutableState.update { it.copy(flavors = list.filter { flavor -> flavor.active }, loading = false, loadError = null) }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                mutableState.update { it.copy(loading = false, loadError = "Não foi possível carregar os sabores. Tente novamente.") }
            }
        }
    }

    private fun canEdit() = !state.value.busy && state.value.review == null

    fun changeQuantity(id: Long, value: String) {
        if (!canEdit()) return
        mutableState.update { it.copy(quantities = it.quantities + (id to value), quantityErrors = it.quantityErrors - id, error = null, message = null) }
    }

    fun changeNotes(value: String) {
        if (canEdit()) mutableState.update { it.copy(notes = value, error = null, message = null) }
    }

    fun selectDay(day: Long) {
        if (canEdit()) mutableState.update { it.copy(producedAt = ProductionDates.withPickerDay(it.producedAt, day), error = null, message = null) }
    }

    fun review() {
        val snapshot = state.value
        if (!canEdit() || snapshot.loading || snapshot.loadError != null) return
        val parsed = parseProductionQuantities(snapshot.flavors, snapshot.quantities)
        if (parsed.error != null) {
            mutableState.update { it.copy(quantityErrors = parsed.errors, error = parsed.error, message = null) }
            return
        }
        val request = ProductionRequest(operationId, snapshot.producedAt, parsed.items, snapshot.notes.trim().ifEmpty { null })
        mutableState.update {
            it.copy(review = ProductionReview(request, snapshot.flavors.associate { flavor -> flavor.id to flavor.name }), error = null, message = null)
        }
    }

    fun backToEdit() {
        if (!state.value.busy && !state.value.outcomeUncertain) {
            mutableState.update { it.copy(review = null, error = null) }
        }
    }

    fun confirm() {
        val snapshot = state.value
        val review = snapshot.review ?: return
        if (snapshot.busy) return
        mutableState.update { it.copy(busy = true, error = null, message = null) }
        viewModelScope.launch {
            try {
                production.record(review.request)
                operationId = UUID.randomUUID().toString()
                mutableState.update {
                    it.copy(quantities = emptyMap(), quantityErrors = emptyMap(), notes = "", producedAt = now(),
                        review = null, outcomeUncertain = false, message = "Produção registrada. Estoque atualizado.")
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: IllegalArgumentException) {
                // O repositório rejeitou a entrada sem commit. É seguro voltar e corrigir.
                mutableState.update { it.copy(error = exception.message ?: "Confira os dados da produção.", outcomeUncertain = false) }
            } catch (_: Exception) {
                // Uma falha de resposta pode ocorrer após o commit: nunca recriar o pedido neste caso.
                mutableState.update {
                    it.copy(error = "Não foi possível confirmar o registro. Tente confirmar novamente para verificar a produção.", outcomeUncertain = true)
                }
            } finally {
                mutableState.update { it.copy(busy = false) }
            }
        }
    }

    class Factory(private val flavors: FlavorRepository, private val production: ProductionRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ProductionViewModel::class.java))
            @Suppress("UNCHECKED_CAST")
            return ProductionViewModel(flavors, production) as T
        }
    }
}
