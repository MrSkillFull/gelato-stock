package com.flstudios.a2lopes.domain.model

import org.junit.Assert.*
import org.junit.Test

class ProductionRequestTest {
    @Test fun acceptsOneOrMultiplePositiveItems() {
        ProductionRequest("one", 100, listOf(ProductionQuantity(1, 2))).validate()
        ProductionRequest("two", 100, listOf(ProductionQuantity(1, 2), ProductionQuantity(2, 5))).validate()
    }

    @Test fun rejectsEmptyDuplicateAndNonPositiveItems() {
        val invalid = listOf(
            emptyList(), listOf(ProductionQuantity(1, 0)), listOf(ProductionQuantity(1, -1)),
            listOf(ProductionQuantity(0, 1)), listOf(ProductionQuantity(1, 1), ProductionQuantity(1, 2)),
        )
        invalid.forEach { items ->
            try { ProductionRequest("id", 100, items).validate(); fail("Entrada inválida aceita") }
            catch (_: IllegalArgumentException) { }
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsMissingOperationIdentifier() {
        ProductionRequest(" ", 100, listOf(ProductionQuantity(1, 1))).validate()
    }
}
