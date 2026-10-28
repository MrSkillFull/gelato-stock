package com.flstudios.a2lopes.domain.model

import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class FlavorRulesTest {
    @Test
    fun normalizesSpacesAndCaseWithoutRemovingAccents() {
        val input = FlavorRules.validate(" \tCOCO\u00a0  Cremoso \n", "")
        assertEquals("COCO Cremoso", input.name)
        assertEquals("coco cremoso", input.normalizedName)
        assertEquals(FlavorNames.key("  Coco   Cremoso"), input.normalizedName)
        assertNotEquals(FlavorNames.key("Maracujá"), FlavorNames.key("Maracuja"))
    }

    @Test
    fun normalizationDoesNotDependOnDeviceLocale() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            assertEquals("kiwi", FlavorNames.key("KIWI"))
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test
    fun rejectsEmptyAndWhitespaceOnlyNames() {
        listOf("", "   ", "\t\n", "\u00a0").forEach { name ->
            val error = assertThrows(FlavorValidationException::class.java) {
                FlavorRules.validate(name, "")
            }
            assertEquals(FlavorField.NAME, error.field)
        }
    }

    @Test
    fun acceptsOptionalPriceAndExactNonNegativeCents() {
        assertNull(FlavorRules.parsePrice(""))
        assertNull(FlavorRules.parsePrice("  "))
        mapOf("0" to 0L, "0,00" to 0L, "5" to 500L, "5,5" to 550L,
            " 5,50 " to 550L, "12.34" to 1234L, "0,01" to 1L,
            "92233720368547758,07" to Long.MAX_VALUE).forEach { (input, expected) ->
            assertEquals(expected, FlavorRules.parsePrice(input))
        }
    }

    @Test
    fun rejectsInvalidNegativeFractionalAndOverflowPrices() {
        listOf("-1", "-0,01", "abc", "1,234", "1.234,50", "1e2", "NaN", "R$ 5,50",
            "1,", ",50", "5 50", "92233720368547758,08").forEach { input ->
            val error = assertThrows(FlavorValidationException::class.java) {
                FlavorRules.parsePrice(input)
            }
            assertEquals(FlavorField.PRICE, error.field)
        }
    }

    @Test
    fun formatsCentsWithoutLosingPrecision() {
        assertEquals("", FlavorRules.priceText(null))
        assertEquals("0,00", FlavorRules.priceText(0))
        assertEquals("5,50", FlavorRules.priceText(550))
        assertEquals(Long.MAX_VALUE, FlavorRules.parsePrice(FlavorRules.priceText(Long.MAX_VALUE)))
    }
}
