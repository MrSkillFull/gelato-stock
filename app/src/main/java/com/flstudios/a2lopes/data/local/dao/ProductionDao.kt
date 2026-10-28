package com.flstudios.a2lopes.data.local.dao

import androidx.room.*
import com.flstudios.a2lopes.data.local.entity.*

@Dao
interface ProductionDao {
    @Insert suspend fun insertSession(session: ProductionSessionEntity)
    @Insert suspend fun insertItem(item: ProductionItemEntity): Long
    @Insert suspend fun insertMovement(movement: StockMovementEntity): Long

    @Query("SELECT * FROM production_sessions WHERE id = :id")
    suspend fun findSession(id: String): ProductionSessionEntity?

    @Query("SELECT * FROM production_items WHERE session_id = :sessionId ORDER BY flavor_id")
    suspend fun itemsForSession(sessionId: String): List<ProductionItemEntity>

    @Query("SELECT * FROM stock_movements WHERE production_item_id IN (SELECT id FROM production_items WHERE session_id = :sessionId) ORDER BY flavor_id")
    suspend fun movementsForSession(sessionId: String): List<StockMovementEntity>
}
