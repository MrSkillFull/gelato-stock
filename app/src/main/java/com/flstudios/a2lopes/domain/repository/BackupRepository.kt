package com.flstudios.a2lopes.domain.repository

import java.io.InputStream
import java.io.OutputStream

data class BackupSummary(
    val createdAt: Long,
    val flavors: Int,
    val productions: Int,
    val sales: Int,
    val corrections: Int,
    val movements: Int,
    val records: Int,
    val lowStockThreshold: Long = com.flstudios.a2lopes.domain.model.DEFAULT_LOW_STOCK_THRESHOLD,
)

/** Validated document retained until confirmation; presentation sees only its summary. */
interface BackupDocument { val summary: BackupSummary }

interface BackupRepository {
    suspend fun export(open: () -> OutputStream?)
    suspend fun inspect(open: () -> InputStream?): BackupDocument
    suspend fun restore(data: BackupDocument)
}
