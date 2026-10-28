package com.flstudios.a2lopes.data.repository

import androidx.room.withTransaction
import com.flstudios.a2lopes.data.local.AppDatabase
import com.flstudios.a2lopes.data.local.entity.*
import com.flstudios.a2lopes.domain.model.*
import com.flstudios.a2lopes.domain.repository.CorrectionRepository

class RoomCorrectionRepository(private val database: AppDatabase, private val now: () -> Long = System::currentTimeMillis) : CorrectionRepository {
    override suspend fun record(request: CorrectionRequest): CorrectionReceipt {
        val input = try { request.normalized() } catch (e: IllegalArgumentException) { throw CorrectionRejected(e.message ?: "Confira os dados.") }
        return database.withTransaction {
            val dao = database.correctionDao()
            val saleId = input.sourceId.takeIf { input.type == CorrectionType.SALE_REVERSAL }
            val productionId = input.sourceId.takeIf { input.type == CorrectionType.PRODUCTION_REVERSAL }
            val existing = dao.find(input.operationId)
            if (existing != null) {
                rejectUnless(existing.type == input.type.name && existing.reason == input.reason &&
                    existing.flavorId == input.flavorId && existing.inputQuantity == input.quantity &&
                    existing.saleId == saleId && existing.productionId == productionId,
                    "Este identificador já foi utilizado com outro conteúdo.")
                return@withTransaction CorrectionReceipt(existing.id, existing.createdAt, dao.items(existing.id).any { it.quantityDelta != 0L })
            }
            if (saleId != null || productionId != null) {
                rejectUnless(dao.reversal(saleId, productionId) == null, "Este registro já foi estornado.")
            }
            val items = when (input.type) {
                CorrectionType.LOSS, CorrectionType.ADJUSTMENT -> {
                    val flavorId = input.flavorId!!
                    rejectUnless(database.flavorDao().findById(flavorId) != null, "O sabor não foi encontrado.")
                    val before = balance(flavorId)
                    val after = if (input.type == CorrectionType.LOSS) {
                        rejectUnless(input.quantity!! <= before, "A perda supera o estoque disponível. Nenhuma alteração foi realizada.")
                        before - input.quantity
                    } else input.quantity!!
                    listOf(CorrectionItemEntity(correctionId = input.operationId, flavorId = flavorId,
                        previousQuantity = before, resultingQuantity = after, quantityDelta = after - before))
                }
                CorrectionType.SALE_REVERSAL -> {
                    rejectUnless(database.saleDao().findSale(saleId!!) != null, "A venda não foi encontrada.")
                    database.saleDao().itemsForSale(saleId).map { item ->
                        val before = balance(item.flavorId)
                        rejectUnless(item.quantity <= Long.MAX_VALUE - before, "O estorno ultrapassa o limite de estoque. Nenhuma alteração foi realizada.")
                        CorrectionItemEntity(correctionId = input.operationId, flavorId = item.flavorId, previousQuantity = before,
                            resultingQuantity = before + item.quantity, quantityDelta = item.quantity, sourceSaleItemId = item.id)
                    }
                }
                CorrectionType.PRODUCTION_REVERSAL -> {
                    rejectUnless(database.productionDao().findSession(productionId!!) != null, "A produção não foi encontrada.")
                    database.productionDao().itemsForSession(productionId).map { item ->
                        val before = balance(item.flavorId)
                        rejectUnless(item.quantity <= before, "Estoque insuficiente para estornar toda a produção. Nenhum sabor foi alterado.")
                        CorrectionItemEntity(correctionId = input.operationId, flavorId = item.flavorId, previousQuantity = before,
                            resultingQuantity = before - item.quantity, quantityDelta = -item.quantity, sourceProductionItemId = item.id)
                    }
                }
            }
            rejectUnless(items.isNotEmpty(), "O registro não possui itens para estornar.")
            val createdAt = now()
            dao.insert(CorrectionEntity(input.operationId, input.type.name, input.reason, createdAt,
                input.flavorId, input.quantity, saleId, productionId))
            items.forEach { item ->
                val itemId = dao.insertItem(item)
                if (item.quantityDelta != 0L) dao.insertMovement(StockMovementEntity(
                    flavorId = item.flavorId, type = input.type.name, quantityDelta = item.quantityDelta,
                    referenceType = "CORRECTION_ITEM", referenceId = itemId, createdAt = createdAt,
                    notes = input.reason, correctionItemId = itemId))
            }
            CorrectionReceipt(input.operationId, createdAt, items.any { it.quantityDelta != 0L })
        }
    }

    private suspend fun balance(flavorId: Long): Long = database.stockDao().quantity(flavorId).also {
        check(it >= 0) { "O saldo existente está inválido." }
    }
    private fun rejectUnless(condition: Boolean, message: String) { if (!condition) throw CorrectionRejected(message) }
}
