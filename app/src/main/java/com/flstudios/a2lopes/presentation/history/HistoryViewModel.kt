package com.flstudios.a2lopes.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.flstudios.a2lopes.domain.model.*
import com.flstudios.a2lopes.domain.repository.HistoryRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HistoryUiState(
    val filter: HistoryFilter = HistoryFilter.ALL,
    val page: Int = 0,
    val entries: List<HistoryEntry> = emptyList(),
    val hasNext: Boolean = false,
    val loading: Boolean = true,
    val error: String? = null,
    val selectedKind: HistoryKind? = null,
    val selectedId: String? = null,
    val detail: HistoryDetail? = null,
    val detailLoading: Boolean = false,
    val detailError: String? = null,
)

class HistoryViewModel(private val repository: HistoryRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(HistoryUiState())
    val state = mutableState.asStateFlow()
    private var observation: Job? = null
    private var detailJob: Job? = null

    init { reload() }

    fun selectFilter(filter: HistoryFilter) {
        closeDetail()
        mutableState.update { it.copy(filter = filter, page = 0) }
        reload()
    }

    fun nextPage() { if (!state.value.loading && state.value.hasNext) changePage(state.value.page + 1) }
    fun previousPage() { if (!state.value.loading && state.value.page > 0) changePage(state.value.page - 1) }
    private fun changePage(page: Int) {
        mutableState.update { it.copy(page = page) }
        reload()
    }

    fun reload() {
        observation?.cancel()
        val snapshot = state.value
        mutableState.update { it.copy(loading = true, error = null, entries = emptyList(), hasNext = false) }
        observation = viewModelScope.launch {
            try {
                repository.observePage(snapshot.filter, snapshot.page).collect { page ->
                    mutableState.update { it.copy(entries = page.entries, hasNext = page.hasNext, loading = false, error = null) }
                }
            } catch (exception: CancellationException) { throw exception
            } catch (_: Exception) {
                mutableState.update { it.copy(loading = false, error = "Não foi possível carregar o histórico. Tente novamente.") }
            }
        }
    }

    fun openDetail(kind: HistoryKind, id: String) {
        if (kind == HistoryKind.MOVEMENT) return
        detailJob?.cancel()
        mutableState.update { it.copy(selectedKind = kind, selectedId = id, detail = null, detailLoading = true, detailError = null) }
        detailJob = viewModelScope.launch {
            try {
                repository.observeDetail(kind, id).collect { detail ->
                    mutableState.update { it.copy(detail = detail, detailLoading = false,
                        detailError = if (detail == null) "Registro não encontrado." else null) }
                }
            } catch (exception: CancellationException) { throw exception
            } catch (_: Exception) {
                mutableState.update { it.copy(detailLoading = false, detailError = "Não foi possível carregar os detalhes. Tente novamente.") }
            }
        }
    }

    fun retryDetail() { state.value.let { if (it.selectedKind != null && it.selectedId != null) openDetail(it.selectedKind, it.selectedId) } }
    fun closeDetail() {
        detailJob?.cancel()
        mutableState.update { it.copy(selectedKind = null, selectedId = null, detail = null, detailLoading = false, detailError = null) }
    }

    class Factory(private val repository: HistoryRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(HistoryViewModel::class.java))
            @Suppress("UNCHECKED_CAST")
            return HistoryViewModel(repository) as T
        }
    }
}
