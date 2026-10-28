package com.flstudios.a2lopes.data.backup

import com.flstudios.a2lopes.data.local.entity.*

/** Immutable in-memory snapshot. IDs are also operation idempotency keys. */
data class BackupSnapshot(
    val createdAt: Long,
    val datasetId: String,
    val flavors: List<FlavorEntity> = emptyList(),
    val production_sessions: List<ProductionSessionEntity> = emptyList(),
    val production_items: List<ProductionItemEntity> = emptyList(),
    val sales: List<SaleEntity> = emptyList(),
    val sale_items: List<SaleItemEntity> = emptyList(),
    val corrections: List<CorrectionEntity> = emptyList(),
    val correction_items: List<CorrectionItemEntity> = emptyList(),
    val stock_movements: List<StockMovementEntity> = emptyList(),
    val lowStockThreshold: Long = com.flstudios.a2lopes.domain.model.DEFAULT_LOW_STOCK_THRESHOLD,
) : com.flstudios.a2lopes.domain.repository.BackupDocument {
    override val summary get() = com.flstudios.a2lopes.domain.repository.BackupSummary(
        createdAt, flavors.size, production_sessions.size, sales.size, corrections.size, stock_movements.size, recordCount, lowStockThreshold,
    )
    val recordCount: Int get() = flavors.size + production_sessions.size + production_items.size + sales.size + sale_items.size + corrections.size + correction_items.size + stock_movements.size
}
