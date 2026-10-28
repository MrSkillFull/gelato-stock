package com.flstudios.a2lopes.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.flstudios.a2lopes.domain.model.FlavorStock
import kotlinx.coroutines.flow.Flow

@Dao
interface StockDao {
    @Query("SELECT f.id AS flavorId, f.name, f.active, COALESCE(SUM(m.quantity_delta), 0) AS quantity FROM flavors f LEFT JOIN stock_movements m ON m.flavor_id = f.id GROUP BY f.id ORDER BY f.normalized_name, f.id")
    fun observeAll(): Flow<List<FlavorStock>>

    @Query("SELECT COALESCE(SUM(quantity_delta), 0) FROM stock_movements WHERE flavor_id = :flavorId")
    fun observeQuantity(flavorId: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(quantity_delta), 0) FROM stock_movements WHERE flavor_id = :flavorId")
    suspend fun quantity(flavorId: Long): Long

    @Query("SELECT EXISTS(SELECT 1 FROM production_items WHERE flavor_id = :flavorId) OR EXISTS(SELECT 1 FROM sale_items WHERE flavor_id = :flavorId) OR EXISTS(SELECT 1 FROM stock_movements WHERE flavor_id = :flavorId) OR EXISTS(SELECT 1 FROM correction_items WHERE flavor_id = :flavorId)")
    suspend fun hasHistory(flavorId: Long): Boolean
}
