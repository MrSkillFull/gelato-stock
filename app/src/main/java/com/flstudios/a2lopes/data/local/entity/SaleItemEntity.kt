package com.flstudios.a2lopes.data.local.entity

import androidx.room.*

@Entity(
    tableName = "sale_items",
    foreignKeys = [
        ForeignKey(entity = SaleEntity::class, parentColumns = ["id"], childColumns = ["sale_id"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = FlavorEntity::class, parentColumns = ["id"], childColumns = ["flavor_id"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [Index(value = ["sale_id", "flavor_id"], unique = true), Index("flavor_id"), Index(value = ["id", "flavor_id"], unique = true)],
)
data class SaleItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "sale_id") val saleId: String,
    @ColumnInfo(name = "flavor_id") val flavorId: Long,
    val quantity: Long,
    @ColumnInfo(name = "unit_price_cents") val unitPriceCents: Long?,
)
