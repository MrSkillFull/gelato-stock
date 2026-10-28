package com.flstudios.a2lopes.presentation.production

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductionScreen(
    state: ProductionUiState,
    onQuantityChange: (Long, String) -> Unit,
    onNotesChange: (String) -> Unit,
    onSelectDay: (Long) -> Unit,
    onReview: () -> Unit,
    onBackToEdit: () -> Unit,
    onConfirm: () -> Unit,
    onRetry: () -> Unit,
) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    BackHandler(enabled = state.review != null) { onBackToEdit() }

    Column(Modifier.fillMaxSize().imePadding().padding(horizontal = 16.dp)) {
        Text("Produção", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(vertical = 16.dp))
        val review = state.review
        if (review != null) {
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Revisar produção", style = MaterialTheme.typography.titleLarge)
                Text("Data da produção: ${ProductionDates.text(review.request.producedAt)}")
                review.request.items.forEach { item ->
                    Text("${review.names[item.flavorId]}: ${item.quantity} unidades", style = MaterialTheme.typography.titleMedium)
                }
                Text("Total: ${review.total} unidades", style = MaterialTheme.typography.titleLarge)
                review.request.notes?.let { Text("Observações: $it") }
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (state.busy) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text("Registrando produção…")
                }
            }
            Button(onClick = onConfirm, enabled = !state.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text("Confirmar produção")
            }
            TextButton(onClick = onBackToEdit, enabled = !state.busy && !state.outcomeUncertain,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).heightIn(min = 48.dp)) {
                Text("Voltar e editar")
            }
        } else {
            state.message?.let { Text(it, modifier = Modifier.padding(bottom = 12.dp)) }
            when {
                state.loading -> CircularProgressIndicator(Modifier.padding(24.dp))
                state.loadError != null -> {
                    Text(state.loadError, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = onRetry) { Text("Tentar novamente") }
                }
                state.flavors.isEmpty() -> Text("Nenhum sabor ativo. Cadastre ou reative um sabor na aba Sabores.")
                else -> {
                    LazyColumn(Modifier.weight(1f).fillMaxWidth().testTag("production-form"), verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)) {
                        item {
                            Text("Informe somente unidades prontas para venda.")
                            OutlinedButton(onClick = { focus.clearFocus(); showDatePicker = true },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                                Text("Data da produção: ${ProductionDates.text(state.producedAt)}")
                            }
                            Text("Deixe vazios os sabores que não foram produzidos.")
                        }
                        items(state.flavors, key = { it.id }) { flavor ->
                            OutlinedTextField(
                                value = state.quantities[flavor.id].orEmpty(),
                                onValueChange = { onQuantityChange(flavor.id, it) },
                                label = { Text("Quantidade de ${flavor.name}") },
                                singleLine = true,
                                isError = state.quantityErrors[flavor.id] != null,
                                supportingText = { state.quantityErrors[flavor.id]?.let { Text(it) } },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        item {
                            OutlinedTextField(value = state.notes, onValueChange = onNotesChange,
                                label = { Text("Observações (opcional)") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                        }
                    }
                    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 8.dp)) }
                    Button(onClick = { focus.clearFocus(); onReview() }, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).heightIn(min = 48.dp)) {
                        Text("Revisar produção")
                    }
                }
            }
        }
    }
    if (showDatePicker && state.review == null) {
        val dateState = remember {
            DatePickerState(locale = java.util.Locale.forLanguageTag("pt-BR"),
                initialSelectedDateMillis = ProductionDates.pickerDay(state.producedAt))
        }
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = {
                dateState.selectedDateMillis?.let(onSelectDay)
                showDatePicker = false
            }, enabled = dateState.selectedDateMillis != null) { Text("Usar data") } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") } },
        ) {
            DatePicker(state = dateState, title = { Text("Data da produção", modifier = Modifier.padding(16.dp)) },
                headline = { Text(dateState.selectedDateMillis?.let {
                    ProductionDates.text(it, java.util.TimeZone.getTimeZone("UTC"))
                } ?: "Selecione uma data", modifier = Modifier.padding(horizontal = 16.dp)) },
                showModeToggle = false)
        }
    }
}
