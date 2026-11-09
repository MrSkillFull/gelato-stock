package com.flstudios.a2lopes.presentation.production

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().imePadding().padding(horizontal = 16.dp)) {
            // Chocolate & Cream Hero Header Banner for Production
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp),
                shape = MaterialTheme.shapes.medium,
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF3E2723), // Cobertura de Chocolate Amargo
                                    Color(0xFF5D4037), // Chocolate ao Leite
                                    Color(0xFF8D6E63), // Creme Aveludado
                                )
                            )
                        )
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            "Produção de Sacolés 🍓🍫",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFFFFE0B2) // Creme Suave
                        )
                        Text(
                            "Gerenciamento de produção",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color(0xFFFFF8E1) // Creme Alvo
                        )
                        Text(
                            "Organize e registre a produção dos seus produtos",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFFD180)
                        )
                    }
                }
            }

            val review = state.review
            if (review != null) {
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ElevatedCard(
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Revisar Produção", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("Data: ${ProductionDates.text(review.request.producedAt)}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Itens Produzidos", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            HorizontalDivider()
                            review.request.items.forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(review.names[item.flavorId] ?: "Sabor", style = MaterialTheme.typography.bodyLarge)
                                    Text("${item.quantity} un", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            HorizontalDivider()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total Geral", style = MaterialTheme.typography.titleMedium)
                                Text("${review.total} unidades", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    review.request.notes?.let { notes ->
                        OutlinedCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Observações", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)
                                Text(notes, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }

                    state.error?.let {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                            Text(it, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                    if (state.busy) {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                        Text("Registrando produção…", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Button(onClick = onConfirm, enabled = !state.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp)) {
                    Text("Confirmar produção", style = MaterialTheme.typography.titleMedium)
                }
                TextButton(onClick = onBackToEdit, enabled = !state.busy && !state.outcomeUncertain,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).heightIn(min = 48.dp)) {
                    Text("Voltar e editar")
                }
            } else {
                state.message?.let {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                        Text(it, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                when {
                    state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    state.loadError != null -> Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(state.loadError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = onRetry) { Text("Tentar novamente") }
                    }
                    state.flavors.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Text("Nenhum sabor ativo. Cadastre ou reative um sabor na aba Sabores.", modifier = Modifier.padding(16.dp))
                        }
                    }
                    else -> {
                        LazyColumn(Modifier.weight(1f).fillMaxWidth().testTag("production-form"), verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("Informe somente unidades prontas para venda. Deixe vazios os sabores não produzidos.", style = MaterialTheme.typography.bodyMedium)
                                        OutlinedButton(onClick = { focus.clearFocus(); showDatePicker = true },
                                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                                            Text("📅 Data da produção: ${ProductionDates.text(state.producedAt)}")
                                        }
                                    }
                                }
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
                                    shape = MaterialTheme.shapes.medium,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            item {
                                OutlinedTextField(
                                    value = state.notes,
                                    onValueChange = onNotesChange,
                                    label = { Text("Observações (opcional)") },
                                    minLines = 2,
                                    shape = MaterialTheme.shapes.medium,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 8.dp)) }
                        Button(onClick = { focus.clearFocus(); onReview() }, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).heightIn(min = 50.dp)) {
                            Text("Revisar produção", style = MaterialTheme.typography.titleMedium)
                        }
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
