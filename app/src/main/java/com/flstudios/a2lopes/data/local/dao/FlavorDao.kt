package com.flstudios.a2lopes.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.flstudios.a2lopes.data.local.entity.FlavorEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FlavorDao {
    @Query("SELECT * FROM flavors ORDER BY normalized_name, id")
    fun observeAll(): Flow<List<FlavorEntity>>

    @Query("SELECT * FROM flavors WHERE id = :id")
    suspend fun findById(id: Long): FlavorEntity?

    @Query("DELETE FROM flavors WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Insert
    suspend fun insert(flavor: FlavorEntity): Long

    @Update
    suspend fun update(flavor: FlavorEntity): Int

    @Query("SELECT * FROM flavors WHERE normalized_name = :key LIMIT 1")
    suspend fun findByNormalizedName(key: String): FlavorEntity?

    @Query("UPDATE flavors SET name = :name, normalized_name = :key, default_price_cents = :price WHERE id = :id")
    suspend fun updateDetails(id: Long, name: String, key: String, price: Long?): Int

    @Query("UPDATE flavors SET active = :active WHERE id = :id")
    suspend fun setActive(id: Long, active: Boolean): Int
}
