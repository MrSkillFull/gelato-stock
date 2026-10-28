package com.flstudios.a2lopes.domain.model

enum class CorrectionType { LOSS, ADJUSTMENT, SALE_REVERSAL, PRODUCTION_REVERSAL }

data class CorrectionRequest(
    val operationId: String,
    val type: CorrectionType,
    val reason: String,
    val flavorId: Long? = null,
    // Lost units for LOSS; physical count for ADJUSTMENT.
    val quantity: Long? = null,
    val sourceId: String? = null,
) {
    fun normalized(): CorrectionRequest {
        val input = copy(reason = FlavorNames.display(reason))
        require(input.operationId.isNotBlank()) { "Identificador da operação inválido." }
        require(input.reason.isNotEmpty()) { "Informe o motivo." }
        require(input.reason.length <= 16_384) { "O motivo deve ter até 16.384 caracteres." }
        when (type) {
            CorrectionType.LOSS, CorrectionType.ADJUSTMENT -> {
                require(flavorId != null && flavorId > 0 && sourceId == null) { "Selecione um sabor válido." }
                require(quantity != null && quantity >= if (type == CorrectionType.LOSS) 1 else 0) {
                    if (type == CorrectionType.LOSS) "Informe uma quantidade inteira maior que zero." else "Informe uma contagem inteira igual ou maior que zero."
                }
            }
            else -> require(!sourceId.isNullOrBlank() && flavorId == null && quantity == null) { "Selecione o registro a estornar." }
        }
        return input
    }
}

/** Business rejection is known not to have committed; other failures must retry the same request. */
class CorrectionRejected(message: String) : IllegalArgumentException(message)

data class CorrectionReceipt(val id: String, val createdAt: Long, val changedStock: Boolean)

data class CorrectionHistoryItem(
    val flavorName: String,
    val previousQuantity: Long,
    val resultingQuantity: Long,
    val quantityDelta: Long,
    val movementId: Long?,
)
