package com.flstudios.a2lopes.data.repository

import androidx.room.withTransaction
import com.flstudios.a2lopes.data.local.AppDatabase
import com.flstudios.a2lopes.domain.model.*
import com.flstudios.a2lopes.domain.repository.HistoryRepository
import com.flstudios.a2lopes.domain.repository.HistoryRepository.Companion.PAGE_SIZE
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.math.BigInteger

class RoomHistoryRepository(private val database: AppDatabase) : HistoryRepository {
    private val dao = database.historyDao()

    override fun observePage(filter: HistoryFilter, page: Int): Flow<HistoryPage> {
        require(page >= 0 && page <= Int.MAX_VALUE / PAGE_SIZE)
        val offset = page * PAGE_SIZE
        return if (filter == HistoryFilter.MOVEMENTS) {
            dao.observeMovements(PAGE_SIZE + 1, offset).map { rows ->
                HistoryPage(rows.take(PAGE_SIZE).map { row ->
                    HistoryEntry(row.id.toString(), HistoryKind.MOVEMENT, row.createdAt,
                        (if (row.quantityDelta > 0) "+" else "") + row.quantityDelta,
                        flavorName = row.flavorName, movementType = row.type, notes = row.notes,
                        originId = row.correctionId ?: row.productionId ?: row.saleId,
                        originKind = when { row.correctionId != null -> HistoryKind.CORRECTION; row.productionId != null -> HistoryKind.PRODUCTION; row.saleId != null -> HistoryKind.SALE; else -> null },
                        reference = "${row.referenceType} #${row.referenceId}")
                }, rows.size > PAGE_SIZE)
            }
        } else {
            dao.observeEvents(filter.name, PAGE_SIZE + 1, offset).map { rows ->
                HistoryPage(rows.take(PAGE_SIZE).map { row ->
                    val units = row.quantityParts?.split(',')?.fold(BigInteger.ZERO) { sum, part -> sum + part.toBigInteger() } ?: BigInteger.ZERO
                    HistoryEntry(row.id, HistoryKind.valueOf(row.kind), row.occurredAt,
                        (if (row.kind == "CORRECTION" && units.signum() > 0) "+" else "") + units, row.flavorCount,
                        correctionType = row.correctionType?.let(CorrectionType::valueOf), reversalId = row.reversalId,
                        notes = row.notes, flavorName = row.flavorName)
                }, rows.size > PAGE_SIZE)
            }
        }
    }

    override suspend fun detail(kind: HistoryKind, id: String): HistoryDetail? = database.withTransaction {
        require(kind != HistoryKind.MOVEMENT)
        if (kind == HistoryKind.CORRECTION) {
            return@withTransaction database.correctionDao().find(id)?.let { correction ->
                HistoryDetail(id, kind, correction.createdAt, correction.createdAt, correction.reason, emptyList(),
                    correctionType = CorrectionType.valueOf(correction.type), correctionItems = database.correctionDao().historyItems(id),
                    sourceKind = when { correction.saleId != null -> HistoryKind.SALE; correction.productionId != null -> HistoryKind.PRODUCTION; else -> null },
                    sourceId = correction.saleId ?: correction.productionId)
            }
        }
        val header = if (kind == HistoryKind.PRODUCTION) dao.productionHeader(id) else dao.saleHeader(id)
        header?.let {
            HistoryDetail(it.id, kind, it.occurredAt, it.createdAt, it.notes,
                if (kind == HistoryKind.PRODUCTION) dao.productionItems(id) else dao.saleItems(id),
                reversalId = database.correctionDao().reversal(id.takeIf { kind == HistoryKind.SALE }, id.takeIf { kind == HistoryKind.PRODUCTION }))
        }
    }

    override fun observeDetail(kind: HistoryKind, id: String): Flow<HistoryDetail?> =
        if (kind == HistoryKind.PRODUCTION || kind == HistoryKind.SALE) database.correctionDao()
            .observeReversal(id.takeIf { kind == HistoryKind.SALE }, id.takeIf { kind == HistoryKind.PRODUCTION })
            .map { detail(kind, id) }
        else flow { emit(detail(kind, id)) }
}
