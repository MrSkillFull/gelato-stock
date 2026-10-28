package com.flstudios.a2lopes.data.backup

import android.util.JsonReader
import android.util.JsonToken
import android.util.JsonWriter
import com.flstudios.a2lopes.data.local.entity.*
import com.flstudios.a2lopes.domain.model.BackupInvalid
import java.io.*
import java.nio.charset.CodingErrorAction

/** Writes format 2/schema 6; explicitly reads legacy format 1/schema 5. */
object BackupJson {
    const val MAX_BYTES = 32 * 1024 * 1024
    const val MAX_RECORDS = 100_000
    private const val MAX_TEXT = 16_384
    private fun invalid(): Nothing = throw BackupInvalid("Arquivo de backup inválido ou incompleto.")
    private fun JsonReader.text(): String {
        if (peek() != JsonToken.STRING) invalid()
        return nextString().also { if (it.length > MAX_TEXT) invalid() }
    }
    private fun JsonReader.number(): Long {
        if (peek() != JsonToken.NUMBER) invalid()
        val value = nextString()
        if (!Regex("-?(0|[1-9][0-9]*)").matches(value)) invalid()
        return value.toLongOrNull() ?: invalid()
    }
    private fun JsonReader.bool(): Boolean { if (peek() != JsonToken.BOOLEAN) invalid(); return nextBoolean() }
    private fun <T> JsonReader.nullable(read: JsonReader.() -> T): T? =
        if (peek() == JsonToken.NULL) { nextNull(); null } else read()
    private fun JsonReader.row(columns: Set<String>, read: (String) -> Unit) {
        val seen = mutableSetOf<String>()
        beginObject()
        while (hasNext()) {
            val name = nextName()
            if (name !in columns || !seen.add(name)) invalid()
            read(name)
        }
        endObject()
        if (seen != columns) invalid()
    }
    fun read(input: InputStream): BackupSnapshot {
        try {
            val limited = object : FilterInputStream(input) {
                var count = 0L
                private fun checked(n: Int): Int { if (n > 0) count += n; if (count > MAX_BYTES) throw BackupInvalid("O backup excede o limite de 32 MiB."); return n }
                override fun read(): Int = super.read().also { if (it >= 0) checked(1) }
                override fun read(b: ByteArray, off: Int, len: Int): Int = checked(`in`.read(b, off, len))
            }
            val decoder = Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)
            return JsonReader(InputStreamReader(limited, decoder)).use { r ->
                r.isLenient = false
                var format = ""; var version = 0L; var schema = 0L; var created = 0L; var dataset = ""
                var count = 0
                var threshold: Long? = null
                var flavors = emptyList<FlavorEntity>()
                var production_sessions = emptyList<ProductionSessionEntity>()
                var production_items = emptyList<ProductionItemEntity>()
                var sales = emptyList<SaleEntity>()
                var sale_items = emptyList<SaleItemEntity>()
                var corrections = emptyList<CorrectionEntity>()
                var correction_items = emptyList<CorrectionItemEntity>()
                var stock_movements = emptyList<StockMovementEntity>()
                r.row(setOf("format","formatVersion","schemaVersion","createdAt","datasetId","settings","flavors","production_sessions","production_items","sales","sale_items","corrections","correction_items","stock_movements")) { key ->
                    when (key) {
                        "format" -> format = r.text()
                        "formatVersion" -> version = r.number()
                        "schemaVersion" -> schema = r.number()
                        "createdAt" -> created = r.number()
                        "datasetId" -> dataset = r.text()
                        "settings" -> {
                            r.beginObject()
                            while (r.hasNext()) {
                                if (r.nextName() != "lowStockThreshold" || threshold != null) invalid()
                                threshold = r.number()
                            }
                            r.endObject()
                        }
                        "flavors" -> {
                            val rows = mutableListOf<FlavorEntity>()
                            r.beginArray()
                            while (r.hasNext()) {
                                if (++count > MAX_RECORDS) throw BackupInvalid("O backup excede 100.000 registros.")
                                rows.add(readFlavor(r))
                            }
                            r.endArray(); flavors = rows.toList()
                        }
                        "production_sessions" -> {
                            val rows = mutableListOf<ProductionSessionEntity>()
                            r.beginArray()
                            while (r.hasNext()) {
                                if (++count > MAX_RECORDS) throw BackupInvalid("O backup excede 100.000 registros.")
                                rows.add(readProductionSession(r))
                            }
                            r.endArray(); production_sessions = rows.toList()
                        }
                        "production_items" -> {
                            val rows = mutableListOf<ProductionItemEntity>()
                            r.beginArray()
                            while (r.hasNext()) {
                                if (++count > MAX_RECORDS) throw BackupInvalid("O backup excede 100.000 registros.")
                                rows.add(readProductionItem(r))
                            }
                            r.endArray(); production_items = rows.toList()
                        }
                        "sales" -> {
                            val rows = mutableListOf<SaleEntity>()
                            r.beginArray()
                            while (r.hasNext()) {
                                if (++count > MAX_RECORDS) throw BackupInvalid("O backup excede 100.000 registros.")
                                rows.add(readSale(r))
                            }
                            r.endArray(); sales = rows.toList()
                        }
                        "sale_items" -> {
                            val rows = mutableListOf<SaleItemEntity>()
                            r.beginArray()
                            while (r.hasNext()) {
                                if (++count > MAX_RECORDS) throw BackupInvalid("O backup excede 100.000 registros.")
                                rows.add(readSaleItem(r))
                            }
                            r.endArray(); sale_items = rows.toList()
                        }
                        "corrections" -> {
                            val rows = mutableListOf<CorrectionEntity>()
                            r.beginArray()
                            while (r.hasNext()) {
                                if (++count > MAX_RECORDS) throw BackupInvalid("O backup excede 100.000 registros.")
                                rows.add(readCorrection(r))
                            }
                            r.endArray(); corrections = rows.toList()
                        }
                        "correction_items" -> {
                            val rows = mutableListOf<CorrectionItemEntity>()
                            r.beginArray()
                            while (r.hasNext()) {
                                if (++count > MAX_RECORDS) throw BackupInvalid("O backup excede 100.000 registros.")
                                rows.add(readCorrectionItem(r))
                            }
                            r.endArray(); correction_items = rows.toList()
                        }
                        "stock_movements" -> {
                            val rows = mutableListOf<StockMovementEntity>()
                            r.beginArray()
                            while (r.hasNext()) {
                                if (++count > MAX_RECORDS) throw BackupInvalid("O backup excede 100.000 registros.")
                                rows.add(readStockMovement(r))
                            }
                            r.endArray(); stock_movements = rows.toList()
                        }
                    }
                }
                if (r.peek() != JsonToken.END_DOCUMENT) invalid()
                if (format != "2lopes-backup") throw BackupInvalid("Formato de backup não reconhecido.")
                val limit = when {
                    version == 1L && schema == 5L && threshold == null -> com.flstudios.a2lopes.domain.model.DEFAULT_LOW_STOCK_THRESHOLD
                    version == 2L && schema == 6L && threshold != null -> threshold!!
                    else -> throw BackupInvalid("Versão de backup incompatível ou configuração ausente. Use formato 2/esquema 6 ou formato 1/esquema 5.")
                }
                BackupSnapshot(created, dataset,flavors,production_sessions,production_items,sales,sale_items,corrections,correction_items,stock_movements, limit).also(BackupValidator::validate)
            }
        } catch (e: BackupInvalid) { throw e }
        catch (e: IOException) { throw e }
        catch (_: Exception) { invalid() }
    }

    fun write(snapshot: BackupSnapshot, output: OutputStream) {
        BackupValidator.validate(snapshot)
        JsonWriter(OutputStreamWriter(output, Charsets.UTF_8)).use { w ->
            w.beginObject()
            w.name("format").value("2lopes-backup")
            w.name("formatVersion").value(2)
            w.name("schemaVersion").value(6)
            w.name("createdAt").value(snapshot.createdAt)
            w.name("datasetId").value(snapshot.datasetId)
            w.name("settings").beginObject().name("lowStockThreshold").value(snapshot.lowStockThreshold).endObject()
            w.name("flavors").beginArray()
            snapshot.flavors.forEach { row ->
                w.beginObject()
                w.name("id").value(row.id)
                w.name("name").value(row.name)
                w.name("defaultPriceCents").value(row.defaultPriceCents)
                w.name("active").value(row.active)
                w.name("normalizedName").value(row.normalizedName)
                w.endObject()
            }
            w.endArray()
            w.name("production_sessions").beginArray()
            snapshot.production_sessions.forEach { row ->
                w.beginObject()
                w.name("id").value(row.id)
                w.name("producedAt").value(row.producedAt)
                w.name("createdAt").value(row.createdAt)
                w.name("notes").value(row.notes)
                w.endObject()
            }
            w.endArray()
            w.name("production_items").beginArray()
            snapshot.production_items.forEach { row ->
                w.beginObject()
                w.name("id").value(row.id)
                w.name("sessionId").value(row.sessionId)
                w.name("flavorId").value(row.flavorId)
                w.name("quantity").value(row.quantity)
                w.endObject()
            }
            w.endArray()
            w.name("sales").beginArray()
            snapshot.sales.forEach { row ->
                w.beginObject()
                w.name("id").value(row.id)
                w.name("soldAt").value(row.soldAt)
                w.name("createdAt").value(row.createdAt)
                w.endObject()
            }
            w.endArray()
            w.name("sale_items").beginArray()
            snapshot.sale_items.forEach { row ->
                w.beginObject()
                w.name("id").value(row.id)
                w.name("saleId").value(row.saleId)
                w.name("flavorId").value(row.flavorId)
                w.name("quantity").value(row.quantity)
                w.name("unitPriceCents").value(row.unitPriceCents)
                w.endObject()
            }
            w.endArray()
            w.name("corrections").beginArray()
            snapshot.corrections.forEach { row ->
                w.beginObject()
                w.name("id").value(row.id)
                w.name("type").value(row.type)
                w.name("reason").value(row.reason)
                w.name("createdAt").value(row.createdAt)
                w.name("flavorId").value(row.flavorId)
                w.name("inputQuantity").value(row.inputQuantity)
                w.name("saleId").value(row.saleId)
                w.name("productionId").value(row.productionId)
                w.endObject()
            }
            w.endArray()
            w.name("correction_items").beginArray()
            snapshot.correction_items.forEach { row ->
                w.beginObject()
                w.name("id").value(row.id)
                w.name("correctionId").value(row.correctionId)
                w.name("flavorId").value(row.flavorId)
                w.name("previousQuantity").value(row.previousQuantity)
                w.name("resultingQuantity").value(row.resultingQuantity)
                w.name("quantityDelta").value(row.quantityDelta)
                w.name("sourceProductionItemId").value(row.sourceProductionItemId)
                w.name("sourceSaleItemId").value(row.sourceSaleItemId)
                w.endObject()
            }
            w.endArray()
            w.name("stock_movements").beginArray()
            snapshot.stock_movements.forEach { row ->
                w.beginObject()
                w.name("id").value(row.id)
                w.name("flavorId").value(row.flavorId)
                w.name("type").value(row.type)
                w.name("quantityDelta").value(row.quantityDelta)
                w.name("referenceType").value(row.referenceType)
                w.name("referenceId").value(row.referenceId)
                w.name("createdAt").value(row.createdAt)
                w.name("notes").value(row.notes)
                w.name("productionItemId").value(row.productionItemId)
                w.name("saleItemId").value(row.saleItemId)
                w.name("correctionItemId").value(row.correctionItemId)
                w.endObject()
            }
            w.endArray()
            w.endObject()
        }
    }
    private fun readFlavor(r: JsonReader): FlavorEntity {
        var id: Long = 0L
        var name: String = ""
        var defaultPriceCents: Long? = null
        var active: Boolean = false
        var normalizedName: String = ""
        r.row(setOf("id","name","defaultPriceCents","active","normalizedName")) { key ->
            when (key) {
                "id" -> id = r.number()
                "name" -> name = r.text()
                "defaultPriceCents" -> defaultPriceCents = r.nullable { number() }
                "active" -> active = r.bool()
                "normalizedName" -> normalizedName = r.text()
            }
        }
        return FlavorEntity(id,name,defaultPriceCents,active,normalizedName)
    }
    private fun readProductionSession(r: JsonReader): ProductionSessionEntity {
        var id: String = ""
        var producedAt: Long = 0L
        var createdAt: Long = 0L
        var notes: String? = null
        r.row(setOf("id","producedAt","createdAt","notes")) { key ->
            when (key) {
                "id" -> id = r.text()
                "producedAt" -> producedAt = r.number()
                "createdAt" -> createdAt = r.number()
                "notes" -> notes = r.nullable { text() }
            }
        }
        return ProductionSessionEntity(id,producedAt,createdAt,notes)
    }
    private fun readProductionItem(r: JsonReader): ProductionItemEntity {
        var id: Long = 0L
        var sessionId: String = ""
        var flavorId: Long = 0L
        var quantity: Long = 0L
        r.row(setOf("id","sessionId","flavorId","quantity")) { key ->
            when (key) {
                "id" -> id = r.number()
                "sessionId" -> sessionId = r.text()
                "flavorId" -> flavorId = r.number()
                "quantity" -> quantity = r.number()
            }
        }
        return ProductionItemEntity(id,sessionId,flavorId,quantity)
    }
    private fun readSale(r: JsonReader): SaleEntity {
        var id: String = ""
        var soldAt: Long = 0L
        var createdAt: Long = 0L
        r.row(setOf("id","soldAt","createdAt")) { key ->
            when (key) {
                "id" -> id = r.text()
                "soldAt" -> soldAt = r.number()
                "createdAt" -> createdAt = r.number()
            }
        }
        return SaleEntity(id,soldAt,createdAt)
    }
    private fun readSaleItem(r: JsonReader): SaleItemEntity {
        var id: Long = 0L
        var saleId: String = ""
        var flavorId: Long = 0L
        var quantity: Long = 0L
        var unitPriceCents: Long? = null
        r.row(setOf("id","saleId","flavorId","quantity","unitPriceCents")) { key ->
            when (key) {
                "id" -> id = r.number()
                "saleId" -> saleId = r.text()
                "flavorId" -> flavorId = r.number()
                "quantity" -> quantity = r.number()
                "unitPriceCents" -> unitPriceCents = r.nullable { number() }
            }
        }
        return SaleItemEntity(id,saleId,flavorId,quantity,unitPriceCents)
    }
    private fun readCorrection(r: JsonReader): CorrectionEntity {
        var id: String = ""
        var type: String = ""
        var reason: String = ""
        var createdAt: Long = 0L
        var flavorId: Long? = null
        var inputQuantity: Long? = null
        var saleId: String? = null
        var productionId: String? = null
        r.row(setOf("id","type","reason","createdAt","flavorId","inputQuantity","saleId","productionId")) { key ->
            when (key) {
                "id" -> id = r.text()
                "type" -> type = r.text()
                "reason" -> reason = r.text()
                "createdAt" -> createdAt = r.number()
                "flavorId" -> flavorId = r.nullable { number() }
                "inputQuantity" -> inputQuantity = r.nullable { number() }
                "saleId" -> saleId = r.nullable { text() }
                "productionId" -> productionId = r.nullable { text() }
            }
        }
        return CorrectionEntity(id,type,reason,createdAt,flavorId,inputQuantity,saleId,productionId)
    }
    private fun readCorrectionItem(r: JsonReader): CorrectionItemEntity {
        var id: Long = 0L
        var correctionId: String = ""
        var flavorId: Long = 0L
        var previousQuantity: Long = 0L
        var resultingQuantity: Long = 0L
        var quantityDelta: Long = 0L
        var sourceProductionItemId: Long? = null
        var sourceSaleItemId: Long? = null
        r.row(setOf("id","correctionId","flavorId","previousQuantity","resultingQuantity","quantityDelta","sourceProductionItemId","sourceSaleItemId")) { key ->
            when (key) {
                "id" -> id = r.number()
                "correctionId" -> correctionId = r.text()
                "flavorId" -> flavorId = r.number()
                "previousQuantity" -> previousQuantity = r.number()
                "resultingQuantity" -> resultingQuantity = r.number()
                "quantityDelta" -> quantityDelta = r.number()
                "sourceProductionItemId" -> sourceProductionItemId = r.nullable { number() }
                "sourceSaleItemId" -> sourceSaleItemId = r.nullable { number() }
            }
        }
        return CorrectionItemEntity(id,correctionId,flavorId,previousQuantity,resultingQuantity,quantityDelta,sourceProductionItemId,sourceSaleItemId)
    }
    private fun readStockMovement(r: JsonReader): StockMovementEntity {
        var id: Long = 0L
        var flavorId: Long = 0L
        var type: String = ""
        var quantityDelta: Long = 0L
        var referenceType: String = ""
        var referenceId: Long = 0L
        var createdAt: Long = 0L
        var notes: String? = null
        var productionItemId: Long? = null
        var saleItemId: Long? = null
        var correctionItemId: Long? = null
        r.row(setOf("id","flavorId","type","quantityDelta","referenceType","referenceId","createdAt","notes","productionItemId","saleItemId","correctionItemId")) { key ->
            when (key) {
                "id" -> id = r.number()
                "flavorId" -> flavorId = r.number()
                "type" -> type = r.text()
                "quantityDelta" -> quantityDelta = r.number()
                "referenceType" -> referenceType = r.text()
                "referenceId" -> referenceId = r.number()
                "createdAt" -> createdAt = r.number()
                "notes" -> notes = r.nullable { text() }
                "productionItemId" -> productionItemId = r.nullable { number() }
                "saleItemId" -> saleItemId = r.nullable { number() }
                "correctionItemId" -> correctionItemId = r.nullable { number() }
            }
        }
        return StockMovementEntity(id,flavorId,type,quantityDelta,referenceType,referenceId,createdAt,notes,productionItemId,saleItemId,correctionItemId)
    }
}
