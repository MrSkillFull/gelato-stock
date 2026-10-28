package com.flstudios.a2lopes.data.backup

import com.flstudios.a2lopes.domain.model.FlavorNames
import com.flstudios.a2lopes.domain.model.BackupInvalid
import com.flstudios.a2lopes.domain.model.SaleQuantity
import com.flstudios.a2lopes.domain.model.saleTotalCents
import java.math.BigInteger
import java.util.UUID

/** Cross-table invariants, independent of SQLite accepting the foreign keys. */
object BackupValidator {
    private fun valid(value: Boolean) {
        if (!value) throw BackupInvalid("O backup contém registros, vínculos ou estoque inconsistentes.")
    }
    private fun <T, K> unique(rows: List<T>, key: (T) -> K): Map<K, T> =
        rows.associateBy(key).also { valid(it.size == rows.size) }
    private fun date(value: Long) = valid(value in 0..253402300799999L)
    private fun key(value: String) = valid(value.isNotBlank() && value.length <= 16_384)
    private fun price(value: Long?) = valid(value == null || value >= 0)
    private fun text(value: String?) = valid(value == null || value.length <= 16_384)

    fun validate(b: BackupSnapshot) {
        if (b.lowStockThreshold < 0) throw BackupInvalid("O limite de estoque baixo do backup deve ser igual ou maior que zero.")
        valid(b.recordCount <= BackupJson.MAX_RECORDS)
        date(b.createdAt)
        try { valid(UUID.fromString(b.datasetId).toString() == b.datasetId) }
        catch (_: IllegalArgumentException) { throw BackupInvalid("Identificador do conjunto de dados inválido.") }
        val flavors = unique(b.flavors) { it.id }
        unique(b.flavors) { it.normalizedName }
        b.flavors.forEach {
            text(it.name); text(it.normalizedName)
            valid(it.id > 0 && it.name.isNotBlank() && FlavorNames.display(it.name) == it.name &&
                it.normalizedName == FlavorNames.key(it.name)); price(it.defaultPriceCents)
        }
        val productions = unique(b.production_sessions) { it.id }
        val sales = unique(b.sales) { it.id }
        val corrections = unique(b.corrections) { it.id }
        val pi = unique(b.production_items) { it.id }
        val si = unique(b.sale_items) { it.id }
        val ci = unique(b.correction_items) { it.id }
        unique(b.production_items) { it.sessionId to it.flavorId }
        unique(b.sale_items) { it.saleId to it.flavorId }
        unique(b.correction_items) { it.correctionId to it.flavorId }
        unique(b.stock_movements) { it.id }
        val byProduction = b.production_items.groupBy { it.sessionId }
        val bySale = b.sale_items.groupBy { it.saleId }
        val byCorrection = b.correction_items.groupBy { it.correctionId }
        b.production_sessions.forEach { key(it.id); text(it.notes); date(it.createdAt); date(it.producedAt); valid(!byProduction[it.id].isNullOrEmpty()) }
        b.sales.forEach { key(it.id); date(it.createdAt); date(it.soldAt); valid(!bySale[it.id].isNullOrEmpty()) }
        b.production_items.forEach { valid(it.id > 0 && it.sessionId in productions && it.flavorId in flavors && it.quantity > 0) }
        b.sale_items.forEach { valid(it.id > 0 && it.saleId in sales && it.flavorId in flavors && it.quantity > 0); price(it.unitPriceCents) }
        bySale.values.forEach { items ->
            try { saleTotalCents(items.map { SaleQuantity(it.flavorId, it.quantity, it.unitPriceCents) }) }
            catch (_: IllegalArgumentException) { throw BackupInvalid("O backup contém uma venda com valor total fora do limite permitido.") }
        }
        unique(b.corrections.filter { it.saleId != null }) { it.saleId }
        unique(b.corrections.filter { it.productionId != null }) { it.productionId }
        b.corrections.forEach { c ->
            key(c.id); date(c.createdAt)
            text(c.reason)
            valid(c.reason.isNotBlank() && FlavorNames.display(c.reason) == c.reason)
            val items = byCorrection[c.id].orEmpty()
            valid(items.isNotEmpty())
            when (c.type) {
                "LOSS", "ADJUSTMENT" -> {
                    valid(c.flavorId in flavors && c.saleId == null && c.productionId == null &&
                        c.inputQuantity != null && c.inputQuantity >= if (c.type == "LOSS") 1L else 0L)
                    valid(items.size == 1 && items.single().flavorId == c.flavorId)
                }
                "SALE_REVERSAL" -> {
                    valid(c.saleId in sales && c.productionId == null && c.flavorId == null && c.inputQuantity == null)
                    valid(items.map { it.sourceSaleItemId }.toSet() == bySale[c.saleId].orEmpty().map { it.id }.toSet())
                }
                "PRODUCTION_REVERSAL" -> {
                    valid(c.productionId in productions && c.saleId == null && c.flavorId == null && c.inputQuantity == null)
                    valid(items.map { it.sourceProductionItemId }.toSet() == byProduction[c.productionId].orEmpty().map { it.id }.toSet())
                }
                else -> valid(false)
            }
        }
        b.correction_items.forEach { i ->
            valid(i.id > 0 && i.correctionId in corrections && i.flavorId in flavors)
            valid(i.previousQuantity >= 0 && i.resultingQuantity >= 0 && i.quantityDelta == i.resultingQuantity - i.previousQuantity)
            val c = corrections.getValue(i.correctionId)
            when (c.type) {
                "LOSS", "ADJUSTMENT" -> {
                    valid(i.sourceSaleItemId == null && i.sourceProductionItemId == null)
                    if (c.type == "LOSS") valid(i.quantityDelta == -c.inputQuantity!!)
                    else valid(i.resultingQuantity == c.inputQuantity)
                }
                "SALE_REVERSAL" -> {
                    val source = si[i.sourceSaleItemId]
                    valid(source != null && source.saleId == c.saleId && source.flavorId == i.flavorId &&
                        i.quantityDelta == source.quantity && i.sourceProductionItemId == null)
                }
                "PRODUCTION_REVERSAL" -> {
                    val source = pi[i.sourceProductionItemId]
                    valid(source != null && source.sessionId == c.productionId && source.flavorId == i.flavorId &&
                        i.quantityDelta == -source.quantity && i.sourceSaleItemId == null)
                }
            }
        }
        val usedProduction = mutableSetOf<Long>()
        val usedSale = mutableSetOf<Long>()
        val usedCorrection = mutableSetOf<Long>()
        val balances = mutableMapOf<Long, BigInteger>()
        val auditBalances = mutableMapOf<Long, MutableSet<Long>>()
        b.stock_movements.sortedBy { it.id }.forEach { m ->
            text(m.notes)
            valid(m.id > 0 && m.flavorId in flavors && m.quantityDelta != 0L); date(m.createdAt)
            when (m.type) {
                "PRODUCTION" -> {
                    val i = pi[m.productionItemId]
                    valid(i != null && usedProduction.add(i.id) && i.flavorId == m.flavorId && m.quantityDelta == i.quantity &&
                        m.referenceType == "PRODUCTION_ITEM" && m.referenceId == i.id && m.saleItemId == null && m.correctionItemId == null)
                    val p = productions.getValue(i!!.sessionId)
                    valid(m.createdAt == p.createdAt && m.notes == p.notes)
                }
                "SALE" -> {
                    val i = si[m.saleItemId]
                    valid(i != null && usedSale.add(i.id) && i.flavorId == m.flavorId && m.quantityDelta == -i.quantity &&
                        m.referenceType == "SALE_ITEM" && m.referenceId == i.id && m.productionItemId == null && m.correctionItemId == null)
                    valid(m.createdAt == sales.getValue(i!!.saleId).createdAt && m.notes == null)
                }
                "LOSS", "ADJUSTMENT", "SALE_REVERSAL", "PRODUCTION_REVERSAL" -> {
                    val i = ci[m.correctionItemId]
                    valid(i != null && usedCorrection.add(i.id) && i.flavorId == m.flavorId && m.quantityDelta == i.quantityDelta &&
                        m.referenceType == "CORRECTION_ITEM" && m.referenceId == i.id && m.productionItemId == null && m.saleItemId == null)
                    val c = corrections.getValue(i!!.correctionId)
                    valid(m.type == c.type && m.createdAt == c.createdAt && m.notes == c.reason)
                    valid((balances[m.flavorId] ?: BigInteger.ZERO) == BigInteger.valueOf(i.previousQuantity))
                }
                else -> valid(false)
            }
            val next = (balances[m.flavorId] ?: BigInteger.ZERO) + BigInteger.valueOf(m.quantityDelta)
            valid(next.signum() >= 0 && next <= BigInteger.valueOf(Long.MAX_VALUE))
            balances[m.flavorId] = next
            auditBalances.getOrPut(m.flavorId) { mutableSetOf(0L) }.add(next.toLong())
        }
        valid(usedProduction == pi.keys && usedSale == si.keys && usedCorrection == ci.values.filter { it.quantityDelta != 0L }.map { it.id }.toSet())
        // A zero-difference conference has no movement ID; its audited balance must
        // still have existed in the ledger. Do not infer order from a device clock.
        ci.values.filter { it.quantityDelta == 0L }.forEach {
            valid(it.previousQuantity == 0L || it.previousQuantity in auditBalances[it.flavorId].orEmpty())
        }
    }
}
