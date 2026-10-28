package com.flstudios.a2lopes.domain.repository

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observeLowStockThreshold(): Flow<Long>
    suspend fun setLowStockThreshold(value: Long)
}
