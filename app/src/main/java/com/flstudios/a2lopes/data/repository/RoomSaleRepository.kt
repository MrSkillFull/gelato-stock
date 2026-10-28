package com.flstudios.a2lopes.data.repository

import androidx.room.withTransaction
import com.flstudios.a2lopes.data.local.AppDatabase
import com.flstudios.a2lopes.data.local.entity.*
import com.flstudios.a2lopes.domain.model.*
import com.flstudios.a2lopes.domain.repository.SaleRepository

class RoomSaleRepository(
    private val database: AppDatabase,
    private val now: () -> Long = System::currentTimeMillis,
) : SaleRepository {
    override suspend fun record(request: SaleRequest): Sale {
        val input = request.copy(items = request.items.toList())
        input.validate()
        return database.withTransaction {
            val dao = database.saleDao()
            val items = input.items.sortedBy { it.flavorId }
            val existing = dao.findSale(input.operationId)
            if (existing != null) {
                require(existing.soldAt == input.soldAt && dao.itemsForSale(existing.id).map {
                    SaleQuantity(it.flavorId, it.quantity, it.unitPriceCents)
                } == items) { "Este identificador já foi utilizado por outra venda." }
                Sale(existing.id, existing.soldAt, existing.createdAt)
            } else {
                val sale = SaleEntity(input.operationId, input.soldAt, now())
                dao.insertSale(sale)
                items.forEach { item ->
                    val flavor = database.flavorDao().findById(item.flavorId)
                    require(flavor != null && flavor.active) { "A venda exige sabores existentes e ativos. Confira o carrinho." }
                    require(flavor.defaultPriceCents == item.unitPriceCents) { "O preço de ${flavor.name} mudou. Confira o carrinho antes de tentar novamente." }
                    val available = database.stockDao().quantity(item.flavorId)
                    require(available >= item.quantity) { "Estoque insuficiente de ${flavor.name}. Disponível: $available unidades." }
                    val itemId = dao.insertItem(SaleItemEntity(
                        saleId = sale.id, flavorId = item.flavorId, quantity = item.quantity, unitPriceCents = item.unitPriceCents,
                    ))
                    dao.insertMovement(StockMovementEntity(
                        flavorId = item.flavorId, type = "SALE", quantityDelta = -item.quantity,
                        referenceType = "SALE_ITEM", referenceId = itemId, createdAt = sale.createdAt,
                        notes = null, saleItemId = itemId,
                    ))
                }
                Sale(sale.id, sale.soldAt, sale.createdAt)
            }
        }
    }
}
