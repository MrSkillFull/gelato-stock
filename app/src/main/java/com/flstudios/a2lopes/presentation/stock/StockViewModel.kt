package com.flstudios.a2lopes.presentation.stock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.flstudios.a2lopes.domain.model.FlavorStock
import com.flstudios.a2lopes.domain.repository.StockRepository
import com.flstudios.a2lopes.presentation.production.totalUnits
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import com.flstudios.a2lopes.domain.repository.SettingsRepository
import kotlinx.coroutines.launch

const val DEFAULT_LOW_STOCK_THRESHOLD = com.flstudios.a2lopes.domain.model.DEFAULT_LOW_STOCK_THRESHOLD

data class StockUiState(
    val flavors: List<FlavorStock> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
    val lowStockThreshold: Long = DEFAULT_LOW_STOCK_THRESHOLD,
) {
    val total: String get() = totalUnits(flavors.map { it.quantity })
}

class StockViewModel(private val repository: StockRepository, lowStockThreshold: Long = DEFAULT_LOW_STOCK_THRESHOLD,
    private val settings: SettingsRepository? = null) : ViewModel() {
    private val mutableState = MutableStateFlow(StockUiState(lowStockThreshold = lowStockThreshold))
    val state = mutableState.asStateFlow()
    private var observation: Job? = null

    init { reload() }

    fun reload() {
        observation?.cancel()
        mutableState.update { it.copy(loading = true, error = null) }
        observation = viewModelScope.launch {
            try {
                val snapshots = if (settings == null) repository.observeAll().map { it to state.value.lowStockThreshold }
                    else combine(repository.observeAll(), settings.observeLowStockThreshold()) { flavors, limit -> flavors to limit }
                snapshots.collect { (flavors, limit) ->
                    mutableState.update { it.copy(flavors = flavors, lowStockThreshold = limit, loading = false, error = null) }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                mutableState.update { it.copy(loading = false, error = "Não foi possível carregar o estoque. Tente novamente.") }
            }
        }
    }

    class Factory(private val repository: StockRepository, private val settings: SettingsRepository? = null) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(StockViewModel::class.java))
            @Suppress("UNCHECKED_CAST")
            return StockViewModel(repository, settings = settings) as T
        }
    }
}
