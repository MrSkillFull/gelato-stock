package com.flstudios.a2lopes.domain.repository

import com.flstudios.a2lopes.domain.model.Sale
import com.flstudios.a2lopes.domain.model.SaleRequest

interface SaleRepository {
    suspend fun record(request: SaleRequest): Sale
}
