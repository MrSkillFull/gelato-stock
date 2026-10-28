package com.flstudios.a2lopes.data.local.entity

import androidx.room.*

@Entity(
    tableName = "production_items",
    foreignKeys = [
        ForeignKey(entity = ProductionSessionEntity::class, parentColumns = ["id"], childColumns = ["session_id"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = FlavorEntity::class, parentColumns = ["id"], childColumns = ["flavor_id"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [Index(value = ["session_id", "flavor_id"], unique = true), Index("flavor_id"), Index(value = ["id", "flavor_id"], unique = true)],
)
data class ProductionItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "session_id") val sessionId: String,
    @ColumnInfo(name = "flavor_id") val flavorId: Long,
    val quantity: Long,
)
