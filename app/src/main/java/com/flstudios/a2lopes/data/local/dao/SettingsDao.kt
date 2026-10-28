package com.flstudios.a2lopes.data.local.dao

import androidx.room.*
import com.flstudios.a2lopes.data.local.entity.AppSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {
    @Query("SELECT low_stock_threshold FROM app_settings WHERE id = 1")
    suspend fun read(): Long?

    @Query("SELECT low_stock_threshold FROM app_settings WHERE id = 1")
    fun observe(): Flow<Long?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun write(settings: AppSettingsEntity)
}
