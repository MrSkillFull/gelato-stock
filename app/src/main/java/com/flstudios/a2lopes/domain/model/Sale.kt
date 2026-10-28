package com.flstudios.a2lopes.domain.model

data class SaleQuantity(val flavorId: Long, val quantity: Long, val unitPriceCents: Long?)

/** O preço é a cotação exibida ao operador; null significa não informado. */
data class SaleRequest(val operationId: String, val soldAt: Long, val items: List<SaleQuantity>) {
    fun validate() {
        require(operationId.isNotBlank()) { "Informe o identificador da venda." }
        require(items.isNotEmpty()) { "Adicione pelo menos um sabor ao carrinho." }
        require(items.all { it.flavorId > 0 && it.quantity > 0 && (it.unitPriceCents == null || it.unitPriceCents >= 0) }) {
            "Informe sabores, quantidades e preços válidos."
        }
        require(items.map { it.flavorId }.distinct().size == items.size) { "Cada sabor deve aparecer uma única vez na venda." }
        saleTotalCents(items)
    }
}

/** Total conhecido somente quando todos os itens têm preço; zero informado permanece zero. */
fun saleTotalCents(items: List<SaleQuantity>): Long? {
    if (items.any { it.unitPriceCents == null }) return null
    try {
        return items.fold(0L) { sum, item -> Math.addExact(sum, Math.multiplyExact(item.quantity, item.unitPriceCents!!)) }
    } catch (_: ArithmeticException) {
        throw IllegalArgumentException("O valor total ultrapassa o limite permitido. Reduza as quantidades.")
    }
}

data class Sale(val id: String, val soldAt: Long, val createdAt: Long)
