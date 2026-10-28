package com.flstudios.a2lopes.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.flstudios.a2lopes.domain.model.HistoryItem
import kotlinx.coroutines.flow.Flow

data class HistorySummaryRow(
    val id: String, val kind: String, val occurredAt: Long, val quantityParts: String?, val flavorCount: Long,
    val correctionType: String?, val reversalId: String?,
    val notes: String?, val flavorName: String?,
)
data class HistoryHeaderRow(val id: String, val occurredAt: Long, val createdAt: Long, val notes: String?)
data class HistoryMovementRow(
    val id: Long, val flavorName: String, val type: String, val quantityDelta: Long,
    val createdAt: Long, val notes: String?, val productionId: String?, val saleId: String?,
    val referenceType: String, val referenceId: Long,
    val correctionId: String?,
)

@Dao
interface HistoryDao {
    // LIMIT is applied before reading item summaries. No details or current prices are loaded here.
    // GROUP_CONCAT keeps integer quantities exact even when the event total exceeds SQLite's SUM range.
    @Query("""
        SELECT e.id, e.kind, e.occurredAt,
          CASE WHEN e.kind = 'PRODUCTION' THEN
            (SELECT GROUP_CONCAT(quantity) FROM production_items WHERE session_id = e.id)
          WHEN e.kind = 'SALE' THEN (SELECT GROUP_CONCAT(quantity) FROM sale_items WHERE sale_id = e.id)
          ELSE (SELECT GROUP_CONCAT(quantity_delta) FROM correction_items WHERE correction_id = e.id) END AS quantityParts,
          CASE WHEN e.kind = 'PRODUCTION' THEN
            (SELECT COUNT(*) FROM production_items WHERE session_id = e.id)
          WHEN e.kind = 'SALE' THEN (SELECT COUNT(*) FROM sale_items WHERE sale_id = e.id)
          ELSE (SELECT COUNT(*) FROM correction_items WHERE correction_id = e.id) END AS flavorCount,
          (SELECT type FROM corrections WHERE id = e.id AND e.kind = 'CORRECTION') AS correctionType,
          (SELECT id FROM corrections WHERE (sale_id = e.id AND e.kind = 'SALE') OR (production_id = e.id AND e.kind = 'PRODUCTION')) AS reversalId,
          (SELECT reason FROM corrections WHERE id = e.id AND e.kind = 'CORRECTION') AS notes,
          (SELECT f.name FROM corrections c JOIN flavors f ON f.id = c.flavor_id WHERE c.id = e.id AND e.kind = 'CORRECTION') AS flavorName
        FROM (
          SELECT id, 'PRODUCTION' AS kind, produced_at AS occurredAt FROM production_sessions WHERE :filter IN ('ALL', 'PRODUCTIONS')
          UNION ALL
          SELECT id, 'SALE' AS kind, sold_at AS occurredAt FROM sales WHERE :filter IN ('ALL', 'SALES')
          UNION ALL
          SELECT id, 'CORRECTION' AS kind, created_at AS occurredAt FROM corrections WHERE :filter IN ('ALL', 'CORRECTIONS')
          ORDER BY occurredAt DESC, kind ASC, id DESC LIMIT :limit OFFSET :offset
        ) e
        ORDER BY e.occurredAt DESC, e.kind ASC, e.id DESC
    """)
    fun observeEvents(filter: String, limit: Int, offset: Int): Flow<List<HistorySummaryRow>>

    @Query("""
        SELECT m.id, f.name AS flavorName, m.type, m.quantity_delta AS quantityDelta,
          m.created_at AS createdAt, m.notes, p.session_id AS productionId, s.sale_id AS saleId,
          m.reference_type AS referenceType, m.reference_id AS referenceId, c.correction_id AS correctionId
        FROM stock_movements m JOIN flavors f ON f.id = m.flavor_id
        LEFT JOIN production_items p ON p.id = m.production_item_id
        LEFT JOIN sale_items s ON s.id = m.sale_item_id
        LEFT JOIN correction_items c ON c.id = m.correction_item_id
        ORDER BY m.created_at DESC, m.id DESC LIMIT :limit OFFSET :offset
    """)
    fun observeMovements(limit: Int, offset: Int): Flow<List<HistoryMovementRow>>

    @Query("SELECT id, produced_at AS occurredAt, created_at AS createdAt, notes FROM production_sessions WHERE id = :id")
    suspend fun productionHeader(id: String): HistoryHeaderRow?

    @Query("SELECT id, sold_at AS occurredAt, created_at AS createdAt, NULL AS notes FROM sales WHERE id = :id")
    suspend fun saleHeader(id: String): HistoryHeaderRow?

    @Query("""
        SELECT i.flavor_id AS flavorId, f.name AS flavorName, i.quantity, NULL AS unitPriceCents
        FROM production_items i JOIN flavors f ON f.id = i.flavor_id
        WHERE i.session_id = :id ORDER BY f.normalized_name, i.flavor_id
    """)
    suspend fun productionItems(id: String): List<HistoryItem>

    @Query("""
        SELECT i.flavor_id AS flavorId, f.name AS flavorName, i.quantity, i.unit_price_cents AS unitPriceCents
        FROM sale_items i JOIN flavors f ON f.id = i.flavor_id
        WHERE i.sale_id = :id ORDER BY f.normalized_name, i.flavor_id
    """)
    suspend fun saleItems(id: String): List<HistoryItem>
}
