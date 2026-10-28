package com.flstudios.a2lopes.presentation.flavors

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
    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 840.dp).fillMaxWidth().padding(horizontal = 16.dp)) {
                Text("Sabores", style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                Button(onClick = onNew, enabled = !state.loading && state.loadError == null && !state.busy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text("Novo sabor")
                }
                if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                when {
                    state.loading -> CircularProgressIndicator(Modifier.padding(24.dp))
                    state.loadError != null -> Column(Modifier.padding(vertical = 24.dp)) {
                        Text(state.loadError, color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = onRetry) { Text("Tentar novamente") }
                    }
                    state.flavors.isEmpty() -> Text(
                        "Nenhum sabor cadastrado. Toque em Novo sabor para começar.",
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentPadding = PaddingValues(vertical = 12.dp),
                    ) {
                        items(state.flavors, key = { it.id }) { flavor ->
                            Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                                Text(flavor.name, style = MaterialTheme.typography.titleLarge)
                                Text(if (flavor.active) "Ativo" else "Inativo")
                                Text(flavor.defaultPriceCents?.let { "Preço padrão: R$ ${FlavorRules.priceText(it)}" }
                                    ?: "Preço padrão não informado")
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(onClick = { onEdit(flavor) }, enabled = !state.busy,
                                        modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = "Editar sabor ${flavor.name}" }) { Text("Editar") }
                                    TextButton(onClick = { onSetActive(flavor) }, enabled = !state.busy,
                                        modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = "${if (flavor.active) "Desativar" else "Reativar"} sabor ${flavor.name}" }) {
                                        Text(if (flavor.active) "Desativar" else "Reativar")
                                    }
                                    TextButton(
                                        onClick = { onDelete(flavor) }, enabled = !state.busy,
                                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                        modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = "Excluir sabor ${flavor.name}" },
                                    ) { Text("Excluir") }
                                }
                            }
                            HorizontalDivider()
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
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = form.price, onValueChange = onPriceChange, enabled = !state.busy,
                        label = { Text("Preço padrão (opcional)") }, prefix = { Text("R$ ") },
                        singleLine = true, isError = form.priceError != null,
                        supportingText = { Text(form.priceError ?: "Ex.: 5,50. Deixe vazio se não houver preço.") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
