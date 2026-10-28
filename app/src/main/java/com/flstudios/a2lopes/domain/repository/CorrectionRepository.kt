package com.flstudios.a2lopes.domain.repository

import com.flstudios.a2lopes.domain.model.CorrectionReceipt
import com.flstudios.a2lopes.domain.model.CorrectionRequest

interface CorrectionRepository {
    suspend fun record(request: CorrectionRequest): CorrectionReceipt
}
