package com.flstudios.a2lopes.domain.repository

import com.flstudios.a2lopes.domain.model.*
import kotlinx.coroutines.flow.Flow

interface HistoryRepository {
    fun observeDetail(kind: HistoryKind, id: String): Flow<HistoryDetail?> = kotlinx.coroutines.flow.flow { emit(detail(kind, id)) }
    fun observePage(filter: HistoryFilter, page: Int): Flow<HistoryPage>
    suspend fun detail(kind: HistoryKind, id: String): HistoryDetail?

    companion object { const val PAGE_SIZE = 30 }
}
