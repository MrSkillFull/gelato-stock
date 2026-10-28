package com.flstudios.a2lopes.domain.model

import org.junit.Assert.*
import org.junit.Test

class SettingsRulesTest {
    @Test fun defaultAndNonNegativeIntegers() {
        assertEquals(10L, DEFAULT_LOW_STOCK_THRESHOLD)
        assertEquals(0L, SettingsRules.parseThreshold("0"))
        assertEquals(25L, SettingsRules.parseThreshold(" 025 "))
        assertEquals(Long.MAX_VALUE, SettingsRules.parseThreshold(Long.MAX_VALUE.toString()))
    }
    @Test fun invalidInputNeverBecomesAValue() {
        for (input in listOf("", " ", "-1", "+2", "1.5", "2,5", "texto", "1e2", "9223372036854775808")) {
            assertThrows(IllegalArgumentException::class.java) { SettingsRules.parseThreshold(input) }
        }
        assertThrows(IllegalArgumentException::class.java) { SettingsRules.validateThreshold(-1) }
    }
}
