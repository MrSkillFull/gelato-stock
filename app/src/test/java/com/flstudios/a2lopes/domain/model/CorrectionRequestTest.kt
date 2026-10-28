package com.flstudios.a2lopes.domain.model

import org.junit.Assert.*
import org.junit.Test

class CorrectionRequestTest {
    @Test fun reasonIsNormalizedAndZeroCountIsValid() {
        val request = CorrectionRequest("id", CorrectionType.ADJUSTMENT, " \u00a0 Contagem \n  física  ", 1, 0).normalized()
        assertEquals("Contagem física", request.reason)
        assertEquals(0L, request.quantity)
    }
    @Test fun invalidRequestsRejectBlankReasonsInvalidQuantitiesAndAmbiguousTargets() {
        val loss = CorrectionRequest("id", CorrectionType.LOSS, "Perda", 1, 2)
        listOf(loss.copy(reason = " \n \u00a0"), loss.copy(quantity = 0), loss.copy(quantity = -1), loss.copy(flavorId = null),
            loss.copy(operationId = ""), loss.copy(sourceId = "p"), loss.copy(type = CorrectionType.ADJUSTMENT, quantity = -1),
            CorrectionRequest("r", CorrectionType.SALE_REVERSAL, "Erro"),
            CorrectionRequest("r", CorrectionType.PRODUCTION_REVERSAL, "Erro", 1, sourceId = "p")).forEach {
            try { it.normalized(); fail("Pedido inválido aceito: $it") } catch (_: IllegalArgumentException) { }
        }
    }
}
