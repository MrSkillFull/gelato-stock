package com.flstudios.a2lopes.domain.model

import com.flstudios.a2lopes.data.backup.*
import com.flstudios.a2lopes.data.local.entity.*
import org.junit.Assert.*
import org.junit.Test

class BackupValidatorTest {
    private val empty = BackupSnapshot(1000, "12345678-1234-1234-1234-123456789012")
    private fun sample() = empty.copy(
        flavors = listOf(FlavorEntity(1, "Coco", null, false)),
        production_sessions = listOf(ProductionSessionEntity("p", 1, 2, null)),
        production_items = listOf(ProductionItemEntity(1, "p", 1, 10)),
        sales = listOf(SaleEntity("s", 3, 3)),
        sale_items = listOf(SaleItemEntity(1, "s", 1, 2, 550)),
        stock_movements = listOf(
            StockMovementEntity(1, 1, "PRODUCTION", 10, "PRODUCTION_ITEM", 1, 2, null, productionItemId = 1),
            StockMovementEntity(2, 1, "SALE", -2, "SALE_ITEM", 1, 3, null, saleItemId = 1),
        ),
    )
    private fun rejects(data: BackupSnapshot) {
        assertThrows(BackupInvalid::class.java) { BackupValidator.validate(data) }
    }
    @Test fun emptyAndHistoricalPricesAndInactiveFlavorsAreValid() {
        BackupValidator.validate(empty)
        BackupValidator.validate(sample())
        BackupValidator.validate(sample().copy(sale_items = listOf(sample().sale_items.single().copy(unitPriceCents = null))))
    }
    @Test fun duplicateIdsNamesKeysAndItemPairsAreRejected() {
        val b = sample()
        rejects(b.copy(flavors = b.flavors + b.flavors))
        rejects(b.copy(flavors = b.flavors + b.flavors.single().copy(id = 2)))
        rejects(b.copy(production_sessions = b.production_sessions + b.production_sessions))
        rejects(b.copy(sales = b.sales + b.sales))
        rejects(b.copy(production_items = b.production_items + b.production_items.single().copy(id = 2)))
        rejects(b.copy(sale_items = b.sale_items + b.sale_items.single().copy(id = 2)))
        rejects(b.copy(stock_movements = b.stock_movements + b.stock_movements))
    }
    @Test fun missingReferencesOrMovementsAndEmptyOperationsAreRejected() {
        val b = sample()
        rejects(b.copy(flavors = emptyList()))
        rejects(b.copy(production_sessions = emptyList()))
        rejects(b.copy(sales = emptyList()))
        rejects(b.copy(production_items = emptyList()))
        rejects(b.copy(stock_movements = b.stock_movements.drop(1)))
        rejects(b.copy(stock_movements = b.stock_movements.map { it.copy(referenceId = 99) }))
    }
    @Test fun semanticValuesTypesAndStockAreRejected() {
        val b = sample()
        rejects(b.copy(flavors = listOf(b.flavors.single().copy(defaultPriceCents = -1))))
        rejects(b.copy(flavors = listOf(b.flavors.single().copy(normalizedName = "wrong"))))
        rejects(b.copy(production_sessions = listOf(b.production_sessions.single().copy(createdAt = -1))))
        rejects(b.copy(sale_items = listOf(b.sale_items.single().copy(quantity = 0))))
        rejects(b.copy(sale_items = listOf(b.sale_items.single().copy(unitPriceCents = Long.MAX_VALUE))))
        rejects(b.copy(stock_movements = b.stock_movements.map { it.copy(type = "UNKNOWN") }))
        rejects(b.copy(stock_movements = b.stock_movements.map { it.copy(quantityDelta = -1) }))
        rejects(b.copy(sale_items = listOf(b.sale_items.single().copy(quantity = 11)),
            stock_movements = listOf(b.stock_movements[0], b.stock_movements[1].copy(quantityDelta = -11))))
    }
    @Test fun auditCorrectionsMustMatchSourceAndBeforeAfterBalances() {
        val b = sample().copy(
            corrections = listOf(CorrectionEntity("c", "SALE_REVERSAL", "Erro", 4, null, null, "s", null)),
            correction_items = listOf(CorrectionItemEntity(1, "c", 1, 8, 10, 2, sourceSaleItemId = 1)),
            stock_movements = sample().stock_movements + StockMovementEntity(3, 1, "SALE_REVERSAL", 2, "CORRECTION_ITEM", 1, 4, "Erro", correctionItemId = 1),
        )
        BackupValidator.validate(b)
        rejects(b.copy(correction_items = listOf(b.correction_items.single().copy(previousQuantity = 9, resultingQuantity = 11))))
        rejects(b.copy(correction_items = listOf(b.correction_items.single().copy(sourceSaleItemId = 99))))
        rejects(b.copy(corrections = listOf(b.corrections.single().copy(saleId = "absent"))))
        rejects(b.copy(corrections = b.corrections + b.corrections.single().copy(id = "other")))
    }
    @Test fun overflowAndIntermediateNegativeBalanceAreRejected() {
        val b = sample()
        rejects(b.copy(stock_movements = listOf(b.stock_movements[1].copy(id = 1), b.stock_movements[0].copy(id = 2))))
        val huge = b.copy(production_items = listOf(b.production_items.single().copy(quantity = Long.MAX_VALUE)),
            stock_movements = listOf(b.stock_movements[0].copy(quantityDelta = Long.MAX_VALUE), b.stock_movements[1]))
        BackupValidator.validate(huge)
        rejects(huge.copy(production_sessions = huge.production_sessions + ProductionSessionEntity("q", 5, 5, null),
            production_items = huge.production_items + ProductionItemEntity(2, "q", 1, 3),
            stock_movements = huge.stock_movements + StockMovementEntity(3, 1, "PRODUCTION", 3, "PRODUCTION_ITEM", 2, 5, null, productionItemId = 2)))
    }
}
