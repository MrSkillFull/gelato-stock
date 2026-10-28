package com.flstudios.a2lopes.presentation.production

import com.flstudios.a2lopes.domain.model.Flavor
import com.flstudios.a2lopes.domain.model.ProductionQuantity
import java.math.BigInteger
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

data class ParsedProduction(
    val items: List<ProductionQuantity>,
    val errors: Map<Long, String>,
    val error: String?,
)

/** Converte campos de texto; as regras de persistência continuam no repositório. */
fun parseProductionQuantities(flavors: List<Flavor>, quantities: Map<Long, String>): ParsedProduction {
    val errors = mutableMapOf<Long, String>()
    val items = flavors.filter { it.active }.mapNotNull { flavor ->
        val raw = quantities[flavor.id].orEmpty().trim()
        if (raw.isEmpty()) return@mapNotNull null
        val quantity = raw.takeIf { it.all { character -> character in '0'..'9' } }?.toLongOrNull()
        if (quantity == null || quantity <= 0) {
            errors[flavor.id] = "Informe uma quantidade inteira maior que zero."
            null
        } else ProductionQuantity(flavor.id, quantity)
    }
    return ParsedProduction(items, errors, when {
        errors.isNotEmpty() -> "Confira as quantidades informadas."
        items.isEmpty() -> "Informe a quantidade de pelo menos um sabor."
        else -> null
    })
}

// O total da tela pode ultrapassar Long mesmo com cada saldo individual válido.
fun totalUnits(quantities: Iterable<Long>): String = quantities.fold(BigInteger.ZERO) { total, quantity ->
    total + BigInteger.valueOf(quantity)
}.toString()

object ProductionDates {
    fun text(timestamp: Long, zone: TimeZone = TimeZone.getDefault()): String =
        SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("pt-BR")).apply { timeZone = zone }.format(timestamp)

    /** O DatePicker Material 3 representa dias em UTC, independentemente do fuso local. */
    fun pickerDay(timestamp: Long, zone: TimeZone = TimeZone.getDefault()): Long {
        val local = Calendar.getInstance(zone).apply { timeInMillis = timestamp }
        return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
        }.timeInMillis
    }

    fun withPickerDay(timestamp: Long, day: Long, zone: TimeZone = TimeZone.getDefault()): Long {
        val selected = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = day }
        return Calendar.getInstance(zone).apply {
            timeInMillis = timestamp
            set(selected.get(Calendar.YEAR), selected.get(Calendar.MONTH), selected.get(Calendar.DAY_OF_MONTH))
        }.timeInMillis
    }
}
