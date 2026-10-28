package com.flstudios.a2lopes.data.local.dao

import androidx.room.*
import com.flstudios.a2lopes.data.local.entity.*

@Dao
interface BackupDao {
    @Query("SELECT (SELECT COUNT(*) FROM flavors) + (SELECT COUNT(*) FROM production_sessions) + (SELECT COUNT(*) FROM production_items) + (SELECT COUNT(*) FROM sales) + (SELECT COUNT(*) FROM sale_items) + (SELECT COUNT(*) FROM corrections) + (SELECT COUNT(*) FROM correction_items) + (SELECT COUNT(*) FROM stock_movements)")
    suspend fun recordCount(): Long
    @Query("SELECT * FROM flavors ORDER BY id") suspend fun readFlavor(): List<FlavorEntity>
    @Insert suspend fun insertFlavor(rows: List<FlavorEntity>)
    @Query("DELETE FROM flavors") suspend fun deleteFlavor()
    @Query("SELECT * FROM production_sessions ORDER BY id") suspend fun readProductionSession(): List<ProductionSessionEntity>
    @Insert suspend fun insertProductionSession(rows: List<ProductionSessionEntity>)
    @Query("DELETE FROM production_sessions") suspend fun deleteProductionSession()
    @Query("SELECT * FROM production_items ORDER BY id") suspend fun readProductionItem(): List<ProductionItemEntity>
    @Insert suspend fun insertProductionItem(rows: List<ProductionItemEntity>)
    @Query("DELETE FROM production_items") suspend fun deleteProductionItem()
    @Query("SELECT * FROM sales ORDER BY id") suspend fun readSale(): List<SaleEntity>
    @Insert suspend fun insertSale(rows: List<SaleEntity>)
    @Query("DELETE FROM sales") suspend fun deleteSale()
    @Query("SELECT * FROM sale_items ORDER BY id") suspend fun readSaleItem(): List<SaleItemEntity>
    @Insert suspend fun insertSaleItem(rows: List<SaleItemEntity>)
    @Query("DELETE FROM sale_items") suspend fun deleteSaleItem()
    @Query("SELECT * FROM corrections ORDER BY id") suspend fun readCorrection(): List<CorrectionEntity>
    @Insert suspend fun insertCorrection(rows: List<CorrectionEntity>)
    @Query("DELETE FROM corrections") suspend fun deleteCorrection()
    @Query("SELECT * FROM correction_items ORDER BY id") suspend fun readCorrectionItem(): List<CorrectionItemEntity>
    @Insert suspend fun insertCorrectionItem(rows: List<CorrectionItemEntity>)
    @Query("DELETE FROM correction_items") suspend fun deleteCorrectionItem()
    @Query("SELECT * FROM stock_movements ORDER BY id") suspend fun readStockMovement(): List<StockMovementEntity>
    @Insert suspend fun insertStockMovement(rows: List<StockMovementEntity>)
    @Query("DELETE FROM stock_movements") suspend fun deleteStockMovement()
}
