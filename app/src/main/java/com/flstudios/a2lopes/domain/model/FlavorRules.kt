package com.flstudios.a2lopes.domain.model

import java.math.BigDecimal
import java.util.Locale

object FlavorNames {
    private val spaces = Regex("[\\s\\p{Z}]+")

    fun display(value: String): String = value.replace(spaces, " ").trim()
    fun key(value: String): String = display(value).lowercase(Locale.ROOT)
}

enum class FlavorField { NAME, PRICE }

class FlavorValidationException(val field: FlavorField, message: String) :
    IllegalArgumentException(message)

data class ValidFlavorInput(val name: String, val normalizedName: String, val priceCents: Long?)

object FlavorRules {
    private val pricePattern = Regex("[0-9]+([,.][0-9]{1,2})?")

    fun validate(name: String, price: String): ValidFlavorInput {
        val displayName = FlavorNames.display(name)
        if (displayName.isEmpty()) {
            throw FlavorValidationException(FlavorField.NAME, "Informe o nome do sabor.")
        }
        val normalizedName = FlavorNames.key(displayName)
        // Both persisted forms must fit the existing backup text contract.
        if (displayName.length > 16_384 || normalizedName.length > 16_384) {
            throw FlavorValidationException(FlavorField.NAME, "O nome do sabor é muito longo. Reduza o texto para até 16.384 caracteres.")
        }
        return ValidFlavorInput(displayName, normalizedName, parsePrice(price))
    }

    fun parsePrice(value: String): Long? {
        val text = value.trim()
        if (text.isEmpty()) return null
        val cents = if (pricePattern.matches(text)) {
            try {
                BigDecimal(text.replace(',', '.')).movePointRight(2).longValueExact()
            } catch (_: ArithmeticException) {
                null
            }
        } else null
        return cents ?: throw FlavorValidationException(
            FlavorField.PRICE, "Informe um preço não negativo, com até 2 casas decimais, como 5,50.",
        )
    }

    fun priceText(cents: Long?): String =
        cents?.let { BigDecimal.valueOf(it, 2).toPlainString().replace('.', ',') }.orEmpty()
}
