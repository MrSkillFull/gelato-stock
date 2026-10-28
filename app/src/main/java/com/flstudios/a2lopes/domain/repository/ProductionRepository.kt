package com.flstudios.a2lopes.domain.repository

import com.flstudios.a2lopes.domain.model.ProductionRequest
import com.flstudios.a2lopes.domain.model.ProductionSession

interface ProductionRepository {
    suspend fun record(request: ProductionRequest): ProductionSession
}
