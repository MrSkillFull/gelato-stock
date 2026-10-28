package com.flstudios.a2lopes.domain.model

import org.junit.Assert.*
import org.junit.Test

class HistoryDetailTest {
    private fun sale(price: Long?) = HistoryDetail("s", HistoryKind.SALE, 100, 200, null,
        listOf(HistoryItem(1, "Coco", 2, 550), HistoryItem(2, "Morango", 3, price)))

    @Test fun knownPricesUseIntegerCentsAndExplicitZeroIsKnown() {
        assertEquals(3200L, sale(700).totalCents)
        assertEquals(1100L, sale(0).totalCents)
        assertEquals("5", sale(700).totalUnits)
    }

    @Test fun anyMissingPriceMakesFinancialTotalUnavailable() { assertNull(sale(null).totalCents) }

    @Test fun productionUnitTotalCanExceedLong() {
        val detail = HistoryDetail("p", HistoryKind.PRODUCTION, 10, 20, "Lote",
            listOf(HistoryItem(1, "Coco", Long.MAX_VALUE, null), HistoryItem(2, "Morango", 1, null)))
        assertEquals("9223372036854775808", detail.totalUnits)
        assertNull(detail.totalCents)
    }
}
