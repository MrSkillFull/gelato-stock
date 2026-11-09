package com.flstudios.a2lopes.presentation.flavors

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.*
import androidx.compose.ui.window.DialogProperties
import com.flstudios.a2lopes.domain.model.Flavor
import com.flstudios.a2lopes.domain.model.FlavorRules

@Composable
fun FlavorsScreen(
    state: FlavorsUiState,
    onDelete: (Flavor) -> Unit,
    onConfirmDelete: () -> Unit,
    onCancelDelete: () -> Unit,
    onNew: () -> Unit,
    onEdit: (Flavor) -> Unit,
    onSetActive: (Flavor) -> Unit,
    onNameChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onMessageShown: () -> Unit,
) {
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.message) {
        state.message?.let { snackbar.showSnackbar(it); onMessageShown() }
    }
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbar) },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            Column(
                Modifier.fillMaxSize().padding(padding).imePadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(Modifier.widthIn(max = 840.dp).fillMaxWidth().padding(horizontal = 16.dp)) {
                    // Chocolate & Cream Hero Header Banner for Flavors Screen
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 12.dp),
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
                                    "Catálogo de Sabores 🍧🍦",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color(0xFFFFE0B2) // Creme Suave
                                )
                                Text(
                                    "Gerenciamento de sabores",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = Color(0xFFFFF8E1) // Creme Alvo
                                )
                                Text(
                                    "Organize e gerencie o catálogo de sabores do aplicativo",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFFFD180)
                                )
                            }
                        }
                    }

                    Button(onClick = onNew, enabled = !state.loading && state.loadError == null && !state.busy,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).padding(bottom = 8.dp)) {
                        Text("➕ Novo sabor", style = MaterialTheme.typography.titleMedium)
                    }
                    if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth().padding(bottom = 8.dp))
                    when {
                        state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                        state.loadError != null -> Column(Modifier.padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(state.loadError, color = MaterialTheme.colorScheme.error)
                            TextButton(onClick = onRetry) { Text("Tentar novamente") }
                        }
                        state.flavors.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                Text(
                                    "Nenhum sabor cadastrado. Toque em Novo sabor para começar.",
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                        else -> LazyColumn(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            contentPadding = PaddingValues(vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.flavors, key = { it.id }) { flavor ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (flavor.active) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                    )
                                ) {
                                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(flavor.name, style = MaterialTheme.typography.titleLarge)
                                            Badge(containerColor = if (flavor.active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.outline) {
                                                Text(if (flavor.active) "Ativo" else "Inativo", color = if (flavor.active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onError, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                            }
                                        }
                                        Text(
                                            flavor.defaultPriceCents?.let { "Preço padrão: R$ ${FlavorRules.priceText(it)}" }
                                                ?: "Preço padrão não informado",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                            OutlinedButton(onClick = { onEdit(flavor) }, enabled = !state.busy,
                                                modifier = Modifier.weight(1f).heightIn(min = 40.dp).semantics { contentDescription = "Editar sabor ${flavor.name}" }) { Text("Editar") }
                                            OutlinedButton(onClick = { onSetActive(flavor) }, enabled = !state.busy,
                                                modifier = Modifier.weight(1f).heightIn(min = 40.dp).semantics { contentDescription = "${if (flavor.active) "Desativar" else "Reativar"} sabor ${flavor.name}" }) {
                                                Text(if (flavor.active) "Desativar" else "Reativar")
                                            }
                                            TextButton(
                                                onClick = { onDelete(flavor) }, enabled = !state.busy,
                                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                                modifier = Modifier.heightIn(min = 40.dp).semantics { contentDescription = "Excluir sabor ${flavor.name}" },
                                            ) { Text("Excluir") }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    state.pendingDeletion?.let { flavor ->
        AlertDialog(
            onDismissRequest = onCancelDelete,
            properties = DialogProperties(dismissOnBackPress = !state.busy, dismissOnClickOutside = !state.busy),
            title = { Text("Excluir sabor?") },
            text = { Text("Excluir definitivamente o sabor “${flavor.name}”? Esta operação é irreversível.") },
            confirmButton = {
                TextButton(
                    onClick = onConfirmDelete, enabled = !state.busy,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Excluir definitivamente") }
            },
            dismissButton = {
                TextButton(onClick = onCancelDelete, enabled = !state.busy) { Text("Cancelar") }
            },
        )
    }
    state.form?.let { form ->
        AlertDialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(dismissOnBackPress = !state.busy, dismissOnClickOutside = !state.busy),
            title = { Text(if (form.id == null) "Novo sabor" else "Editar sabor") },
            text = {
                Column(
                    Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedTextField(
                        value = form.name, onValueChange = onNameChange, enabled = !state.busy,
                        label = { Text("Nome") }, singleLine = true, isError = form.nameError != null,
                        supportingText = { form.nameError?.let { Text(it) } },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = form.price, onValueChange = onPriceChange, enabled = !state.busy,
                        label = { Text("Preço padrão (opcional)") }, prefix = { Text("R$ ") },
                        singleLine = true, isError = form.priceError != null,
                        supportingText = { Text(form.priceError ?: "Ex.: 5,50. Deixe vazio se não houver preço.") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    form.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = { TextButton(onClick = onSave, enabled = !state.busy) {
                Text(if (state.busy) "Salvando…" else "Salvar")
            } },
            dismissButton = { TextButton(onClick = onDismiss, enabled = !state.busy) { Text("Cancelar") } },
        )
    }
}
