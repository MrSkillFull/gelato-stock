package com.flstudios.a2lopes.data.repository

import androidx.room.withTransaction
import com.flstudios.a2lopes.data.local.AppDatabase
import com.flstudios.a2lopes.data.local.entity.*
import com.flstudios.a2lopes.domain.model.*
import com.flstudios.a2lopes.domain.repository.ProductionRepository

class RoomProductionRepository(
    private val database: AppDatabase,
    private val now: () -> Long = System::currentTimeMillis,
) : ProductionRepository {
    override suspend fun record(request: ProductionRequest): ProductionSession {
        // Snapshot para que mudanças na lista do chamador não alterem uma operação suspensa.
        val input = request.copy(items = request.items.toList())
        input.validate()
        return database.withTransaction {
            val dao = database.productionDao()
            val existing = dao.findSession(input.operationId)
            val items = input.items.sortedBy { it.flavorId }
            if (existing != null) {
                require(existing.producedAt == input.producedAt && existing.notes == input.notes &&
                    dao.itemsForSession(existing.id).map { ProductionQuantity(it.flavorId, it.quantity) } == items) {
                    "Este identificador já foi utilizado por outra produção."
                }
                existing.toDomain()
            } else {
                val session = ProductionSessionEntity(input.operationId, input.producedAt, now(), input.notes)
                dao.insertSession(session)
                items.forEach { item ->
                    val flavor = database.flavorDao().findById(item.flavorId)
                    require(flavor != null && flavor.active) { "A produção exige sabores existentes e ativos." }
                    val stock = database.stockDao().quantity(item.flavorId)
                    check(stock >= 0) { "O estoque existente está inválido." }
                    require(item.quantity <= Long.MAX_VALUE - stock) { "Quantidade de estoque fora do limite permitido. Reduza a quantidade produzida." }
                    val itemId = dao.insertItem(ProductionItemEntity(sessionId = session.id, flavorId = item.flavorId, quantity = item.quantity))
                    dao.insertMovement(StockMovementEntity(
                        flavorId = item.flavorId, type = "PRODUCTION", quantityDelta = item.quantity,
                        referenceType = "PRODUCTION_ITEM", referenceId = itemId,
                        createdAt = session.createdAt, notes = session.notes, productionItemId = itemId,
                    ))
                }
                session.toDomain()
            }
        }
    }

    private fun ProductionSessionEntity.toDomain() = ProductionSession(id, producedAt, createdAt, notes)
}
