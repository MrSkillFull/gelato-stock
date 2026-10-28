package com.flstudios.a2lopes.data.repository

import com.flstudios.a2lopes.data.local.AppDatabase
import com.flstudios.a2lopes.domain.repository.StockRepository

class RoomStockRepository(database: AppDatabase) : StockRepository {
    private val dao = database.stockDao()
    override fun observeAll() = dao.observeAll()
    override fun observeQuantity(flavorId: Long) = dao.observeQuantity(flavorId)
}
