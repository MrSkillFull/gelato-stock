package com.flstudios.a2lopes.data.local.entity

import androidx.room.*

@Entity(tableName = "corrections", foreignKeys = [
    ForeignKey(entity = FlavorEntity::class, parentColumns = ["id"], childColumns = ["flavor_id"], onDelete = ForeignKey.RESTRICT),
    ForeignKey(entity = SaleEntity::class, parentColumns = ["id"], childColumns = ["sale_id"], onDelete = ForeignKey.RESTRICT),
    ForeignKey(entity = ProductionSessionEntity::class, parentColumns = ["id"], childColumns = ["production_id"], onDelete = ForeignKey.RESTRICT),
], indices = [Index("flavor_id"), Index(value = ["sale_id"], unique = true), Index(value = ["production_id"], unique = true)])
data class CorrectionEntity(
    @PrimaryKey val id: String,
    val type: String,
    val reason: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "flavor_id") val flavorId: Long?,
    @ColumnInfo(name = "input_quantity") val inputQuantity: Long?,
    @ColumnInfo(name = "sale_id") val saleId: String?,
    @ColumnInfo(name = "production_id") val productionId: String?,
)

@Entity(tableName = "correction_items", foreignKeys = [
    ForeignKey(entity = CorrectionEntity::class, parentColumns = ["id"], childColumns = ["correction_id"], onDelete = ForeignKey.RESTRICT),
    ForeignKey(entity = FlavorEntity::class, parentColumns = ["id"], childColumns = ["flavor_id"], onDelete = ForeignKey.RESTRICT),
    ForeignKey(entity = ProductionItemEntity::class, parentColumns = ["id", "flavor_id"], childColumns = ["source_production_item_id", "flavor_id"], onDelete = ForeignKey.RESTRICT),
    ForeignKey(entity = SaleItemEntity::class, parentColumns = ["id", "flavor_id"], childColumns = ["source_sale_item_id", "flavor_id"], onDelete = ForeignKey.RESTRICT),
], indices = [Index(value = ["correction_id", "flavor_id"], unique = true), Index("flavor_id"),
    Index(value = ["id", "flavor_id"], unique = true), Index(value = ["source_production_item_id", "flavor_id"]),
    Index(value = ["source_sale_item_id", "flavor_id"])])
data class CorrectionItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "correction_id") val correctionId: String,
    @ColumnInfo(name = "flavor_id") val flavorId: Long,
    @ColumnInfo(name = "previous_quantity") val previousQuantity: Long,
    @ColumnInfo(name = "resulting_quantity") val resultingQuantity: Long,
    @ColumnInfo(name = "quantity_delta") val quantityDelta: Long,
    @ColumnInfo(name = "source_production_item_id") val sourceProductionItemId: Long? = null,
    @ColumnInfo(name = "source_sale_item_id") val sourceSaleItemId: Long? = null,
)
