package com.flstudios.a2lopes.data.local.entity

import androidx.room.*

@Entity(
    tableName = "stock_movements",
    foreignKeys = [
        ForeignKey(entity = FlavorEntity::class, parentColumns = ["id"], childColumns = ["flavor_id"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = ProductionItemEntity::class, parentColumns = ["id", "flavor_id"], childColumns = ["production_item_id", "flavor_id"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = SaleItemEntity::class, parentColumns = ["id", "flavor_id"], childColumns = ["sale_item_id", "flavor_id"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = CorrectionItemEntity::class, parentColumns = ["id", "flavor_id"], childColumns = ["correction_item_id", "flavor_id"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [Index("flavor_id"), Index(value = ["production_item_id", "flavor_id"]), Index(value = ["production_item_id"], unique = true),
        Index(value = ["sale_item_id", "flavor_id"]), Index(value = ["sale_item_id"], unique = true),
        Index(value = ["correction_item_id", "flavor_id"]), Index(value = ["correction_item_id"], unique = true)],
)
data class StockMovementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "flavor_id") val flavorId: Long,
    val type: String,
    @ColumnInfo(name = "quantity_delta") val quantityDelta: Long,
    @ColumnInfo(name = "reference_type") val referenceType: String,
    @ColumnInfo(name = "reference_id") val referenceId: Long,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    val notes: String?,
    // FKs concretas de origem, preenchidas conforme o tipo da operação.
    @ColumnInfo(name = "production_item_id") val productionItemId: Long? = null,
    @ColumnInfo(name = "sale_item_id") val saleItemId: Long? = null,
    @ColumnInfo(name = "correction_item_id") val correctionItemId: Long? = null,
)
