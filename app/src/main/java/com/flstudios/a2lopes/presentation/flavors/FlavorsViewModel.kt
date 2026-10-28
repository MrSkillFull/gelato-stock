package com.flstudios.a2lopes.presentation.flavors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.flstudios.a2lopes.domain.model.Flavor
import com.flstudios.a2lopes.domain.model.FlavorDataException
import com.flstudios.a2lopes.domain.model.FlavorHistoryException
import com.flstudios.a2lopes.domain.model.FlavorField
import com.flstudios.a2lopes.domain.model.FlavorRules
import com.flstudios.a2lopes.domain.model.FlavorValidationException
import com.flstudios.a2lopes.domain.repository.FlavorRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FlavorForm(
    val id: Long? = null,
    val name: String = "",
    val price: String = "",
    val nameError: String? = null,
    val priceError: String? = null,
    val error: String? = null,
)

data class FlavorsUiState(
    val flavors: List<Flavor> = emptyList(),
    val loading: Boolean = true,
    val loadError: String? = null,
    val busy: Boolean = false,
    val form: FlavorForm? = null,
    val message: String? = null,
    val pendingDeletion: Flavor? = null,
)

class FlavorsViewModel(private val repository: FlavorRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(FlavorsUiState())
    val state = mutableState.asStateFlow()
    private var observation: Job? = null

    init { reload() }

    fun reload() {
        observation?.cancel()
        mutableState.update { it.copy(loading = true, loadError = null) }
        observation = viewModelScope.launch {
            try {
                repository.observeAll().collect { flavors ->
                    mutableState.update { it.copy(flavors = flavors, loading = false, loadError = null) }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                val dataError = generateSequence<Throwable>(exception) { it.cause }
                    .filterIsInstance<FlavorDataException>().firstOrNull()
                mutableState.update {
                    it.copy(loading = false, loadError = dataError?.message
                        ?: "Não foi possível carregar os sabores. Tente novamente.")
                }
            }
        }
    }

    fun openForm(flavor: Flavor? = null) {
        if (state.value.busy) return
        mutableState.update {
            it.copy(form = FlavorForm(
                id = flavor?.id, name = flavor?.name.orEmpty(),
                price = FlavorRules.priceText(flavor?.defaultPriceCents),
            ))
        }
    }

    fun closeForm() {
        if (!state.value.busy) mutableState.update { it.copy(form = null) }
    }

    fun changeName(value: String) {
        if (!state.value.busy) mutableState.update {
            it.copy(form = it.form?.copy(name = value, nameError = null, error = null))
        }
    }

    fun changePrice(value: String) {
        if (!state.value.busy) mutableState.update {
            it.copy(form = it.form?.copy(price = value, priceError = null, error = null))
        }
    }

    fun save() {
        val snapshot = state.value
        val form = snapshot.form ?: return
        if (snapshot.busy) return
        mutableState.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                repository.save(form.id, form.name, form.price)
                mutableState.update {
                    it.copy(form = null, message = if (form.id == null) "Sabor cadastrado." else "Sabor atualizado.")
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: FlavorValidationException) {
                mutableState.update {
                    it.copy(form = it.form?.copy(
                        nameError = exception.message.takeIf { exception.field == FlavorField.NAME },
                        priceError = exception.message.takeIf { exception.field == FlavorField.PRICE },
                    ))
                }
            } catch (_: Exception) {
                mutableState.update {
                    it.copy(form = it.form?.copy(error = "Não foi possível salvar. Tente novamente."))
                }
            } finally {
                mutableState.update { it.copy(busy = false) }
            }
        }
    }

    fun setActive(flavor: Flavor) {
        if (state.value.busy) return
        mutableState.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                repository.setActive(flavor.id, !flavor.active)
                mutableState.update {
                    it.copy(message = if (flavor.active) "Sabor desativado." else "Sabor reativado.")
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                mutableState.update { it.copy(message = "Não foi possível alterar o status. Tente novamente.") }
            } finally {
                mutableState.update { it.copy(busy = false) }
            }
        }
    }

    fun requestDeletion(flavor: Flavor) {
        if (!state.value.busy) mutableState.update {
            it.copy(pendingDeletion = flavor, message = null)
        }
    }

    fun cancelDeletion() {
        if (!state.value.busy) mutableState.update { it.copy(pendingDeletion = null) }
    }

    fun confirmDeletion() {
        val snapshot = state.value
        val flavor = snapshot.pendingDeletion ?: return
        if (snapshot.busy) return
        mutableState.update { it.copy(busy = true, message = null) }
        viewModelScope.launch {
            try {
                repository.delete(flavor.id)
                mutableState.update { it.copy(pendingDeletion = null, message = "Sabor excluído definitivamente.") }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: FlavorHistoryException) {
                mutableState.update { it.copy(pendingDeletion = null, message = exception.message) }
            } catch (_: Exception) {
                mutableState.update {
                    it.copy(pendingDeletion = null, message = "Não foi possível excluir o sabor. Tente novamente.")
                }
            } finally {
                mutableState.update { it.copy(busy = false) }
            }
        }
    }

    fun clearMessage() { mutableState.update { it.copy(message = null) } }

    class Factory(private val repository: FlavorRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(FlavorsViewModel::class.java))
            @Suppress("UNCHECKED_CAST")
            return FlavorsViewModel(repository) as T
        }
    }
}
