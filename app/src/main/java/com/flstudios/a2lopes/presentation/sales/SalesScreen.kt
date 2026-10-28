package com.flstudios.a2lopes.presentation.sales

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.flstudios.a2lopes.domain.model.FlavorRules

@Composable
fun SalesScreen(state: SalesUiState, onIncrement: (Long) -> Unit, onDecrement: (Long) -> Unit,
    onRegister: () -> Unit, onRetry: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text("Vendas", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(vertical = 12.dp))
        state.message?.let { Text(it, modifier = Modifier.padding(bottom = 8.dp)) }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp)) }
        if (state.busy) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Text("Registrando venda…")
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                state.loading -> CircularProgressIndicator(Modifier.padding(24.dp))
                state.loadError != null -> Column {
                    Text(state.loadError, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = onRetry) { Text("Tentar novamente") }
                }
                state.products.isEmpty() -> Text("Nenhum sabor ativo com estoque disponível. Registre uma produção para vender.")
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = (152 * LocalDensity.current.fontScale.coerceAtLeast(1f)).dp), modifier = Modifier.fillMaxSize().testTag("sale-products"),
                    contentPadding = PaddingValues(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.products, key = { it.id }) { product ->
                        val quantity = state.cart[product.id]?.quantity ?: 0
                        Card(onClick = { onIncrement(product.id) }, enabled = state.editable,
                            modifier = Modifier.fillMaxWidth().testTag("sale-product-${product.id}")) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(product.name, style = MaterialTheme.typography.titleLarge)
                                Text(product.unitPriceCents?.let { "R$ ${FlavorRules.priceText(it)}" } ?: "Preço não informado")
                                Text("Disponível: ${product.available}")
                                Text("Selecionado: $quantity")
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = { onDecrement(product.id) }, enabled = state.editable && quantity > 0,
                                        contentPadding = PaddingValues(0.dp), modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                            .semantics { contentDescription = "Diminuir ${product.name}" }) { Text("−") }
                                    Button(onClick = { onIncrement(product.id) }, enabled = state.editable && quantity < product.available,
                                        contentPadding = PaddingValues(0.dp), modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                            .semantics { contentDescription = "Adicionar ${product.name}" }) { Text("+") }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (state.cart.isEmpty()) {
            Text("Carrinho vazio. Toque em um sabor para adicionar.", modifier = Modifier.padding(vertical = 8.dp))
        } else {
            Column(Modifier.fillMaxWidth().heightIn(max = 220.dp).verticalScroll(rememberScrollState()).testTag("sale-cart"),
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Carrinho", style = MaterialTheme.typography.titleLarge)
                state.cart.values.forEach { line ->
                    Text("${line.product.name}: ${line.quantity} unidades")
                    if (!line.product.active || line.quantity > line.product.available) {
                        Text("Disponível agora: ${if (line.product.active) line.product.available else 0}. Reduza a quantidade ou remova este item.", color = MaterialTheme.colorScheme.error)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { onDecrement(line.product.id) }, enabled = state.editable,
                            modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = "Diminuir ${line.product.name} no carrinho" }) { Text("−") }
                        TextButton(onClick = { onIncrement(line.product.id) }, enabled = state.editable && line.product.active && line.quantity < line.product.available,
                            modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = "Adicionar ${line.product.name} no carrinho" }) { Text("+") }
                    }
                }
            }
            Text("Total: ${state.totalUnits} unidades", style = MaterialTheme.typography.titleMedium)
            val total = state.financialTotal
            when {
                total.error != null -> Text(total.error, color = MaterialTheme.colorScheme.error)
                total.cents != null -> Text("Valor total: R$ ${FlavorRules.priceText(total.cents)}")
                else -> Text("Valor total indisponível: há itens sem preço cadastrado.")
            }
        }
        Button(onClick = onRegister, enabled = state.canRegister,
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).heightIn(min = 48.dp)) { Text("Registrar venda") }
    }
}
