package com.flstudios.a2lopes.presentation.settings

import androidx.lifecycle.*
import com.flstudios.a2lopes.domain.model.SettingsRules
import com.flstudios.a2lopes.domain.repository.SettingsRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class SettingsUiState(
    val input: String = "",
    val saved: Long? = null,
    val loading: Boolean = true,
    val busy: Boolean = false,
    val dirty: Boolean = false,
    val error: String? = null,
    val loadError: String? = null,
    val message: String? = null,
)

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {
    private val mutable = MutableStateFlow(SettingsUiState())
    val state = mutable.asStateFlow()
    private var observation: Job? = null
    init { reload() }
    fun reload() {
        observation?.cancel()
        mutable.update { it.copy(loading = true, loadError = null) }
        observation = viewModelScope.launch {
            try {
                repository.observeLowStockThreshold().collect { value ->
                    mutable.update { it.copy(saved = value, input = if (it.dirty) it.input else value.toString(), loading = false, loadError = null) }
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { mutable.update { it.copy(loading = false, loadError = "Não foi possível carregar a configuração. Tente novamente.") } }
        }
    }
    fun change(value: String) {
        if (!state.value.busy) mutable.update { it.copy(input = value, dirty = true, error = null, message = null) }
    }
    fun save() {
        val current = state.value
        if (current.busy || current.loading || current.loadError != null) return
        val value = try { SettingsRules.parseThreshold(current.input) }
        catch (e: IllegalArgumentException) { mutable.update { it.copy(error = e.message, message = null) }; return }
        mutable.update { it.copy(busy = true, error = null, message = null) }
        viewModelScope.launch {
            try {
                repository.setLowStockThreshold(value)
                mutable.update { it.copy(saved = value, input = value.toString(), dirty = false, message = "Limite de estoque baixo salvo.") }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { mutable.update { it.copy(error = "Não foi possível salvar o limite. Tente novamente.") } }
            finally { mutable.update { it.copy(busy = false) } }
        }
    }
    class Factory(private val repository: SettingsRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(SettingsViewModel::class.java))
            @Suppress("UNCHECKED_CAST") return SettingsViewModel(repository) as T
        }
    }
}
