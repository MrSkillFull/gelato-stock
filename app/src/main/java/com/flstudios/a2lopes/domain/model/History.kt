package com.flstudios.a2lopes.domain.model

import java.math.BigInteger

enum class HistoryFilter { ALL, PRODUCTIONS, SALES, MOVEMENTS, CORRECTIONS }
enum class HistoryKind { PRODUCTION, SALE, MOVEMENT, CORRECTION }

data class HistoryEntry(
    val id: String,
    val kind: HistoryKind,
    val occurredAt: Long,
    val units: String,
    val flavorCount: Long = 0,
    val flavorName: String? = null,
    val movementType: String? = null,
    val notes: String? = null,
    val originId: String? = null,
    val originKind: HistoryKind? = null,
    val reference: String? = null,
    val correctionType: CorrectionType? = null,
    val reversalId: String? = null,
) {
    val key: String get() = "${kind.name}:$id"
}

data class HistoryItem(val flavorId: Long, val flavorName: String, val quantity: Long, val unitPriceCents: Long?)

data class HistoryDetail(
    val id: String,
    val kind: HistoryKind,
    val occurredAt: Long,
    val createdAt: Long,
    val notes: String?,
    val items: List<HistoryItem>,
    val correctionType: CorrectionType? = null,
    val correctionItems: List<CorrectionHistoryItem> = emptyList(),
    val reversalId: String? = null,
    val sourceKind: HistoryKind? = null,
    val sourceId: String? = null,
) {
    val totalUnits: String get() = items.fold(BigInteger.ZERO) { sum, item -> sum + BigInteger.valueOf(item.quantity) }.toString()
    val totalCents: Long? get() = if (kind == HistoryKind.SALE) {
        saleTotalCents(items.map { SaleQuantity(it.flavorId, it.quantity, it.unitPriceCents) })
    } else null
}

data class HistoryPage(val entries: List<HistoryEntry>, val hasNext: Boolean)
