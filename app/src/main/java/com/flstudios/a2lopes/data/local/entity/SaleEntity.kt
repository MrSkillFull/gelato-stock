package com.flstudios.a2lopes.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sales")
data class SaleEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "sold_at") val soldAt: Long,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)
