package com.flstudios.a2lopes.presentation.stock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.flstudios.a2lopes.domain.model.FlavorStock

@Composable
fun StockScreen(state: StockUiState, onRetry: () -> Unit,
    onLoss: (FlavorStock) -> Unit = {}, onCount: (FlavorStock) -> Unit = {}) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            // Chocolate & Cream Hero Header Banner for Stock Screen
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
                            "Controle de Estoque 📊🍦",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFFFFE0B2) // Creme Suave
                        )
                        Text(
                            "Gestão de estoque",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color(0xFFFFF8E1) // Creme Alvo
                        )
                        Text(
                            "Acompanhe e gerencie o estoque dos seus produtos",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFFD180)
                        )
                    }
                }
            }

            when {
                state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.error != null -> Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(state.error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = onRetry) { Text("Tentar novamente") }
                }
                state.flavors.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Text("Nenhum sabor cadastrado. Cadastre um sabor na aba Sabores.", modifier = Modifier.padding(16.dp))
                    }
                }
                else -> {
                    ElevatedCard(
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Total Geral em Estoque", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                    Text("${state.total} un", color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                            Text("${state.total} unidades", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(
                                if (state.lowStockThreshold == 0L) "Alertas de estoque baixo desativados (limite zero)."
                                else "⚠️ Alerta de estoque baixo: menos de ${state.lowStockThreshold} unidades por sabor.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                        }
                    }

                    LazyColumn(
                        Modifier.fillMaxWidth().weight(1f).testTag("stock-list"),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.flavors, key = { it.flavorId }) { flavor ->
                            val low = flavor.quantity < state.lowStockThreshold
                            Card(
                                modifier = Modifier.fillMaxWidth().testTag("stock-flavor-${flavor.flavorId}"),
                                colors = CardDefaults.cardColors(containerColor = when {
                                    !flavor.active -> MaterialTheme.colorScheme.surfaceVariant
                                    low -> MaterialTheme.colorScheme.errorContainer
                                    else -> MaterialTheme.colorScheme.surface
                                }),
                            ) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(flavor.name, style = MaterialTheme.typography.titleLarge)
                                        if (!flavor.active) {
                                            Badge(containerColor = MaterialTheme.colorScheme.outline) {
                                                Text("Inativo", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                            }
                                        } else if (low) {
                                            Badge(containerColor = MaterialTheme.colorScheme.error) {
                                                Text("Estoque Baixo", color = MaterialTheme.colorScheme.onError, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                            }
                                        } else {
                                            Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                                                Text("OK", color = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                            }
                                        }
                                    }
                                    Text("${flavor.quantity} unidades disponíveis", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { onLoss(flavor) },
                                            enabled = flavor.quantity > 0,
                                            modifier = Modifier.weight(1f).heightIn(min = 40.dp).testTag("stock-loss-${flavor.flavorId}").semantics { contentDescription = "Registrar perda de ${flavor.name}" }
                                        ) { Text("Registrar perda") }
                                        Button(
                                            onClick = { onCount(flavor) },
                                            modifier = Modifier.weight(1f).heightIn(min = 40.dp).testTag("stock-count-${flavor.flavorId}").semantics { contentDescription = "Ajustar contagem de ${flavor.name}" }
                                        ) { Text("Ajustar contagem") }
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
