package com.flstudios.a2lopes.presentation.sales

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.flstudios.a2lopes.domain.model.FlavorRules

@Composable
fun SalesScreen(state: SalesUiState, onIncrement: (Long) -> Unit, onDecrement: (Long) -> Unit,
    onRegister: () -> Unit, onRetry: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            // Chocolate & Cream Hero Header Banner
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "GelatoStock 🍦",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color(0xFFFFE0B2) // Creme Suave
                                )
                                Text(
                                    "Gerenciamento de vendas",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = Color(0xFFFFF8E1) // Creme Alvo
                                )
                                Text(
                                    "Registre e gerencie as vendas dos seus produtos",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFFFD180)
                                )
                            }
                        }
                    }
                }
            }

            state.message?.let {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Text(it, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            state.error?.let {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Text(it, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }

            if (state.busy) {
                LinearProgressIndicator(Modifier.fillMaxWidth().padding(bottom = 8.dp))
                Text("Registrando venda…", style = MaterialTheme.typography.bodyMedium)
            }

            Box(Modifier.weight(1f).fillMaxWidth()) {
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
                    state.products.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Nenhum sabor ativo com estoque disponível. Registre uma produção para vender.",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    else -> LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = (160 * LocalDensity.current.fontScale.coerceAtLeast(1f)).dp),
                        modifier = Modifier.fillMaxSize().testTag("sale-products"),
                        contentPadding = PaddingValues(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.products, key = { it.id }) { product ->
                            val quantity = state.cart[product.id]?.quantity ?: 0
                            val isSelected = quantity > 0
                            ElevatedCard(
                                onClick = { onIncrement(product.id) },
                                enabled = state.editable,
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("sale-product-${product.id}")
                            ) {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        product.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        product.unitPriceCents?.let { "R$ ${FlavorRules.priceText(it)}" } ?: "Preço não informado",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                                            Text("Disp: ${product.available}", modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                        }
                                        if (isSelected) {
                                            Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                                Text("Sel: $quantity", color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                        OutlinedButton(
                                            onClick = { onDecrement(product.id) },
                                            enabled = state.editable && quantity > 0,
                                            contentPadding = PaddingValues(0.dp),
                                            modifier = Modifier.weight(1f).heightIn(min = 40.dp)
                                                .semantics { contentDescription = "Diminuir ${product.name}" }
                                        ) { Text("−", style = MaterialTheme.typography.titleMedium) }
                                        Button(
                                            onClick = { onIncrement(product.id) },
                                            enabled = state.editable && quantity < product.available,
                                            contentPadding = PaddingValues(0.dp),
                                            modifier = Modifier.weight(1f).heightIn(min = 40.dp)
                                                .semantics { contentDescription = "Adicionar ${product.name}" }
                                        ) { Text("+", style = MaterialTheme.typography.titleMedium) }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (state.cart.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Text(
                        "🛒 Carrinho vazio. Toque em um sabor acima para adicionar.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Column(
                        Modifier.fillMaxWidth().heightIn(max = 200.dp).verticalScroll(rememberScrollState()).padding(12.dp).testTag("sale-cart"),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🛒 Carrinho de Vendas", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        state.cart.values.forEach { line ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${line.product.name}: ${line.quantity} un", style = MaterialTheme.typography.bodyMedium)
                                    if (!line.product.active || line.quantity > line.product.available) {
                                        Text(
                                            "Disponível agora: ${if (line.product.active) line.product.available else 0}. Reduza a quantidade.",
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    TextButton(
                                        onClick = { onDecrement(line.product.id) },
                                        enabled = state.editable,
                                        modifier = Modifier.size(36.dp).semantics { contentDescription = "Diminuir ${line.product.name} no carrinho" }
                                    ) { Text("−") }
                                    TextButton(
                                        onClick = { onIncrement(line.product.id) },
                                        enabled = state.editable && line.product.active && line.quantity < line.product.available,
                                        modifier = Modifier.size(36.dp).semantics { contentDescription = "Adicionar ${line.product.name} no carrinho" }
                                    ) { Text("+") }
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Total: ${state.totalUnits} unidades", style = MaterialTheme.typography.titleMedium)
                    val total = state.financialTotal
                    when {
                        total.error != null -> Text(total.error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                        total.cents != null -> Text("R$ ${FlavorRules.priceText(total.cents)}", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                        else -> Text("Valor indisponível", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Button(
                onClick = onRegister,
                enabled = state.canRegister,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).heightIn(min = 50.dp)
            ) {
                Text("Registrar venda", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
