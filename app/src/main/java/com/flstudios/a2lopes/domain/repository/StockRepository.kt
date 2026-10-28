package com.flstudios.a2lopes.domain.repository

import com.flstudios.a2lopes.domain.model.FlavorStock
import kotlinx.coroutines.flow.Flow

interface StockRepository {
    fun observeAll(): Flow<List<FlavorStock>>
    fun observeQuantity(flavorId: Long): Flow<Long>
}
