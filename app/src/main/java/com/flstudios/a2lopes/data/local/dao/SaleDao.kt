package com.flstudios.a2lopes.data.local.dao

import androidx.room.*
import com.flstudios.a2lopes.data.local.entity.*

@Dao
interface SaleDao {
    @Insert suspend fun insertSale(sale: SaleEntity)
    @Insert suspend fun insertItem(item: SaleItemEntity): Long
    @Insert suspend fun insertMovement(movement: StockMovementEntity): Long

    @Query("SELECT * FROM sales WHERE id = :id")
    suspend fun findSale(id: String): SaleEntity?

    @Query("SELECT * FROM sale_items WHERE sale_id = :saleId ORDER BY flavor_id")
    suspend fun itemsForSale(saleId: String): List<SaleItemEntity>

    @Query("SELECT * FROM stock_movements WHERE sale_item_id IN (SELECT id FROM sale_items WHERE sale_id = :saleId) ORDER BY flavor_id")
    suspend fun movementsForSale(saleId: String): List<StockMovementEntity>
}
