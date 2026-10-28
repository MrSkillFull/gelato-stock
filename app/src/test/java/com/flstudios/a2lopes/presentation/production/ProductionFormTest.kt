package com.flstudios.a2lopes.presentation.production

import com.flstudios.a2lopes.domain.model.Flavor
import com.flstudios.a2lopes.domain.model.ProductionQuantity
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class ProductionFormTest {
    private val flavors = listOf(Flavor(1, "Coco", null, true), Flavor(2, "Morango", null, true), Flavor(3, "Uva", null, false))

    @Test fun acceptsMultipleQuantitiesAndIgnoresEmptyFieldsAndInactiveFlavors() {
        val result = parseProductionQuantities(flavors, mapOf(1L to "12", 2L to " 3 ", 3L to "99"))
        assertEquals(listOf(ProductionQuantity(1, 12), ProductionQuantity(2, 3)), result.items)
        assertNull(result.error)
        assertEquals(listOf(ProductionQuantity(2, 5)), parseProductionQuantities(flavors, mapOf(1L to " ", 2L to "5")).items)
    }

    @Test fun rejectsZeroNegativeFractionalTextAndOverflowWithoutSilentlyChangingInput() {
        for (invalid in listOf("0", "-1", "1,5", "2.5", "+3", "abc", "9223372036854775808")) {
            val parsed = parseProductionQuantities(flavors, mapOf(1L to invalid, 2L to "2"))
            assertNotNull(parsed.error)
            assertTrue(parsed.errors.containsKey(1))
        }
    }

    @Test fun requiresAtLeastOneQuantityAndTotalsDoNotOverflow() {
        assertNotNull(parseProductionQuantities(flavors, emptyMap()).error)
        assertEquals("18446744073709551614", totalUnits(listOf(Long.MAX_VALUE, Long.MAX_VALUE)))
    }

    @Test fun datePickerPreservesLocalDayAndTimeWithoutUtcDayShift() {
        val zone = TimeZone.getTimeZone("America/Sao_Paulo")
        val local = Calendar.getInstance(zone).apply { clear(); set(2026, Calendar.OCTOBER, 22, 23, 30) }
        val selected = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { clear(); set(2026, Calendar.OCTOBER, 15) }
        val updated = Calendar.getInstance(zone).apply { timeInMillis = ProductionDates.withPickerDay(local.timeInMillis, selected.timeInMillis, zone) }
        assertEquals(15, updated.get(Calendar.DAY_OF_MONTH))
        assertEquals(23, updated.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, updated.get(Calendar.MINUTE))
        assertEquals(selected.timeInMillis, ProductionDates.pickerDay(updated.timeInMillis, zone))
    }
}
