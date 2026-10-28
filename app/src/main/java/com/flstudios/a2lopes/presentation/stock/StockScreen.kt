package com.flstudios.a2lopes.presentation.stock

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.flstudios.a2lopes.domain.model.FlavorStock

@Composable
fun StockScreen(state: StockUiState, onRetry: () -> Unit,
    onLoss: (FlavorStock) -> Unit = {}, onCount: (FlavorStock) -> Unit = {}) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text("Estoque", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(vertical = 16.dp).semantics { heading() })
        when {
            state.loading -> CircularProgressIndicator(Modifier.padding(24.dp))
            state.error != null -> {
                Text(state.error, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = onRetry) { Text("Tentar novamente") }
            }
            state.flavors.isEmpty() -> Text("Nenhum sabor cadastrado. Cadastre um sabor na aba Sabores.")
            else -> {
                Text("Total disponível: ${state.total} unidades", style = MaterialTheme.typography.titleLarge)
                Text(if (state.lowStockThreshold == 0L) "Alertas de estoque baixo desativados (limite zero)."
                    else "Estoque baixo: menos de ${state.lowStockThreshold} unidades.", modifier = Modifier.padding(vertical = 8.dp))
                LazyColumn(Modifier.fillMaxWidth().testTag("stock-list"), contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.flavors, key = { it.flavorId }) { flavor ->
                        val low = flavor.quantity < state.lowStockThreshold
                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("stock-flavor-${flavor.flavorId}"),
                            colors = CardDefaults.cardColors(containerColor = when {
                                !flavor.active -> MaterialTheme.colorScheme.surfaceVariant
                                low -> MaterialTheme.colorScheme.errorContainer
                                else -> MaterialTheme.colorScheme.surfaceContainer
                            }),
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(flavor.name, style = MaterialTheme.typography.titleLarge)
                                if (!flavor.active) Text("Inativo")
                                Text("${flavor.quantity} unidades", style = MaterialTheme.typography.titleMedium)
                                if (low) Text("Estoque baixo")
                                TextButton(onClick = { onLoss(flavor) }, enabled = flavor.quantity > 0,
                                    modifier = Modifier.heightIn(min = 48.dp).testTag("stock-loss-${flavor.flavorId}").semantics { contentDescription = "Registrar perda de ${flavor.name}" }) { Text("Registrar perda") }
                                TextButton(onClick = { onCount(flavor) }, modifier = Modifier.heightIn(min = 48.dp).testTag("stock-count-${flavor.flavorId}").semantics { contentDescription = "Ajustar contagem de ${flavor.name}" }) {
                                    Text("Ajustar por contagem física")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
