package com.flstudios.a2lopes.data.repository

import com.flstudios.a2lopes.data.local.AppDatabase
import com.flstudios.a2lopes.data.local.entity.AppSettingsEntity
import com.flstudios.a2lopes.domain.model.DEFAULT_LOW_STOCK_THRESHOLD
import com.flstudios.a2lopes.domain.model.SettingsRules
import com.flstudios.a2lopes.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged

class RoomSettingsRepository(private val database: AppDatabase) : SettingsRepository {
    override fun observeLowStockThreshold() = database.settingsDao().observe()
        .map { (it ?: DEFAULT_LOW_STOCK_THRESHOLD).also(SettingsRules::validateThreshold) }.distinctUntilChanged()

    override suspend fun setLowStockThreshold(value: Long) {
        SettingsRules.validateThreshold(value)
        database.settingsDao().write(AppSettingsEntity(lowStockThreshold = value))
    }
}
