package com.flstudios.a2lopes.domain.model

data class ProductionQuantity(val flavorId: Long, val quantity: Long)

/** Instantes em milissegundos UTC. O chamador conserva operationId entre tentativas. */
data class ProductionRequest(
    val operationId: String,
    val producedAt: Long,
    val items: List<ProductionQuantity>,
    val notes: String? = null,
) {
    fun validate() {
        require(operationId.isNotBlank()) { "Informe o identificador da operação." }
        // Keep user-entered dates and notes exportable by the versioned backup.
        require(producedAt in 0..253402300799999L) { "Informe uma data de produção entre 1970 e 9999." }
        require(notes == null || notes.length <= 16_384) { "As observações devem ter até 16.384 caracteres." }
        require(items.isNotEmpty()) { "Informe pelo menos um item de produção." }
        require(items.all { it.flavorId > 0 && it.quantity > 0 }) {
            "Informe sabores válidos e quantidades inteiras maiores que zero."
        }
        require(items.map { it.flavorId }.distinct().size == items.size) {
            "Cada sabor deve aparecer uma única vez na produção."
        }
    }
}

data class ProductionSession(val id: String, val producedAt: Long, val createdAt: Long, val notes: String?)

data class FlavorStock(val flavorId: Long, val name: String, val active: Boolean, val quantity: Long)

class FlavorHistoryException : IllegalStateException(
    "Este sabor possui histórico e não pode ser excluído. Desative o sabor quando o estoque estiver zerado.",
)
