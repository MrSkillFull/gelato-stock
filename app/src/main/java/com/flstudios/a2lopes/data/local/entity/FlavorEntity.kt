package com.flstudios.a2lopes.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import com.flstudios.a2lopes.domain.model.FlavorNames

@Entity(tableName = "flavors", indices = [Index(value = ["normalized_name"], unique = true)])
data class FlavorEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "default_price_cents") val defaultPriceCents: Long? = null,
    val active: Boolean = true,
    @ColumnInfo(name = "normalized_name") val normalizedName: String = FlavorNames.key(name),
)
