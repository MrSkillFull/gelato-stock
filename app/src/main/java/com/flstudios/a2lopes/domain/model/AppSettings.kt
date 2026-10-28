package com.flstudios.a2lopes.domain.model

const val DEFAULT_LOW_STOCK_THRESHOLD = 10L

object SettingsRules {
    fun parseThreshold(text: String): Long {
        val value = text.trim()
        require(value.matches(Regex("[0-9]+"))) { "Informe um número inteiro igual ou maior que zero." }
        return value.toLongOrNull()
            ?: throw IllegalArgumentException("O limite ultrapassa o maior inteiro permitido.")
    }
    fun validateThreshold(value: Long) {
        require(value >= 0) { "O limite de estoque baixo deve ser igual ou maior que zero." }
    }
}
