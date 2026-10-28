package com.flstudios.a2lopes.domain.repository

import com.flstudios.a2lopes.domain.model.Flavor
import kotlinx.coroutines.flow.Flow

interface FlavorRepository {
    fun observeAll(): Flow<List<Flavor>>
    suspend fun save(id: Long?, name: String, price: String)
    suspend fun delete(id: Long)
    suspend fun setActive(id: Long, active: Boolean)
}
