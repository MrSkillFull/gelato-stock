package com.flstudios.a2lopes.presentation.corrections

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.flstudios.a2lopes.domain.model.CorrectionType

fun correctionTitle(type: CorrectionType): String = when (type) {
    CorrectionType.LOSS -> "Perda"
    CorrectionType.ADJUSTMENT -> "Ajuste por contagem"
    CorrectionType.SALE_REVERSAL -> "Estorno de venda"
    CorrectionType.PRODUCTION_REVERSAL -> "Estorno de produção"
}

@Composable
fun CorrectionForm(state: CorrectionUiState, onQuantity: (String) -> Unit, onReason: (String) -> Unit,
    onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val type = state.type ?: return
    val reversal = type == CorrectionType.SALE_REVERSAL || type == CorrectionType.PRODUCTION_REVERSAL
    AlertDialog(onDismissRequest = { if (state.editable) onDismiss() },
        title = { Text("${correctionTitle(type)} — ${state.targetName}") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (reversal) {
                    Text(if (type == CorrectionType.SALE_REVERSAL)
                        "Confirme o estorno integral: todas as unidades serão devolvidas ao estoque. A venda e os preços históricos serão preservados."
                        else "Confirme o estorno integral: todas as quantidades produzidas serão retiradas do saldo atual de cada sabor. Sem estoque suficiente, nenhum sabor será alterado.")
                } else {
                    Text(if (type == CorrectionType.LOSS) "Informe as unidades perdidas." else "Informe a quantidade física encontrada. O saldo será conferido no momento da gravação.")
                    OutlinedTextField(value = state.quantity, onValueChange = onQuantity, enabled = state.editable,
                        label = { Text(if (type == CorrectionType.LOSS) "Quantidade perdida" else "Quantidade contada") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true,
                        modifier = Modifier.fillMaxWidth())
                }
                OutlinedTextField(value = state.reason, onValueChange = onReason, enabled = state.editable,
                    label = { Text("Motivo obrigatório") }, modifier = Modifier.fillMaxWidth())
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (state.busy) { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("Registrando operação…") }
            }
        },
        confirmButton = { Button(onClick = onConfirm, enabled = !state.busy, modifier = Modifier.heightIn(min = 48.dp),
            colors = if (reversal || type == CorrectionType.LOSS) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError)
                else ButtonDefaults.buttonColors()) {
            Text(if (state.uncertain) "Repetir confirmação" else if (reversal) "Confirmar estorno" else "Registrar")
        } },
        dismissButton = { TextButton(onClick = onDismiss, enabled = state.editable) { Text("Cancelar") } },
    )
}
