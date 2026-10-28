package com.flstudios.a2lopes.data.local.dao

import androidx.room.*
import com.flstudios.a2lopes.data.local.entity.*
import com.flstudios.a2lopes.domain.model.CorrectionHistoryItem
import kotlinx.coroutines.flow.Flow

@Dao
interface CorrectionDao {
    @Insert suspend fun insert(correction: CorrectionEntity)
    @Insert suspend fun insertItem(item: CorrectionItemEntity): Long
    @Insert suspend fun insertMovement(movement: StockMovementEntity): Long
    @Query("SELECT * FROM corrections WHERE id = :id")
    suspend fun find(id: String): CorrectionEntity?
    @Query("SELECT * FROM correction_items WHERE correction_id = :id ORDER BY flavor_id")
    suspend fun items(id: String): List<CorrectionItemEntity>
    @Query("SELECT id FROM corrections WHERE sale_id = :saleId OR production_id = :productionId LIMIT 1")
    suspend fun reversal(saleId: String?, productionId: String?): String?
    @Query("SELECT id FROM corrections WHERE sale_id = :saleId OR production_id = :productionId LIMIT 1")
    fun observeReversal(saleId: String?, productionId: String?): Flow<String?>
    @Query("""
        SELECT f.name AS flavorName, i.previous_quantity AS previousQuantity,
          i.resulting_quantity AS resultingQuantity, i.quantity_delta AS quantityDelta, m.id AS movementId
        FROM correction_items i JOIN flavors f ON f.id = i.flavor_id
        LEFT JOIN stock_movements m ON m.correction_item_id = i.id
        WHERE i.correction_id = :id ORDER BY f.normalized_name, f.id
    """)
    suspend fun historyItems(id: String): List<CorrectionHistoryItem>
}
