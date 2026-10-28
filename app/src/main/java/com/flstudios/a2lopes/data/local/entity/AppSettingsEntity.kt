package com.flstudios.a2lopes.data.local.entity

import androidx.room.*
import com.flstudios.a2lopes.domain.model.DEFAULT_LOW_STOCK_THRESHOLD

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,
    @ColumnInfo(name = "low_stock_threshold") val lowStockThreshold: Long = DEFAULT_LOW_STOCK_THRESHOLD,
)
