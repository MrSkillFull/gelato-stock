package com.flstudios.a2lopes.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "production_sessions")
data class ProductionSessionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "produced_at") val producedAt: Long,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    val notes: String?,
)
