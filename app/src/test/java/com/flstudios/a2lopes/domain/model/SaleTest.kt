package com.flstudios.a2lopes.domain.model

import org.junit.Assert.*
import org.junit.Test

class SaleTest {
    @Test fun validatesSingleAndMultipleItemsAndCalculatesIntegerCents() {
        SaleRequest("one", 100, listOf(SaleQuantity(1, 2, 550))).validate()
        val items = listOf(SaleQuantity(1, 2, 550), SaleQuantity(2, 3, 125))
        SaleRequest("multiple", 100, items).validate()
        assertEquals(1475L, saleTotalCents(items))
    }

    @Test fun absentPriceIsDifferentFromExplicitZeroAndSuppressesTotal() {
        val items = listOf(SaleQuantity(1, 2, 550), SaleQuantity(2, 1, null))
        SaleRequest("unknown-price", 100, items).validate()
        assertNull(saleTotalCents(items))
        assertEquals(0L, saleTotalCents(listOf(SaleQuantity(1, 2, 0))))
    }

    @Test fun rejectsEmptyDuplicateNonPositiveInvalidPriceAndMissingIdentifier() {
        val invalid = listOf(
            SaleRequest("", 100, listOf(SaleQuantity(1, 1, null))),
            SaleRequest("empty", 100, emptyList()),
            SaleRequest("zero", 100, listOf(SaleQuantity(1, 0, 10))),
            SaleRequest("negative", 100, listOf(SaleQuantity(1, -1, 10))),
            SaleRequest("id", 100, listOf(SaleQuantity(0, 1, 10))),
            SaleRequest("price", 100, listOf(SaleQuantity(1, 1, -1))),
            SaleRequest("duplicate", 100, listOf(SaleQuantity(1, 1, 10), SaleQuantity(1, 2, 10))),
        )
        invalid.forEach {
            try { it.validate(); fail("Venda inválida aceita") } catch (_: IllegalArgumentException) { }
        }
    }

    @Test fun multiplicationAndSumOverflowAreRejected() {
        for (items in listOf(listOf(SaleQuantity(1, 2, Long.MAX_VALUE)),
            listOf(SaleQuantity(1, 1, Long.MAX_VALUE), SaleQuantity(2, 1, 1)))) {
            try { SaleRequest("overflow", 100, items).validate(); fail("Overflow aceito") }
            catch (_: IllegalArgumentException) { }
        }
    }
}
